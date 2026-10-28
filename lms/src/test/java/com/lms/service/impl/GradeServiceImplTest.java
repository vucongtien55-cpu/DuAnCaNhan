package com.lms.service.impl;

import com.lms.dto.grade.GradeResponse;
import com.lms.dto.grade.GradeSubmissionRequest;
import com.lms.dto.grade.StudentGradeResponse;
import com.lms.entity.Assignment;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Grade;
import com.lms.entity.Role;
import com.lms.entity.Submission;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.GradeType;
import com.lms.entity.enums.GradingPolicy;
import com.lms.entity.enums.RoleName;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.AssignmentRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.GradeRepository;
import com.lms.repository.SubmissionRepository;
import com.lms.repository.UserRepository;
import com.lms.service.support.OfficialGradeResolver;
import com.lms.service.support.SectionAccessChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long OTHER_STUDENT_ID = 5L;
    private static final Long SECTION_ID = 20L;
    private static final Long ASSIGNMENT_ID = 30L;
    private static final Long SUBMISSION_ID = 40L;
    private static final Long GRADE_ID = 60L;

    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;

    private GradeServiceImpl service;

    @BeforeEach
    void setUp() {
        SectionAccessChecker access = new SectionAccessChecker(userRepository, enrollmentRepository);
        service = new GradeServiceImpl(gradeRepository, submissionRepository, assignmentRepository,
                enrollmentRepository, access, new OfficialGradeResolver(submissionRepository, gradeRepository));
    }

    // ---------- helpers ----------

    private User user(Long id, boolean enabled, RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setFullName("User " + id);
        user.setEnabled(enabled);
        user.setRoles(Set.of(role));
        return user;
    }

    private User asUser(Long id, RoleName roleName) {
        User user = user(id, true, roleName);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private CourseSection section(User teacher) {
        CourseSection section = new CourseSection();
        section.setId(SECTION_ID);
        section.setTeacher(teacher);
        return section;
    }

    private Assignment assignment(Long id, CourseSection section, GradingPolicy policy) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setSection(section);
        assignment.setTitle("Bài " + id);
        assignment.setMaxScore(10.0);
        assignment.setGradingPolicy(policy);
        return assignment;
    }

    private Assignment stubAssignment(CourseSection section, GradingPolicy policy) {
        Assignment assignment = assignment(ASSIGNMENT_ID, section, policy);
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        return assignment;
    }

    private Submission submission(Long id, Assignment assignment, Long studentId, int attempt) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setAssignment(assignment);
        submission.setStudent(user(studentId, true, RoleName.STUDENT));
        submission.setAttemptNumber(attempt);
        return submission;
    }

    private Submission stubSubmission(Assignment assignment) {
        Submission submission = submission(SUBMISSION_ID, assignment, STUDENT_ID, 1);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));
        return submission;
    }

    private Enrollment enrollment(CourseSection section, Long studentId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(70L);
        enrollment.setSection(section);
        enrollment.setStudent(user(studentId, true, RoleName.STUDENT));
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        return enrollment;
    }

    private Enrollment stubEnrollment(CourseSection section, Long studentId) {
        Enrollment enrollment = enrollment(section, studentId);
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, SECTION_ID)).thenReturn(Optional.of(enrollment));
        return enrollment;
    }

    private Grade grade(Long id, Submission submission, double score, boolean published) {
        Grade grade = new Grade();
        grade.setId(id);
        grade.setEnrollment(enrollment(submission.getAssignment().getSection(), submission.getStudent().getId()));
        grade.setSubmission(submission);
        grade.setType(GradeType.ASSIGNMENT);
        grade.setScore(score);
        grade.setMaxScore(10.0);
        grade.setPublished(published);
        if (published) {
            grade.setPublishedAt(LocalDateTime.now().minusDays(1));
        }
        return grade;
    }

    private GradeSubmissionRequest request(Double score) {
        return new GradeSubmissionRequest(score, "  Làm tốt ", "  Nghi vấn chép bài ");
    }

    // ---------- gradeSubmission ----------

    @Test
    @DisplayName("Admin chấm bài nộp lần đầu: tạo Grade gắn bài nộp, chưa công bố")
    void grade_adminCreatesGrade() {
        User admin = asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = section(null);
        Assignment assignment = assignment(ASSIGNMENT_ID, section, GradingPolicy.LATEST);
        Submission submission = stubSubmission(assignment);
        Enrollment enrollment = stubEnrollment(section, STUDENT_ID);
        when(gradeRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeResponse response = service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(8.5));

        ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
        verify(gradeRepository).save(captor.capture());
        Grade saved = captor.getValue();
        assertThat(saved.getSubmission()).isSameAs(submission);
        assertThat(saved.getEnrollment()).isSameAs(enrollment);
        assertThat(saved.getType()).isEqualTo(GradeType.ASSIGNMENT);
        assertThat(saved.getMaxScore()).isEqualTo(10.0);
        assertThat(saved.getGradedBy()).isSameAs(admin);
        assertThat(saved.isPublished()).isFalse();
        assertThat(response.score()).isEqualTo(8.5);
        assertThat(response.feedback()).isEqualTo("Làm tốt");
        assertThat(response.internalNote()).isEqualTo("Nghi vấn chép bài");
        assertThat(response.published()).isFalse();
        assertThat(response.studentId()).isEqualTo(STUDENT_ID);
        assertThat(response.assignmentId()).isEqualTo(ASSIGNMENT_ID);
    }

    @Test
    @DisplayName("Giảng viên phụ trách chấm bài, điểm biên 0 và điểm tối đa đều hợp lệ")
    void grade_lecturerBoundaryScores() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        CourseSection section = section(lecturer);
        Assignment assignment = assignment(ASSIGNMENT_ID, section, GradingPolicy.LATEST);
        stubSubmission(assignment);
        stubEnrollment(section, STUDENT_ID);
        when(gradeRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.gradeSubmission(LECTURER_ID, SUBMISSION_ID, request(0.0)).score()).isEqualTo(0.0);
        assertThat(service.gradeSubmission(LECTURER_ID, SUBMISSION_ID, request(10.0)).score()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Chấm lại bài chưa công bố: cập nhật Grade cũ, không tạo Grade thứ hai")
    void grade_updatesExistingUnpublishedGrade() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST);
        Submission submission = stubSubmission(assignment);
        Grade existing = grade(GRADE_ID, submission, 5.0, false);
        when(gradeRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.of(existing));
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeResponse response = service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(9.0));

        assertThat(response.id()).isEqualTo(GRADE_ID);
        assertThat(existing.getScore()).isEqualTo(9.0);
        verify(enrollmentRepository, never()).findByStudentIdAndSectionId(any(), any());
    }

    @Test
    @DisplayName("Điểm âm -> BusinessException, không lưu")
    void grade_negativeScore() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSubmission(assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST));

        assertThatThrownBy(() -> service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(-0.5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("âm");

        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Điểm vượt điểm tối đa của bài tập -> BusinessException, không lưu")
    void grade_scoreAboveMax() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSubmission(assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST));

        assertThatThrownBy(() -> service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(10.01)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vượt quá");

        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Điểm NaN, vô cực hoặc null -> BusinessException")
    void grade_invalidNumbers() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSubmission(assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST));

        assertThatThrownBy(() -> service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(Double.NaN)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(Double.POSITIVE_INFINITY)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(null)))
                .isInstanceOf(BusinessException.class);

        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sửa điểm đã công bố -> BusinessException, điểm giữ nguyên")
    void grade_publishedCannotBeEdited() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST);
        Submission submission = stubSubmission(assignment);
        Grade published = grade(GRADE_ID, submission, 7.0, true);
        when(gradeRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(10.0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hủy công bố");

        assertThat(published.getScore()).isEqualTo(7.0);
        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Giảng viên không phụ trách chấm bài -> ForbiddenException")
    void grade_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSubmission(assignment(ASSIGNMENT_ID, section(assigned), GradingPolicy.LATEST));

        assertThatThrownBy(() -> service.gradeSubmission(OTHER_LECTURER_ID, SUBMISSION_ID, request(5.0)))
                .isInstanceOf(ForbiddenException.class);

        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên tự chấm điểm -> ForbiddenException")
    void grade_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSubmission(assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST));

        assertThatThrownBy(() -> service.gradeSubmission(STUDENT_ID, SUBMISSION_ID, request(10.0)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Chấm bài nộp không tồn tại -> ResourceNotFoundException")
    void grade_submissionNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(5.0)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên không có đăng ký trong lớp -> BusinessException")
    void grade_noEnrollment() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSubmission(assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST));
        when(gradeRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(5.0)))
                .isInstanceOf(BusinessException.class);

        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản bị khóa chấm bài -> ForbiddenException")
    void grade_disabledUser() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, false, RoleName.ADMIN)));

        assertThatThrownBy(() -> service.gradeSubmission(ADMIN_ID, SUBMISSION_ID, request(5.0)))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- getBySubmission / listByAssignment ----------

    @Test
    @DisplayName("Giảng viên phụ trách xem điểm của bài nộp, gồm nhận xét nội bộ")
    void getBySubmission_manager() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        Assignment assignment = assignment(ASSIGNMENT_ID, section(lecturer), GradingPolicy.LATEST);
        Submission submission = stubSubmission(assignment);
        Grade grade = grade(GRADE_ID, submission, 8.0, false);
        grade.setInternalNote("Ghi chú riêng");
        when(gradeRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.of(grade));

        assertThat(service.getBySubmission(LECTURER_ID, SUBMISSION_ID).internalNote()).isEqualTo("Ghi chú riêng");
    }

    @Test
    @DisplayName("Bài nộp chưa chấm -> ResourceNotFoundException")
    void getBySubmission_notGraded() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSubmission(assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST));
        when(gradeRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySubmission(ADMIN_ID, SUBMISSION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên xem điểm theo bài nộp qua API quản lý -> ForbiddenException")
    void getBySubmission_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSubmission(assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST));

        assertThatThrownBy(() -> service.getBySubmission(STUDENT_ID, SUBMISSION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(gradeRepository, never()).findBySubmissionId(any());
    }

    @Test
    @DisplayName("Giảng viên phụ trách xem điểm của cả bài tập")
    void listByAssignment_manager() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        Assignment assignment = stubAssignment(section(lecturer), GradingPolicy.LATEST);
        when(gradeRepository.findBySubmissionOfAssignment(ASSIGNMENT_ID)).thenReturn(List.of(
                grade(61L, submission(41L, assignment, STUDENT_ID, 1), 6.0, false),
                grade(62L, submission(42L, assignment, OTHER_STUDENT_ID, 1), 9.0, true)));

        assertThat(service.listByAssignment(LECTURER_ID, ASSIGNMENT_ID)).hasSize(2);
    }

    @Test
    @DisplayName("Sinh viên xem điểm cả bài tập -> ForbiddenException")
    void listByAssignment_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubAssignment(section(null), GradingPolicy.LATEST);

        assertThatThrownBy(() -> service.listByAssignment(STUDENT_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(gradeRepository, never()).findBySubmissionOfAssignment(any());
    }

    @Test
    @DisplayName("Xem điểm của bài tập không tồn tại -> ResourceNotFoundException")
    void listByAssignment_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByAssignment(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- công bố điểm ----------

    @Test
    @DisplayName("Giảng viên phụ trách công bố điểm: ghi thời điểm và người công bố")
    void publish_success() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        Assignment assignment = assignment(ASSIGNMENT_ID, section(lecturer), GradingPolicy.LATEST);
        Grade grade = grade(GRADE_ID, submission(SUBMISSION_ID, assignment, STUDENT_ID, 1), 8.0, false);
        when(gradeRepository.findById(GRADE_ID)).thenReturn(Optional.of(grade));

        GradeResponse response = service.publishGrade(LECTURER_ID, GRADE_ID);

        assertThat(response.published()).isTrue();
        assertThat(response.publishedAt()).isNotNull();
        assertThat(grade.getPublishedBy()).isSameAs(lecturer);
    }

    @Test
    @DisplayName("Công bố điểm đã công bố -> BusinessException")
    void publish_alreadyPublished() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST);
        Grade grade = grade(GRADE_ID, submission(SUBMISSION_ID, assignment, STUDENT_ID, 1), 8.0, true);
        when(gradeRepository.findById(GRADE_ID)).thenReturn(Optional.of(grade));

        assertThatThrownBy(() -> service.publishGrade(ADMIN_ID, GRADE_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách công bố điểm -> ForbiddenException, điểm vẫn ẩn")
    void publish_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        Assignment assignment = assignment(ASSIGNMENT_ID, section(assigned), GradingPolicy.LATEST);
        Grade grade = grade(GRADE_ID, submission(SUBMISSION_ID, assignment, STUDENT_ID, 1), 8.0, false);
        when(gradeRepository.findById(GRADE_ID)).thenReturn(Optional.of(grade));

        assertThatThrownBy(() -> service.publishGrade(OTHER_LECTURER_ID, GRADE_ID))
                .isInstanceOf(ForbiddenException.class);

        assertThat(grade.isPublished()).isFalse();
    }

    @Test
    @DisplayName("Sinh viên tự công bố điểm -> ForbiddenException")
    void publish_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Assignment assignment = assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST);
        Grade grade = grade(GRADE_ID, submission(SUBMISSION_ID, assignment, STUDENT_ID, 1), 8.0, false);
        when(gradeRepository.findById(GRADE_ID)).thenReturn(Optional.of(grade));

        assertThatThrownBy(() -> service.publishGrade(STUDENT_ID, GRADE_ID))
                .isInstanceOf(ForbiddenException.class);

        assertThat(grade.isPublished()).isFalse();
    }

    @Test
    @DisplayName("Công bố điểm không tồn tại -> ResourceNotFoundException")
    void publish_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(gradeRepository.findById(GRADE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publishGrade(ADMIN_ID, GRADE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Hủy công bố điểm: xóa thời điểm và người công bố")
    void unpublish_success() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST);
        Grade grade = grade(GRADE_ID, submission(SUBMISSION_ID, assignment, STUDENT_ID, 1), 8.0, true);
        when(gradeRepository.findById(GRADE_ID)).thenReturn(Optional.of(grade));

        GradeResponse response = service.unpublishGrade(ADMIN_ID, GRADE_ID);

        assertThat(response.published()).isFalse();
        assertThat(grade.getPublishedAt()).isNull();
        assertThat(grade.getPublishedBy()).isNull();
    }

    @Test
    @DisplayName("Hủy công bố điểm chưa công bố -> BusinessException")
    void unpublish_notPublished() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = assignment(ASSIGNMENT_ID, section(null), GradingPolicy.LATEST);
        Grade grade = grade(GRADE_ID, submission(SUBMISSION_ID, assignment, STUDENT_ID, 1), 8.0, false);
        when(gradeRepository.findById(GRADE_ID)).thenReturn(Optional.of(grade));

        assertThatThrownBy(() -> service.unpublishGrade(ADMIN_ID, GRADE_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Công bố cả bài tập: chỉ công bố các điểm chưa công bố và trả về số lượng")
    void publishAssignmentGrades() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        Assignment assignment = stubAssignment(section(lecturer), GradingPolicy.LATEST);
        Grade pendingOne = grade(61L, submission(41L, assignment, STUDENT_ID, 1), 6.0, false);
        Grade pendingTwo = grade(62L, submission(42L, assignment, OTHER_STUDENT_ID, 1), 9.0, false);
        Grade alreadyPublished = grade(63L, submission(43L, assignment, OTHER_STUDENT_ID, 2), 7.0, true);
        LocalDateTime publishedAt = alreadyPublished.getPublishedAt();
        when(gradeRepository.findBySubmissionOfAssignment(ASSIGNMENT_ID))
                .thenReturn(List.of(pendingOne, pendingTwo, alreadyPublished));

        int count = service.publishAssignmentGrades(LECTURER_ID, ASSIGNMENT_ID);

        assertThat(count).isEqualTo(2);
        assertThat(pendingOne.isPublished()).isTrue();
        assertThat(pendingTwo.isPublished()).isTrue();
        assertThat(alreadyPublished.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách công bố cả bài tập -> ForbiddenException")
    void publishAssignmentGrades_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubAssignment(section(assigned), GradingPolicy.LATEST);

        assertThatThrownBy(() -> service.publishAssignmentGrades(OTHER_LECTURER_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(gradeRepository, never()).findBySubmissionOfAssignment(any());
    }

    // ---------- điểm chính thức theo chính sách (góc nhìn giảng viên) ----------

    @Test
    @DisplayName("LATEST: điểm chính thức là điểm của lần nộp mới nhất, kể cả khi lần trước cao hơn")
    void official_latest() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubAssignment(section(null), GradingPolicy.LATEST);
        Submission latest = submission(42L, assignment, STUDENT_ID, 2);
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(Optional.of(latest));
        when(gradeRepository.findBySubmissionId(42L)).thenReturn(Optional.of(grade(62L, latest, 5.0, false)));

        assertThat(service.getOfficialGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID).score()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("HIGHEST: điểm chính thức là điểm cao nhất, bằng điểm thì lấy lần nộp sau")
    void official_highest() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubAssignment(section(null), GradingPolicy.HIGHEST);
        Submission first = submission(41L, assignment, STUDENT_ID, 1);
        Submission second = submission(42L, assignment, STUDENT_ID, 2);
        Submission third = submission(43L, assignment, STUDENT_ID, 3);
        when(gradeRepository.findBySubmissionOfAssignmentAndStudent(ASSIGNMENT_ID, STUDENT_ID)).thenReturn(List.of(
                grade(61L, first, 9.0, false), grade(62L, second, 7.0, false), grade(63L, third, 9.0, false)));

        GradeResponse official = service.getOfficialGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID);

        assertThat(official.score()).isEqualTo(9.0);
        assertThat(official.attemptNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("FIRST: điểm chính thức là điểm của lần nộp đầu tiên")
    void official_first() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubAssignment(section(null), GradingPolicy.FIRST);
        Submission first = submission(41L, assignment, STUDENT_ID, 1);
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberAsc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(Optional.of(first));
        when(gradeRepository.findBySubmissionId(41L)).thenReturn(Optional.of(grade(61L, first, 6.5, false)));

        assertThat(service.getOfficialGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID).attemptNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("MANUAL: điểm chính thức là điểm của bài nộp giảng viên đã chọn")
    void official_manual() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubAssignment(section(null), GradingPolicy.MANUAL);
        Submission first = submission(41L, assignment, STUDENT_ID, 1);
        first.setSelectedForGrading(true);
        Submission second = submission(42L, assignment, STUDENT_ID, 2);
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(List.of(second, first));
        when(gradeRepository.findBySubmissionId(41L)).thenReturn(Optional.of(grade(61L, first, 7.5, false)));

        assertThat(service.getOfficialGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID).score()).isEqualTo(7.5);
    }

    @Test
    @DisplayName("Bài nộp được tính điểm chưa chấm -> ResourceNotFoundException (không tự lấy bài khác)")
    void official_selectedNotGradedYet() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubAssignment(section(null), GradingPolicy.LATEST);
        Submission latest = submission(42L, assignment, STUDENT_ID, 2);
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(Optional.of(latest));
        when(gradeRepository.findBySubmissionId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOfficialGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên gọi API điểm chính thức của giảng viên -> ForbiddenException")
    void official_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubAssignment(section(null), GradingPolicy.LATEST);

        assertThatThrownBy(() -> service.getOfficialGrade(STUDENT_ID, ASSIGNMENT_ID, OTHER_STUDENT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- sinh viên xem điểm ----------

    private void stubLatestGrade(Assignment assignment, Grade grade) {
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(
                assignment.getId(), STUDENT_ID)).thenReturn(Optional.of(grade.getSubmission()));
        when(gradeRepository.findBySubmissionId(grade.getSubmission().getId())).thenReturn(Optional.of(grade));
    }

    @Test
    @DisplayName("Sinh viên xem điểm đã công bố: có điểm, nhận xét, lần nộp; không lộ nhận xét nội bộ")
    void myGrade_published() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = section(user(LECTURER_ID, true, RoleName.LECTURER));
        Assignment assignment = stubAssignment(section, GradingPolicy.LATEST);
        stubEnrollment(section, STUDENT_ID);
        Grade grade = grade(GRADE_ID, submission(SUBMISSION_ID, assignment, STUDENT_ID, 2), 8.5, true);
        grade.setFeedback("Làm tốt");
        grade.setInternalNote("BÍ-MẬT-NỘI-BỘ");
        grade.setGradedBy(user(LECTURER_ID, true, RoleName.LECTURER));
        stubLatestGrade(assignment, grade);

        StudentGradeResponse response = service.getMyGrade(STUDENT_ID, ASSIGNMENT_ID);

        assertThat(response.published()).isTrue();
        assertThat(response.score()).isEqualTo(8.5);
        assertThat(response.maxScore()).isEqualTo(10.0);
        assertThat(response.feedback()).isEqualTo("Làm tốt");
        assertThat(response.attemptNumber()).isEqualTo(2);
        assertThat(response.toString()).doesNotContain("BÍ-MẬT-NỘI-BỘ").doesNotContain("User 2");
    }

    @Test
    @DisplayName("Điểm đã chấm nhưng chưa công bố: sinh viên không thấy điểm, nhận xét hay thông tin nào")
    void myGrade_gradedButNotPublished() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = section(null);
        Assignment assignment = stubAssignment(section, GradingPolicy.LATEST);
        stubEnrollment(section, STUDENT_ID);
        Grade grade = grade(GRADE_ID, submission(SUBMISSION_ID, assignment, STUDENT_ID, 1), 8.5, false);
        grade.setFeedback("Nhận xét chưa công bố");
        stubLatestGrade(assignment, grade);

        StudentGradeResponse response = service.getMyGrade(STUDENT_ID, ASSIGNMENT_ID);

        assertThat(response.published()).isFalse();
        assertThat(response.score()).isNull();
        assertThat(response.maxScore()).isNull();
        assertThat(response.feedback()).isNull();
        assertThat(response.attemptNumber()).isNull();
        assertThat(response.toString()).doesNotContain("8.5").doesNotContain("Nhận xét chưa công bố");
    }

    @Test
    @DisplayName("Chưa có bài nộp hoặc chưa chấm: sinh viên chỉ nhận trạng thái chưa công bố")
    void myGrade_noSubmission() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = section(null);
        stubAssignment(section, GradingPolicy.LATEST);
        stubEnrollment(section, STUDENT_ID);
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        StudentGradeResponse response = service.getMyGrade(STUDENT_ID, ASSIGNMENT_ID);

        assertThat(response.published()).isFalse();
        assertThat(response.score()).isNull();
    }

    @Test
    @DisplayName("LATEST: lần nộp trước đã công bố nhưng lần mới nhất chưa -> sinh viên vẫn chỉ thấy chưa công bố")
    void myGrade_latestUnpublishedHidesOlderPublished() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = section(null);
        Assignment assignment = stubAssignment(section, GradingPolicy.LATEST);
        stubEnrollment(section, STUDENT_ID);
        Grade latest = grade(62L, submission(42L, assignment, STUDENT_ID, 2), 9.0, false);
        stubLatestGrade(assignment, latest);

        StudentGradeResponse response = service.getMyGrade(STUDENT_ID, ASSIGNMENT_ID);

        assertThat(response.published()).isFalse();
        assertThat(response.score()).isNull();
    }

    @Test
    @DisplayName("HIGHEST: sinh viên thấy điểm cao nhất khi điểm đó đã công bố")
    void myGrade_highestPublished() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = section(null);
        Assignment assignment = stubAssignment(section, GradingPolicy.HIGHEST);
        stubEnrollment(section, STUDENT_ID);
        when(gradeRepository.findBySubmissionOfAssignmentAndStudent(ASSIGNMENT_ID, STUDENT_ID)).thenReturn(List.of(
                grade(61L, submission(41L, assignment, STUDENT_ID, 1), 6.0, true),
                grade(62L, submission(42L, assignment, STUDENT_ID, 2), 9.5, true)));

        StudentGradeResponse response = service.getMyGrade(STUDENT_ID, ASSIGNMENT_ID);

        assertThat(response.score()).isEqualTo(9.5);
        assertThat(response.attemptNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("HIGHEST: điểm cao nhất chưa công bố -> không lộ điểm thấp hơn đã công bố")
    void myGrade_highestUnpublishedNotLeaked() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = section(null);
        Assignment assignment = stubAssignment(section, GradingPolicy.HIGHEST);
        stubEnrollment(section, STUDENT_ID);
        when(gradeRepository.findBySubmissionOfAssignmentAndStudent(ASSIGNMENT_ID, STUDENT_ID)).thenReturn(List.of(
                grade(61L, submission(41L, assignment, STUDENT_ID, 1), 6.0, true),
                grade(62L, submission(42L, assignment, STUDENT_ID, 2), 9.5, false)));

        StudentGradeResponse response = service.getMyGrade(STUDENT_ID, ASSIGNMENT_ID);

        assertThat(response.published()).isFalse();
        assertThat(response.score()).isNull();
    }

    @Test
    @DisplayName("Sinh viên chỉ truy vấn dữ liệu của chính mình, không bao giờ của người khác")
    void myGrade_onlyOwnData() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = section(null);
        stubAssignment(section, GradingPolicy.LATEST);
        stubEnrollment(section, STUDENT_ID);
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        service.getMyGrade(STUDENT_ID, ASSIGNMENT_ID);

        verify(submissionRepository, never()).findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(
                ASSIGNMENT_ID, OTHER_STUDENT_ID);
        verify(gradeRepository, never()).findBySubmissionOfAssignment(any());
    }

    @Test
    @DisplayName("Sinh viên không có đăng ký trong lớp xem điểm -> ForbiddenException")
    void myGrade_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubAssignment(section(null), GradingPolicy.LATEST);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyGrade(STUDENT_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(submissionRepository, never()).findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(any(), any());
    }

    @Test
    @DisplayName("Giảng viên dùng API điểm của sinh viên -> ForbiddenException")
    void myGrade_nonStudentForbidden() {
        asUser(LECTURER_ID, RoleName.LECTURER);

        assertThatThrownBy(() -> service.getMyGrade(LECTURER_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Xem điểm của bài tập không tồn tại -> ResourceNotFoundException")
    void myGrade_assignmentNotFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyGrade(STUDENT_ID, ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Danh sách điểm của sinh viên trong lớp: chỉ bài đã công bố có điểm")
    void listMyGrades() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = section(null);
        stubEnrollment(section, STUDENT_ID);
        Assignment published = assignment(30L, section, GradingPolicy.LATEST);
        Assignment unpublished = assignment(31L, section, GradingPolicy.LATEST);
        when(assignmentRepository.findBySectionIdOrderByDueDateAsc(SECTION_ID))
                .thenReturn(List.of(published, unpublished));
        stubLatestGrade(published, grade(61L, submission(41L, published, STUDENT_ID, 1), 8.0, true));
        stubLatestGrade(unpublished, grade(62L, submission(42L, unpublished, STUDENT_ID, 1), 6.0, false));

        List<StudentGradeResponse> result = service.listMyGrades(STUDENT_ID, SECTION_ID);

        assertThat(result).extracting(StudentGradeResponse::published).containsExactly(true, false);
        assertThat(result).extracting(StudentGradeResponse::score).containsExactly(8.0, null);
    }

    @Test
    @DisplayName("Sinh viên không thuộc lớp xem danh sách điểm -> ForbiddenException")
    void listMyGrades_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listMyGrades(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(assignmentRepository, never()).findBySectionIdOrderByDueDateAsc(any());
    }
}
