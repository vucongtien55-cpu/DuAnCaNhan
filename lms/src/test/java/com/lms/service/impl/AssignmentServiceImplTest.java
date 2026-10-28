package com.lms.service.impl;

import com.lms.dto.assignment.AssignmentResponse;
import com.lms.dto.assignment.CreateAssignmentRequest;
import com.lms.dto.assignment.UpdateAssignmentRequest;
import com.lms.entity.Assignment;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.GradingPolicy;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.AssignmentRepository;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.SubmissionRepository;
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
class AssignmentServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long SECTION_ID = 20L;
    private static final Long ASSIGNMENT_ID = 30L;

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    private AssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        SectionAccessChecker access = new SectionAccessChecker(userRepository, enrollmentRepository);
        service = new AssignmentServiceImpl(assignmentRepository, sectionRepository, submissionRepository, access);
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
        section.setSectionCode("JAVA101-ABC123");
        section.setTeacher(teacher);
        section.setStatus(status);
        return section;
    }

    private CourseSection stubSection(SectionStatus status, User teacher) {
        CourseSection section = section(status, teacher);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        return section;
    }

    private Assignment assignment(CourseSection section) {
        Assignment assignment = new Assignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setSection(section);
        assignment.setTitle("Bài 1");
        assignment.setMaxScore(10.0);
        assignment.setMaxSubmissions(3);
        assignment.setAllowLateSubmission(true);
        assignment.setGradingPolicy(GradingPolicy.LATEST);
        return assignment;
    }

    private Assignment stubForUpdate(CourseSection section) {
        Assignment assignment = assignment(section);
        when(assignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        return assignment;
    }

    private CreateAssignmentRequest createRequest(LocalDateTime dueDate, Boolean allowLate, GradingPolicy policy) {
        return new CreateAssignmentRequest(SECTION_ID, "  Bài 1 ", "Mô tả", dueDate, 10.0, 3, allowLate, policy);
    }

    private UpdateAssignmentRequest updateRequest(double maxScore, Integer maxSubmissions, GradingPolicy policy) {
        return new UpdateAssignmentRequest("Tiêu đề mới", "Mô tả mới", LocalDateTime.now().plusDays(7),
                maxScore, maxSubmissions, false, policy);
    }

    // ---------- create ----------

    @Test
    @DisplayName("Admin tạo bài tập: mặc định cho nộp muộn và chọn bài nộp mới nhất")
    void create_adminWithDefaults() {
        User admin = asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignmentResponse response = service.create(ADMIN_ID, createRequest(LocalDateTime.now().plusDays(3), null, null));

        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isSameAs(admin);
        assertThat(captor.getValue().getTitle()).isEqualTo("Bài 1");
        assertThat(response.allowLateSubmission()).isTrue();
        assertThat(response.gradingPolicy()).isEqualTo(GradingPolicy.LATEST);
        assertThat(response.maxScore()).isEqualTo(10.0);
        assertThat(response.maxSubmissions()).isEqualTo(3);
        assertThat(response.sectionCode()).isEqualTo("JAVA101-ABC123");
    }

    @Test
    @DisplayName("Giảng viên phụ trách tạo bài tập với cấu hình tự chọn")
    void create_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSection(SectionStatus.IN_PROGRESS, lecturer);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignmentResponse response = service.create(LECTURER_ID, createRequest(null, false, GradingPolicy.MANUAL));

        assertThat(response.allowLateSubmission()).isFalse();
        assertThat(response.gradingPolicy()).isEqualTo(GradingPolicy.MANUAL);
        assertThat(response.dueDate()).isNull();
        assertThat(response.createdById()).isEqualTo(LECTURER_ID);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách tạo bài tập -> ForbiddenException")
    void create_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSection(SectionStatus.IN_PROGRESS, assigned);

        assertThatThrownBy(() -> service.create(OTHER_LECTURER_ID, createRequest(null, null, null)))
                .isInstanceOf(ForbiddenException.class);

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên tạo bài tập -> ForbiddenException")
    void create_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(SectionStatus.IN_PROGRESS, null);

        assertThatThrownBy(() -> service.create(STUDENT_ID, createRequest(null, null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Tài khoản bị khóa tạo bài tập -> ForbiddenException")
    void create_disabledUser() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, false, RoleName.ADMIN)));

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Tạo bài tập cho lớp không tồn tại -> ResourceNotFoundException")
    void create_sectionNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tạo bài tập cho lớp đã ENDED -> BusinessException")
    void create_sectionEnded() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.ENDED, null);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kết thúc");
    }

    @Test
    @DisplayName("Tạo bài tập có hạn nộp trong quá khứ -> BusinessException")
    void create_dueDateInPast() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.IN_PROGRESS, null);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(LocalDateTime.now().minusDays(1), null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Hạn nộp");

        verify(assignmentRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    @DisplayName("Cập nhật bài tập chưa có bài nộp: đổi được mọi cấu hình")
    void update_noSubmissions() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        Assignment assignment = stubForUpdate(section(SectionStatus.IN_PROGRESS, lecturer));
        when(submissionRepository.existsByAssignmentId(ASSIGNMENT_ID)).thenReturn(false);

        AssignmentResponse response = service.update(LECTURER_ID, ASSIGNMENT_ID, updateRequest(20.0, 5, GradingPolicy.MANUAL));

        assertThat(assignment.getMaxScore()).isEqualTo(20.0);
        assertThat(assignment.getGradingPolicy()).isEqualTo(GradingPolicy.MANUAL);
        assertThat(response.title()).isEqualTo("Tiêu đề mới");
        assertThat(response.allowLateSubmission()).isFalse();
    }

    @Test
    @DisplayName("Đã có bài nộp: vẫn đổi được tiêu đề, hạn nộp và tăng số lần nộp")
    void update_withSubmissionsAllowedChanges() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubForUpdate(section(SectionStatus.IN_PROGRESS, null));
        when(submissionRepository.existsByAssignmentId(ASSIGNMENT_ID)).thenReturn(true);
        when(submissionRepository.findMaxAttemptNumberByAssignmentId(ASSIGNMENT_ID)).thenReturn(2);

        service.update(ADMIN_ID, ASSIGNMENT_ID, updateRequest(10.0, 2, GradingPolicy.LATEST));

        assertThat(assignment.getTitle()).isEqualTo("Tiêu đề mới");
        assertThat(assignment.getMaxSubmissions()).isEqualTo(2);
        assertThat(assignment.getDueDate()).isNotNull();
    }

    @Test
    @DisplayName("Đã có bài nộp: đổi điểm tối đa -> BusinessException")
    void update_withSubmissionsMaxScoreLocked() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubForUpdate(section(SectionStatus.IN_PROGRESS, null));
        when(submissionRepository.existsByAssignmentId(ASSIGNMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(ADMIN_ID, ASSIGNMENT_ID, updateRequest(100.0, 3, GradingPolicy.LATEST)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("điểm tối đa");

        assertThat(assignment.getMaxScore()).isEqualTo(10.0);
        assertThat(assignment.getTitle()).isEqualTo("Bài 1");
    }

    @Test
    @DisplayName("Đã có bài nộp: đổi chính sách chọn bài chấm -> BusinessException")
    void update_withSubmissionsPolicyLocked() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubForUpdate(section(SectionStatus.IN_PROGRESS, null));
        when(submissionRepository.existsByAssignmentId(ASSIGNMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(ADMIN_ID, ASSIGNMENT_ID, updateRequest(10.0, 3, GradingPolicy.FIRST)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chính sách");

        assertThat(assignment.getGradingPolicy()).isEqualTo(GradingPolicy.LATEST);
    }

    @Test
    @DisplayName("Đã có bài nộp: giảm số lần nộp thấp hơn số lần đã dùng -> BusinessException")
    void update_maxSubmissionsBelowUsed() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubForUpdate(section(SectionStatus.IN_PROGRESS, null));
        when(submissionRepository.existsByAssignmentId(ASSIGNMENT_ID)).thenReturn(true);
        when(submissionRepository.findMaxAttemptNumberByAssignmentId(ASSIGNMENT_ID)).thenReturn(3);

        assertThatThrownBy(() -> service.update(ADMIN_ID, ASSIGNMENT_ID, updateRequest(10.0, 2, GradingPolicy.LATEST)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("số lần");

        assertThat(assignment.getMaxSubmissions()).isEqualTo(3);
    }

    @Test
    @DisplayName("Đã có bài nộp: bỏ giới hạn số lần nộp (null) là hợp lệ")
    void update_withSubmissionsRemoveLimit() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubForUpdate(section(SectionStatus.IN_PROGRESS, null));
        when(submissionRepository.existsByAssignmentId(ASSIGNMENT_ID)).thenReturn(true);

        service.update(ADMIN_ID, ASSIGNMENT_ID, updateRequest(10.0, null, GradingPolicy.LATEST));

        assertThat(assignment.getMaxSubmissions()).isNull();
    }

    @Test
    @DisplayName("Giảng viên không phụ trách cập nhật bài tập -> ForbiddenException")
    void update_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubForUpdate(section(SectionStatus.IN_PROGRESS, assigned));

        assertThatThrownBy(() -> service.update(OTHER_LECTURER_ID, ASSIGNMENT_ID,
                updateRequest(10.0, 3, GradingPolicy.LATEST)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Cập nhật bài tập không tồn tại -> ResourceNotFoundException")
    void update_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(assignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ADMIN_ID, ASSIGNMENT_ID, updateRequest(10.0, 3, GradingPolicy.LATEST)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Cập nhật bài tập của lớp đã ENDED -> BusinessException")
    void update_sectionEnded() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(section(SectionStatus.ENDED, null));

        assertThatThrownBy(() -> service.update(ADMIN_ID, ASSIGNMENT_ID, updateRequest(10.0, 3, GradingPolicy.LATEST)))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- getById / listBySection ----------

    @Test
    @DisplayName("Sinh viên đang theo học xem chi tiết bài tập")
    void getById_enrolledStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(assignmentRepository.findById(ASSIGNMENT_ID))
                .thenReturn(Optional.of(assignment(section(SectionStatus.IN_PROGRESS, null))));
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.of(enrollment));

        assertThat(service.getById(STUDENT_ID, ASSIGNMENT_ID).title()).isEqualTo("Bài 1");
    }

    @Test
    @DisplayName("Sinh viên chưa đăng ký lớp xem chi tiết bài tập -> ForbiddenException")
    void getById_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(assignmentRepository.findById(ASSIGNMENT_ID))
                .thenReturn(Optional.of(assignment(section(SectionStatus.IN_PROGRESS, null))));
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(STUDENT_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Xem bài tập không tồn tại -> ResourceNotFoundException")
    void getById_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Giảng viên phụ trách xem danh sách bài tập của lớp")
    void list_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        CourseSection section = stubSection(SectionStatus.IN_PROGRESS, lecturer);
        when(assignmentRepository.findBySectionIdOrderByDueDateAsc(SECTION_ID))
                .thenReturn(List.of(assignment(section), assignment(section)));

        assertThat(service.listBySection(LECTURER_ID, SECTION_ID)).hasSize(2);
    }

    @Test
    @DisplayName("Sinh viên không thuộc lớp xem danh sách bài tập -> ForbiddenException")
    void list_notEnrolledStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(SectionStatus.IN_PROGRESS, null);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listBySection(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(assignmentRepository, never()).findBySectionIdOrderByDueDateAsc(any());
    }

    // ---------- delete ----------

    @Test
    @DisplayName("Xóa bài tập chưa có bài nộp")
    void delete_success() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubForUpdate(section(SectionStatus.IN_PROGRESS, null));
        when(submissionRepository.existsByAssignmentId(ASSIGNMENT_ID)).thenReturn(false);

        service.delete(ADMIN_ID, ASSIGNMENT_ID);

        verify(assignmentRepository).delete(assignment);
    }

    @Test
    @DisplayName("Xóa bài tập đã có bài nộp -> BusinessException, giữ lịch sử nộp bài")
    void delete_hasSubmissions() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(section(SectionStatus.IN_PROGRESS, null));
        when(submissionRepository.existsByAssignmentId(ASSIGNMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bài nộp");

        verify(assignmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Giảng viên không phụ trách xóa bài tập -> ForbiddenException")
    void delete_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubForUpdate(section(SectionStatus.IN_PROGRESS, assigned));

        assertThatThrownBy(() -> service.delete(OTHER_LECTURER_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(assignmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa bài tập không tồn tại -> ResourceNotFoundException")
    void delete_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(assignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Xóa bài tập của lớp đã ENDED -> BusinessException")
    void delete_sectionEnded() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(section(SectionStatus.ENDED, null));

        assertThatThrownBy(() -> service.delete(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(BusinessException.class);

        verify(assignmentRepository, never()).delete(any());
    }
}
