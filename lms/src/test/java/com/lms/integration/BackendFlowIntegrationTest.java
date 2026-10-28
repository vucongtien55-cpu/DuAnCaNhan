package com.lms.integration;

import com.lms.dto.assignment.AssignmentAttachment;
import com.lms.dto.assignment.AssignmentResponse;
import com.lms.dto.assignment.CreateAssignmentRequest;
import com.lms.dto.course.CreateCourseRequest;
import com.lms.dto.course.UpdateCourseRequest;
import com.lms.dto.discussion.CreateDiscussionRequest;
import com.lms.dto.discussion.ReplyRequest;
import com.lms.dto.enrollment.EnrollmentResponse;
import com.lms.dto.grade.FinalGradeResponse;
import com.lms.dto.grade.FinalGradeStatus;
import com.lms.dto.grade.GradeResponse;
import com.lms.dto.grade.GradeSubmissionRequest;
import com.lms.dto.grade.GradeWeightRequest;
import com.lms.dto.grade.StudentFinalGradeResponse;
import com.lms.dto.grade.StudentGradeResponse;
import com.lms.dto.material.MaterialFile;
import com.lms.dto.material.MaterialResponse;
import com.lms.dto.material.UploadMaterialRequest;
import com.lms.dto.notification.AnnouncementRequest;
import com.lms.dto.notification.NotificationContent;
import com.lms.dto.notification.NotificationResponse;
import com.lms.dto.quiz.CreateQuizRequest;
import com.lms.dto.quiz.QuizAttemptResponse;
import com.lms.dto.quiz.QuizOptionRequest;
import com.lms.dto.quiz.QuizQuestionRequest;
import com.lms.dto.quiz.QuizResponse;
import com.lms.dto.quiz.QuizResultResponse;
import com.lms.dto.quiz.SaveAnswerRequest;
import com.lms.dto.section.CreateSectionRequest;
import com.lms.dto.section.SectionResponse;
import com.lms.dto.section.UpdateSectionRequest;
import com.lms.dto.submission.SubmissionResponse;
import com.lms.dto.submission.SubmitRequest;
import com.lms.dto.user.CreateUserRequest;
import com.lms.dto.user.UserResponse;
import com.lms.entity.User;
import com.lms.entity.enums.CourseStatus;
import com.lms.entity.enums.GradeType;
import com.lms.entity.enums.GradingPolicy;
import com.lms.entity.enums.NotificationType;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.repository.UserRepository;
import com.lms.service.AssignmentAttachmentService;
import com.lms.service.AssignmentService;
import com.lms.service.CourseSectionService;
import com.lms.service.CourseService;
import com.lms.service.DiscussionService;
import com.lms.service.EnrollmentService;
import com.lms.service.FinalGradeService;
import com.lms.service.GradeService;
import com.lms.service.MaterialService;
import com.lms.service.NotificationService;
import com.lms.service.QuizAttemptService;
import com.lms.service.QuizService;
import com.lms.service.SubmissionService;
import com.lms.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chạy các nghiệp vụ chính trên PostgreSQL thật qua các bean Spring (proxy, validation, giao dịch, khóa dòng, truy vấn).
 * Toàn bộ dữ liệu nằm trong một giao dịch được hoàn tác khi kết thúc test nên không để lại gì trong database.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {"app.storage.upload-dir=build/integration-uploads",
        "app.admin.username=", "app.admin.password="})
class BackendFlowIntegrationTest {

    private static final byte[] PDF = "%PDF-1.7 integration test".getBytes(StandardCharsets.UTF_8);

    @Autowired UserService userService;
    @Autowired CourseService courseService;
    @Autowired CourseSectionService sectionService;
    @Autowired EnrollmentService enrollmentService;
    @Autowired MaterialService materialService;
    @Autowired AssignmentService assignmentService;
    @Autowired AssignmentAttachmentService attachmentService;
    @Autowired SubmissionService submissionService;
    @Autowired GradeService gradeService;
    @Autowired QuizService quizService;
    @Autowired QuizAttemptService quizAttemptService;
    @Autowired FinalGradeService finalGradeService;
    @Autowired DiscussionService discussionService;
    @Autowired NotificationService notificationService;
    @Autowired UserRepository userRepository;
    @PersistenceContext EntityManager em;

    /** Ghi xuống database (kiểm tra ràng buộc) và xóa cache để lần đọc sau đi qua truy vấn và nạp lazy thật. */
    private void sync() {
        em.flush();
        em.clear();
    }

    private Long newUser(String username, RoleName role) {
        UserResponse response = userService.createUser(new CreateUserRequest(username, username + "@example.com",
                "Password123", "Nguoi dung " + username, null, Set.of(role)));
        return response.id();
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "text/html", PDF);
    }

    @Test
    @DisplayName("Luồng đầy đủ: tài khoản -> khóa học -> lớp -> đăng ký -> tài liệu -> bài tập -> nộp -> chấm -> công bố "
            + "-> quiz -> điểm tổng kết -> thảo luận -> thông báo")
    void fullFlow() throws IOException {
        // ===== Tài khoản =====
        Long adminId = newUser("it_admin", RoleName.ADMIN);
        Long lecturerId = newUser("it_lecturer", RoleName.LECTURER);
        Long studentId = newUser("it_student1", RoleName.STUDENT);
        Long student2Id = newUser("it_student2", RoleName.STUDENT);
        Long outsiderId = newUser("it_outsider", RoleName.STUDENT);
        sync();

        UserResponse admin = userService.getById(adminId);
        assertThat(admin.roles()).containsExactly(RoleName.ADMIN);
        assertThat(admin.toString()).doesNotContain("Password123");
        User stored = userRepository.findById(adminId).orElseThrow();
        assertThat(stored.getPassword()).startsWith("$2").isNotEqualTo("Password123");

        assertThatThrownBy(() -> userService.createUser(new CreateUserRequest("it_admin", "other@example.com",
                "Password123", "Trùng tên", null, Set.of(RoleName.STUDENT))))
                .isInstanceOf(BusinessException.class);
        // Kiểm tra đầu vào qua proxy @Validated
        assertThatThrownBy(() -> userService.createUser(new CreateUserRequest("x", "khong-phai-email", "ngan", "",
                null, Set.of())))
                .isInstanceOf(ConstraintViolationException.class);

        // ===== Khóa học và lớp học phần =====
        Long courseId = courseService.create(adminId, new CreateCourseRequest("it101", "Lap trinh Java", "Mo ta", 3,
                null)).id();
        assertThatThrownBy(() -> courseService.create(lecturerId, new CreateCourseRequest("it999", "Khong duoc", null,
                3, null))).isInstanceOf(ForbiddenException.class);
        courseService.update(adminId, courseId, new UpdateCourseRequest("IT101", "Lap trinh Java", "Mo ta", 3, null,
                CourseStatus.ACTIVE));
        sync();

        SectionResponse section = sectionService.create(adminId, new CreateSectionRequest(courseId, null, "Lop 1",
                "HK1", null, null, 1, "T2", lecturerId));
        Long sectionId = section.id();
        assertThat(section.sectionCode()).startsWith("IT101-");
        assertThat(section.status()).isEqualTo(SectionStatus.DRAFT);
        sectionService.changeStatus(adminId, sectionId, SectionStatus.OPEN_REGISTRATION);
        sync();

        // ===== Đăng ký lớp: trùng, sức chứa, đóng đăng ký =====
        EnrollmentResponse enrollment = enrollmentService.enroll(studentId, section.sectionCode());
        assertThat(enrollment.status().name()).isEqualTo("ENROLLED");
        sync();
        assertThatThrownBy(() -> enrollmentService.enroll(studentId, section.sectionCode()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> enrollmentService.enroll(student2Id, section.sectionCode()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("sĩ số");
        sectionService.update(adminId, sectionId, new UpdateSectionRequest("Lop 1", "HK1", null, null, 5, "T2"));
        enrollmentService.enroll(student2Id, section.sectionCode());
        sync();
        // Không giảm sức chứa thấp hơn số sinh viên đang học (2)
        assertThatThrownBy(() -> sectionService.update(adminId, sectionId,
                new UpdateSectionRequest("Lop 1", "HK1", null, null, 1, "T2")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Sức chứa");
        sectionService.changeStatus(adminId, sectionId, SectionStatus.IN_PROGRESS);
        sync();
        assertThatThrownBy(() -> enrollmentService.enroll(outsiderId, section.sectionCode()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("không mở đăng ký");

        // ===== Tài liệu: lưu file thật, phân quyền, tải xuống =====
        MaterialResponse material = materialService.upload(lecturerId, sectionId,
                new UploadMaterialRequest("Slide tuan 1", "Mo ta"), pdf("../../slide.pdf"));
        sync();
        assertThat(material.originalFileName()).isEqualTo("slide.pdf");
        assertThat(material.contentType()).isEqualTo("application/pdf");
        assertThat(materialService.listBySection(studentId, sectionId)).hasSize(1);
        MaterialFile file = materialService.download(studentId, material.id());
        assertThat(file.resource().getContentAsByteArray()).isEqualTo(PDF);
        assertThatThrownBy(() -> materialService.download(outsiderId, material.id()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> materialService.upload(studentId, sectionId, new UploadMaterialRequest("X", null),
                pdf("a.pdf"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> materialService.upload(lecturerId, sectionId, new UploadMaterialRequest("X", null),
                new MockMultipartFile("file", "virus.exe", "application/pdf", PDF)))
                .isInstanceOf(BusinessException.class);

        // ===== Bài tập, file đính kèm, nộp bài nhiều lần =====
        AssignmentResponse assignment = assignmentService.create(lecturerId, new CreateAssignmentRequest(sectionId,
                "Bai tap 1", "Mo ta", LocalDateTime.now().plusDays(1), 10.0, 2, true, GradingPolicy.HIGHEST));
        Long assignmentId = assignment.id();
        attachmentService.upload(lecturerId, assignmentId, pdf("de-bai.pdf"));
        sync();
        AssignmentAttachment attachment = attachmentService.download(studentId, assignmentId);
        assertThat(attachment.resource().getContentAsByteArray()).isEqualTo(PDF);
        assertThat(assignmentService.getById(studentId, assignmentId).attachmentFileName()).isEqualTo("de-bai.pdf");

        SubmissionResponse first = submissionService.submit(studentId, assignmentId, new SubmitRequest("lan 1"),
                pdf("bai-lam.pdf"));
        SubmissionResponse second = submissionService.submit(studentId, assignmentId, new SubmitRequest("lan 2"),
                pdf("bai-lam.pdf"));
        sync();
        assertThat(first.attemptNumber()).isEqualTo(1);
        assertThat(second.attemptNumber()).isEqualTo(2);
        assertThat(first.late()).isFalse();
        assertThatThrownBy(() -> submissionService.submit(studentId, assignmentId, null, pdf("bai-lam.pdf")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("lần nộp");
        assertThatThrownBy(() -> submissionService.submit(outsiderId, assignmentId, null, pdf("bai-lam.pdf")))
                .isInstanceOf(ForbiddenException.class);
        assertThat(submissionService.listHistory(studentId, assignmentId, studentId)).hasSize(2);
        assertThatThrownBy(() -> submissionService.getById(student2Id, first.id()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> assignmentService.delete(lecturerId, assignmentId))
                .isInstanceOf(BusinessException.class);

        // ===== Chấm điểm: đã chấm khác đã công bố =====
        gradeService.gradeSubmission(lecturerId, first.id(), new GradeSubmissionRequest(7.0, "Kha", "NOI-BO-1"));
        GradeResponse bestGrade = gradeService.gradeSubmission(lecturerId, second.id(),
                new GradeSubmissionRequest(9.0, "Tot", "NOI-BO-2"));
        sync();
        assertThat(bestGrade.published()).isFalse();
        assertThatThrownBy(() -> gradeService.gradeSubmission(lecturerId, second.id(),
                new GradeSubmissionRequest(11.0, null, null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> gradeService.gradeSubmission(studentId, second.id(),
                new GradeSubmissionRequest(10.0, null, null))).isInstanceOf(ForbiddenException.class);

        StudentGradeResponse hidden = gradeService.getMyGrade(studentId, assignmentId);
        assertThat(hidden.published()).isFalse();
        assertThat(hidden.score()).isNull();

        assertThat(gradeService.publishAssignmentGrades(lecturerId, assignmentId)).isEqualTo(2);
        sync();
        StudentGradeResponse visible = gradeService.getMyGrade(studentId, assignmentId);
        assertThat(visible.published()).isTrue();
        assertThat(visible.score()).isEqualTo(9.0);
        assertThat(visible.feedback()).isEqualTo("Tot");
        assertThat(visible.toString()).doesNotContain("NOI-BO");
        assertThat(gradeService.getMyGrade(student2Id, assignmentId).published()).isFalse();
        assertThatThrownBy(() -> gradeService.gradeSubmission(lecturerId, second.id(),
                new GradeSubmissionRequest(10.0, null, null))).isInstanceOf(BusinessException.class);

        // ===== Quiz: làm bài, tiếp tục, chấm tự động, không lộ đáp án =====
        QuizResponse quiz = quizService.create(lecturerId, new CreateQuizRequest(sectionId, "Kiem tra 1", null, 30,
                null, null, null, false, List.of(
                new QuizQuestionRequest("2 + 2 = ?", 2.0, "Vi 2 + 2 = 4", List.of(
                        new QuizOptionRequest("4", true), new QuizOptionRequest("5", false),
                        new QuizOptionRequest("6", false))),
                new QuizQuestionRequest("Thu do Viet Nam?", 2.0, "Ha Noi", List.of(
                        new QuizOptionRequest("Ha Noi", true), new QuizOptionRequest("Hue", false))))));
        Long quizId = quiz.id();
        sync();
        quizService.publish(lecturerId, quizId);
        sync();

        Map<Long, Long> correctOption = new HashMap<>();
        Map<Long, Long> wrongOption = new HashMap<>();
        for (QuizResponse.QuestionDetail question : quizService.getForManagement(lecturerId, quizId).questions()) {
            for (QuizResponse.OptionDetail option : question.options()) {
                (option.correct() ? correctOption : wrongOption).putIfAbsent(question.id(), option.id());
            }
        }
        assertThatThrownBy(() -> quizService.getForManagement(studentId, quizId)).isInstanceOf(ForbiddenException.class);

        QuizAttemptResponse attempt = quizAttemptService.startAttempt(studentId, quizId);
        assertThat(attempt.questions()).hasSize(2);
        assertThat(attempt.toString()).doesNotContain("Vi 2 + 2 = 4").doesNotContain("correct");
        assertThat(quizAttemptService.startAttempt(studentId, quizId).attemptId()).isEqualTo(attempt.attemptId());

        List<Long> questionIds = attempt.questions().stream().map(QuizAttemptResponse.QuestionView::id).toList();
        quizAttemptService.saveAnswer(studentId, attempt.attemptId(),
                new SaveAnswerRequest(questionIds.get(0), correctOption.get(questionIds.get(0))));
        quizAttemptService.saveAnswer(studentId, attempt.attemptId(),
                new SaveAnswerRequest(questionIds.get(1), wrongOption.get(questionIds.get(1))));
        quizAttemptService.saveAnswer(studentId, attempt.attemptId(),
                new SaveAnswerRequest(questionIds.get(1), correctOption.get(questionIds.get(1))));
        sync();
        assertThat(quizAttemptService.getAttempt(studentId, attempt.attemptId()).savedAnswers()).hasSize(2);
        assertThatThrownBy(() -> quizAttemptService.getAttempt(student2Id, attempt.attemptId()))
                .isInstanceOf(ForbiddenException.class);

        QuizResultResponse result = quizAttemptService.submit(studentId, attempt.attemptId());
        sync();
        assertThat(result.gradePublished()).isFalse();
        assertThat(result.score()).isNull();
        assertThatThrownBy(() -> quizAttemptService.startAttempt(studentId, quizId))
                .isInstanceOf(BusinessException.class);
        assertThat(quizAttemptService.listAttempts(lecturerId, quizId)).singleElement()
                .satisfies(a -> assertThat(a.score()).isEqualTo(4.0));
        assertThat(quizAttemptService.publishQuizGrades(lecturerId, quizId)).isEqualTo(1);
        sync();
        QuizResultResponse published = quizAttemptService.getMyResult(studentId, attempt.attemptId());
        assertThat(published.gradePublished()).isTrue();
        assertThat(published.score()).isEqualTo(4.0);
        assertThat(published.maxScore()).isEqualTo(4.0);

        // Lượt dở dang vẫn còn thời gian: tác vụ tự chốt không đụng tới
        QuizAttemptResponse open = quizAttemptService.startAttempt(student2Id, quizId);
        assertThat(quizAttemptService.autoSubmitExpiredAttempts()).isGreaterThanOrEqualTo(0);
        assertThat(quizAttemptService.getAttempt(student2Id, open.attemptId()).remainingSeconds()).isPositive();

        // ===== Điểm tổng kết theo trọng số =====
        assertThatThrownBy(() -> finalGradeService.setWeights(lecturerId, sectionId, List.of(
                new GradeWeightRequest(GradeType.ASSIGNMENT, new BigDecimal("60")),
                new GradeWeightRequest(GradeType.QUIZ, new BigDecimal("30")))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("100");
        finalGradeService.setWeights(lecturerId, sectionId, List.of(
                new GradeWeightRequest(GradeType.ASSIGNMENT, new BigDecimal("60")),
                new GradeWeightRequest(GradeType.QUIZ, new BigDecimal("40"))));
        sync();
        List<FinalGradeResponse> finals = finalGradeService.calculate(lecturerId, sectionId);
        sync();
        FinalGradeResponse mine = finals.stream().filter(f -> f.studentId().equals(studentId)).findFirst().orElseThrow();
        FinalGradeResponse theirs = finals.stream().filter(f -> f.studentId().equals(student2Id)).findFirst().orElseThrow();
        assertThat(mine.status()).isEqualTo(FinalGradeStatus.COMPUTED);
        // bài tập 9/10 = 90% x 60 + quiz 4/4 = 100% x 40 -> 94% -> 9.40
        assertThat(mine.score()).isEqualTo(9.4);
        assertThat(theirs.status()).isEqualTo(FinalGradeStatus.INCOMPLETE);
        assertThat(theirs.score()).isNull();
        assertThat(finalGradeService.getMyFinalGrade(studentId, sectionId).published()).isFalse();
        assertThat(finalGradeService.publishFinalGrades(lecturerId, sectionId)).isEqualTo(1);
        sync();
        StudentFinalGradeResponse finalGrade = finalGradeService.getMyFinalGrade(studentId, sectionId);
        assertThat(finalGrade.published()).isTrue();
        assertThat(finalGrade.score()).isEqualTo(9.4);
        assertThat(finalGradeService.getMyFinalGrade(student2Id, sectionId).published()).isFalse();
        assertThatThrownBy(() -> finalGradeService.getMyFinalGrade(outsiderId, sectionId))
                .isInstanceOf(ForbiddenException.class);
        // Tính lại sau khi công bố: giữ nguyên
        assertThat(finalGradeService.calculate(lecturerId, sectionId).stream()
                .filter(f -> f.studentId().equals(studentId)).findFirst().orElseThrow().status())
                .isEqualTo(FinalGradeStatus.LOCKED);
        assertThat(gradeService.listByAssignment(lecturerId, assignmentId))
                .extracting(GradeResponse::type).containsOnly(GradeType.ASSIGNMENT);

        // ===== Thảo luận =====
        Long threadId = discussionService.create(studentId, new CreateDiscussionRequest(sectionId, "Hoi bai 1",
                "Em chua hieu cau 2")).id();
        discussionService.reply(lecturerId, threadId, new ReplyRequest("Em xem lai slide", null));
        sync();
        assertThat(discussionService.get(studentId, threadId).replies()).hasSize(1);
        assertThat(discussionService.listBySection(student2Id, sectionId)).singleElement()
                .satisfies(d -> assertThat(d.replyCount()).isEqualTo(1));
        assertThatThrownBy(() -> discussionService.get(outsiderId, threadId)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> discussionService.delete(studentId, threadId)).isInstanceOf(BusinessException.class);

        // ===== Thông báo =====
        assertThat(notificationService.announceToSection(lecturerId, sectionId,
                new AnnouncementRequest("Nghi hoc", "Lop nghi thu Hai"))).isEqualTo(2);
        sync();
        assertThatThrownBy(() -> notificationService.announceToSection(studentId, sectionId,
                new AnnouncementRequest("Gia mao", "x"))).isInstanceOf(ForbiddenException.class);
        List<NotificationResponse> inbox = notificationService.listMyNotifications(studentId, false, PageRequest.of(0, 10))
                .getContent();
        assertThat(inbox).singleElement().satisfies(n -> {
            assertThat(n.read()).isFalse();
            assertThat(n.title()).isEqualTo("Nghi hoc");
            assertThat(n.senderId()).isEqualTo(lecturerId);
        });
        assertThat(notificationService.countUnread(studentId)).isEqualTo(1);
        assertThatThrownBy(() -> notificationService.markAsRead(student2Id, inbox.get(0).id()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(notificationService.markAsRead(studentId, inbox.get(0).id()).read()).isTrue();
        sync();
        assertThat(notificationService.countUnread(studentId)).isZero();

        // Chống gửi trùng khi cùng một sự kiện xử lý lại (ràng buộc unique trên database)
        NotificationContent event = new NotificationContent(NotificationType.GRADE, "Diem da cong bo", null, null,
                lecturerId, sectionId, "GRADE_PUBLISHED:it");
        assertThat(notificationService.notifyUser(studentId, event)).isTrue();
        sync();
        assertThat(notificationService.notifyUser(studentId, event)).isFalse();
        sync();
        assertThat(notificationService.countUnread(studentId)).isEqualTo(1);
        assertThat(notificationService.markAllAsRead(studentId)).isEqualTo(1);

        // ===== Rút lớp giữ lịch sử =====
        enrollmentService.withdraw(student2Id, sectionId, student2Id, "Ban viec");
        sync();
        assertThat(enrollmentService.listMyEnrollments(student2Id)).singleElement().satisfies(e -> {
            assertThat(e.status().name()).isEqualTo("DROPPED");
            assertThat(e.droppedAt()).isNotNull();
        });
        assertThatThrownBy(() -> discussionService.get(student2Id, threadId)).isInstanceOf(ForbiddenException.class);
    }
}
