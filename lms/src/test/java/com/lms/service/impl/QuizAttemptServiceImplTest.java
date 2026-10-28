package com.lms.service.impl;

import com.lms.dto.quiz.QuizAttemptResponse;
import com.lms.dto.quiz.QuizAttemptStaffResponse;
import com.lms.dto.quiz.QuizResultResponse;
import com.lms.dto.quiz.SaveAnswerRequest;
import com.lms.dto.quiz.SaveAnswerResponse;
import com.lms.entity.AnswerOption;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Grade;
import com.lms.entity.Question;
import com.lms.entity.Quiz;
import com.lms.entity.QuizAnswer;
import com.lms.entity.QuizAttempt;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.AttemptStatus;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.GradeType;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.AttemptExpiredException;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.GradeRepository;
import com.lms.repository.QuizAnswerRepository;
import com.lms.repository.QuizAttemptRepository;
import com.lms.repository.QuizRepository;
import com.lms.repository.UserRepository;
import com.lms.service.support.SectionAccessChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long OTHER_STUDENT_ID = 5L;
    private static final Long SECTION_ID = 20L;
    private static final Long QUIZ_ID = 50L;
    private static final Long ATTEMPT_ID = 100L;

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAttemptRepository attemptRepository;
    @Mock
    private QuizAnswerRepository answerRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;

    private QuizAttemptServiceImpl service;

    @BeforeEach
    void setUp() {
        SectionAccessChecker access = new SectionAccessChecker(userRepository, enrollmentRepository);
        service = new QuizAttemptServiceImpl(quizRepository, attemptRepository, answerRepository, gradeRepository,
                enrollmentRepository, access, TransactionOperations.withoutTransaction());
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

    private CourseSection section(SectionStatus status, User teacher) {
        CourseSection section = new CourseSection();
        section.setId(SECTION_ID);
        section.setStatus(status);
        section.setTeacher(teacher);
        return section;
    }

    /** Bài kiểm tra 3 câu x 2 điểm (tối đa 6), mỗi câu 3 lựa chọn, lựa chọn thứ nhất là đáp án đúng. */
    private Quiz quiz(CourseSection section) {
        Quiz quiz = new Quiz();
        quiz.setId(QUIZ_ID);
        quiz.setSection(section);
        quiz.setTitle("Kiểm tra 1");
        quiz.setDurationMinutes(30);
        quiz.setMaxAttempts(1);
        quiz.setPublished(true);
        for (long q = 1; q <= 3; q++) {
            Question question = new Question();
            question.setId(q);
            question.setQuiz(quiz);
            question.setContent("Câu " + q);
            question.setScore(2.0);
            question.setOrderIndex((int) q);
            question.setExplanation("LỜI-GIẢI-" + q);
            for (long o = 1; o <= 3; o++) {
                AnswerOption option = new AnswerOption();
                option.setId(q * 10 + o);
                option.setQuestion(question);
                option.setContent("Lựa chọn " + o);
                option.setCorrect(o == 1);
                option.setOrderIndex((int) o);
                question.getOptions().add(option);
            }
            quiz.getQuestions().add(question);
        }
        return quiz;
    }

    private Quiz openQuiz() {
        return quiz(section(SectionStatus.IN_PROGRESS, null));
    }

    private AnswerOption option(Quiz quiz, int questionNo, int optionNo) {
        return quiz.getQuestions().get(questionNo - 1).getOptions().get(optionNo - 1);
    }

    private Enrollment stubEnrollment(EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(70L);
        enrollment.setStatus(status);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.of(enrollment));
        return enrollment;
    }

    private Quiz stubQuizForStart(Quiz quiz) {
        when(quizRepository.findByIdForUpdate(QUIZ_ID)).thenReturn(Optional.of(quiz));
        return quiz;
    }

    private QuizAttempt attempt(Quiz quiz, Long studentId, AttemptStatus status, LocalDateTime deadline) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(ATTEMPT_ID);
        attempt.setQuiz(quiz);
        attempt.setStudent(user(studentId, true, RoleName.STUDENT));
        attempt.setAttemptNumber(1);
        attempt.setStartedAt(deadline.minusMinutes(30));
        attempt.setDeadlineAt(deadline);
        attempt.setStatus(status);
        return attempt;
    }

    private QuizAttempt stubAttempt(QuizAttempt attempt) {
        when(attemptRepository.findByIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.of(attempt));
        return attempt;
    }

    /** Lượt đang làm của chính sinh viên, còn 20 phút. */
    private QuizAttempt runningAttempt(Quiz quiz) {
        return stubAttempt(attempt(quiz, STUDENT_ID, AttemptStatus.IN_PROGRESS, LocalDateTime.now().plusMinutes(20)));
    }

    /** Lượt đang làm của chính sinh viên nhưng đã quá hạn 5 phút. */
    private QuizAttempt overdueAttempt(Quiz quiz) {
        return stubAttempt(attempt(quiz, STUDENT_ID, AttemptStatus.IN_PROGRESS, LocalDateTime.now().minusMinutes(5)));
    }

    private QuizAnswer savedAnswer(QuizAttempt attempt, Question question, AnswerOption selected) {
        QuizAnswer answer = new QuizAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(question);
        answer.setSelectedOptions(new HashSet<>(Set.of(selected)));
        return answer;
    }

    private void stubGradeSave() {
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- startAttempt ----------

    @Test
    @DisplayName("Sinh viên đang theo học bắt đầu làm bài: hạn chót do server tính, đề không có đáp án đúng")
    void start_success() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubQuizForStart(openQuiz());
        when(attemptRepository.findFirstByQuizIdAndStudentIdAndStatus(QUIZ_ID, STUDENT_ID, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(0L);
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
            QuizAttempt saved = inv.getArgument(0);
            saved.setId(ATTEMPT_ID);
            return saved;
        });
        LocalDateTime before = LocalDateTime.now();

        QuizAttemptResponse response = service.startAttempt(STUDENT_ID, QUIZ_ID);

        ArgumentCaptor<QuizAttempt> captor = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(attemptRepository).save(captor.capture());
        QuizAttempt saved = captor.getValue();
        assertThat(saved.getStudent()).isSameAs(student);
        assertThat(saved.getAttemptNumber()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(Duration.between(saved.getStartedAt(), saved.getDeadlineAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(saved.getStartedAt()).isAfterOrEqualTo(before);

        assertThat(response.attemptId()).isEqualTo(ATTEMPT_ID);
        assertThat(response.questions()).hasSize(3);
        assertThat(response.remainingSeconds()).isBetween(1790L, 1800L);
        assertThat(response.savedAnswers()).isEmpty();
    }

    @Test
    @DisplayName("Đề gửi cho sinh viên không chứa cờ đáp án đúng hay lời giải")
    void start_neverLeaksCorrectAnswers() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubQuizForStart(openQuiz());
        when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(0L);
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
            QuizAttempt saved = inv.getArgument(0);
            saved.setId(ATTEMPT_ID);
            return saved;
        });

        QuizAttemptResponse response = service.startAttempt(STUDENT_ID, QUIZ_ID);

        assertThat(response.toString()).doesNotContain("LỜI-GIẢI").doesNotContain("correct");
        assertThat(Arrays.stream(QuizAttemptResponse.QuestionView.class.getRecordComponents())
                .map(RecordComponent::getName)).noneMatch(n -> n.toLowerCase().contains("explanation")
                || n.toLowerCase().contains("correct"));
        assertThat(Arrays.stream(QuizAttemptResponse.OptionView.class.getRecordComponents())
                .map(RecordComponent::getName)).noneMatch(n -> n.toLowerCase().contains("correct"));
    }

    @Test
    @DisplayName("Hạn chót không vượt quá giờ đóng bài kiểm tra")
    void start_deadlineCappedByEndTime() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = stubQuizForStart(openQuiz());
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(10);
        quiz.setEndTime(endTime);
        when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(0L);
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
            QuizAttempt saved = inv.getArgument(0);
            saved.setId(ATTEMPT_ID);
            return saved;
        });

        QuizAttemptResponse response = service.startAttempt(STUDENT_ID, QUIZ_ID);

        assertThat(response.deadlineAt()).isEqualTo(endTime);
        assertThat(response.remainingSeconds()).isLessThanOrEqualTo(600L);
    }

    @Test
    @DisplayName("Mất kết nối rồi vào lại: nhận lại đúng lượt đang làm dở cùng các câu đã lưu, không tính là làm lại")
    void start_resumesOpenAttempt() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = stubQuizForStart(openQuiz());
        QuizAttempt open = attempt(quiz, STUDENT_ID, AttemptStatus.IN_PROGRESS, LocalDateTime.now().plusMinutes(12));
        when(attemptRepository.findFirstByQuizIdAndStudentIdAndStatus(QUIZ_ID, STUDENT_ID, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(open));
        when(answerRepository.findByAttemptId(ATTEMPT_ID))
                .thenReturn(List.of(savedAnswer(open, quiz.getQuestions().get(0), option(quiz, 1, 2))));

        QuizAttemptResponse response = service.startAttempt(STUDENT_ID, QUIZ_ID);

        assertThat(response.attemptId()).isEqualTo(ATTEMPT_ID);
        assertThat(response.savedAnswers()).containsEntry(1L, 12L);
        assertThat(response.remainingSeconds()).isBetween(700L, 720L);
        verify(attemptRepository, never()).save(any());
        verify(attemptRepository, never()).countByQuizIdAndStudentId(any(), any());
    }

    @Test
    @DisplayName("Lượt cũ đã quá hạn: được chốt tự động rồi tính vào số lần làm, không cho làm lại khi chỉ được 1 lần")
    void start_overdueOpenAttemptIsFinalizedThenRetakeBlocked() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = stubQuizForStart(openQuiz());
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5);
        QuizAttempt open = attempt(quiz, STUDENT_ID, AttemptStatus.IN_PROGRESS, deadline);
        when(attemptRepository.findFirstByQuizIdAndStudentIdAndStatus(QUIZ_ID, STUDENT_ID, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(open));
        when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(1L);
        stubGradeSave();

        assertThatThrownBy(() -> service.startAttempt(STUDENT_ID, QUIZ_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tối đa 1 lần");

        assertThat(open.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(open.getSubmittedAt()).isEqualTo(deadline);
        verify(gradeRepository).save(any(Grade.class));
        verify(attemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Làm lại khi đã dùng hết số lần -> BusinessException, không tạo lượt mới")
    void start_retakeBlocked() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubQuizForStart(openQuiz());
        when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.startAttempt(STUDENT_ID, QUIZ_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1 lần");

        verify(attemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bài cho phép làm 2 lần: lần làm lại nhận attemptNumber = 2")
    void start_retakeAllowedWhenMaxAttemptsHigher() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = stubQuizForStart(openQuiz());
        quiz.setMaxAttempts(2);
        when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(1L);
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
            QuizAttempt saved = inv.getArgument(0);
            saved.setId(ATTEMPT_ID);
            return saved;
        });

        assertThat(service.startAttempt(STUDENT_ID, QUIZ_ID).attemptNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Bài kiểm tra chưa đến giờ mở -> BusinessException")
    void start_notOpenYet() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = stubQuizForStart(openQuiz());
        quiz.setStartTime(LocalDateTime.now().plusHours(1));
        when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(0L);

        assertThatThrownBy(() -> service.startAttempt(STUDENT_ID, QUIZ_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa mở");

        verify(attemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bài kiểm tra đã quá giờ đóng -> BusinessException")
    void start_alreadyClosed() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = stubQuizForStart(openQuiz());
        quiz.setEndTime(LocalDateTime.now().minusMinutes(1));
        when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(0L);

        assertThatThrownBy(() -> service.startAttempt(STUDENT_ID, QUIZ_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã đóng");
    }

    @Test
    @DisplayName("Sinh viên chưa đăng ký lớp làm bài -> ForbiddenException")
    void start_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());
        stubQuizForStart(openQuiz());

        assertThatThrownBy(() -> service.startAttempt(STUDENT_ID, QUIZ_ID)).isInstanceOf(ForbiddenException.class);

        verify(attemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên đã rút khỏi lớp làm bài -> ForbiddenException")
    void start_droppedStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.DROPPED);
        stubQuizForStart(openQuiz());

        assertThatThrownBy(() -> service.startAttempt(STUDENT_ID, QUIZ_ID)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Bài kiểm tra chưa công bố: sinh viên nhận 'không tồn tại'")
    void start_unpublished() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = stubQuizForStart(openQuiz());
        quiz.setPublished(false);

        assertThatThrownBy(() -> service.startAttempt(STUDENT_ID, QUIZ_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Làm bài khi lớp đã ENDED -> BusinessException")
    void start_sectionEnded() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubQuizForStart(quiz(section(SectionStatus.ENDED, null)));

        assertThatThrownBy(() -> service.startAttempt(STUDENT_ID, QUIZ_ID)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Làm bài kiểm tra không tồn tại -> ResourceNotFoundException")
    void start_quizNotFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(quizRepository.findByIdForUpdate(QUIZ_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startAttempt(STUDENT_ID, QUIZ_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Giảng viên và Admin không được làm bài thay sinh viên -> ForbiddenException")
    void start_nonStudentForbidden() {
        asUser(LECTURER_ID, RoleName.LECTURER);
        asUser(ADMIN_ID, RoleName.ADMIN);

        assertThatThrownBy(() -> service.startAttempt(LECTURER_ID, QUIZ_ID)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.startAttempt(ADMIN_ID, QUIZ_ID)).isInstanceOf(ForbiddenException.class);

        verify(quizRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Tài khoản bị khóa làm bài -> ForbiddenException")
    void start_disabledStudent() {
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(user(STUDENT_ID, false, RoleName.STUDENT)));

        assertThatThrownBy(() -> service.startAttempt(STUDENT_ID, QUIZ_ID)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Xáo trộn câu hỏi: thứ tự ổn định giữa các lần lấy đề của cùng một lượt")
    void getAttempt_shuffleIsStable() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Quiz quiz = openQuiz();
        quiz.setShuffleQuestions(true);
        runningAttempt(quiz);

        List<Long> first = service.getAttempt(STUDENT_ID, ATTEMPT_ID).questions().stream()
                .map(QuizAttemptResponse.QuestionView::id).toList();
        List<Long> second = service.getAttempt(STUDENT_ID, ATTEMPT_ID).questions().stream()
                .map(QuizAttemptResponse.QuestionView::id).toList();

        assertThat(first).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(second).isEqualTo(first);
    }

    // ---------- getAttempt ----------

    @Test
    @DisplayName("Lấy lại đề của lượt đang làm: có các câu đã lưu")
    void getAttempt_success() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Quiz quiz = openQuiz();
        QuizAttempt attempt = runningAttempt(quiz);
        when(answerRepository.findByAttemptId(ATTEMPT_ID))
                .thenReturn(List.of(savedAnswer(attempt, quiz.getQuestions().get(1), option(quiz, 2, 3))));

        QuizAttemptResponse response = service.getAttempt(STUDENT_ID, ATTEMPT_ID);

        assertThat(response.savedAnswers()).containsEntry(2L, 23L);
        assertThat(response.status()).isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Lấy đề khi đã hết giờ: bài được chốt tự động bằng câu đã lưu rồi báo hết giờ")
    void getAttempt_expiredFinalizesAndThrows() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = openQuiz();
        QuizAttempt attempt = overdueAttempt(quiz);
        stubGradeSave();

        assertThatThrownBy(() -> service.getAttempt(STUDENT_ID, ATTEMPT_ID))
                .isInstanceOf(AttemptExpiredException.class);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(attempt.getScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Lấy đề của lượt đã nộp -> BusinessException")
    void getAttempt_alreadySubmitted() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubAttempt(attempt(openQuiz(), STUDENT_ID, AttemptStatus.SUBMITTED, LocalDateTime.now().plusMinutes(5)));

        assertThatThrownBy(() -> service.getAttempt(STUDENT_ID, ATTEMPT_ID)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Lấy đề của lượt làm bài người khác -> ForbiddenException")
    void getAttempt_otherStudentForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        stubAttempt(attempt(openQuiz(), STUDENT_ID, AttemptStatus.IN_PROGRESS, LocalDateTime.now().plusMinutes(5)));

        assertThatThrownBy(() -> service.getAttempt(OTHER_STUDENT_ID, ATTEMPT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Lấy đề của lượt không tồn tại -> ResourceNotFoundException")
    void getAttempt_notFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(attemptRepository.findByIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAttempt(STUDENT_ID, ATTEMPT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- saveAnswer ----------

    @Test
    @DisplayName("Lưu câu trả lời mới trong thời gian làm bài")
    void saveAnswer_new() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Quiz quiz = openQuiz();
        runningAttempt(quiz);
        when(answerRepository.findByAttemptIdAndQuestionId(ATTEMPT_ID, 1L)).thenReturn(Optional.empty());

        SaveAnswerResponse response = service.saveAnswer(STUDENT_ID, ATTEMPT_ID, new SaveAnswerRequest(1L, 12L));

        ArgumentCaptor<QuizAnswer> captor = ArgumentCaptor.forClass(QuizAnswer.class);
        verify(answerRepository).save(captor.capture());
        assertThat(captor.getValue().getSelectedOptions()).extracting(AnswerOption::getId).containsExactly(12L);
        assertThat(captor.getValue().getQuestion().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getCorrect()).isNull();
        assertThat(response.selectedOptionId()).isEqualTo(12L);
        assertThat(response.remainingSeconds()).isBetween(1190L, 1200L);
    }

    @Test
    @DisplayName("Đổi đáp án: câu trả lời cũ được cập nhật, không tạo bản ghi thứ hai")
    void saveAnswer_changeExisting() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Quiz quiz = openQuiz();
        QuizAttempt attempt = runningAttempt(quiz);
        QuizAnswer existing = savedAnswer(attempt, quiz.getQuestions().get(0), option(quiz, 1, 2));
        when(answerRepository.findByAttemptIdAndQuestionId(ATTEMPT_ID, 1L)).thenReturn(Optional.of(existing));

        service.saveAnswer(STUDENT_ID, ATTEMPT_ID, new SaveAnswerRequest(1L, 13L));

        assertThat(existing.getSelectedOptions()).extracting(AnswerOption::getId).containsExactly(13L);
        verify(answerRepository).save(existing);
    }

    @Test
    @DisplayName("Bỏ chọn (selectedOptionId = null): câu trả lời được xóa, trở thành bỏ trống")
    void saveAnswer_clear() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Quiz quiz = openQuiz();
        QuizAttempt attempt = runningAttempt(quiz);
        QuizAnswer existing = savedAnswer(attempt, quiz.getQuestions().get(0), option(quiz, 1, 2));
        when(answerRepository.findByAttemptIdAndQuestionId(ATTEMPT_ID, 1L)).thenReturn(Optional.of(existing));

        SaveAnswerResponse response = service.saveAnswer(STUDENT_ID, ATTEMPT_ID, new SaveAnswerRequest(1L, null));

        assertThat(existing.getSelectedOptions()).isEmpty();
        assertThat(response.selectedOptionId()).isNull();
    }

    @Test
    @DisplayName("Lựa chọn không thuộc câu hỏi -> BusinessException, không lưu")
    void saveAnswer_optionOfAnotherQuestion() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        runningAttempt(openQuiz());
        when(answerRepository.findByAttemptIdAndQuestionId(ATTEMPT_ID, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveAnswer(STUDENT_ID, ATTEMPT_ID, new SaveAnswerRequest(1L, 21L)))
                .isInstanceOf(BusinessException.class);

        verify(answerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Câu hỏi không thuộc bài kiểm tra -> BusinessException")
    void saveAnswer_unknownQuestion() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        runningAttempt(openQuiz());

        assertThatThrownBy(() -> service.saveAnswer(STUDENT_ID, ATTEMPT_ID, new SaveAnswerRequest(99L, 991L)))
                .isInstanceOf(BusinessException.class);

        verify(answerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lưu câu trả lời sau hạn: bài được chốt, câu trả lời muộn bị từ chối và không được lưu")
    void saveAnswer_afterDeadline() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = openQuiz();
        QuizAttempt attempt = overdueAttempt(quiz);
        stubGradeSave();

        assertThatThrownBy(() -> service.saveAnswer(STUDENT_ID, ATTEMPT_ID, new SaveAnswerRequest(1L, 11L)))
                .isInstanceOf(AttemptExpiredException.class);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        verify(answerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lưu câu trả lời cho lượt đã nộp -> BusinessException")
    void saveAnswer_alreadySubmitted() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubAttempt(attempt(openQuiz(), STUDENT_ID, AttemptStatus.SUBMITTED, LocalDateTime.now().plusMinutes(5)));

        assertThatThrownBy(() -> service.saveAnswer(STUDENT_ID, ATTEMPT_ID, new SaveAnswerRequest(1L, 11L)))
                .isInstanceOf(BusinessException.class);

        verify(answerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lưu câu trả lời vào lượt của người khác -> ForbiddenException")
    void saveAnswer_otherStudentForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        stubAttempt(attempt(openQuiz(), STUDENT_ID, AttemptStatus.IN_PROGRESS, LocalDateTime.now().plusMinutes(5)));

        assertThatThrownBy(() -> service.saveAnswer(OTHER_STUDENT_ID, ATTEMPT_ID, new SaveAnswerRequest(1L, 11L)))
                .isInstanceOf(ForbiddenException.class);

        verify(answerRepository, never()).save(any());
    }

    // ---------- submit và chấm điểm ----------

    @Test
    @DisplayName("Nộp bài: 1 câu đúng, 1 câu sai, 1 câu bỏ trống -> 2/6 điểm, tạo Grade QUIZ chưa công bố")
    void submit_gradesAnswers() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Enrollment enrollment = stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = openQuiz();
        QuizAttempt attempt = runningAttempt(quiz);
        QuizAnswer right = savedAnswer(attempt, quiz.getQuestions().get(0), option(quiz, 1, 1));
        QuizAnswer wrong = savedAnswer(attempt, quiz.getQuestions().get(1), option(quiz, 2, 2));
        when(answerRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(List.of(right, wrong));
        stubGradeSave();

        QuizResultResponse result = service.submit(STUDENT_ID, ATTEMPT_ID);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(attempt.getScore()).isEqualTo(2.0);
        assertThat(attempt.getSubmittedAt()).isNotNull();
        assertThat(right.getCorrect()).isTrue();
        assertThat(right.getScoreAwarded()).isEqualTo(2.0);
        assertThat(wrong.getCorrect()).isFalse();
        assertThat(wrong.getScoreAwarded()).isEqualTo(0.0);

        ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
        verify(gradeRepository).save(captor.capture());
        Grade grade = captor.getValue();
        assertThat(grade.getType()).isEqualTo(GradeType.QUIZ);
        assertThat(grade.getScore()).isEqualTo(2.0);
        assertThat(grade.getMaxScore()).isEqualTo(6.0);
        assertThat(grade.isPublished()).isFalse();
        assertThat(grade.getQuizAttempt()).isSameAs(attempt);
        assertThat(grade.getEnrollment()).isSameAs(enrollment);

        assertThat(result.status()).isEqualTo(AttemptStatus.SUBMITTED);
    }

    @Test
    @DisplayName("Nộp bài trắng (không trả lời câu nào) -> 0 điểm")
    void submit_allBlank() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        QuizAttempt attempt = runningAttempt(openQuiz());
        stubGradeSave();

        service.submit(STUDENT_ID, ATTEMPT_ID);

        assertThat(attempt.getScore()).isEqualTo(0.0);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
    }

    @Test
    @DisplayName("Trả lời đúng cả 3 câu -> điểm tối đa 6")
    void submit_allCorrect() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = openQuiz();
        QuizAttempt attempt = runningAttempt(quiz);
        when(answerRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(List.of(
                savedAnswer(attempt, quiz.getQuestions().get(0), option(quiz, 1, 1)),
                savedAnswer(attempt, quiz.getQuestions().get(1), option(quiz, 2, 1)),
                savedAnswer(attempt, quiz.getQuestions().get(2), option(quiz, 3, 1))));
        stubGradeSave();

        service.submit(STUDENT_ID, ATTEMPT_ID);

        assertThat(attempt.getScore()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("Câu đã bỏ chọn được tính là bỏ trống, không bị chấm sai")
    void submit_clearedAnswerIsBlank() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = openQuiz();
        QuizAttempt attempt = runningAttempt(quiz);
        QuizAnswer cleared = savedAnswer(attempt, quiz.getQuestions().get(0), option(quiz, 1, 1));
        cleared.setSelectedOptions(new HashSet<>());
        when(answerRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(List.of(cleared));
        stubGradeSave();

        service.submit(STUDENT_ID, ATTEMPT_ID);

        assertThat(attempt.getScore()).isEqualTo(0.0);
        assertThat(cleared.getCorrect()).isNull();
    }

    @Test
    @DisplayName("Hết giờ rồi mới nộp: chốt bằng các câu đã lưu, thời điểm nộp ghi nhận là hạn chót")
    void submit_afterDeadline() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Quiz quiz = openQuiz();
        QuizAttempt attempt = overdueAttempt(quiz);
        LocalDateTime deadline = attempt.getDeadlineAt();
        when(answerRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(List.of(
                savedAnswer(attempt, quiz.getQuestions().get(0), option(quiz, 1, 1))));
        stubGradeSave();

        QuizResultResponse result = service.submit(STUDENT_ID, ATTEMPT_ID);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(attempt.getSubmittedAt()).isEqualTo(deadline);
        assertThat(attempt.getScore()).isEqualTo(2.0);
        assertThat(result.status()).isEqualTo(AttemptStatus.EXPIRED);
    }

    @Test
    @DisplayName("Nộp lại bài đã nộp: trả kết quả cũ, không chấm lại hay tạo thêm Grade")
    void submit_idempotent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        QuizAttempt attempt = stubAttempt(attempt(openQuiz(), STUDENT_ID, AttemptStatus.SUBMITTED,
                LocalDateTime.now().plusMinutes(5)));
        attempt.setScore(4.0);

        QuizResultResponse result = service.submit(STUDENT_ID, ATTEMPT_ID);

        assertThat(result.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(attempt.getScore()).isEqualTo(4.0);
        verify(gradeRepository, never()).save(any());
        verify(answerRepository, never()).findByAttemptId(any());
    }

    @Test
    @DisplayName("Nộp bài của người khác -> ForbiddenException")
    void submit_otherStudentForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        QuizAttempt attempt = stubAttempt(attempt(openQuiz(), STUDENT_ID, AttemptStatus.IN_PROGRESS,
                LocalDateTime.now().plusMinutes(5)));

        assertThatThrownBy(() -> service.submit(OTHER_STUDENT_ID, ATTEMPT_ID)).isInstanceOf(ForbiddenException.class);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Giảng viên nộp bài thay sinh viên -> ForbiddenException")
    void submit_lecturerForbidden() {
        asUser(LECTURER_ID, RoleName.LECTURER);

        assertThatThrownBy(() -> service.submit(LECTURER_ID, ATTEMPT_ID)).isInstanceOf(ForbiddenException.class);

        verify(attemptRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Nộp lượt không tồn tại -> ResourceNotFoundException")
    void submit_notFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(attemptRepository.findByIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ATTEMPT_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- kết quả cho sinh viên ----------

    @Test
    @DisplayName("Điểm đã chấm nhưng chưa công bố: sinh viên không thấy điểm")
    void result_unpublishedHidesScore() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        QuizAttempt attempt = stubAttempt(attempt(openQuiz(), STUDENT_ID, AttemptStatus.SUBMITTED,
                LocalDateTime.now().minusMinutes(1)));
        attempt.setScore(4.0);
        Grade grade = new Grade();
        grade.setScore(4.0);
        grade.setMaxScore(6.0);
        grade.setPublished(false);
        when(gradeRepository.findByQuizAttemptId(ATTEMPT_ID)).thenReturn(Optional.of(grade));

        QuizResultResponse result = service.getMyResult(STUDENT_ID, ATTEMPT_ID);

        assertThat(result.gradePublished()).isFalse();
        assertThat(result.score()).isNull();
        assertThat(result.maxScore()).isNull();
        assertThat(result.toString()).doesNotContain("4.0");
    }

    @Test
    @DisplayName("Điểm đã công bố: sinh viên thấy điểm, vẫn không có đáp án đúng")
    void result_publishedShowsScoreOnly() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubAttempt(attempt(openQuiz(), STUDENT_ID, AttemptStatus.SUBMITTED, LocalDateTime.now().minusMinutes(1)));
        Grade grade = new Grade();
        grade.setScore(4.0);
        grade.setMaxScore(6.0);
        grade.setPublished(true);
        when(gradeRepository.findByQuizAttemptId(ATTEMPT_ID)).thenReturn(Optional.of(grade));

        QuizResultResponse result = service.getMyResult(STUDENT_ID, ATTEMPT_ID);

        assertThat(result.gradePublished()).isTrue();
        assertThat(result.score()).isEqualTo(4.0);
        assertThat(result.maxScore()).isEqualTo(6.0);
        assertThat(Arrays.stream(QuizResultResponse.class.getRecordComponents()).map(RecordComponent::getName))
                .noneMatch(n -> n.toLowerCase().contains("correct") || n.toLowerCase().contains("explanation"));
    }

    @Test
    @DisplayName("Xem kết quả khi bài còn đang làm -> BusinessException")
    void result_stillInProgress() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        runningAttempt(openQuiz());

        assertThatThrownBy(() -> service.getMyResult(STUDENT_ID, ATTEMPT_ID)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Xem kết quả của bài đã quá hạn: bài được chốt trước rồi trả kết quả")
    void result_overdueIsFinalized() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        QuizAttempt attempt = overdueAttempt(openQuiz());
        stubGradeSave();

        QuizResultResponse result = service.getMyResult(STUDENT_ID, ATTEMPT_ID);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(result.status()).isEqualTo(AttemptStatus.EXPIRED);
    }

    @Test
    @DisplayName("Xem kết quả của người khác -> ForbiddenException")
    void result_otherStudentForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        stubAttempt(attempt(openQuiz(), STUDENT_ID, AttemptStatus.SUBMITTED, LocalDateTime.now().minusMinutes(1)));

        assertThatThrownBy(() -> service.getMyResult(OTHER_STUDENT_ID, ATTEMPT_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(gradeRepository, never()).findByQuizAttemptId(any());
    }

    @Test
    @DisplayName("Sinh viên liệt kê các lượt làm bài của chính mình")
    void listMyAttempts() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Quiz quiz = openQuiz();
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        QuizAttempt second = attempt(quiz, STUDENT_ID, AttemptStatus.IN_PROGRESS, LocalDateTime.now().plusMinutes(5));
        second.setAttemptNumber(2);
        QuizAttempt first = attempt(quiz, STUDENT_ID, AttemptStatus.SUBMITTED, LocalDateTime.now().minusDays(1));
        when(attemptRepository.findByQuizIdAndStudentIdOrderByAttemptNumberDesc(QUIZ_ID, STUDENT_ID))
                .thenReturn(List.of(second, first));

        List<QuizResultResponse> results = service.listMyAttempts(STUDENT_ID, QUIZ_ID);

        assertThat(results).extracting(QuizResultResponse::attemptNumber).containsExactly(2, 1);
        assertThat(results).allMatch(r -> r.score() == null);
    }

    @Test
    @DisplayName("Liệt kê lượt làm của bài kiểm tra không tồn tại -> ResourceNotFoundException")
    void listMyAttempts_quizNotFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listMyAttempts(STUDENT_ID, QUIZ_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- Admin / giảng viên ----------

    @Test
    @DisplayName("Giảng viên phụ trách xem các lượt làm bài kèm điểm, kể cả chưa công bố")
    void staff_listAttempts() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        Quiz quiz = quiz(section(SectionStatus.IN_PROGRESS, lecturer));
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        QuizAttempt attempt = attempt(quiz, STUDENT_ID, AttemptStatus.SUBMITTED, LocalDateTime.now().minusMinutes(1));
        attempt.setScore(4.0);
        when(attemptRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(attempt));
        Grade grade = new Grade();
        grade.setId(60L);
        grade.setQuizAttempt(attempt);
        grade.setPublished(false);
        when(gradeRepository.findByQuizAttemptOfQuiz(QUIZ_ID)).thenReturn(List.of(grade));

        List<QuizAttemptStaffResponse> result = service.listAttempts(LECTURER_ID, QUIZ_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isEqualTo(4.0);
        assertThat(result.get(0).maxScore()).isEqualTo(6.0);
        assertThat(result.get(0).gradeId()).isEqualTo(60L);
        assertThat(result.get(0).gradePublished()).isFalse();
    }

    @Test
    @DisplayName("Giảng viên không phụ trách xem các lượt làm bài -> ForbiddenException")
    void staff_listAttemptsOtherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        when(quizRepository.findById(QUIZ_ID))
                .thenReturn(Optional.of(quiz(section(SectionStatus.IN_PROGRESS, assigned))));

        assertThatThrownBy(() -> service.listAttempts(OTHER_LECTURER_ID, QUIZ_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Sinh viên xem điểm của cả lớp qua API giảng viên -> ForbiddenException")
    void staff_listAttemptsStudentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(openQuiz()));

        assertThatThrownBy(() -> service.listAttempts(STUDENT_ID, QUIZ_ID)).isInstanceOf(ForbiddenException.class);

        verify(attemptRepository, never()).findByQuizId(any());
    }

    @Test
    @DisplayName("Công bố điểm bài kiểm tra: chỉ công bố các điểm chưa công bố")
    void staff_publishQuizGrades() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        when(quizRepository.findById(QUIZ_ID))
                .thenReturn(Optional.of(quiz(section(SectionStatus.IN_PROGRESS, lecturer))));
        Grade pendingOne = new Grade();
        Grade pendingTwo = new Grade();
        Grade done = new Grade();
        done.setPublished(true);
        LocalDateTime publishedAt = LocalDateTime.now().minusDays(1);
        done.setPublishedAt(publishedAt);
        when(gradeRepository.findByQuizAttemptOfQuiz(QUIZ_ID)).thenReturn(List.of(pendingOne, pendingTwo, done));

        int count = service.publishQuizGrades(LECTURER_ID, QUIZ_ID);

        assertThat(count).isEqualTo(2);
        assertThat(pendingOne.isPublished()).isTrue();
        assertThat(pendingOne.getPublishedBy()).isSameAs(lecturer);
        assertThat(pendingTwo.getPublishedAt()).isNotNull();
        assertThat(done.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách công bố điểm bài kiểm tra -> ForbiddenException")
    void staff_publishQuizGradesForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        when(quizRepository.findById(QUIZ_ID))
                .thenReturn(Optional.of(quiz(section(SectionStatus.IN_PROGRESS, assigned))));

        assertThatThrownBy(() -> service.publishQuizGrades(OTHER_LECTURER_ID, QUIZ_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(gradeRepository, never()).findByQuizAttemptOfQuiz(any());
    }

    // ---------- tự động chốt bài quá hạn ----------

    @Test
    @DisplayName("Tác vụ định kỳ chốt lượt quá hạn và bỏ qua lượt đã nộp hoặc chưa hết giờ")
    void autoSubmit_finalizesOnlyOverdue() {
        stubEnrollmentFor(STUDENT_ID);
        Quiz quiz = openQuiz();
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(3);
        QuizAttempt overdue = attempt(quiz, STUDENT_ID, AttemptStatus.IN_PROGRESS, deadline);
        overdue.setId(1L);
        QuizAttempt submitted = attempt(quiz, STUDENT_ID, AttemptStatus.SUBMITTED, deadline);
        submitted.setId(2L);
        QuizAttempt stillRunning = attempt(quiz, STUDENT_ID, AttemptStatus.IN_PROGRESS,
                LocalDateTime.now().plusMinutes(5));
        stillRunning.setId(3L);
        when(attemptRepository.findIdsByStatusAndDeadlineAtBefore(eq(AttemptStatus.IN_PROGRESS), any()))
                .thenReturn(List.of(1L, 2L, 3L));
        when(attemptRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(overdue));
        when(attemptRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(submitted));
        when(attemptRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(stillRunning));
        stubGradeSave();

        int count = service.autoSubmitExpiredAttempts();

        assertThat(count).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(overdue.getSubmittedAt()).isEqualTo(deadline);
        assertThat(submitted.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(stillRunning.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Một lượt lỗi khi tự chốt không làm các lượt khác bị bỏ sót")
    void autoSubmit_continuesAfterFailure() {
        stubEnrollmentFor(STUDENT_ID);
        Quiz quiz = openQuiz();
        QuizAttempt overdue = attempt(quiz, STUDENT_ID, AttemptStatus.IN_PROGRESS, LocalDateTime.now().minusMinutes(3));
        overdue.setId(2L);
        when(attemptRepository.findIdsByStatusAndDeadlineAtBefore(eq(AttemptStatus.IN_PROGRESS), any()))
                .thenReturn(List.of(1L, 2L));
        when(attemptRepository.findByIdForUpdate(1L)).thenThrow(new IllegalStateException("db error"));
        when(attemptRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(overdue));
        stubGradeSave();

        int count = service.autoSubmitExpiredAttempts();

        assertThat(count).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
    }

    @Test
    @DisplayName("Không có lượt quá hạn -> không làm gì")
    void autoSubmit_nothingToDo() {
        when(attemptRepository.findIdsByStatusAndDeadlineAtBefore(eq(AttemptStatus.IN_PROGRESS), any()))
                .thenReturn(List.of());

        assertThat(service.autoSubmitExpiredAttempts()).isZero();

        verify(gradeRepository, never()).save(any());
    }

    private void stubEnrollmentFor(Long studentId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(70L);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, SECTION_ID))
                .thenReturn(Optional.of(enrollment));
    }
}
