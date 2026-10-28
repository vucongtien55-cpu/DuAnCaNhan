package com.lms.service.impl;

import com.lms.dto.grade.FinalGradeResponse;
import com.lms.dto.grade.FinalGradeStatus;
import com.lms.dto.grade.GradeResponse;
import com.lms.dto.grade.GradeWeightRequest;
import com.lms.dto.grade.GradeWeightResponse;
import com.lms.dto.grade.StudentFinalGradeResponse;
import com.lms.entity.Assignment;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Grade;
import com.lms.entity.Quiz;
import com.lms.entity.Role;
import com.lms.entity.SectionGradeWeight;
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
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.GradeRepository;
import com.lms.repository.QuizRepository;
import com.lms.repository.SectionGradeWeightRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
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
class FinalGradeServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long OTHER_STUDENT_ID = 5L;
    private static final Long SECTION_ID = 20L;

    @Mock
    private SectionGradeWeightRepository weightRepository;
    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;

    private FinalGradeServiceImpl service;

    @BeforeEach
    void setUp() {
        SectionAccessChecker access = new SectionAccessChecker(userRepository, enrollmentRepository);
        OfficialGradeResolver resolver = new OfficialGradeResolver(submissionRepository, gradeRepository);
        service = new FinalGradeServiceImpl(weightRepository, sectionRepository, assignmentRepository, quizRepository,
                enrollmentRepository, gradeRepository, resolver, access);
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

    private CourseSection stubSectionForUpdate(User teacher) {
        CourseSection section = section(teacher);
        when(sectionRepository.findByIdForUpdate(SECTION_ID)).thenReturn(Optional.of(section));
        return section;
    }

    private CourseSection stubSection(User teacher) {
        CourseSection section = section(teacher);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        return section;
    }

    private GradeWeightRequest request(GradeType type, String weight) {
        return new GradeWeightRequest(type, weight == null ? null : new BigDecimal(weight));
    }

    private SectionGradeWeight weightRow(GradeType type, String weight) {
        SectionGradeWeight row = new SectionGradeWeight();
        row.setGradeType(type);
        row.setWeight(new BigDecimal(weight));
        return row;
    }

    private void stubWeights(SectionGradeWeight... rows) {
        when(weightRepository.findBySectionId(SECTION_ID)).thenReturn(List.of(rows));
    }

    private Enrollment enrollment(Long id, Long studentId, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setStudent(user(studentId, true, RoleName.STUDENT));
        enrollment.setStatus(status);
        return enrollment;
    }

    private Assignment assignment(long id, GradingPolicy policy) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setGradingPolicy(policy);
        return assignment;
    }

    private Grade grade(GradeType type, double score, double max, boolean published) {
        Grade grade = new Grade();
        grade.setType(type);
        grade.setScore(score);
        grade.setMaxScore(max);
        grade.setPublished(published);
        if (published) {
            grade.setPublishedAt(LocalDateTime.now().minusDays(1));
        }
        return grade;
    }

    /** Sinh viên có bài nộp mới nhất (chính sách LATEST) với điểm đã chấm. */
    private void stubAssignmentGrade(Assignment assignment, Long studentId, double score, double max,
                                     boolean published) {
        Submission submission = new Submission();
        submission.setId(assignment.getId() * 100 + studentId);
        submission.setAttemptNumber(1);
        Grade grade = grade(GradeType.ASSIGNMENT, score, max, published);
        grade.setSubmission(submission);
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignment.getId(),
                studentId)).thenReturn(Optional.of(submission));
        when(gradeRepository.findBySubmissionId(submission.getId())).thenReturn(Optional.of(grade));
    }

    private Grade quizGrade(Enrollment enrollment, double score, double max, boolean published) {
        Grade grade = grade(GradeType.QUIZ, score, max, published);
        grade.setEnrollment(enrollment);
        return grade;
    }

    private Quiz stubQuiz(long id, Grade... grades) {
        Quiz quiz = new Quiz();
        quiz.setId(id);
        when(gradeRepository.findByQuizAttemptOfQuiz(id)).thenReturn(List.of(grades));
        return quiz;
    }

    /** Dữ liệu của lớp: danh sách bài tập, bài kiểm tra, sinh viên và các điểm tổng kết đã lưu. */
    private void stubSectionData(List<Assignment> assignments, List<Quiz> quizzes, List<Enrollment> enrollments,
                                 List<Grade> existingTotals) {
        when(assignmentRepository.findBySectionIdOrderByDueDateAsc(SECTION_ID)).thenReturn(assignments);
        when(quizRepository.findBySectionId(SECTION_ID)).thenReturn(quizzes);
        when(enrollmentRepository.findBySectionId(SECTION_ID)).thenReturn(enrollments);
        when(gradeRepository.findBySectionIdAndType(SECTION_ID, GradeType.COURSE_TOTAL)).thenReturn(existingTotals);
    }

    private Grade existingTotal(Enrollment enrollment, double score, boolean published) {
        Grade total = grade(GradeType.COURSE_TOTAL, score, 10.0, published);
        total.setId(900L);
        total.setEnrollment(enrollment);
        return total;
    }

    private void stubGradeSave() {
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- cấu hình trọng số: hợp lệ ----------

    @Test
    @DisplayName("Admin cấu hình trọng số 60% bài tập, 40% trắc nghiệm (tổng 100%)")
    void setWeights_success() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        when(weightRepository.save(any(SectionGradeWeight.class))).thenAnswer(inv -> inv.getArgument(0));

        List<GradeWeightResponse> result = service.setWeights(ADMIN_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "60"), request(GradeType.QUIZ, "40")));

        assertThat(result).extracting(GradeWeightResponse::gradeType)
                .containsExactly(GradeType.ASSIGNMENT, GradeType.QUIZ);
        assertThat(result).extracting(w -> w.weight().toPlainString()).containsExactly("60.00", "40.00");
    }

    @Test
    @DisplayName("Tổng trọng số có phần thập phân (33.30 + 66.70) được so sánh chính xác bằng 100")
    void setWeights_exactDecimalSum() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSectionForUpdate(lecturer);
        when(weightRepository.save(any(SectionGradeWeight.class))).thenAnswer(inv -> inv.getArgument(0));

        List<GradeWeightResponse> result = service.setWeights(LECTURER_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "33.30"), request(GradeType.QUIZ, "66.7")));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Cấu hình lại: cập nhật tại chỗ thành phần đã có và xóa thành phần không còn")
    void setWeights_replacesExisting() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        SectionGradeWeight assignmentRow = weightRow(GradeType.ASSIGNMENT, "50");
        SectionGradeWeight quizRow = weightRow(GradeType.QUIZ, "50");
        stubWeights(assignmentRow, quizRow);
        when(weightRepository.save(any(SectionGradeWeight.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setWeights(ADMIN_ID, SECTION_ID, List.of(request(GradeType.ASSIGNMENT, "100")));

        assertThat(assignmentRow.getWeight()).isEqualByComparingTo("100");
        ArgumentCaptor<Collection<SectionGradeWeight>> removed = ArgumentCaptor.forClass(Collection.class);
        verify(weightRepository).deleteAll(removed.capture());
        assertThat(removed.getValue()).containsExactly(quizRow);
    }

    // ---------- cấu hình trọng số: không hợp lệ ----------

    @Test
    @DisplayName("Tổng trọng số 99% -> BusinessException, không lưu gì")
    void setWeights_totalBelow100() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);

        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "60"), request(GradeType.QUIZ, "39"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100%")
                .hasMessageContaining("99%");

        verify(weightRepository, never()).save(any());
        verify(weightRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Tổng trọng số 101% hoặc 100.01% -> BusinessException")
    void setWeights_totalAbove100() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);

        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "60"), request(GradeType.QUIZ, "41"))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "60.01"), request(GradeType.QUIZ, "40"))))
                .isInstanceOf(BusinessException.class);

        verify(weightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chỉ một thành phần nhưng không đủ 100% -> BusinessException")
    void setWeights_singleComponentNot100() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);

        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID, List.of(request(GradeType.ASSIGNMENT, "80"))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Thành phần khai báo trùng -> BusinessException")
    void setWeights_duplicateComponent() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);

        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "50"), request(GradeType.ASSIGNMENT, "50"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("trùng");
    }

    @Test
    @DisplayName("Thành phần chưa hỗ trợ (MIDTERM) -> BusinessException")
    void setWeights_unsupportedComponent() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);

        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "50"), request(GradeType.MIDTERM, "50"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MIDTERM");
    }

    @Test
    @DisplayName("Trọng số 0, âm, hơn 100, quá 2 chữ số thập phân hoặc thiếu giá trị -> BusinessException")
    void setWeights_invalidValues() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);

        for (String bad : new String[]{"0", "-10", "100.5", "33.333"}) {
            assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID,
                    List.of(request(GradeType.ASSIGNMENT, bad), request(GradeType.QUIZ, "50"))))
                    .as("weight " + bad)
                    .isInstanceOf(BusinessException.class);
        }
        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, null), request(GradeType.QUIZ, "50"))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID, List.of(request(null, "100"))))
                .isInstanceOf(BusinessException.class);

        verify(weightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Danh sách trọng số rỗng hoặc null -> BusinessException")
    void setWeights_emptyList() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);

        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID, List.of()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Đã có điểm tổng kết công bố: không đổi được trọng số")
    void setWeights_blockedWhenFinalGradesPublished() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        Enrollment enrollment = enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED);
        when(gradeRepository.findBySectionIdAndType(SECTION_ID, GradeType.COURSE_TOTAL))
                .thenReturn(List.of(existingTotal(enrollment, 8.0, true)));

        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "60"), request(GradeType.QUIZ, "40"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hủy công bố");

        verify(weightRepository, never()).save(any());
    }

    // ---------- cấu hình trọng số: quyền ----------

    @Test
    @DisplayName("Giảng viên không phụ trách cấu hình trọng số -> ForbiddenException")
    void setWeights_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSectionForUpdate(assigned);

        assertThatThrownBy(() -> service.setWeights(OTHER_LECTURER_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "100"))))
                .isInstanceOf(ForbiddenException.class);

        verify(weightRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên cấu hình trọng số -> ForbiddenException")
    void setWeights_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSectionForUpdate(null);

        assertThatThrownBy(() -> service.setWeights(STUDENT_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "100"))))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Cấu hình trọng số cho lớp không tồn tại -> ResourceNotFoundException")
    void setWeights_sectionNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findByIdForUpdate(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "100"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tài khoản bị khóa cấu hình trọng số -> ForbiddenException")
    void setWeights_disabledUser() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, false, RoleName.ADMIN)));

        assertThatThrownBy(() -> service.setWeights(ADMIN_ID, SECTION_ID,
                List.of(request(GradeType.ASSIGNMENT, "100"))))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Giảng viên phụ trách xem trọng số")
    void getWeights_manager() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSection(lecturer);
        stubWeights(weightRow(GradeType.QUIZ, "40"), weightRow(GradeType.ASSIGNMENT, "60"));

        assertThat(service.getWeights(LECTURER_ID, SECTION_ID)).extracting(GradeWeightResponse::gradeType)
                .containsExactly(GradeType.ASSIGNMENT, GradeType.QUIZ);
    }

    @Test
    @DisplayName("Sinh viên đang theo học xem trọng số của lớp mình")
    void getWeights_enrolledStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(null);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.of(enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED)));
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));

        assertThat(service.getWeights(STUDENT_ID, SECTION_ID)).hasSize(1);
    }

    @Test
    @DisplayName("Sinh viên không thuộc lớp xem trọng số -> ForbiddenException")
    void getWeights_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(null);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWeights(STUDENT_ID, SECTION_ID)).isInstanceOf(ForbiddenException.class);
    }

    // ---------- tính điểm tổng kết ----------

    @Test
    @DisplayName("Tính đúng: bài tập 70% x 60 + trắc nghiệm 100% x 40 = 8.20/10, lưu Grade COURSE_TOTAL chưa công bố")
    void calculate_correctWeightedScore() {
        User admin = asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "60"), weightRow(GradeType.QUIZ, "40"));
        Enrollment enrollment = enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED);
        Assignment first = assignment(30, GradingPolicy.LATEST);
        Assignment second = assignment(31, GradingPolicy.LATEST);
        stubAssignmentGrade(first, STUDENT_ID, 8, 10, true);
        stubAssignmentGrade(second, STUDENT_ID, 6, 10, true);
        Quiz quiz = stubQuiz(50, quizGrade(enrollment, 4, 5, true), quizGrade(enrollment, 5, 5, true));
        stubSectionData(List.of(first, second), List.of(quiz), List.of(enrollment), List.of());
        stubGradeSave();

        List<FinalGradeResponse> results = service.calculate(ADMIN_ID, SECTION_ID);

        assertThat(results).hasSize(1);
        FinalGradeResponse result = results.get(0);
        assertThat(result.status()).isEqualTo(FinalGradeStatus.COMPUTED);
        assertThat(result.score()).isEqualTo(8.2);
        assertThat(result.maxScore()).isEqualTo(10.0);
        assertThat(result.published()).isFalse();
        assertThat(result.missingComponents()).isEmpty();
        assertThat(result.components()).extracting(FinalGradeResponse.ComponentResult::type)
                .containsExactly(GradeType.ASSIGNMENT, GradeType.QUIZ);
        assertThat(result.components().get(0).percent()).isEqualByComparingTo("70.00");
        assertThat(result.components().get(0).itemCount()).isEqualTo(2);
        assertThat(result.components().get(1).percent()).isEqualByComparingTo("100.00");
        assertThat(result.components().get(1).itemCount()).isEqualTo(1);

        ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
        verify(gradeRepository).save(captor.capture());
        Grade saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(GradeType.COURSE_TOTAL);
        assertThat(saved.getScore()).isEqualTo(8.2);
        assertThat(saved.getMaxScore()).isEqualTo(10.0);
        assertThat(saved.isPublished()).isFalse();
        assertThat(saved.getEnrollment()).isSameAs(enrollment);
        assertThat(saved.getGradedBy()).isSameAs(admin);
    }

    @Test
    @DisplayName("Làm tròn HALF_UP đến 2 chữ số: 7.345 -> 7.35, 7.344 -> 7.34")
    void calculate_roundingHalfUp() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));
        Assignment assignment = assignment(30, GradingPolicy.LATEST);
        stubAssignmentGrade(assignment, STUDENT_ID, 7.345, 10, true);
        stubAssignmentGrade(assignment, OTHER_STUDENT_ID, 7.344, 10, true);
        stubSectionData(List.of(assignment), List.of(),
                List.of(enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED),
                        enrollment(71L, OTHER_STUDENT_ID, EnrollmentStatus.ENROLLED)), List.of());
        stubGradeSave();

        List<FinalGradeResponse> results = service.calculate(ADMIN_ID, SECTION_ID);

        assertThat(results).extracting(FinalGradeResponse::score).containsExactly(7.35, 7.34);
    }

    @Test
    @DisplayName("Điểm các bài tập có thang điểm khác nhau được quy về tỷ lệ trước khi lấy trung bình")
    void calculate_normalizesDifferentMaxScores() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));
        Assignment tenPoint = assignment(30, GradingPolicy.LATEST);
        Assignment hundredPoint = assignment(31, GradingPolicy.LATEST);
        stubAssignmentGrade(tenPoint, STUDENT_ID, 10, 10, true);
        stubAssignmentGrade(hundredPoint, STUDENT_ID, 50, 100, true);
        stubSectionData(List.of(tenPoint, hundredPoint), List.of(),
                List.of(enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED)), List.of());
        stubGradeSave();

        assertThat(service.calculate(ADMIN_ID, SECTION_ID).get(0).score()).isEqualTo(7.5);
    }

    @Test
    @DisplayName("Điểm bài tập chưa công bố không được đưa vào tính")
    void calculate_ignoresUnpublishedItems() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));
        Assignment published = assignment(30, GradingPolicy.LATEST);
        Assignment unpublished = assignment(31, GradingPolicy.LATEST);
        stubAssignmentGrade(published, STUDENT_ID, 8, 10, true);
        stubAssignmentGrade(unpublished, STUDENT_ID, 2, 10, false);
        stubSectionData(List.of(published, unpublished), List.of(),
                List.of(enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED)), List.of());
        stubGradeSave();

        FinalGradeResponse result = service.calculate(ADMIN_ID, SECTION_ID).get(0);

        assertThat(result.score()).isEqualTo(8.0);
        assertThat(result.components().get(0).itemCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Chính sách HIGHEST: dùng điểm công bố cao nhất của bài tập")
    void calculate_assignmentHighestPolicy() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));
        Assignment assignment = assignment(30, GradingPolicy.HIGHEST);
        Submission first = new Submission();
        first.setAttemptNumber(1);
        Submission second = new Submission();
        second.setAttemptNumber(2);
        Grade low = grade(GradeType.ASSIGNMENT, 6, 10, true);
        low.setSubmission(first);
        Grade high = grade(GradeType.ASSIGNMENT, 9, 10, true);
        high.setSubmission(second);
        when(gradeRepository.findBySubmissionOfAssignmentAndStudent(30L, STUDENT_ID)).thenReturn(List.of(low, high));
        stubSectionData(List.of(assignment), List.of(),
                List.of(enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED)), List.of());
        stubGradeSave();

        assertThat(service.calculate(ADMIN_ID, SECTION_ID).get(0).score()).isEqualTo(9.0);
    }

    @Test
    @DisplayName("Quiz: lấy lượt có điểm công bố cao nhất, bỏ qua lượt cao hơn nhưng chưa công bố")
    void calculate_quizBestPublishedAttempt() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.QUIZ, "100"));
        Enrollment enrollment = enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED);
        Quiz quiz = stubQuiz(50, quizGrade(enrollment, 5, 5, false), quizGrade(enrollment, 3, 5, true));
        stubSectionData(List.of(), List.of(quiz), List.of(enrollment), List.of());
        stubGradeSave();

        assertThat(service.calculate(ADMIN_ID, SECTION_ID).get(0).score()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("Điểm quiz của sinh viên khác không bị lẫn vào")
    void calculate_quizIgnoresOtherStudents() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.QUIZ, "100"));
        Enrollment mine = enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED);
        Enrollment other = enrollment(71L, OTHER_STUDENT_ID, EnrollmentStatus.ENROLLED);
        Quiz quiz = stubQuiz(50, quizGrade(other, 5, 5, true));
        stubSectionData(List.of(), List.of(quiz), List.of(mine, other), List.of());
        stubGradeSave();

        List<FinalGradeResponse> results = service.calculate(ADMIN_ID, SECTION_ID);

        assertThat(results.get(0).status()).isEqualTo(FinalGradeStatus.INCOMPLETE);
        assertThat(results.get(1).status()).isEqualTo(FinalGradeStatus.COMPUTED);
        assertThat(results.get(1).score()).isEqualTo(10.0);
    }

    // ---------- thiếu điểm thành phần ----------

    @Test
    @DisplayName("Thiếu điểm trắc nghiệm đã công bố: INCOMPLETE, không coi là 0, không lưu điểm tổng kết")
    void calculate_missingComponentIsNotZero() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "60"), weightRow(GradeType.QUIZ, "40"));
        Assignment assignment = assignment(30, GradingPolicy.LATEST);
        stubAssignmentGrade(assignment, STUDENT_ID, 10, 10, true);
        Enrollment enrollment = enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED);
        stubSectionData(List.of(assignment), List.of(), List.of(enrollment), List.of());

        FinalGradeResponse result = service.calculate(ADMIN_ID, SECTION_ID).get(0);

        assertThat(result.status()).isEqualTo(FinalGradeStatus.INCOMPLETE);
        assertThat(result.score()).isNull();
        assertThat(result.maxScore()).isNull();
        assertThat(result.gradeId()).isNull();
        assertThat(result.missingComponents()).containsExactly(GradeType.QUIZ);
        assertThat(result.components()).extracting(FinalGradeResponse.ComponentResult::type)
                .containsExactly(GradeType.ASSIGNMENT);
        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thành phần chỉ có điểm chưa công bố cũng coi là thiếu")
    void calculate_onlyUnpublishedMeansMissing() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));
        Assignment assignment = assignment(30, GradingPolicy.LATEST);
        stubAssignmentGrade(assignment, STUDENT_ID, 10, 10, false);
        stubSectionData(List.of(assignment), List.of(),
                List.of(enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED)), List.of());

        FinalGradeResponse result = service.calculate(ADMIN_ID, SECTION_ID).get(0);

        assertThat(result.status()).isEqualTo(FinalGradeStatus.INCOMPLETE);
        assertThat(result.missingComponents()).containsExactly(GradeType.ASSIGNMENT);
        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên chưa nộp bài nào: thành phần bài tập thiếu, không bị tính 0")
    void calculate_noSubmissionMeansMissing() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));
        Assignment assignment = assignment(30, GradingPolicy.LATEST);
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(30L, STUDENT_ID))
                .thenReturn(Optional.empty());
        stubSectionData(List.of(assignment), List.of(),
                List.of(enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED)), List.of());

        assertThat(service.calculate(ADMIN_ID, SECTION_ID).get(0).status()).isEqualTo(FinalGradeStatus.INCOMPLETE);
    }

    @Test
    @DisplayName("Điểm tổng kết cũ chưa công bố mà nay thiếu thành phần: bị xóa vì không còn đúng")
    void calculate_staleUnpublishedTotalIsRemoved() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));
        Assignment assignment = assignment(30, GradingPolicy.LATEST);
        stubAssignmentGrade(assignment, STUDENT_ID, 10, 10, false);
        Enrollment enrollment = enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED);
        Grade stale = existingTotal(enrollment, 9.0, false);
        stubSectionData(List.of(assignment), List.of(), List.of(enrollment), List.of(stale));

        FinalGradeResponse result = service.calculate(ADMIN_ID, SECTION_ID).get(0);

        assertThat(result.status()).isEqualTo(FinalGradeStatus.INCOMPLETE);
        verify(gradeRepository).delete(stale);
    }

    @Test
    @DisplayName("Tính lại khi đã có điểm tổng kết chưa công bố: cập nhật đúng dòng cũ, không tạo thêm")
    void calculate_updatesExistingUnpublishedTotal() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));
        Assignment assignment = assignment(30, GradingPolicy.LATEST);
        stubAssignmentGrade(assignment, STUDENT_ID, 9, 10, true);
        Enrollment enrollment = enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED);
        Grade existing = existingTotal(enrollment, 5.0, false);
        stubSectionData(List.of(assignment), List.of(), List.of(enrollment), List.of(existing));
        stubGradeSave();

        FinalGradeResponse result = service.calculate(ADMIN_ID, SECTION_ID).get(0);

        assertThat(result.status()).isEqualTo(FinalGradeStatus.COMPUTED);
        assertThat(result.gradeId()).isEqualTo(900L);
        assertThat(existing.getScore()).isEqualTo(9.0);
        verify(gradeRepository).save(existing);
    }

    @Test
    @DisplayName("Điểm tổng kết đã công bố: LOCKED, giữ nguyên, không tính lại")
    void calculate_publishedTotalIsLocked() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));
        Assignment assignment = assignment(30, GradingPolicy.LATEST);
        Enrollment enrollment = enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED);
        Grade published = existingTotal(enrollment, 7.5, true);
        stubSectionData(List.of(assignment), List.of(), List.of(enrollment), List.of(published));

        FinalGradeResponse result = service.calculate(ADMIN_ID, SECTION_ID).get(0);

        assertThat(result.status()).isEqualTo(FinalGradeStatus.LOCKED);
        assertThat(result.score()).isEqualTo(7.5);
        assertThat(result.published()).isTrue();
        assertThat(published.getScore()).isEqualTo(7.5);
        verify(gradeRepository, never()).save(any());
        verify(gradeRepository, never()).delete(any());
        verify(submissionRepository, never()).findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(any(), any());
    }

    @Test
    @DisplayName("Sinh viên đã rút bị loại, sinh viên đã hoàn thành (COMPLETED) vẫn được tính")
    void calculate_skipsDroppedStudents() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "100"));
        Assignment assignment = assignment(30, GradingPolicy.LATEST);
        stubAssignmentGrade(assignment, STUDENT_ID, 8, 10, true);
        stubSectionData(List.of(assignment), List.of(), List.of(
                enrollment(70L, STUDENT_ID, EnrollmentStatus.COMPLETED),
                enrollment(71L, OTHER_STUDENT_ID, EnrollmentStatus.DROPPED)), List.of());
        stubGradeSave();

        List<FinalGradeResponse> results = service.calculate(ADMIN_ID, SECTION_ID);

        assertThat(results).extracting(FinalGradeResponse::studentId).containsExactly(STUDENT_ID);
    }

    // ---------- xác thực cấu hình trước khi tính ----------

    @Test
    @DisplayName("Lớp chưa cấu hình trọng số -> BusinessException, không tính")
    void calculate_noConfiguration() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights();

        assertThatThrownBy(() -> service.calculate(ADMIN_ID, SECTION_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa cấu hình");

        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cấu hình đã lưu có tổng khác 100% -> BusinessException trước khi tính")
    void calculate_invalidStoredConfiguration() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "50"), weightRow(GradeType.QUIZ, "40"));

        assertThatThrownBy(() -> service.calculate(ADMIN_ID, SECTION_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100%");

        verify(gradeRepository, never()).save(any());
        verify(enrollmentRepository, never()).findBySectionId(any());
    }

    @Test
    @DisplayName("Cấu hình đã lưu có thành phần chưa hỗ trợ -> BusinessException")
    void calculate_unsupportedStoredComponent() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        stubWeights(weightRow(GradeType.ASSIGNMENT, "50"), weightRow(GradeType.MIDTERM, "50"));

        assertThatThrownBy(() -> service.calculate(ADMIN_ID, SECTION_ID)).isInstanceOf(BusinessException.class);
    }

    // ---------- tính điểm: quyền ----------

    @Test
    @DisplayName("Giảng viên không phụ trách tính điểm tổng kết -> ForbiddenException")
    void calculate_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSectionForUpdate(assigned);

        assertThatThrownBy(() -> service.calculate(OTHER_LECTURER_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(weightRepository, never()).findBySectionId(any());
    }

    @Test
    @DisplayName("Sinh viên tính điểm tổng kết -> ForbiddenException")
    void calculate_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSectionForUpdate(null);

        assertThatThrownBy(() -> service.calculate(STUDENT_ID, SECTION_ID)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Tính điểm cho lớp không tồn tại -> ResourceNotFoundException")
    void calculate_sectionNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findByIdForUpdate(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(ADMIN_ID, SECTION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- xem danh sách, công bố ----------

    @Test
    @DisplayName("Giảng viên phụ trách xem điểm tổng kết đã lưu của cả lớp, kể cả chưa công bố")
    void list_manager() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSection(lecturer);
        Grade unpublished = existingTotal(enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED), 8.0, false);
        Grade published = existingTotal(enrollment(71L, OTHER_STUDENT_ID, EnrollmentStatus.ENROLLED), 9.0, true);
        when(gradeRepository.findBySectionIdAndType(SECTION_ID, GradeType.COURSE_TOTAL))
                .thenReturn(List.of(unpublished, published));

        List<GradeResponse> result = service.listFinalGrades(LECTURER_ID, SECTION_ID);

        assertThat(result).extracting(GradeResponse::published).containsExactly(false, true);
    }

    @Test
    @DisplayName("Sinh viên xem điểm tổng kết cả lớp -> ForbiddenException")
    void list_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(null);

        assertThatThrownBy(() -> service.listFinalGrades(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(gradeRepository, never()).findBySectionIdAndType(any(), any());
    }

    @Test
    @DisplayName("Công bố điểm tổng kết: chỉ công bố các điểm chưa công bố")
    void publishFinalGrades() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSectionForUpdate(lecturer);
        Grade pending = existingTotal(enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED), 8.0, false);
        Grade done = existingTotal(enrollment(71L, OTHER_STUDENT_ID, EnrollmentStatus.ENROLLED), 9.0, true);
        LocalDateTime publishedAt = done.getPublishedAt();
        when(gradeRepository.findBySectionIdAndType(SECTION_ID, GradeType.COURSE_TOTAL))
                .thenReturn(List.of(pending, done));

        int count = service.publishFinalGrades(LECTURER_ID, SECTION_ID);

        assertThat(count).isEqualTo(1);
        assertThat(pending.isPublished()).isTrue();
        assertThat(pending.getPublishedBy()).isSameAs(lecturer);
        assertThat(pending.getPublishedAt()).isNotNull();
        assertThat(done.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    @DisplayName("Hủy công bố điểm tổng kết")
    void unpublishFinalGrades() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSectionForUpdate(null);
        Grade published = existingTotal(enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED), 8.0, true);
        Grade unpublished = existingTotal(enrollment(71L, OTHER_STUDENT_ID, EnrollmentStatus.ENROLLED), 9.0, false);
        when(gradeRepository.findBySectionIdAndType(SECTION_ID, GradeType.COURSE_TOTAL))
                .thenReturn(List.of(published, unpublished));

        int count = service.unpublishFinalGrades(ADMIN_ID, SECTION_ID);

        assertThat(count).isEqualTo(1);
        assertThat(published.isPublished()).isFalse();
        assertThat(published.getPublishedAt()).isNull();
        assertThat(published.getPublishedBy()).isNull();
    }

    @Test
    @DisplayName("Giảng viên không phụ trách công bố hoặc hủy công bố điểm tổng kết -> ForbiddenException")
    void publish_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSectionForUpdate(assigned);

        assertThatThrownBy(() -> service.publishFinalGrades(OTHER_LECTURER_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.unpublishFinalGrades(OTHER_LECTURER_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(gradeRepository, never()).findBySectionIdAndType(any(), any());
    }

    @Test
    @DisplayName("Sinh viên tự công bố điểm tổng kết -> ForbiddenException")
    void publish_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSectionForUpdate(null);

        assertThatThrownBy(() -> service.publishFinalGrades(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- sinh viên xem điểm tổng kết ----------

    private Enrollment stubOwnEnrollment() {
        Enrollment enrollment = enrollment(70L, STUDENT_ID, EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.of(enrollment));
        return enrollment;
    }

    @Test
    @DisplayName("Điểm tổng kết đã công bố: sinh viên thấy điểm của chính mình")
    void myFinalGrade_published() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Enrollment enrollment = stubOwnEnrollment();
        when(gradeRepository.findByEnrollmentIdAndType(70L, GradeType.COURSE_TOTAL))
                .thenReturn(Optional.of(existingTotal(enrollment, 8.25, true)));

        StudentFinalGradeResponse response = service.getMyFinalGrade(STUDENT_ID, SECTION_ID);

        assertThat(response.published()).isTrue();
        assertThat(response.score()).isEqualTo(8.25);
        assertThat(response.maxScore()).isEqualTo(10.0);
        assertThat(response.publishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Điểm tổng kết đã tính nhưng chưa công bố: sinh viên không thấy điểm")
    void myFinalGrade_unpublishedHidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Enrollment enrollment = stubOwnEnrollment();
        when(gradeRepository.findByEnrollmentIdAndType(70L, GradeType.COURSE_TOTAL))
                .thenReturn(Optional.of(existingTotal(enrollment, 8.25, false)));

        StudentFinalGradeResponse response = service.getMyFinalGrade(STUDENT_ID, SECTION_ID);

        assertThat(response.published()).isFalse();
        assertThat(response.score()).isNull();
        assertThat(response.maxScore()).isNull();
        assertThat(response.publishedAt()).isNull();
        assertThat(response.toString()).doesNotContain("8.25");
    }

    @Test
    @DisplayName("Chưa có điểm tổng kết: sinh viên chỉ nhận trạng thái chưa công bố")
    void myFinalGrade_notCalculated() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubOwnEnrollment();
        when(gradeRepository.findByEnrollmentIdAndType(70L, GradeType.COURSE_TOTAL)).thenReturn(Optional.empty());

        assertThat(service.getMyFinalGrade(STUDENT_ID, SECTION_ID).published()).isFalse();
    }

    @Test
    @DisplayName("Sinh viên không có đăng ký trong lớp xem điểm tổng kết -> ForbiddenException")
    void myFinalGrade_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyFinalGrade(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(gradeRepository, never()).findByEnrollmentIdAndType(any(), any());
    }

    @Test
    @DisplayName("Giảng viên dùng API điểm tổng kết của sinh viên -> ForbiddenException")
    void myFinalGrade_nonStudentForbidden() {
        asUser(LECTURER_ID, RoleName.LECTURER);

        assertThatThrownBy(() -> service.getMyFinalGrade(LECTURER_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);
    }
}
