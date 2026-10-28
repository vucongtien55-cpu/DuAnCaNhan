package com.lms.service.impl;

import com.lms.dto.assignment.AssignmentAttachment;
import com.lms.dto.assignment.AssignmentResponse;
import com.lms.entity.Assignment;
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
import com.lms.exception.StorageException;
import com.lms.repository.AssignmentRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.UserRepository;
import com.lms.service.support.SectionAccessChecker;
import com.lms.storage.FileStorage;
import com.lms.storage.UploadValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentAttachmentServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long SECTION_ID = 20L;
    private static final Long ASSIGNMENT_ID = 30L;
    private static final String NEW_PATH = "assignment-attachments/30/new.pdf";
    private static final String OLD_PATH = "assignment-attachments/30/old.pdf";
    private static final long MAX = 20L * 1024 * 1024;

    private static final byte[] PDF = "%PDF-1.7 test".getBytes(StandardCharsets.UTF_8);

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private FileStorage fileStorage;

    private AssignmentAttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        SectionAccessChecker access = new SectionAccessChecker(userRepository, enrollmentRepository);
        service = new AssignmentAttachmentServiceImpl(assignmentRepository, fileStorage, new UploadValidator(), access);
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

    private Assignment assignment(SectionStatus status, User teacher, String attachmentPath) {
        CourseSection section = new CourseSection();
        section.setId(SECTION_ID);
        section.setTeacher(teacher);
        section.setStatus(status);
        Assignment assignment = new Assignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setSection(section);
        assignment.setTitle("Bài 1");
        if (attachmentPath != null) {
            assignment.setAttachmentPath(attachmentPath);
            assignment.setAttachmentFileName("de-bai.pdf");
            assignment.setAttachmentContentType("application/pdf");
            assignment.setAttachmentSize(999L);
        }
        return assignment;
    }

    private Assignment stubForUpdate(Assignment assignment) {
        when(assignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        return assignment;
    }

    private Assignment stubFind(Assignment assignment) {
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        return assignment;
    }

    private void stubStoreAndSave() {
        when(fileStorage.store(anyString(), anyString(), any())).thenReturn(NEW_PATH);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "text/html", content);
    }

    private void stubEnrollment(EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus(status);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.of(enrollment));
    }

    // ---------- upload ----------

    @Test
    @DisplayName("Admin tải file đính kèm: lưu metadata và đường dẫn, kiểu nội dung do server xác định")
    void upload_adminSuccess() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, null));
        stubStoreAndSave();

        AssignmentResponse response = service.upload(ADMIN_ID, ASSIGNMENT_ID, file("De bai.PDF", PDF));

        ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
        verify(fileStorage).store(eq("assignment-attachments/30"), storedName.capture(), eq(PDF));
        assertThat(storedName.getValue()).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(assignment.getAttachmentPath()).isEqualTo(NEW_PATH);
        assertThat(assignment.getAttachmentContentType()).isEqualTo("application/pdf");
        assertThat(assignment.getAttachmentSize()).isEqualTo(PDF.length);
        assertThat(response.attachmentFileName()).isEqualTo("De bai.PDF");
        assertThat(response.attachmentSize()).isEqualTo(PDF.length);
        assertThat(response.toString()).doesNotContain(NEW_PATH);
        verify(fileStorage, never()).delete(any());
    }

    @Test
    @DisplayName("Giảng viên phụ trách tải file đính kèm")
    void upload_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, lecturer, null));
        stubStoreAndSave();

        assertThat(service.upload(LECTURER_ID, ASSIGNMENT_ID, file("a.pdf", PDF)).attachmentFileName())
                .isEqualTo("a.pdf");
    }

    @Test
    @DisplayName("Tải file mới thay file cũ: file cũ bị xóa sau khi bản ghi đã trỏ sang file mới")
    void upload_replacesOldFile() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, OLD_PATH));
        stubStoreAndSave();

        service.upload(ADMIN_ID, ASSIGNMENT_ID, file("moi.pdf", PDF));

        verify(fileStorage).delete(OLD_PATH);
        verify(fileStorage, never()).delete(NEW_PATH);
    }

    @Test
    @DisplayName("Tên file chứa '../': tên lưu trên đĩa do server sinh, tên hiển thị chỉ còn tên file")
    void upload_pathTraversalFileName() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, null));
        stubStoreAndSave();

        AssignmentResponse response = service.upload(ADMIN_ID, ASSIGNMENT_ID, file("../../etc/passwd.pdf", PDF));

        ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
        verify(fileStorage).store(eq("assignment-attachments/30"), storedName.capture(), any());
        assertThat(storedName.getValue()).doesNotContain("..").doesNotContain("/").doesNotContain("passwd");
        assertThat(response.attachmentFileName()).isEqualTo("passwd.pdf");
    }

    @Test
    @DisplayName("File .exe -> BusinessException, không lưu file, file cũ giữ nguyên")
    void upload_disallowedExtension() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, OLD_PATH));

        assertThatThrownBy(() -> service.upload(ADMIN_ID, ASSIGNMENT_ID, file("virus.exe", PDF)))
                .isInstanceOf(BusinessException.class);

        verify(fileStorage, never()).store(any(), any(), any());
        assertThat(assignment.getAttachmentPath()).isEqualTo(OLD_PATH);
    }

    @Test
    @DisplayName("Đuôi .pdf nhưng nội dung không phải PDF -> BusinessException")
    void upload_wrongContent() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, null));

        assertThatThrownBy(() -> service.upload(ADMIN_ID, ASSIGNMENT_ID,
                file("fake.pdf", "MZ-executable".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nội dung file");
    }

    @Test
    @DisplayName("File vượt 20 MB -> BusinessException")
    void upload_tooLarge() throws Exception {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, null));
        MultipartFile big = mock(MultipartFile.class);
        when(big.isEmpty()).thenReturn(false);
        when(big.getSize()).thenReturn(MAX + 1);

        assertThatThrownBy(() -> service.upload(ADMIN_ID, ASSIGNMENT_ID, big))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("20 MB");

        verify(big, never()).getBytes();
        verify(fileStorage, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("Không đính kèm file -> BusinessException")
    void upload_nullFile() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, null));

        assertThatThrownBy(() -> service.upload(ADMIN_ID, ASSIGNMENT_ID, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách tải file -> ForbiddenException")
    void upload_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, assigned, null));

        assertThatThrownBy(() -> service.upload(OTHER_LECTURER_ID, ASSIGNMENT_ID, file("a.pdf", PDF)))
                .isInstanceOf(ForbiddenException.class);

        verify(fileStorage, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("Sinh viên tải file đính kèm lên -> ForbiddenException")
    void upload_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, null));

        assertThatThrownBy(() -> service.upload(STUDENT_ID, ASSIGNMENT_ID, file("a.pdf", PDF)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Tải file cho bài tập không tồn tại -> ResourceNotFoundException")
    void upload_assignmentNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(assignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(ADMIN_ID, ASSIGNMENT_ID, file("a.pdf", PDF)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tải file cho bài tập của lớp đã ENDED -> BusinessException")
    void upload_sectionEnded() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(assignment(SectionStatus.ENDED, null, null));

        assertThatThrownBy(() -> service.upload(ADMIN_ID, ASSIGNMENT_ID, file("a.pdf", PDF)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kết thúc");
    }

    @Test
    @DisplayName("Lưu database thất bại -> file mới bị xóa, file cũ giữ nguyên")
    void upload_saveFailsCleansUpNewFile() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, OLD_PATH));
        when(fileStorage.store(anyString(), anyString(), any())).thenReturn(NEW_PATH);
        when(assignmentRepository.save(any(Assignment.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.upload(ADMIN_ID, ASSIGNMENT_ID, file("a.pdf", PDF)))
                .isInstanceOf(IllegalStateException.class);

        verify(fileStorage).delete(NEW_PATH);
        verify(fileStorage, never()).delete(OLD_PATH);
    }

    @Test
    @DisplayName("Ghi file thất bại -> StorageException, không cập nhật bản ghi")
    void upload_storageFails() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, OLD_PATH));
        when(fileStorage.store(anyString(), anyString(), any())).thenThrow(new StorageException("disk full"));

        assertThatThrownBy(() -> service.upload(ADMIN_ID, ASSIGNMENT_ID, file("a.pdf", PDF)))
                .isInstanceOf(StorageException.class);

        verify(assignmentRepository, never()).save(any());
    }

    // ---------- download ----------

    @Test
    @DisplayName("Sinh viên đang theo học tải file đính kèm: tên và kiểu nội dung lấy từ database")
    void download_enrolledStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubFind(assignment(SectionStatus.IN_PROGRESS, null, OLD_PATH));
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Resource resource = new ByteArrayResource(PDF);
        when(fileStorage.open(OLD_PATH)).thenReturn(resource);

        AssignmentAttachment result = service.download(STUDENT_ID, ASSIGNMENT_ID);

        assertThat(result.resource()).isSameAs(resource);
        assertThat(result.fileName()).isEqualTo("de-bai.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.size()).isEqualTo(999L);
    }

    @Test
    @DisplayName("Giảng viên phụ trách tải file đính kèm")
    void download_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubFind(assignment(SectionStatus.IN_PROGRESS, lecturer, OLD_PATH));
        when(fileStorage.open(OLD_PATH)).thenReturn(new ByteArrayResource(PDF));

        assertThat(service.download(LECTURER_ID, ASSIGNMENT_ID).fileName()).isEqualTo("de-bai.pdf");
    }

    @Test
    @DisplayName("Sinh viên chưa đăng ký lớp tải file -> ForbiddenException, không mở file")
    void download_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubFind(assignment(SectionStatus.IN_PROGRESS, null, OLD_PATH));
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(STUDENT_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(fileStorage, never()).open(any());
    }

    @Test
    @DisplayName("Sinh viên đã rút khỏi lớp tải file -> ForbiddenException")
    void download_droppedStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubFind(assignment(SectionStatus.IN_PROGRESS, null, OLD_PATH));
        stubEnrollment(EnrollmentStatus.DROPPED);

        assertThatThrownBy(() -> service.download(STUDENT_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách tải file -> ForbiddenException")
    void download_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubFind(assignment(SectionStatus.IN_PROGRESS, assigned, OLD_PATH));

        assertThatThrownBy(() -> service.download(OTHER_LECTURER_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Tài khoản bị khóa tải file -> ForbiddenException")
    void download_disabledUser() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, false, RoleName.ADMIN)));

        assertThatThrownBy(() -> service.download(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Bài tập chưa có file đính kèm -> ResourceNotFoundException")
    void download_noAttachment() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubFind(assignment(SectionStatus.IN_PROGRESS, null, null));

        assertThatThrownBy(() -> service.download(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileStorage, never()).open(any());
    }

    @Test
    @DisplayName("Tải file của bài tập không tồn tại -> ResourceNotFoundException")
    void download_assignmentNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Bản ghi còn nhưng file đã mất trên máy chủ -> ResourceNotFoundException")
    void download_fileMissing() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubFind(assignment(SectionStatus.IN_PROGRESS, null, OLD_PATH));
        when(fileStorage.open(OLD_PATH)).thenThrow(new ResourceNotFoundException("Tệp tài liệu", OLD_PATH));

        assertThatThrownBy(() -> service.download(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- remove ----------

    @Test
    @DisplayName("Admin gỡ file đính kèm: xóa metadata rồi xóa file")
    void remove_success() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, OLD_PATH));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignmentResponse response = service.remove(ADMIN_ID, ASSIGNMENT_ID);

        assertThat(assignment.getAttachmentPath()).isNull();
        assertThat(assignment.getAttachmentContentType()).isNull();
        assertThat(response.attachmentFileName()).isNull();
        assertThat(response.attachmentSize()).isNull();
        verify(fileStorage).delete(OLD_PATH);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách gỡ file -> ForbiddenException, dữ liệu giữ nguyên")
    void remove_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        Assignment assignment = stubForUpdate(assignment(SectionStatus.IN_PROGRESS, assigned, OLD_PATH));

        assertThatThrownBy(() -> service.remove(OTHER_LECTURER_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);

        assertThat(assignment.getAttachmentPath()).isEqualTo(OLD_PATH);
        verify(fileStorage, never()).delete(any());
    }

    @Test
    @DisplayName("Gỡ file khi bài tập chưa có file đính kèm -> ResourceNotFoundException")
    void remove_noAttachment() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubForUpdate(assignment(SectionStatus.IN_PROGRESS, null, null));

        assertThatThrownBy(() -> service.remove(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Gỡ file của lớp đã ENDED -> BusinessException")
    void remove_sectionEnded() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubForUpdate(assignment(SectionStatus.ENDED, null, OLD_PATH));

        assertThatThrownBy(() -> service.remove(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(BusinessException.class);

        assertThat(assignment.getAttachmentPath()).isEqualTo(OLD_PATH);
    }

    @Test
    @DisplayName("Gỡ file của bài tập không tồn tại -> ResourceNotFoundException")
    void remove_assignmentNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(assignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
