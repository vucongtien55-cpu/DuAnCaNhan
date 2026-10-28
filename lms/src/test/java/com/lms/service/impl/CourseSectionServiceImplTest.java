package com.lms.service.impl;

import com.lms.dto.section.CreateSectionRequest;
import com.lms.dto.section.SectionResponse;
import com.lms.dto.section.UpdateSectionInfoRequest;
import com.lms.dto.section.UpdateSectionRequest;
import com.lms.entity.Course;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.CourseStatus;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
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
class CourseSectionServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long SECTION_ID = 20L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseSectionServiceImpl service;

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

    private Course course(CourseStatus status) {
        Course course = new Course();
        course.setId(10L);
        course.setCode("JAVA101");
        course.setTitle("Java");
        course.setStatus(status);
        return course;
    }

    private CourseSection section(SectionStatus status, User teacher) {
        CourseSection section = new CourseSection();
        section.setId(SECTION_ID);
        section.setCourse(course(CourseStatus.ACTIVE));
        section.setTeacher(teacher);
        section.setSectionCode("JAVA101-ABC123");
        section.setName("Lớp 1");
        section.setMaxStudents(30);
        section.setStatus(status);
        return section;
    }

    private CourseSection stubSection(SectionStatus status, User teacher) {
        CourseSection section = section(status, teacher);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        return section;
    }

    private CreateSectionRequest createRequest(String code, Long teacherId, LocalDate start, LocalDate end) {
        return new CreateSectionRequest(10L, code, "  Lớp 1 ", "HK1", start, end, 40, "T2 7:30", teacherId);
    }

    private UpdateSectionRequest updateRequest(int maxStudents) {
        return new UpdateSectionRequest("Tên mới", "HK2", null, null, maxStudents, "T3");
    }

    // ---------- create ----------

    @Test
    @DisplayName("Admin tạo lớp, mã lớp tự sinh theo mã khóa học, trạng thái DRAFT")
    void create_generatedCode() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(CourseStatus.ACTIVE)));
        when(sectionRepository.existsBySectionCode(any())).thenReturn(false);
        when(sectionRepository.save(any(CourseSection.class))).thenAnswer(inv -> {
            CourseSection saved = inv.getArgument(0);
            saved.setId(SECTION_ID);
            return saved;
        });

        SectionResponse response = service.create(ADMIN_ID, createRequest(null, null, null, null));

        ArgumentCaptor<CourseSection> captor = ArgumentCaptor.forClass(CourseSection.class);
        verify(sectionRepository).save(captor.capture());
        assertThat(captor.getValue().getSectionCode()).matches("JAVA101-[A-Z2-9]{6}");
        assertThat(captor.getValue().getName()).isEqualTo("Lớp 1");
        assertThat(response.status()).isEqualTo(SectionStatus.DRAFT);
        assertThat(response.registrationOpen()).isFalse();
        assertThat(response.teacherId()).isNull();
        assertThat(response.maxStudents()).isEqualTo(40);
    }

    @Test
    @DisplayName("Admin tạo lớp với mã tự đặt và gán giảng viên")
    void create_explicitCodeAndTeacher() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        asUser(LECTURER_ID, RoleName.LECTURER);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(CourseStatus.ACTIVE)));
        when(sectionRepository.existsBySectionCode("JAVA101-A1")).thenReturn(false);
        when(sectionRepository.save(any(CourseSection.class))).thenAnswer(inv -> inv.getArgument(0));

        SectionResponse response = service.create(ADMIN_ID,
                createRequest(" java101-a1 ", LECTURER_ID, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31)));

        assertThat(response.sectionCode()).isEqualTo("JAVA101-A1");
        assertThat(response.teacherId()).isEqualTo(LECTURER_ID);
        assertThat(response.teacherName()).isEqualTo("User 2");
    }

    @Test
    @DisplayName("Tạo lớp với mã đã tồn tại -> BusinessException")
    void create_duplicateCode() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(CourseStatus.ACTIVE)));
        when(sectionRepository.existsBySectionCode("JAVA101-A1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest("JAVA101-A1", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JAVA101-A1");

        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo lớp cho khóa học không tồn tại -> ResourceNotFoundException")
    void create_courseNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(courseRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tạo lớp cho khóa học đã lưu trữ -> BusinessException")
    void create_courseArchived() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(CourseStatus.ARCHIVED)));

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Tạo lớp với giảng viên không tồn tại -> ResourceNotFoundException")
    void create_teacherNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(CourseStatus.ACTIVE)));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, 99L, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Gán người không phải giảng viên làm giảng viên phụ trách -> BusinessException")
    void create_teacherNotLecturer() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(CourseStatus.ACTIVE)));

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest(null, STUDENT_ID, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("giảng viên");
    }

    @Test
    @DisplayName("Tạo lớp có ngày kết thúc trước ngày bắt đầu -> BusinessException")
    void create_invalidDates() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(CourseStatus.ACTIVE)));

        assertThatThrownBy(() -> service.create(ADMIN_ID,
                createRequest(null, null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 9, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ngày kết thúc");
    }

    @Test
    @DisplayName("Giảng viên tạo lớp -> ForbiddenException")
    void create_lecturerForbidden() {
        asUser(LECTURER_ID, RoleName.LECTURER);

        assertThatThrownBy(() -> service.create(LECTURER_ID, createRequest(null, null, null, null)))
                .isInstanceOf(ForbiddenException.class);

        verify(sectionRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    @DisplayName("Admin cập nhật lớp, sức chứa lớn hơn số sinh viên đang học")
    void update_success() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(SectionStatus.OPEN_REGISTRATION, null);
        when(enrollmentRepository.countBySectionIdAndStatus(SECTION_ID, EnrollmentStatus.ENROLLED)).thenReturn(20L);

        SectionResponse response = service.update(ADMIN_ID, SECTION_ID, updateRequest(25));

        assertThat(section.getMaxStudents()).isEqualTo(25);
        assertThat(response.name()).isEqualTo("Tên mới");
        assertThat(response.enrolledCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("Sức chứa bằng đúng số sinh viên đang học vẫn hợp lệ")
    void update_capacityEqualsEnrolled() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(SectionStatus.IN_PROGRESS, null);
        when(enrollmentRepository.countBySectionIdAndStatus(SECTION_ID, EnrollmentStatus.ENROLLED)).thenReturn(20L);

        service.update(ADMIN_ID, SECTION_ID, updateRequest(20));

        assertThat(section.getMaxStudents()).isEqualTo(20);
    }

    @Test
    @DisplayName("Sức chứa nhỏ hơn số sinh viên đang học -> BusinessException, giữ nguyên dữ liệu")
    void update_capacityBelowEnrolled() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(SectionStatus.OPEN_REGISTRATION, null);
        when(enrollmentRepository.countBySectionIdAndStatus(SECTION_ID, EnrollmentStatus.ENROLLED)).thenReturn(20L);

        assertThatThrownBy(() -> service.update(ADMIN_ID, SECTION_ID, updateRequest(19)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sức chứa");

        assertThat(section.getMaxStudents()).isEqualTo(30);
    }

    @Test
    @DisplayName("Cập nhật lớp đã kết thúc -> BusinessException")
    void update_ended() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.ENDED, null);

        assertThatThrownBy(() -> service.update(ADMIN_ID, SECTION_ID, updateRequest(30)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Cập nhật lớp không tồn tại -> ResourceNotFoundException")
    void update_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ADMIN_ID, SECTION_ID, updateRequest(30)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Giảng viên sửa toàn bộ thông tin lớp (kể cả sức chứa) -> ForbiddenException")
    void update_lecturerForbidden() {
        asUser(LECTURER_ID, RoleName.LECTURER);

        assertThatThrownBy(() -> service.update(LECTURER_ID, SECTION_ID, updateRequest(30)))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- updateInfo ----------

    @Test
    @DisplayName("Giảng viên phụ trách sửa tên và lịch học")
    void updateInfo_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        CourseSection section = stubSection(SectionStatus.IN_PROGRESS, lecturer);

        SectionResponse response = service.updateInfo(LECTURER_ID, SECTION_ID,
                new UpdateSectionInfoRequest(" Tên lớp mới ", "T5 13:00"));

        assertThat(response.name()).isEqualTo("Tên lớp mới");
        assertThat(section.getSchedule()).isEqualTo("T5 13:00");
        assertThat(section.getMaxStudents()).isEqualTo(30);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách sửa thông tin lớp -> ForbiddenException")
    void updateInfo_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        CourseSection section = stubSection(SectionStatus.IN_PROGRESS, assigned);

        assertThatThrownBy(() -> service.updateInfo(OTHER_LECTURER_ID, SECTION_ID,
                new UpdateSectionInfoRequest("Tên", "Lịch")))
                .isInstanceOf(ForbiddenException.class);

        assertThat(section.getName()).isEqualTo("Lớp 1");
    }

    // ---------- getById / getByCode ----------

    @Test
    @DisplayName("Admin xem được mọi lớp, kể cả DRAFT")
    void getById_admin() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.DRAFT, null);

        assertThat(service.getById(ADMIN_ID, SECTION_ID).sectionCode()).isEqualTo("JAVA101-ABC123");
    }

    @Test
    @DisplayName("Giảng viên phụ trách xem được lớp của mình")
    void getById_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSection(SectionStatus.DRAFT, lecturer);

        assertThat(service.getById(LECTURER_ID, SECTION_ID).id()).isEqualTo(SECTION_ID);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách không xem được lớp -> ResourceNotFoundException")
    void getById_otherLecturer() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSection(SectionStatus.IN_PROGRESS, assigned);

        assertThatThrownBy(() -> service.getById(OTHER_LECTURER_ID, SECTION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên xem được lớp đang mở đăng ký")
    void getById_studentOpenRegistration() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(SectionStatus.OPEN_REGISTRATION, null);

        SectionResponse response = service.getById(STUDENT_ID, SECTION_ID);

        assertThat(response.registrationOpen()).isTrue();
    }

    @Test
    @DisplayName("Sinh viên không xem được lớp DRAFT chưa đăng ký -> ResourceNotFoundException")
    void getById_studentDraft() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(SectionStatus.DRAFT, null);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên đang học xem được lớp đã IN_PROGRESS")
    void getById_studentEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(SectionStatus.IN_PROGRESS, null);
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.of(enrollment));

        assertThat(service.getById(STUDENT_ID, SECTION_ID).status()).isEqualTo(SectionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Sinh viên đã rút khỏi lớp không còn xem được lớp -> ResourceNotFoundException")
    void getById_studentDropped() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(SectionStatus.IN_PROGRESS, null);
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> service.getById(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tài khoản bị khóa không xem được lớp -> ForbiddenException")
    void getById_disabledUser() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, false, RoleName.ADMIN)));

        assertThatThrownBy(() -> service.getById(ADMIN_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Xem lớp không tồn tại -> ResourceNotFoundException")
    void getById_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(ADMIN_ID, SECTION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên tra cứu lớp bằng mã lớp (không phân biệt hoa thường)")
    void getByCode_student() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(sectionRepository.findBySectionCode("JAVA101-ABC123"))
                .thenReturn(Optional.of(section(SectionStatus.OPEN_REGISTRATION, null)));

        SectionResponse response = service.getByCode(STUDENT_ID, " java101-abc123 ");

        assertThat(response.id()).isEqualTo(SECTION_ID);
    }

    @Test
    @DisplayName("Tra cứu mã lớp không tồn tại -> ResourceNotFoundException")
    void getByCode_notFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(sectionRepository.findBySectionCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCode(STUDENT_ID, "nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- list ----------

    @Test
    @DisplayName("Admin xem tất cả lớp khi không lọc")
    void list_adminAll() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findAll(PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(section(SectionStatus.DRAFT, null))));

        Page<SectionResponse> result = service.list(ADMIN_ID, null, null, PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Admin lọc theo khóa học và trạng thái")
    void list_adminFilter() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findByCourseIdAndStatus(10L, SectionStatus.IN_PROGRESS, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(section(SectionStatus.IN_PROGRESS, null))));

        Page<SectionResponse> result = service.list(ADMIN_ID, 10L, SectionStatus.IN_PROGRESS, PAGEABLE);

        assertThat(result.getContent()).extracting(SectionResponse::status)
                .containsExactly(SectionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Giảng viên chỉ thấy các lớp được phân công")
    void list_lecturerAssigned() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        when(sectionRepository.findByTeacherId(LECTURER_ID, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(section(SectionStatus.IN_PROGRESS, lecturer))));

        Page<SectionResponse> result = service.list(LECTURER_ID, null, null, PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
        verify(sectionRepository, never()).findAll(PAGEABLE);
    }

    @Test
    @DisplayName("Sinh viên chỉ thấy lớp đang mở đăng ký")
    void list_studentOpenOnly() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(sectionRepository.findByStatus(SectionStatus.OPEN_REGISTRATION, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(section(SectionStatus.OPEN_REGISTRATION, null))));

        Page<SectionResponse> result = service.list(STUDENT_ID, null, null, PAGEABLE);

        assertThat(result.getContent()).extracting(SectionResponse::registrationOpen).containsExactly(true);
    }

    @Test
    @DisplayName("Sinh viên lọc lớp DRAFT -> ForbiddenException")
    void list_studentFilterDraftForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);

        assertThatThrownBy(() -> service.list(STUDENT_ID, null, SectionStatus.DRAFT, PAGEABLE))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- assignTeacher ----------

    @Test
    @DisplayName("Admin gán giảng viên phụ trách")
    void assignTeacher_success() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        asUser(LECTURER_ID, RoleName.LECTURER);
        CourseSection section = stubSection(SectionStatus.DRAFT, null);

        SectionResponse response = service.assignTeacher(ADMIN_ID, SECTION_ID, LECTURER_ID);

        assertThat(section.getTeacher().getId()).isEqualTo(LECTURER_ID);
        assertThat(response.teacherId()).isEqualTo(LECTURER_ID);
    }

    @Test
    @DisplayName("Gán giảng viên không tồn tại -> ResourceNotFoundException")
    void assignTeacher_teacherNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.DRAFT, null);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignTeacher(ADMIN_ID, SECTION_ID, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Gán giảng viên bị khóa tài khoản -> BusinessException")
    void assignTeacher_disabledLecturer() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.DRAFT, null);
        when(userRepository.findById(LECTURER_ID))
                .thenReturn(Optional.of(user(LECTURER_ID, false, RoleName.LECTURER)));

        assertThatThrownBy(() -> service.assignTeacher(ADMIN_ID, SECTION_ID, LECTURER_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Gán giảng viên cho lớp đã kết thúc -> BusinessException")
    void assignTeacher_ended() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.ENDED, null);

        assertThatThrownBy(() -> service.assignTeacher(ADMIN_ID, SECTION_ID, LECTURER_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Giảng viên tự gán giảng viên cho lớp -> ForbiddenException")
    void assignTeacher_lecturerForbidden() {
        asUser(LECTURER_ID, RoleName.LECTURER);

        assertThatThrownBy(() -> service.assignTeacher(LECTURER_ID, SECTION_ID, LECTURER_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- changeStatus ----------

    @Test
    @DisplayName("Admin mở đăng ký khi đã có giảng viên và khóa học ACTIVE")
    void changeStatus_openRegistration() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        User lecturer = user(LECTURER_ID, true, RoleName.LECTURER);
        stubSection(SectionStatus.DRAFT, lecturer);

        SectionResponse response = service.changeStatus(ADMIN_ID, SECTION_ID, SectionStatus.OPEN_REGISTRATION);

        assertThat(response.status()).isEqualTo(SectionStatus.OPEN_REGISTRATION);
        assertThat(response.registrationOpen()).isTrue();
    }

    @Test
    @DisplayName("Mở đăng ký khi chưa gán giảng viên -> BusinessException")
    void changeStatus_openWithoutTeacher() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(SectionStatus.DRAFT, null);

        assertThatThrownBy(() -> service.changeStatus(ADMIN_ID, SECTION_ID, SectionStatus.OPEN_REGISTRATION))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("giảng viên");

        assertThat(section.getStatus()).isEqualTo(SectionStatus.DRAFT);
    }

    @Test
    @DisplayName("Mở đăng ký khi khóa học chưa ACTIVE -> BusinessException")
    void changeStatus_openWithInactiveCourse() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(SectionStatus.DRAFT, user(LECTURER_ID, true, RoleName.LECTURER));
        section.getCourse().setStatus(CourseStatus.DRAFT);

        assertThatThrownBy(() -> service.changeStatus(ADMIN_ID, SECTION_ID, SectionStatus.OPEN_REGISTRATION))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("khóa học");
    }

    @Test
    @DisplayName("Chuyển sang IN_PROGRESS thì đóng đăng ký")
    void changeStatus_startClosesRegistration() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.OPEN_REGISTRATION, user(LECTURER_ID, true, RoleName.LECTURER));

        SectionResponse response = service.changeStatus(ADMIN_ID, SECTION_ID, SectionStatus.IN_PROGRESS);

        assertThat(response.status()).isEqualTo(SectionStatus.IN_PROGRESS);
        assertThat(response.registrationOpen()).isFalse();
    }

    @Test
    @DisplayName("Giảng viên phụ trách bắt đầu và kết thúc lớp")
    void changeStatus_lecturerStartsAndEnds() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        CourseSection section = stubSection(SectionStatus.OPEN_REGISTRATION, lecturer);

        service.changeStatus(LECTURER_ID, SECTION_ID, SectionStatus.IN_PROGRESS);
        assertThat(section.getStatus()).isEqualTo(SectionStatus.IN_PROGRESS);

        service.changeStatus(LECTURER_ID, SECTION_ID, SectionStatus.ENDED);
        assertThat(section.getStatus()).isEqualTo(SectionStatus.ENDED);
    }

    @Test
    @DisplayName("Giảng viên mở đăng ký -> ForbiddenException")
    void changeStatus_lecturerCannotOpenRegistration() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        CourseSection section = stubSection(SectionStatus.DRAFT, lecturer);

        assertThatThrownBy(() -> service.changeStatus(LECTURER_ID, SECTION_ID, SectionStatus.OPEN_REGISTRATION))
                .isInstanceOf(ForbiddenException.class);

        assertThat(section.getStatus()).isEqualTo(SectionStatus.DRAFT);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách đổi trạng thái lớp -> ForbiddenException")
    void changeStatus_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSection(SectionStatus.IN_PROGRESS, assigned);

        assertThatThrownBy(() -> service.changeStatus(OTHER_LECTURER_ID, SECTION_ID, SectionStatus.ENDED))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Chuyển trạng thái nhảy cóc DRAFT -> IN_PROGRESS -> BusinessException")
    void changeStatus_invalidTransition() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.DRAFT, null);

        assertThatThrownBy(() -> service.changeStatus(ADMIN_ID, SECTION_ID, SectionStatus.IN_PROGRESS))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể chuyển");
    }

    @Test
    @DisplayName("Lớp đã ENDED không chuyển sang trạng thái nào nữa -> BusinessException")
    void changeStatus_fromEnded() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.ENDED, null);

        assertThatThrownBy(() -> service.changeStatus(ADMIN_ID, SECTION_ID, SectionStatus.IN_PROGRESS))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Chuyển sang cùng trạng thái hiện tại -> BusinessException")
    void changeStatus_sameStatus() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(SectionStatus.DRAFT, null);

        assertThatThrownBy(() -> service.changeStatus(ADMIN_ID, SECTION_ID, SectionStatus.DRAFT))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Quay OPEN_REGISTRATION về DRAFT khi chưa có sinh viên thì được")
    void changeStatus_backToDraftWhenEmpty() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(SectionStatus.OPEN_REGISTRATION, null);

        service.changeStatus(ADMIN_ID, SECTION_ID, SectionStatus.DRAFT);

        assertThat(section.getStatus()).isEqualTo(SectionStatus.DRAFT);
    }

    @Test
    @DisplayName("Quay về DRAFT khi đã có sinh viên đăng ký -> BusinessException")
    void changeStatus_backToDraftWithEnrollments() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(SectionStatus.OPEN_REGISTRATION, null);
        when(enrollmentRepository.countBySectionIdAndStatus(SECTION_ID, EnrollmentStatus.ENROLLED)).thenReturn(3L);

        assertThatThrownBy(() -> service.changeStatus(ADMIN_ID, SECTION_ID, SectionStatus.DRAFT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3");

        assertThat(section.getStatus()).isEqualTo(SectionStatus.OPEN_REGISTRATION);
    }

    // ---------- regenerateSectionCode ----------

    @Test
    @DisplayName("Admin sinh lại mã lớp khi lớp đang DRAFT")
    void regenerateCode_success() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(SectionStatus.DRAFT, null);
        when(sectionRepository.existsBySectionCode(any())).thenReturn(false);

        SectionResponse response = service.regenerateSectionCode(ADMIN_ID, SECTION_ID);

        assertThat(section.getSectionCode()).matches("JAVA101-[A-Z2-9]{6}").isNotEqualTo("JAVA101-ABC123");
        assertThat(response.sectionCode()).isEqualTo(section.getSectionCode());
    }

    @Test
    @DisplayName("Đổi mã lớp khi lớp đang IN_PROGRESS -> BusinessException")
    void regenerateCode_inProgress() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(SectionStatus.IN_PROGRESS, null);

        assertThatThrownBy(() -> service.regenerateSectionCode(ADMIN_ID, SECTION_ID))
                .isInstanceOf(BusinessException.class);

        assertThat(section.getSectionCode()).isEqualTo("JAVA101-ABC123");
    }

    @Test
    @DisplayName("Giảng viên đổi mã lớp -> ForbiddenException")
    void regenerateCode_lecturerForbidden() {
        asUser(LECTURER_ID, RoleName.LECTURER);

        assertThatThrownBy(() -> service.regenerateSectionCode(LECTURER_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);
    }
}
