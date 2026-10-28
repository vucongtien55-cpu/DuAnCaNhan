package com.lms.service.impl;

import com.lms.dto.enrollment.EnrollmentResponse;
import com.lms.entity.Course;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
class EnrollmentServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long OTHER_STUDENT_ID = 5L;
    private static final Long SECTION_ID = 20L;
    private static final String CODE = "JAVA101-ABC123";

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnrollmentServiceImpl service;

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

    private CourseSection section(SectionStatus status, Integer maxStudents, User teacher) {
        Course course = new Course();
        course.setId(10L);
        course.setCode("JAVA101");
        course.setTitle("Java");
        CourseSection section = new CourseSection();
        section.setId(SECTION_ID);
        section.setCourse(course);
        section.setTeacher(teacher);
        section.setSectionCode(CODE);
        section.setName("Lớp 1");
        section.setMaxStudents(maxStudents);
        section.setStatus(status);
        return section;
    }

    /** Lớp để đăng ký bằng mã lớp. */
    private CourseSection stubByCode(SectionStatus status, Integer maxStudents) {
        CourseSection section = section(status, maxStudents, null);
        when(sectionRepository.findBySectionCodeForUpdate(CODE)).thenReturn(Optional.of(section));
        return section;
    }

    /** Lớp để thêm/rút sinh viên theo id. */
    private CourseSection stubById(SectionStatus status, Integer maxStudents, User teacher) {
        CourseSection section = section(status, maxStudents, teacher);
        when(sectionRepository.findByIdForUpdate(SECTION_ID)).thenReturn(Optional.of(section));
        return section;
    }

    private Enrollment enrollment(User student, CourseSection section, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(100L);
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setStatus(status);
        enrollment.setEnrolledAt(LocalDateTime.now().minusDays(10));
        return enrollment;
    }

    private void stubCount(long count) {
        when(enrollmentRepository.countBySectionIdAndStatus(SECTION_ID, EnrollmentStatus.ENROLLED)).thenReturn(count);
    }

    private void stubExisting(Long studentId, Enrollment enrollment) {
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, SECTION_ID))
                .thenReturn(Optional.ofNullable(enrollment));
    }

    // ---------- enroll ----------

    @Test
    @DisplayName("Sinh viên đăng ký bằng mã lớp thành công (chuẩn hóa mã, còn đúng 1 chỗ)")
    void enroll_success() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        stubByCode(SectionStatus.OPEN_REGISTRATION, 30);
        stubExisting(STUDENT_ID, null);
        stubCount(29);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = service.enroll(STUDENT_ID, " java101-abc123 ");

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getStudent()).isSameAs(student);
        assertThat(response.status()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(response.sectionCode()).isEqualTo(CODE);
        assertThat(response.studentId()).isEqualTo(STUDENT_ID);
        assertThat(response.enrolledAt()).isNotNull();
    }

    @Test
    @DisplayName("Lớp không giới hạn sức chứa (maxStudents null) -> không đếm sĩ số")
    void enroll_unlimitedCapacity() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubByCode(SectionStatus.OPEN_REGISTRATION, null);
        stubExisting(STUDENT_ID, null);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.enroll(STUDENT_ID, CODE);

        verify(enrollmentRepository, never()).countBySectionIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("Mã lớp không tồn tại -> ResourceNotFoundException")
    void enroll_codeNotFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(sectionRepository.findBySectionCodeForUpdate("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, "nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Lớp DRAFT chưa mở đăng ký -> BusinessException")
    void enroll_draft() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubByCode(SectionStatus.DRAFT, 30);

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, CODE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không mở đăng ký");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lớp IN_PROGRESS đã đóng đăng ký -> BusinessException")
    void enroll_inProgressClosed() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubByCode(SectionStatus.IN_PROGRESS, 30);

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, CODE))
                .isInstanceOf(BusinessException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng ký trùng lớp đang học -> BusinessException")
    void enroll_duplicate() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubByCode(SectionStatus.OPEN_REGISTRATION, 30);
        stubExisting(STUDENT_ID, enrollment(student, section, EnrollmentStatus.ENROLLED));

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, CODE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã đăng ký");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên đã rút không tự đăng ký lại -> BusinessException, giữ nguyên lịch sử")
    void enroll_previouslyDropped() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubByCode(SectionStatus.OPEN_REGISTRATION, 30);
        Enrollment dropped = enrollment(student, section, EnrollmentStatus.DROPPED);
        stubExisting(STUDENT_ID, dropped);

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, CODE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã rút");

        assertThat(dropped.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lớp đã đủ sĩ số -> BusinessException")
    void enroll_full() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubByCode(SectionStatus.OPEN_REGISTRATION, 30);
        stubExisting(STUDENT_ID, null);
        stubCount(30);

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, CODE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đủ sĩ số");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin không thể tự đăng ký như sinh viên -> ForbiddenException")
    void enroll_nonStudentForbidden() {
        asUser(ADMIN_ID, RoleName.ADMIN);

        assertThatThrownBy(() -> service.enroll(ADMIN_ID, CODE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Sinh viên bị khóa tài khoản không đăng ký được -> ForbiddenException")
    void enroll_disabledStudent() {
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(user(STUDENT_ID, false, RoleName.STUDENT)));

        assertThatThrownBy(() -> service.enroll(STUDENT_ID, CODE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Người đăng ký không tồn tại -> ResourceNotFoundException")
    void enroll_actorNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enroll(99L, CODE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- addStudent ----------

    @Test
    @DisplayName("Admin thêm sinh viên vào lớp đang mở đăng ký")
    void addStudent_adminSuccess() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubById(SectionStatus.OPEN_REGISTRATION, 30, null);
        stubExisting(STUDENT_ID, null);
        stubCount(5);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = service.addStudent(ADMIN_ID, SECTION_ID, STUDENT_ID);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(response.studentId()).isEqualTo(STUDENT_ID);
    }

    @Test
    @DisplayName("Giảng viên phụ trách thêm sinh viên vào lớp đang IN_PROGRESS")
    void addStudent_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubById(SectionStatus.IN_PROGRESS, 30, lecturer);
        stubExisting(STUDENT_ID, null);
        stubCount(5);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = service.addStudent(LECTURER_ID, SECTION_ID, STUDENT_ID);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.ENROLLED);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách thêm sinh viên -> ForbiddenException")
    void addStudent_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubById(SectionStatus.OPEN_REGISTRATION, 30, assigned);

        assertThatThrownBy(() -> service.addStudent(OTHER_LECTURER_ID, SECTION_ID, STUDENT_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên thêm người khác vào lớp -> ForbiddenException")
    void addStudent_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubById(SectionStatus.OPEN_REGISTRATION, 30, null);

        assertThatThrownBy(() -> service.addStudent(STUDENT_ID, SECTION_ID, OTHER_STUDENT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Thêm sinh viên vào lớp DRAFT -> BusinessException")
    void addStudent_draft() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubById(SectionStatus.DRAFT, 30, null);

        assertThatThrownBy(() -> service.addStudent(ADMIN_ID, SECTION_ID, STUDENT_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Thêm sinh viên vào lớp đã ENDED -> BusinessException")
    void addStudent_ended() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubById(SectionStatus.ENDED, 30, null);

        assertThatThrownBy(() -> service.addStudent(ADMIN_ID, SECTION_ID, STUDENT_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Thêm sinh viên không tồn tại -> ResourceNotFoundException")
    void addStudent_studentNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubById(SectionStatus.OPEN_REGISTRATION, 30, null);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addStudent(ADMIN_ID, SECTION_ID, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Thêm người không phải sinh viên -> BusinessException")
    void addStudent_notAStudent() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        asUser(LECTURER_ID, RoleName.LECTURER);
        stubById(SectionStatus.OPEN_REGISTRATION, 30, null);

        assertThatThrownBy(() -> service.addStudent(ADMIN_ID, SECTION_ID, LECTURER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sinh viên");
    }

    @Test
    @DisplayName("Thêm sinh viên đã đăng ký -> BusinessException")
    void addStudent_duplicate() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubById(SectionStatus.OPEN_REGISTRATION, 30, null);
        stubExisting(STUDENT_ID, enrollment(student, section, EnrollmentStatus.ENROLLED));

        assertThatThrownBy(() -> service.addStudent(ADMIN_ID, SECTION_ID, STUDENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã đăng ký");
    }

    @Test
    @DisplayName("Thêm sinh viên vào lớp đã đủ sĩ số -> BusinessException")
    void addStudent_full() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubById(SectionStatus.OPEN_REGISTRATION, 30, null);
        stubExisting(STUDENT_ID, null);
        stubCount(30);

        assertThatThrownBy(() -> service.addStudent(ADMIN_ID, SECTION_ID, STUDENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đủ sĩ số");
    }

    @Test
    @DisplayName("Admin thêm lại sinh viên đã rút: khôi phục bản ghi cũ, không tạo dòng mới")
    void addStudent_restoreDropped() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubById(SectionStatus.IN_PROGRESS, 30, null);
        Enrollment dropped = enrollment(student, section, EnrollmentStatus.DROPPED);
        dropped.setDroppedAt(LocalDateTime.now().minusDays(1));
        dropped.setDropReason("Bận việc");
        stubExisting(STUDENT_ID, dropped);
        stubCount(5);

        EnrollmentResponse response = service.addStudent(ADMIN_ID, SECTION_ID, STUDENT_ID);

        assertThat(dropped.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(dropped.getDroppedAt()).isNull();
        assertThat(dropped.getDropReason()).isNull();
        assertThat(response.id()).isEqualTo(100L);
        verify(enrollmentRepository, never()).save(any());
    }

    // ---------- withdraw ----------

    @Test
    @DisplayName("Sinh viên tự rút lớp: giữ bản ghi, đổi sang DROPPED kèm thời điểm và lý do")
    void withdraw_selfSuccess() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubById(SectionStatus.IN_PROGRESS, 30, null);
        Enrollment enrollment = enrollment(student, section, EnrollmentStatus.ENROLLED);
        stubExisting(STUDENT_ID, enrollment);

        EnrollmentResponse response = service.withdraw(STUDENT_ID, SECTION_ID, STUDENT_ID, "  Trùng lịch ");

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        assertThat(enrollment.getDroppedAt()).isNotNull();
        assertThat(enrollment.getDropReason()).isEqualTo("Trùng lịch");
        assertThat(response.status()).isEqualTo(EnrollmentStatus.DROPPED);
        verify(enrollmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Rút lớp không nêu lý do -> dropReason là null")
    void withdraw_blankReason() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubById(SectionStatus.OPEN_REGISTRATION, 30, null);
        Enrollment enrollment = enrollment(student, section, EnrollmentStatus.ENROLLED);
        stubExisting(STUDENT_ID, enrollment);

        service.withdraw(STUDENT_ID, SECTION_ID, STUDENT_ID, "   ");

        assertThat(enrollment.getDropReason()).isNull();
    }

    @Test
    @DisplayName("Admin gỡ sinh viên khỏi lớp")
    void withdraw_adminRemovesStudent() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        User student = user(STUDENT_ID, true, RoleName.STUDENT);
        CourseSection section = stubById(SectionStatus.IN_PROGRESS, 30, null);
        Enrollment enrollment = enrollment(student, section, EnrollmentStatus.ENROLLED);
        stubExisting(STUDENT_ID, enrollment);

        service.withdraw(ADMIN_ID, SECTION_ID, STUDENT_ID, "Vi phạm quy chế");

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        assertThat(enrollment.getDropReason()).isEqualTo("Vi phạm quy chế");
    }

    @Test
    @DisplayName("Giảng viên phụ trách gỡ sinh viên khỏi lớp")
    void withdraw_assignedLecturerRemovesStudent() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        User student = user(STUDENT_ID, true, RoleName.STUDENT);
        CourseSection section = stubById(SectionStatus.IN_PROGRESS, 30, lecturer);
        Enrollment enrollment = enrollment(student, section, EnrollmentStatus.ENROLLED);
        stubExisting(STUDENT_ID, enrollment);

        service.withdraw(LECTURER_ID, SECTION_ID, STUDENT_ID, null);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
    }

    @Test
    @DisplayName("Sinh viên rút hộ sinh viên khác -> ForbiddenException")
    void withdraw_otherStudentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubById(SectionStatus.IN_PROGRESS, 30, null);

        assertThatThrownBy(() -> service.withdraw(STUDENT_ID, SECTION_ID, OTHER_STUDENT_ID, null))
                .isInstanceOf(ForbiddenException.class);

        verify(enrollmentRepository, never()).findByStudentIdAndSectionId(any(), any());
    }

    @Test
    @DisplayName("Giảng viên không phụ trách gỡ sinh viên -> ForbiddenException")
    void withdraw_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubById(SectionStatus.IN_PROGRESS, 30, assigned);

        assertThatThrownBy(() -> service.withdraw(OTHER_LECTURER_ID, SECTION_ID, STUDENT_ID, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Rút lớp khi chưa có đăng ký -> ResourceNotFoundException")
    void withdraw_enrollmentNotFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubById(SectionStatus.IN_PROGRESS, 30, null);
        stubExisting(STUDENT_ID, null);

        assertThatThrownBy(() -> service.withdraw(STUDENT_ID, SECTION_ID, STUDENT_ID, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rút lớp đã rút từ trước -> BusinessException, không ghi đè thời điểm rút")
    void withdraw_alreadyDropped() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubById(SectionStatus.IN_PROGRESS, 30, null);
        Enrollment dropped = enrollment(student, section, EnrollmentStatus.DROPPED);
        LocalDateTime droppedAt = LocalDateTime.now().minusDays(2);
        dropped.setDroppedAt(droppedAt);
        stubExisting(STUDENT_ID, dropped);

        assertThatThrownBy(() -> service.withdraw(STUDENT_ID, SECTION_ID, STUDENT_ID, "Lại rút"))
                .isInstanceOf(BusinessException.class);

        assertThat(dropped.getDroppedAt()).isEqualTo(droppedAt);
    }

    @Test
    @DisplayName("Rút lớp đã hoàn thành (COMPLETED) -> BusinessException")
    void withdraw_completed() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubById(SectionStatus.IN_PROGRESS, 30, null);
        Enrollment completed = enrollment(student, section, EnrollmentStatus.COMPLETED);
        stubExisting(STUDENT_ID, completed);

        assertThatThrownBy(() -> service.withdraw(STUDENT_ID, SECTION_ID, STUDENT_ID, null))
                .isInstanceOf(BusinessException.class);

        assertThat(completed.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Rút lớp khi lớp đã ENDED -> BusinessException")
    void withdraw_sectionEnded() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubById(SectionStatus.ENDED, 30, null);
        Enrollment enrollment = enrollment(student, section, EnrollmentStatus.ENROLLED);
        stubExisting(STUDENT_ID, enrollment);

        assertThatThrownBy(() -> service.withdraw(STUDENT_ID, SECTION_ID, STUDENT_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kết thúc");

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
    }

    // ---------- listBySection / listMyEnrollments ----------

    @Test
    @DisplayName("Admin xem danh sách đăng ký của lớp, gồm cả sinh viên đã rút")
    void listBySection_admin() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        User student = user(STUDENT_ID, true, RoleName.STUDENT);
        User other = user(OTHER_STUDENT_ID, true, RoleName.STUDENT);
        CourseSection section = section(SectionStatus.IN_PROGRESS, 30, null);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findBySectionId(SECTION_ID)).thenReturn(List.of(
                enrollment(student, section, EnrollmentStatus.ENROLLED),
                enrollment(other, section, EnrollmentStatus.DROPPED)));

        List<EnrollmentResponse> result = service.listBySection(ADMIN_ID, SECTION_ID);

        assertThat(result).extracting(EnrollmentResponse::status)
                .containsExactly(EnrollmentStatus.ENROLLED, EnrollmentStatus.DROPPED);
    }

    @Test
    @DisplayName("Sinh viên xem danh sách đăng ký của lớp -> ForbiddenException")
    void listBySection_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(sectionRepository.findById(SECTION_ID))
                .thenReturn(Optional.of(section(SectionStatus.IN_PROGRESS, 30, null)));

        assertThatThrownBy(() -> service.listBySection(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Xem danh sách đăng ký của lớp không tồn tại -> ResourceNotFoundException")
    void listBySection_sectionNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listBySection(ADMIN_ID, SECTION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên xem lịch sử đăng ký của mình, gồm cả lớp đã rút")
    void listMyEnrollments() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = section(SectionStatus.IN_PROGRESS, 30, null);
        when(enrollmentRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(
                enrollment(student, section, EnrollmentStatus.ENROLLED),
                enrollment(student, section, EnrollmentStatus.DROPPED)));

        List<EnrollmentResponse> result = service.listMyEnrollments(STUDENT_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EnrollmentResponse::studentId).containsOnly(STUDENT_ID);
    }
}
