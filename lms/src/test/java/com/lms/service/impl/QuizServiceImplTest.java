package com.lms.service.impl;

import com.lms.dto.quiz.CreateQuizRequest;
import com.lms.dto.quiz.QuizOptionRequest;
import com.lms.dto.quiz.QuizQuestionRequest;
import com.lms.dto.quiz.QuizResponse;
import com.lms.dto.quiz.QuizSummaryResponse;
import com.lms.dto.quiz.UpdateQuizRequest;
import com.lms.entity.AnswerOption;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Question;
import com.lms.entity.Quiz;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.QuestionType;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
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
class QuizServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long SECTION_ID = 20L;
    private static final Long QUIZ_ID = 50L;

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private QuizAttemptRepository attemptRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    private QuizServiceImpl service;

    @BeforeEach
    void setUp() {
        SectionAccessChecker access = new SectionAccessChecker(userRepository, enrollmentRepository);
        service = new QuizServiceImpl(quizRepository, sectionRepository, attemptRepository, access);
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

    private CourseSection stubSection(SectionStatus status, User teacher) {
        CourseSection section = section(status, teacher);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        return section;
    }

    private Question question(Quiz quiz, long id, double score) {
        Question question = new Question();
        question.setId(id);
        question.setQuiz(quiz);
        question.setContent("Câu " + id);
        question.setType(QuestionType.SINGLE_CHOICE);
        question.setScore(score);
        question.setOrderIndex((int) id);
        question.setExplanation("Lời giải " + id);
        for (int i = 1; i <= 3; i++) {
            AnswerOption option = new AnswerOption();
            option.setId(id * 10 + i);
            option.setQuestion(question);
            option.setContent("Lựa chọn " + i);
            option.setCorrect(i == 1);
            option.setOrderIndex(i);
            question.getOptions().add(option);
        }
        return question;
    }

    private Quiz quiz(CourseSection section, boolean published, int questionCount) {
        Quiz quiz = new Quiz();
        quiz.setId(QUIZ_ID);
        quiz.setSection(section);
        quiz.setTitle("Kiểm tra 1");
        quiz.setDurationMinutes(30);
        quiz.setMaxAttempts(1);
        quiz.setPublished(published);
        quiz.setQuestions(new ArrayList<>());
        for (int i = 1; i <= questionCount; i++) {
            quiz.getQuestions().add(question(quiz, i, 2.0));
        }
        return quiz;
    }

    private Quiz stubQuiz(CourseSection section, boolean published, int questionCount) {
        Quiz quiz = quiz(section, published, questionCount);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        return quiz;
    }

    private QuizQuestionRequest questionRequest(Double score, QuizOptionRequest... options) {
        return new QuizQuestionRequest("  2 + 2 = ? ", score, "Vì 2 + 2 = 4", List.of(options));
    }

    private QuizQuestionRequest validQuestion() {
        return questionRequest(2.0, new QuizOptionRequest("4", true), new QuizOptionRequest("5", false),
                new QuizOptionRequest("6", false));
    }

    private CreateQuizRequest createRequest(LocalDateTime start, LocalDateTime end, Integer maxAttempts,
                                            List<QuizQuestionRequest> questions) {
        return new CreateQuizRequest(SECTION_ID, "  Kiểm tra 1 ", "Mô tả", 30, start, end, maxAttempts, false, questions);
    }

    private UpdateQuizRequest updateRequest(int duration, int maxAttempts) {
        return new UpdateQuizRequest("Tiêu đề mới", "Mô tả mới", duration, null, null, maxAttempts, true);
    }

    // ---------- create ----------

    @Test
    @DisplayName("Admin tạo bài kiểm tra: mặc định chưa công bố, 1 lần làm, điểm tối đa là tổng điểm các câu")
    void create_adminSuccess() {
        User admin = asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizResponse response = service.create(ADMIN_ID, createRequest(null, null, null,
                List.of(validQuestion(), validQuestion())));

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isSameAs(admin);
        assertThat(captor.getValue().getTitle()).isEqualTo("Kiểm tra 1");
        assertThat(response.published()).isFalse();
        assertThat(response.maxAttempts()).isEqualTo(1);
        assertThat(response.durationMinutes()).isEqualTo(30);
        assertThat(response.maxScore()).isEqualTo(4.0);
        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().get(0).options()).extracting(QuizResponse.OptionDetail::correct)
                .containsExactly(true, false, false);
        assertThat(response.questions().get(0).orderIndex()).isEqualTo(1);
        assertThat(response.questions().get(1).orderIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("Giảng viên phụ trách tạo bài kiểm tra có khung giờ và nhiều lần làm; điểm câu null là 1 điểm")
    void create_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSection(SectionStatus.IN_PROGRESS, lecturer);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        QuizResponse response = service.create(LECTURER_ID, createRequest(start, start.plusHours(2), 3,
                List.of(questionRequest(null, new QuizOptionRequest("A", true), new QuizOptionRequest("B", false)))));

        assertThat(response.maxAttempts()).isEqualTo(3);
        assertThat(response.startTime()).isEqualTo(start);
        assertThat(response.maxScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Tạo bài kiểm tra chưa có câu hỏi vẫn được (công bố sau)")
    void create_withoutQuestions() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(ADMIN_ID, createRequest(null, null, null, null)).questions()).isEmpty();
    }

    @Test
    @DisplayName("Câu hỏi không có đáp án đúng -> BusinessException")
    void create_noCorrectOption() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null, List.of(
                questionRequest(1.0, new QuizOptionRequest("A", false), new QuizOptionRequest("B", false))))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đúng một đáp án");

        verify(quizRepository, never()).save(any());
    }

    @Test
    @DisplayName("Câu hỏi có hai đáp án đúng -> BusinessException")
    void create_twoCorrectOptions() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null, List.of(
                questionRequest(1.0, new QuizOptionRequest("A", true), new QuizOptionRequest("B", true))))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đúng một đáp án");
    }

    @Test
    @DisplayName("Câu hỏi chỉ có một lựa chọn -> BusinessException")
    void create_tooFewOptions() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null, List.of(
                questionRequest(1.0, new QuizOptionRequest("A", true))))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("lựa chọn");
    }

    @Test
    @DisplayName("Câu hỏi có hơn 6 lựa chọn -> BusinessException")
    void create_tooManyOptions() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);
        QuizOptionRequest[] options = new QuizOptionRequest[7];
        for (int i = 0; i < 7; i++) {
            options[i] = new QuizOptionRequest("Lựa chọn " + i, i == 0);
        }

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null,
                List.of(questionRequest(1.0, options)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Điểm câu hỏi không dương hoặc lựa chọn trống -> BusinessException")
    void create_invalidScoreAndBlankOption() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null, List.of(
                questionRequest(0.0, new QuizOptionRequest("A", true), new QuizOptionRequest("B", false))))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null, List.of(
                questionRequest(1.0, new QuizOptionRequest("A", true), new QuizOptionRequest("  ", false))))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Thời điểm đóng không sau thời điểm mở -> BusinessException")
    void create_invalidWindow() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);
        LocalDateTime start = LocalDateTime.now().plusDays(2);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(start, start.minusHours(1), null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sau thời điểm mở");
    }

    @Test
    @DisplayName("Thời điểm đóng đã qua -> BusinessException")
    void create_endTimeInPast() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);

        assertThatThrownBy(() -> service.create(ADMIN_ID,
                createRequest(null, LocalDateTime.now().minusHours(1), null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách tạo bài kiểm tra -> ForbiddenException")
    void create_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSection(SectionStatus.IN_PROGRESS, assigned);

        assertThatThrownBy(() -> service.create(OTHER_LECTURER_ID, createRequest(null, null, null, null)))
                .isInstanceOf(ForbiddenException.class);

        verify(quizRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên tạo bài kiểm tra -> ForbiddenException")
    void create_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(SectionStatus.IN_PROGRESS, null);

        assertThatThrownBy(() -> service.create(STUDENT_ID, createRequest(null, null, null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Tạo bài kiểm tra cho lớp không tồn tại -> ResourceNotFoundException")
    void create_sectionNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tạo bài kiểm tra cho lớp đã ENDED -> BusinessException")
    void create_sectionEnded() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.ENDED, null);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kết thúc");
    }

    // ---------- update ----------

    @Test
    @DisplayName("Cập nhật bài kiểm tra chưa có lượt làm bài: đổi được mọi cấu hình")
    void update_noAttempts() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), false, 1);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(false);

        QuizResponse response = service.update(ADMIN_ID, QUIZ_ID, updateRequest(60, 2));

        assertThat(quiz.getDurationMinutes()).isEqualTo(60);
        assertThat(quiz.getMaxAttempts()).isEqualTo(2);
        assertThat(response.title()).isEqualTo("Tiêu đề mới");
        assertThat(response.shuffleQuestions()).isTrue();
    }

    @Test
    @DisplayName("Đã có lượt làm bài: vẫn đổi được tiêu đề và khung giờ")
    void update_withAttemptsAllowedChanges() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), true, 1);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(true);
        LocalDateTime end = LocalDateTime.now().plusDays(3);

        service.update(ADMIN_ID, QUIZ_ID, new UpdateQuizRequest("Tiêu đề mới", null, 30, null, end, 1, false));

        assertThat(quiz.getTitle()).isEqualTo("Tiêu đề mới");
        assertThat(quiz.getEndTime()).isEqualTo(end);
    }

    @Test
    @DisplayName("Đã có lượt làm bài: đổi thời lượng -> BusinessException")
    void update_withAttemptsDurationLocked() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), true, 1);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(ADMIN_ID, QUIZ_ID, updateRequest(90, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("thời lượng");

        assertThat(quiz.getDurationMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("Đã có lượt làm bài: đổi số lần được làm -> BusinessException")
    void update_withAttemptsMaxAttemptsLocked() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), true, 1);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(ADMIN_ID, QUIZ_ID, updateRequest(30, 3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("số lần");

        assertThat(quiz.getMaxAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách cập nhật bài kiểm tra -> ForbiddenException")
    void update_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubQuiz(section(SectionStatus.IN_PROGRESS, assigned), false, 1);

        assertThatThrownBy(() -> service.update(OTHER_LECTURER_ID, QUIZ_ID, updateRequest(30, 1)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Cập nhật bài kiểm tra không tồn tại -> ResourceNotFoundException")
    void update_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ADMIN_ID, QUIZ_ID, updateRequest(30, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Cập nhật bài kiểm tra của lớp đã ENDED -> BusinessException")
    void update_sectionEnded() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubQuiz(section(SectionStatus.ENDED, null), false, 1);

        assertThatThrownBy(() -> service.update(ADMIN_ID, QUIZ_ID, updateRequest(30, 1)))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- replaceQuestions ----------

    @Test
    @DisplayName("Thay câu hỏi khi chưa có lượt làm bài")
    void replaceQuestions_success() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), false, 3);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(false);

        QuizResponse response = service.replaceQuestions(ADMIN_ID, QUIZ_ID, List.of(validQuestion()));

        assertThat(quiz.getQuestions()).hasSize(1);
        assertThat(response.questions()).hasSize(1);
        assertThat(response.maxScore()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Thay câu hỏi khi đã có lượt làm bài -> BusinessException, câu hỏi giữ nguyên")
    void replaceQuestions_withAttempts() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), true, 3);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.replaceQuestions(ADMIN_ID, QUIZ_ID, List.of(validQuestion())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("lượt làm bài");

        assertThat(quiz.getQuestions()).hasSize(3);
    }

    @Test
    @DisplayName("Xóa hết câu hỏi của bài đã công bố -> BusinessException")
    void replaceQuestions_publishedCannotBeEmpty() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), true, 2);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.replaceQuestions(ADMIN_ID, QUIZ_ID, List.of()))
                .isInstanceOf(BusinessException.class);

        assertThat(quiz.getQuestions()).hasSize(2);
    }

    @Test
    @DisplayName("Câu hỏi mới không hợp lệ -> BusinessException, câu hỏi cũ giữ nguyên")
    void replaceQuestions_invalidKeepsOld() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), false, 2);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.replaceQuestions(ADMIN_ID, QUIZ_ID, List.of(
                questionRequest(1.0, new QuizOptionRequest("A", false), new QuizOptionRequest("B", false)))))
                .isInstanceOf(BusinessException.class);

        assertThat(quiz.getQuestions()).hasSize(2);
    }

    // ---------- publish / unpublish ----------

    @Test
    @DisplayName("Công bố bài kiểm tra có câu hỏi")
    void publish_success() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, lecturer), false, 2);

        assertThat(service.publish(LECTURER_ID, QUIZ_ID).published()).isTrue();
        assertThat(quiz.isPublished()).isTrue();
    }

    @Test
    @DisplayName("Công bố bài kiểm tra chưa có câu hỏi -> BusinessException")
    void publish_noQuestions() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), false, 0);

        assertThatThrownBy(() -> service.publish(ADMIN_ID, QUIZ_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("câu hỏi");

        assertThat(quiz.isPublished()).isFalse();
    }

    @Test
    @DisplayName("Công bố bài đã công bố -> BusinessException")
    void publish_alreadyPublished() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubQuiz(section(SectionStatus.IN_PROGRESS, null), true, 1);

        assertThatThrownBy(() -> service.publish(ADMIN_ID, QUIZ_ID)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách công bố bài kiểm tra -> ForbiddenException")
    void publish_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, assigned), false, 1);

        assertThatThrownBy(() -> service.publish(OTHER_LECTURER_ID, QUIZ_ID)).isInstanceOf(ForbiddenException.class);

        assertThat(quiz.isPublished()).isFalse();
    }

    @Test
    @DisplayName("Hủy công bố bài chưa có lượt làm bài")
    void unpublish_success() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubQuiz(section(SectionStatus.IN_PROGRESS, null), true, 1);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(false);

        assertThat(service.unpublish(ADMIN_ID, QUIZ_ID).published()).isFalse();
    }

    @Test
    @DisplayName("Hủy công bố bài đã có lượt làm bài -> BusinessException")
    void unpublish_withAttempts() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), true, 1);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.unpublish(ADMIN_ID, QUIZ_ID)).isInstanceOf(BusinessException.class);

        assertThat(quiz.isPublished()).isTrue();
    }

    @Test
    @DisplayName("Hủy công bố bài chưa công bố -> BusinessException")
    void unpublish_notPublished() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubQuiz(section(SectionStatus.IN_PROGRESS, null), false, 1);

        assertThatThrownBy(() -> service.unpublish(ADMIN_ID, QUIZ_ID)).isInstanceOf(BusinessException.class);
    }

    // ---------- delete ----------

    @Test
    @DisplayName("Xóa bài kiểm tra chưa có lượt làm bài")
    void delete_success() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Quiz quiz = stubQuiz(section(SectionStatus.IN_PROGRESS, null), false, 1);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(false);

        service.delete(ADMIN_ID, QUIZ_ID);

        verify(quizRepository).delete(quiz);
    }

    @Test
    @DisplayName("Xóa bài kiểm tra đã có lượt làm bài -> BusinessException, giữ lịch sử")
    void delete_withAttempts() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubQuiz(section(SectionStatus.IN_PROGRESS, null), true, 1);
        when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(ADMIN_ID, QUIZ_ID)).isInstanceOf(BusinessException.class);

        verify(quizRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Giảng viên không phụ trách xóa bài kiểm tra -> ForbiddenException")
    void delete_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubQuiz(section(SectionStatus.IN_PROGRESS, assigned), false, 1);

        assertThatThrownBy(() -> service.delete(OTHER_LECTURER_ID, QUIZ_ID)).isInstanceOf(ForbiddenException.class);

        verify(quizRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa bài kiểm tra không tồn tại -> ResourceNotFoundException")
    void delete_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(ADMIN_ID, QUIZ_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- getForManagement / listBySection ----------

    @Test
    @DisplayName("Giảng viên phụ trách xem chi tiết bài kiểm tra kèm đáp án đúng và lời giải")
    void getForManagement_lecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubQuiz(section(SectionStatus.IN_PROGRESS, lecturer), true, 1);

        QuizResponse response = service.getForManagement(LECTURER_ID, QUIZ_ID);

        assertThat(response.questions().get(0).explanation()).isEqualTo("Lời giải 1");
        assertThat(response.questions().get(0).options()).anyMatch(QuizResponse.OptionDetail::correct);
    }

    @Test
    @DisplayName("Sinh viên xem chi tiết quản lý (có đáp án) -> ForbiddenException")
    void getForManagement_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubQuiz(section(SectionStatus.IN_PROGRESS, null), true, 1);

        assertThatThrownBy(() -> service.getForManagement(STUDENT_ID, QUIZ_ID)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Admin xem danh sách mọi bài kiểm tra của lớp, kể cả chưa công bố")
    void list_admin() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(SectionStatus.IN_PROGRESS, null);
        when(quizRepository.findBySectionId(SECTION_ID))
                .thenReturn(List.of(quiz(section, true, 2), quiz(section, false, 1)));

        List<QuizSummaryResponse> result = service.listBySection(ADMIN_ID, SECTION_ID);

        assertThat(result).extracting(QuizSummaryResponse::published).containsExactly(true, false);
        assertThat(result).extracting(QuizSummaryResponse::questionCount).containsExactly(2, 1);
    }

    @Test
    @DisplayName("Sinh viên đang theo học chỉ thấy bài kiểm tra đã công bố")
    void list_enrolledStudentSeesPublishedOnly() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubSection(SectionStatus.IN_PROGRESS, null);
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.of(enrollment));
        when(quizRepository.findBySectionIdAndPublishedTrue(SECTION_ID)).thenReturn(List.of(quiz(section, true, 2)));

        assertThat(service.listBySection(STUDENT_ID, SECTION_ID)).hasSize(1);
        verify(quizRepository, never()).findBySectionId(any());
    }

    @Test
    @DisplayName("Sinh viên chưa đăng ký lớp xem danh sách bài kiểm tra -> ForbiddenException")
    void list_notEnrolledStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(SectionStatus.IN_PROGRESS, null);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listBySection(STUDENT_ID, SECTION_ID)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách xem danh sách bài kiểm tra -> ForbiddenException")
    void list_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSection(SectionStatus.IN_PROGRESS, assigned);

        assertThatThrownBy(() -> service.listBySection(OTHER_LECTURER_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);
    }
}
