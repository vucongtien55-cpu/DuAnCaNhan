package com.lms.service.impl;

import com.lms.dto.material.MaterialFile;
import com.lms.dto.material.MaterialResponse;
import com.lms.dto.material.UploadMaterialRequest;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Material;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.MaterialType;
import com.lms.entity.enums.RoleName;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.StorageException;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.MaterialRepository;
import com.lms.repository.UserRepository;
import com.lms.storage.FileStorage;
import com.lms.storage.UploadValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long SECTION_ID = 20L;
    private static final Long MATERIAL_ID = 50L;
    private static final String STORED_PATH = "sections/20/stored.pdf";
    private static final long MAX = 20L * 1024 * 1024;

    private static final byte[] PDF = "%PDF-1.7 test".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ZIP = {'P', 'K', 0x03, 0x04, 0, 0, 0, 0};

    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorage fileStorage;
    @Spy
    private UploadValidator uploadValidator = new UploadValidator();

    @InjectMocks
    private MaterialServiceImpl service;

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

    private CourseSection stubSection(User teacher) {
        CourseSection section = section(teacher);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        return section;
    }

    private Material material(CourseSection section) {
        Material material = new Material();
        material.setId(MATERIAL_ID);
        material.setSection(section);
        material.setTitle("Bài giảng 1");
        material.setType(MaterialType.DOCUMENT);
        material.setFilePath(STORED_PATH);
        material.setOriginalFileName("bai-giang.pdf");
        material.setContentType("application/pdf");
        material.setFileSize(1234L);
        return material;
    }

    private Material stubMaterial(CourseSection section) {
        Material material = material(section);
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
        return material;
    }

    private void stubEnrollment(EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus(status);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.of(enrollment));
    }

    private void stubNoEnrollment() {
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());
    }

    private UploadMaterialRequest request() {
        return new UploadMaterialRequest("  Bài giảng 1 ", "Mô tả");
    }

    /** Client nói gì về Content-Type cũng không quan trọng. */
    private MultipartFile file(String name, String clientContentType, byte[] content) {
        return new MockMultipartFile("file", name, clientContentType, content);
    }

    private void stubStoreAndSave() {
        when(fileStorage.store(anyString(), anyString(), any())).thenReturn(STORED_PATH);
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> {
            Material saved = inv.getArgument(0);
            saved.setId(MATERIAL_ID);
            return saved;
        });
    }

    // ---------- upload: thành công ----------

    @Test
    @DisplayName("Admin tải lên PDF: lưu file với tên sinh bởi server, database chỉ có metadata và đường dẫn")
    void upload_adminSuccess() {
        User admin = asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        stubStoreAndSave();

        MaterialResponse response = service.upload(ADMIN_ID, SECTION_ID, request(),
                file("Bai giang.PDF", "text/html", PDF));

        ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
        verify(fileStorage).store(eq("sections/20"), storedName.capture(), content.capture());
        assertThat(storedName.getValue()).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(content.getValue()).isEqualTo(PDF);

        ArgumentCaptor<Material> saved = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(saved.capture());
        Material material = saved.getValue();
        assertThat(material.getFilePath()).isEqualTo(STORED_PATH);
        assertThat(material.getOriginalFileName()).isEqualTo("Bai giang.PDF");
        assertThat(material.getContentType()).isEqualTo("application/pdf");
        assertThat(material.getFileSize()).isEqualTo(PDF.length);
        assertThat(material.getTitle()).isEqualTo("Bài giảng 1");
        assertThat(material.getUploadedBy()).isSameAs(admin);

        assertThat(response.id()).isEqualTo(MATERIAL_ID);
        assertThat(response.sectionId()).isEqualTo(SECTION_ID);
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.uploadedById()).isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("Giảng viên phụ trách tải lên PPTX")
    void upload_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSection(lecturer);
        stubStoreAndSave();

        MaterialResponse response = service.upload(LECTURER_ID, SECTION_ID, request(),
                file("slide.pptx", "application/octet-stream", ZIP));

        assertThat(response.type()).isEqualTo(MaterialType.SLIDE);
        assertThat(response.contentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    @Test
    @DisplayName("Chấp nhận đủ 5 định dạng PDF, DOCX, PPTX, XLSX, ZIP")
    void upload_allAllowedExtensions() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        stubStoreAndSave();

        service.upload(ADMIN_ID, SECTION_ID, request(), file("a.pdf", null, PDF));
        service.upload(ADMIN_ID, SECTION_ID, request(), file("a.docx", null, ZIP));
        service.upload(ADMIN_ID, SECTION_ID, request(), file("a.pptx", null, ZIP));
        service.upload(ADMIN_ID, SECTION_ID, request(), file("a.xlsx", null, ZIP));
        service.upload(ADMIN_ID, SECTION_ID, request(), file("a.zip", null, ZIP));

        verify(materialRepository, org.mockito.Mockito.times(5)).save(any(Material.class));
    }

    @Test
    @DisplayName("Đúng 20 MB vẫn hợp lệ")
    void upload_exactlyMaxSize() throws Exception {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        stubStoreAndSave();
        MultipartFile big = mock(MultipartFile.class);
        when(big.isEmpty()).thenReturn(false);
        when(big.getSize()).thenReturn(MAX);
        when(big.getOriginalFilename()).thenReturn("big.pdf");
        when(big.getBytes()).thenReturn(PDF);

        service.upload(ADMIN_ID, SECTION_ID, request(), big);

        verify(materialRepository).save(any(Material.class));
    }

    // ---------- upload: tên file an toàn ----------

    @Test
    @DisplayName("Tên file chứa '../' (path traversal): tên lưu trên đĩa vẫn do server sinh, tên hiển thị chỉ còn tên file")
    void upload_pathTraversalFileName() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        stubStoreAndSave();

        service.upload(ADMIN_ID, SECTION_ID, request(), file("../../etc/passwd.pdf", null, PDF));

        ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
        verify(fileStorage).store(eq("sections/20"), storedName.capture(), any());
        assertThat(storedName.getValue()).doesNotContain("..").doesNotContain("/").doesNotContain("passwd");
        ArgumentCaptor<Material> saved = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(saved.capture());
        assertThat(saved.getValue().getOriginalFileName()).isEqualTo("passwd.pdf");
    }

    @Test
    @DisplayName("Tên file kiểu đường dẫn Windows chỉ giữ lại tên file")
    void upload_windowsPathFileName() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        stubStoreAndSave();

        service.upload(ADMIN_ID, SECTION_ID, request(), file("C:\\Users\\x\\evil.pdf", null, PDF));

        ArgumentCaptor<Material> saved = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(saved.capture());
        assertThat(saved.getValue().getOriginalFileName()).isEqualTo("evil.pdf");
    }

    @Test
    @DisplayName("Hai lần tải lên cùng tên file được lưu với hai tên khác nhau (không ghi đè)")
    void upload_sameNameDoesNotCollide() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        stubStoreAndSave();

        service.upload(ADMIN_ID, SECTION_ID, request(), file("a.pdf", null, PDF));
        service.upload(ADMIN_ID, SECTION_ID, request(), file("a.pdf", null, PDF));

        ArgumentCaptor<String> storedNames = ArgumentCaptor.forClass(String.class);
        verify(fileStorage, org.mockito.Mockito.times(2)).store(anyString(), storedNames.capture(), any());
        assertThat(storedNames.getAllValues()).doesNotHaveDuplicates();
    }

    // ---------- upload: file không hợp lệ ----------

    @Test
    @DisplayName("Phần mở rộng không được phép (.exe) -> BusinessException, không lưu file")
    void upload_disallowedExtension() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(),
                file("virus.exe", "application/pdf", PDF)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PDF");

        verify(fileStorage, never()).store(any(), any(), any());
        verify(materialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đuôi kép .pdf.exe bị từ chối")
    void upload_doubleExtension() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), file("a.pdf.exe", null, PDF)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("File không có phần mở rộng -> BusinessException")
    void upload_noExtension() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), file("README", null, PDF)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Đuôi .pdf nhưng nội dung không phải PDF (file thực thi đổi tên) -> BusinessException")
    void upload_pdfWithWrongContent() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(),
                file("fake.pdf", "application/pdf", "MZ-executable".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nội dung file");

        verify(fileStorage, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("Đuôi .docx nhưng nội dung không phải gói ZIP/OOXML -> BusinessException")
    void upload_docxWithWrongContent() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), file("fake.docx", null, PDF)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("File rỗng -> BusinessException")
    void upload_emptyFile() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), file("a.pdf", null, new byte[0])))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Không có file -> BusinessException")
    void upload_nullFile() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Không có tên file -> BusinessException")
    void upload_nullFileName() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        MultipartFile noName = mock(MultipartFile.class);
        when(noName.isEmpty()).thenReturn(false);
        when(noName.getSize()).thenReturn(10L);
        when(noName.getOriginalFilename()).thenReturn(null);

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), noName))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- upload: dung lượng ----------

    @Test
    @DisplayName("File vượt 20 MB -> BusinessException, không lưu file")
    void upload_tooLarge() throws Exception {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        MultipartFile big = mock(MultipartFile.class);
        when(big.isEmpty()).thenReturn(false);
        when(big.getSize()).thenReturn(MAX + 1);

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), big))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("20 MB");

        verify(big, never()).getBytes();
        verify(fileStorage, never()).store(any(), any(), any());
    }

    // ---------- upload: quyền, không tìm thấy, lỗi hạ tầng ----------

    @Test
    @DisplayName("Giảng viên không phụ trách tải lên -> ForbiddenException")
    void upload_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSection(assigned);

        assertThatThrownBy(() -> service.upload(OTHER_LECTURER_ID, SECTION_ID, request(), file("a.pdf", null, PDF)))
                .isInstanceOf(ForbiddenException.class);

        verify(fileStorage, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("Sinh viên tải lên -> ForbiddenException")
    void upload_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(null);

        assertThatThrownBy(() -> service.upload(STUDENT_ID, SECTION_ID, request(), file("a.pdf", null, PDF)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Tài khoản bị khóa tải lên -> ForbiddenException")
    void upload_disabledUser() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, false, RoleName.ADMIN)));

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), file("a.pdf", null, PDF)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Tải lên lớp học phần không tồn tại -> ResourceNotFoundException")
    void upload_sectionNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), file("a.pdf", null, PDF)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Lưu database thất bại -> file vừa ghi bị xóa, không để file mồ côi")
    void upload_saveFailsCleansUpFile() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        when(fileStorage.store(anyString(), anyString(), any())).thenReturn(STORED_PATH);
        when(materialRepository.save(any(Material.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), file("a.pdf", null, PDF)))
                .isInstanceOf(IllegalStateException.class);

        verify(fileStorage).delete(STORED_PATH);
    }

    @Test
    @DisplayName("Ghi file thất bại -> StorageException, không tạo bản ghi")
    void upload_storageFails() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSection(null);
        when(fileStorage.store(anyString(), anyString(), any())).thenThrow(new StorageException("disk full"));

        assertThatThrownBy(() -> service.upload(ADMIN_ID, SECTION_ID, request(), file("a.pdf", null, PDF)))
                .isInstanceOf(StorageException.class);

        verify(materialRepository, never()).save(any());
    }

    // ---------- listBySection ----------

    @Test
    @DisplayName("Admin xem danh sách tài liệu của lớp")
    void list_admin() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        CourseSection section = stubSection(null);
        when(materialRepository.findBySectionIdOrderByCreatedAtDesc(SECTION_ID))
                .thenReturn(List.of(material(section)));

        assertThat(service.listBySection(ADMIN_ID, SECTION_ID)).hasSize(1);
    }

    @Test
    @DisplayName("Giảng viên phụ trách xem danh sách tài liệu")
    void list_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        CourseSection section = stubSection(lecturer);
        when(materialRepository.findBySectionIdOrderByCreatedAtDesc(SECTION_ID))
                .thenReturn(List.of(material(section)));

        assertThat(service.listBySection(LECTURER_ID, SECTION_ID)).hasSize(1);
    }

    @Test
    @DisplayName("Sinh viên đang theo học (ENROLLED) xem danh sách tài liệu")
    void list_enrolledStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        CourseSection section = stubSection(null);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        when(materialRepository.findBySectionIdOrderByCreatedAtDesc(SECTION_ID))
                .thenReturn(List.of(material(section)));

        assertThat(service.listBySection(STUDENT_ID, SECTION_ID)).hasSize(1);
    }

    @Test
    @DisplayName("Sinh viên chưa đăng ký lớp xem danh sách -> ForbiddenException")
    void list_notEnrolledStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(null);
        stubNoEnrollment();

        assertThatThrownBy(() -> service.listBySection(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(materialRepository, never()).findBySectionIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Sinh viên đã rút khỏi lớp xem danh sách -> ForbiddenException")
    void list_droppedStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSection(null);
        stubEnrollment(EnrollmentStatus.DROPPED);

        assertThatThrownBy(() -> service.listBySection(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách xem danh sách -> ForbiddenException")
    void list_otherLecturer() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSection(assigned);

        assertThatThrownBy(() -> service.listBySection(OTHER_LECTURER_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- getById ----------

    @Test
    @DisplayName("Sinh viên đang theo học xem thông tin tài liệu, không lộ đường dẫn file")
    void getById_enrolledStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubMaterial(section(null));
        stubEnrollment(EnrollmentStatus.ENROLLED);

        MaterialResponse response = service.getById(STUDENT_ID, MATERIAL_ID);

        assertThat(response.originalFileName()).isEqualTo("bai-giang.pdf");
        assertThat(response.toString()).doesNotContain(STORED_PATH);
    }

    @Test
    @DisplayName("Xem thông tin tài liệu không tồn tại -> ResourceNotFoundException")
    void getById_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(ADMIN_ID, MATERIAL_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên không thuộc lớp xem thông tin tài liệu -> ForbiddenException")
    void getById_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubMaterial(section(null));
        stubNoEnrollment();

        assertThatThrownBy(() -> service.getById(STUDENT_ID, MATERIAL_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- download ----------

    @Test
    @DisplayName("Sinh viên đang theo học tải tài liệu: tên và kiểu nội dung lấy từ database")
    void download_enrolledStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubMaterial(section(null));
        stubEnrollment(EnrollmentStatus.ENROLLED);
        Resource resource = new ByteArrayResource(PDF);
        when(fileStorage.open(STORED_PATH)).thenReturn(resource);

        MaterialFile result = service.download(STUDENT_ID, MATERIAL_ID);

        assertThat(result.resource()).isSameAs(resource);
        assertThat(result.fileName()).isEqualTo("bai-giang.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.size()).isEqualTo(1234L);
    }

    @Test
    @DisplayName("Giảng viên phụ trách tải tài liệu")
    void download_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubMaterial(section(lecturer));
        when(fileStorage.open(STORED_PATH)).thenReturn(new ByteArrayResource(PDF));

        assertThat(service.download(LECTURER_ID, MATERIAL_ID).fileName()).isEqualTo("bai-giang.pdf");
    }

    @Test
    @DisplayName("Sinh viên không thuộc lớp tải tài liệu -> ForbiddenException, không mở file")
    void download_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubMaterial(section(null));
        stubNoEnrollment();

        assertThatThrownBy(() -> service.download(STUDENT_ID, MATERIAL_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(fileStorage, never()).open(any());
    }

    @Test
    @DisplayName("Tải tài liệu không tồn tại -> ResourceNotFoundException")
    void download_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(ADMIN_ID, MATERIAL_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Bản ghi còn nhưng file đã mất trên máy chủ -> ResourceNotFoundException")
    void download_fileMissing() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubMaterial(section(null));
        when(fileStorage.open(STORED_PATH)).thenThrow(new ResourceNotFoundException("Tệp tài liệu", STORED_PATH));

        assertThatThrownBy(() -> service.download(ADMIN_ID, MATERIAL_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- delete ----------

    @Test
    @DisplayName("Admin xóa tài liệu: xóa bản ghi rồi xóa file")
    void delete_adminSuccess() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Material material = stubMaterial(section(null));

        service.delete(ADMIN_ID, MATERIAL_ID);

        InOrder order = inOrder(materialRepository, fileStorage);
        order.verify(materialRepository).delete(material);
        order.verify(fileStorage).delete(STORED_PATH);
    }

    @Test
    @DisplayName("Giảng viên phụ trách xóa tài liệu của lớp mình")
    void delete_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        Material material = stubMaterial(section(lecturer));

        service.delete(LECTURER_ID, MATERIAL_ID);

        verify(materialRepository).delete(material);
        verify(fileStorage).delete(STORED_PATH);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách xóa tài liệu -> ForbiddenException, dữ liệu giữ nguyên")
    void delete_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubMaterial(section(assigned));

        assertThatThrownBy(() -> service.delete(OTHER_LECTURER_ID, MATERIAL_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(materialRepository, never()).delete(any());
        verify(fileStorage, never()).delete(any());
    }

    @Test
    @DisplayName("Sinh viên xóa tài liệu -> ForbiddenException")
    void delete_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubMaterial(section(null));

        assertThatThrownBy(() -> service.delete(STUDENT_ID, MATERIAL_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(materialRepository, never()).delete(any());
        verify(fileStorage, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa tài liệu không tồn tại -> ResourceNotFoundException")
    void delete_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(ADMIN_ID, MATERIAL_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Xóa bản ghi thất bại -> không xóa file")
    void delete_recordFailureKeepsFile() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Material material = stubMaterial(section(null));
        doThrow(new IllegalStateException("db down")).when(materialRepository).delete(material);

        assertThatThrownBy(() -> service.delete(ADMIN_ID, MATERIAL_ID))
                .isInstanceOf(IllegalStateException.class);

        verify(fileStorage, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa file thất bại sau khi xóa bản ghi -> không ném lỗi (chỉ ghi log)")
    void delete_fileFailureIsLoggedNotThrown() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Material material = stubMaterial(section(null));
        doThrow(new StorageException("locked")).when(fileStorage).delete(STORED_PATH);

        service.delete(ADMIN_ID, MATERIAL_ID);

        verify(materialRepository).delete(material);
    }
}
