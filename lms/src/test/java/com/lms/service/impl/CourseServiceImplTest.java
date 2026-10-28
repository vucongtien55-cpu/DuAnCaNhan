package com.lms.service.impl;

import com.lms.dto.course.CourseResponse;
import com.lms.dto.course.CreateCourseRequest;
import com.lms.dto.course.UpdateCourseRequest;
import com.lms.entity.Course;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.CourseStatus;
import com.lms.entity.enums.RoleName;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.CourseSectionRepository;
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
class CourseServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseSectionRepository courseSectionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    // ---------- helpers ----------

    private User actor(Long id, boolean enabled, RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setEnabled(enabled);
        user.setRoles(Set.of(role));
        return user;
    }

    private void asAdmin() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(actor(ADMIN_ID, true, RoleName.ADMIN)));
    }

    private void asLecturer() {
        when(userRepository.findById(LECTURER_ID))
                .thenReturn(Optional.of(actor(LECTURER_ID, true, RoleName.LECTURER)));
    }

    private Course course(Long id, String code, CourseStatus status) {
        Course course = new Course();
        course.setId(id);
        course.setCode(code);
        course.setTitle("Course " + code);
        course.setCredits(3);
        course.setStatus(status);
        return course;
    }

    private CreateCourseRequest createRequest(String code) {
        return new CreateCourseRequest(code, "  Lập trình Java ", "Mô tả", 3, null);
    }

    private UpdateCourseRequest updateRequest(String code, CourseStatus status) {
        return new UpdateCourseRequest(code, "Tên mới", "Mô tả mới", 4, "http://img/c.png", status);
    }

    // ---------- create ----------

    @Test
    @DisplayName("Admin tạo khóa học thành công: mã viết hoa, trạng thái DRAFT")
    void create_success() {
        asAdmin();
        when(courseRepository.existsByCode("JAVA101")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        CourseResponse response = courseService.create(ADMIN_ID, createRequest(" java101 "));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("JAVA101");
        assertThat(captor.getValue().getTitle()).isEqualTo("Lập trình Java");
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.code()).isEqualTo("JAVA101");
        assertThat(response.status()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("Tạo khóa học với mã đã tồn tại -> BusinessException")
    void create_duplicateCode() {
        asAdmin();
        when(courseRepository.existsByCode("JAVA101")).thenReturn(true);

        assertThatThrownBy(() -> courseService.create(ADMIN_ID, createRequest("java101")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JAVA101");

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Giảng viên tạo khóa học -> ForbiddenException")
    void create_lecturerForbidden() {
        asLecturer();

        assertThatThrownBy(() -> courseService.create(LECTURER_ID, createRequest("JAVA101")))
                .isInstanceOf(ForbiddenException.class);

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin bị khóa tài khoản không được tạo khóa học")
    void create_disabledAdminForbidden() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(actor(ADMIN_ID, false, RoleName.ADMIN)));

        assertThatThrownBy(() -> courseService.create(ADMIN_ID, createRequest("JAVA101")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Người thực hiện không tồn tại -> ResourceNotFoundException")
    void create_actorNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.create(99L, createRequest("JAVA101")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- getById ----------

    @Test
    @DisplayName("Admin xem được khóa học DRAFT")
    void getById_adminSeesDraft() {
        asAdmin();
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L, "A", CourseStatus.DRAFT)));

        assertThat(courseService.getById(ADMIN_ID, 5L).code()).isEqualTo("A");
    }

    @Test
    @DisplayName("Giảng viên xem được khóa học ACTIVE")
    void getById_lecturerSeesActive() {
        asLecturer();
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L, "A", CourseStatus.ACTIVE)));

        assertThat(courseService.getById(LECTURER_ID, 5L).status()).isEqualTo(CourseStatus.ACTIVE);
    }

    @Test
    @DisplayName("Giảng viên xem khóa học DRAFT -> ResourceNotFoundException")
    void getById_lecturerCannotSeeDraft() {
        asLecturer();
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L, "A", CourseStatus.DRAFT)));

        assertThatThrownBy(() -> courseService.getById(LECTURER_ID, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Xem khóa học không tồn tại -> ResourceNotFoundException")
    void getById_notFound() {
        asAdmin();
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getById(ADMIN_ID, 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- list ----------

    @Test
    @DisplayName("Admin xem tất cả khóa học khi không lọc")
    void list_adminAll() {
        asAdmin();
        Page<Course> page = new PageImpl<>(List.of(
                course(1L, "A", CourseStatus.DRAFT), course(2L, "B", CourseStatus.ACTIVE)));
        when(courseRepository.findAll(PAGEABLE)).thenReturn(page);

        Page<CourseResponse> result = courseService.list(ADMIN_ID, null, PAGEABLE);

        assertThat(result.getContent()).extracting(CourseResponse::code).containsExactly("A", "B");
    }

    @Test
    @DisplayName("Admin lọc theo trạng thái")
    void list_adminFilterByStatus() {
        asAdmin();
        when(courseRepository.findByStatus(CourseStatus.ARCHIVED, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(course(3L, "C", CourseStatus.ARCHIVED))));

        Page<CourseResponse> result = courseService.list(ADMIN_ID, CourseStatus.ARCHIVED, PAGEABLE);

        assertThat(result.getContent()).extracting(CourseResponse::code).containsExactly("C");
    }

    @Test
    @DisplayName("Giảng viên chỉ thấy khóa học ACTIVE")
    void list_lecturerOnlyActive() {
        asLecturer();
        when(courseRepository.findByStatus(CourseStatus.ACTIVE, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(course(2L, "B", CourseStatus.ACTIVE))));

        Page<CourseResponse> result = courseService.list(LECTURER_ID, null, PAGEABLE);

        assertThat(result.getContent()).extracting(CourseResponse::code).containsExactly("B");
        verify(courseRepository, never()).findAll(PAGEABLE);
    }

    @Test
    @DisplayName("Giảng viên lọc khóa học DRAFT -> ForbiddenException")
    void list_lecturerFilterDraftForbidden() {
        asLecturer();

        assertThatThrownBy(() -> courseService.list(LECTURER_ID, CourseStatus.DRAFT, PAGEABLE))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- update ----------

    @Test
    @DisplayName("Admin cập nhật khóa học thành công")
    void update_success() {
        asAdmin();
        Course existing = course(5L, "OLD", CourseStatus.DRAFT);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(courseRepository.existsByCode("NEW")).thenReturn(false);

        CourseResponse response = courseService.update(ADMIN_ID, 5L, updateRequest("new", CourseStatus.ACTIVE));

        assertThat(response.code()).isEqualTo("NEW");
        assertThat(response.title()).isEqualTo("Tên mới");
        assertThat(response.credits()).isEqualTo(4);
        assertThat(response.status()).isEqualTo(CourseStatus.ACTIVE);
        assertThat(existing.getCode()).isEqualTo("NEW");
    }

    @Test
    @DisplayName("Cập nhật giữ nguyên mã -> không kiểm tra trùng")
    void update_sameCode() {
        asAdmin();
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L, "SAME", CourseStatus.DRAFT)));

        courseService.update(ADMIN_ID, 5L, updateRequest("same", CourseStatus.DRAFT));

        verify(courseRepository, never()).existsByCode(any());
    }

    @Test
    @DisplayName("Cập nhật sang mã đã thuộc khóa học khác -> BusinessException")
    void update_duplicateCode() {
        asAdmin();
        Course existing = course(5L, "OLD", CourseStatus.DRAFT);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(courseRepository.existsByCode("TAKEN")).thenReturn(true);

        assertThatThrownBy(() -> courseService.update(ADMIN_ID, 5L, updateRequest("TAKEN", CourseStatus.DRAFT)))
                .isInstanceOf(BusinessException.class);

        assertThat(existing.getCode()).isEqualTo("OLD");
    }

    @Test
    @DisplayName("Cập nhật khóa học không tồn tại -> ResourceNotFoundException")
    void update_notFound() {
        asAdmin();
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(ADMIN_ID, 404L, updateRequest("X", CourseStatus.DRAFT)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Giảng viên cập nhật khóa học -> ForbiddenException")
    void update_lecturerForbidden() {
        asLecturer();

        assertThatThrownBy(() -> courseService.update(LECTURER_ID, 5L, updateRequest("X", CourseStatus.DRAFT)))
                .isInstanceOf(ForbiddenException.class);

        verify(courseRepository, never()).findById(any());
    }

    // ---------- delete ----------

    @Test
    @DisplayName("Admin xóa khóa học chưa có lớp học phần thành công")
    void delete_success() {
        asAdmin();
        Course existing = course(5L, "A", CourseStatus.DRAFT);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(courseSectionRepository.existsByCourseId(5L)).thenReturn(false);

        courseService.delete(ADMIN_ID, 5L);

        verify(courseRepository).delete(existing);
    }

    @Test
    @DisplayName("Xóa khóa học đã có lớp học phần -> BusinessException, giữ nguyên dữ liệu")
    void delete_hasSections() {
        asAdmin();
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L, "A", CourseStatus.ACTIVE)));
        when(courseSectionRepository.existsByCourseId(5L)).thenReturn(true);

        assertThatThrownBy(() -> courseService.delete(ADMIN_ID, 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ARCHIVED");

        verify(courseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa khóa học không tồn tại -> ResourceNotFoundException")
    void delete_notFound() {
        asAdmin();
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.delete(ADMIN_ID, 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Giảng viên xóa khóa học -> ForbiddenException")
    void delete_lecturerForbidden() {
        asLecturer();

        assertThatThrownBy(() -> courseService.delete(LECTURER_ID, 5L))
                .isInstanceOf(ForbiddenException.class);

        verify(courseRepository, never()).delete(any());
    }
}
