package com.lms.service.impl;

import com.lms.dto.submission.SubmissionFile;
import com.lms.dto.submission.SubmissionResponse;
import com.lms.dto.submission.SubmitRequest;
import com.lms.entity.Assignment;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Grade;
import com.lms.entity.Role;
import com.lms.entity.Submission;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.GradingPolicy;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.StorageException;
import com.lms.repository.AssignmentRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.GradeRepository;
import com.lms.repository.SubmissionRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long OTHER_STUDENT_ID = 5L;
    private static final Long SECTION_ID = 20L;
    private static final Long ASSIGNMENT_ID = 30L;
    private static final Long SUBMISSION_ID = 40L;
    private static final String STORED_PATH = "assignments/30/4/stored.pdf";
    private static final long MAX = 20L * 1024 * 1024;

    private static final byte[] PDF = "%PDF-1.7 test".getBytes(StandardCharsets.UTF_8);

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private FileStorage fileStorage;

    private SubmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        SectionAccessChecker access = new SectionAccessChecker(userRepository, enrollmentRepository);
        service = new SubmissionServiceImpl(submissionRepository, assignmentRepository, gradeRepository, fileStorage,
                new UploadValidator(), access);
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
        section.setTeacher(teacher);
        section.setStatus(status);
        return section;
    }

    private Assignment assignment(CourseSection section, LocalDateTime dueDate, boolean allowLate,
                                  Integer maxSubmissions, GradingPolicy policy) {
        Assignment assignment = new Assignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setSection(section);
        assignment.setTitle("Bài 1");
        assignment.setDueDate(dueDate);
        assignment.setAllowLateSubmission(allowLate);
        assignment.setMaxSubmissions(maxSubmissions);
        assignment.setGradingPolicy(policy);
        return assignment;
    }

    /** Bài tập đang mở: hạn nộp ngày mai, tối đa 3 lần, cho nộp muộn, chấm bài mới nhất. */
    private Assignment openAssignment() {
        return assignment(section(SectionStatus.IN_PROGRESS, null), LocalDateTime.now().plusDays(1), true, 3,
                GradingPolicy.LATEST);
    }

    private Assignment stubForSubmit(Assignment assignment) {
        when(assignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        return assignment;
    }

    private Assignment stubFind(Assignment assignment) {
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        return assignment;
    }

    private void stubEnrollment(EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus(status);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.of(enrollment));
    }

    private void stubCount(long count) {
        when(submissionRepository.countByAssignmentIdAndStudentId(ASSIGNMENT_ID, STUDENT_ID)).thenReturn(count);
    }

    private void stubLatestAttempt(Submission latest) {
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(Optional.ofNullable(latest));
    }

    private void stubStoreAndSave() {
        when(fileStorage.store(anyString(), anyString(), any())).thenReturn(STORED_PATH);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission saved = inv.getArgument(0);
            saved.setId(SUBMISSION_ID);
            return saved;
        });
    }

    private MultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "text/html", content);
    }

    private Submission submission(Long id, Assignment assignment, Long studentId, int attempt) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setAssignment(assignment);
        submission.setStudent(user(studentId, true, RoleName.STUDENT));
        submission.setAttemptNumber(attempt);
        submission.setFilePath(STORED_PATH);
        submission.setOriginalFileName("bai-lam.pdf");
        submission.setContentType("application/pdf");
        submission.setFileSize(1234L);
        return submission;
    }

    private Submission stubSubmission(Assignment assignment, Long studentId) {
        Submission submission = submission(SUBMISSION_ID, assignment, studentId, 1);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));
        return submission;
    }

    // ---------- submit: thành công ----------

    @Test
    @DisplayName("Sinh viên nộp bài trước hạn: lần nộp 1, không bị đánh dấu muộn, file lưu theo thư mục bài tập/sinh viên")
    void submit_onTime() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(openAssignment());
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(0);
        stubLatestAttempt(null);
        stubStoreAndSave();

        SubmissionResponse response = service.submit(STUDENT_ID, ASSIGNMENT_ID, new SubmitRequest("Ghi chú"),
                file("Bai lam.pdf", PDF));

        ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
        verify(fileStorage).store(eq("assignments/30/4"), storedName.capture(), eq(PDF));
        assertThat(storedName.getValue()).matches("[0-9a-f-]{36}\\.pdf");

        ArgumentCaptor<Submission> saved = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(saved.capture());
        assertThat(saved.getValue().getStudent()).isSameAs(student);
        assertThat(saved.getValue().getFilePath()).isEqualTo(STORED_PATH);
        assertThat(saved.getValue().getContentType()).isEqualTo("application/pdf");

        assertThat(response.attemptNumber()).isEqualTo(1);
        assertThat(response.late()).isFalse();
        assertThat(response.content()).isEqualTo("Ghi chú");
        assertThat(response.originalFileName()).isEqualTo("Bai lam.pdf");
        assertThat(response.toString()).doesNotContain(STORED_PATH);
    }

    @Test
    @DisplayName("Nộp sau hạn khi bài tập cho phép nộp muộn: được chấp nhận và đánh dấu late")
    void submit_lateAllowed() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(assignment(section(SectionStatus.IN_PROGRESS, null), LocalDateTime.now().minusHours(2), true,
                3, GradingPolicy.LATEST));
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(0);
        stubLatestAttempt(null);
        stubStoreAndSave();

        SubmissionResponse response = service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF));

        assertThat(response.late()).isTrue();
    }

    @Test
    @DisplayName("Bài tập không có hạn nộp: không bao giờ bị đánh dấu muộn")
    void submit_noDueDate() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(assignment(section(SectionStatus.IN_PROGRESS, null), null, false, 3, GradingPolicy.LATEST));
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(0);
        stubLatestAttempt(null);
        stubStoreAndSave();

        assertThat(service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)).late()).isFalse();
    }

    @Test
    @DisplayName("Lần nộp thứ hai nhận attemptNumber = 2")
    void submit_secondAttempt() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Assignment assignment = stubForSubmit(openAssignment());
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(1);
        stubLatestAttempt(submission(41L, assignment, STUDENT_ID, 1));
        stubStoreAndSave();

        assertThat(service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)).attemptNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Bài tập không giới hạn số lần nộp: nộp lần thứ 11 vẫn được")
    void submit_unlimitedAttempts() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Assignment assignment = assignment(section(SectionStatus.IN_PROGRESS, null), null, true, null,
                GradingPolicy.LATEST);
        stubForSubmit(assignment);
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(10);
        stubLatestAttempt(submission(41L, assignment, STUDENT_ID, 10));
        stubStoreAndSave();

        assertThat(service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)).attemptNumber()).isEqualTo(11);
    }

    @Test
    @DisplayName("Hai lần nộp cùng tên file được lưu với hai tên khác nhau (không ghi đè)")
    void submit_sameNameDoesNotCollide() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(openAssignment());
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(0);
        stubLatestAttempt(null);
        stubStoreAndSave();

        service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF));
        service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF));

        ArgumentCaptor<String> names = ArgumentCaptor.forClass(String.class);
        verify(fileStorage, times(2)).store(anyString(), names.capture(), any());
        assertThat(names.getAllValues()).doesNotHaveDuplicates();
    }

    // ---------- submit: điều kiện nghiệp vụ ----------

    @Test
    @DisplayName("Nộp sau hạn khi bài tập không cho nộp muộn -> BusinessException")
    void submit_lateNotAllowed() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(assignment(section(SectionStatus.IN_PROGRESS, null), LocalDateTime.now().minusHours(1), false,
                3, GradingPolicy.LATEST));
        stubEnrollment(EnrollmentStatus.ENROLLED);

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("quá hạn");

        verify(fileStorage, never()).store(any(), any(), any());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Vượt số lần nộp tối đa -> BusinessException, không lưu file")
    void submit_attemptLimitReached() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(openAssignment());
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(3);

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3 lần nộp");

        verify(fileStorage, never()).store(any(), any(), any());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên chưa đăng ký lớp nộp bài -> ForbiddenException")
    void submit_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(openAssignment());
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)))
                .isInstanceOf(ForbiddenException.class);

        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên đã rút khỏi lớp nộp bài -> ForbiddenException")
    void submit_droppedStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(openAssignment());
        stubEnrollment(EnrollmentStatus.DROPPED);

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Admin nộp bài thay sinh viên -> ForbiddenException")
    void submit_nonStudentForbidden() {
        asUser(ADMIN_ID, RoleName.ADMIN);

        assertThatThrownBy(() -> service.submit(ADMIN_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)))
                .isInstanceOf(ForbiddenException.class);

        verify(assignmentRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Tài khoản bị khóa nộp bài -> ForbiddenException")
    void submit_disabledStudent() {
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(user(STUDENT_ID, false, RoleName.STUDENT)));

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Nộp bài cho bài tập không tồn tại -> ResourceNotFoundException")
    void submit_assignmentNotFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(assignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Nộp bài khi lớp đã ENDED -> BusinessException")
    void submit_sectionEnded() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(assignment(section(SectionStatus.ENDED, null), null, true, 3, GradingPolicy.LATEST));
        stubEnrollment(EnrollmentStatus.ENROLLED);

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kết thúc");
    }

    // ---------- submit: file không hợp lệ ----------

    private void stubReadyToSubmit() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(openAssignment());
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(0);
    }

    @Test
    @DisplayName("Nộp file .exe -> BusinessException, không lưu file")
    void submit_disallowedExtension() {
        stubReadyToSubmit();

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("virus.exe", PDF)))
                .isInstanceOf(BusinessException.class);

        verify(fileStorage, never()).store(any(), any(), any());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đuôi .pdf nhưng nội dung không phải PDF -> BusinessException")
    void submit_wrongContent() {
        stubReadyToSubmit();

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null,
                file("fake.pdf", "MZ-executable".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nội dung file");

        verify(fileStorage, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("Nộp file vượt 20 MB -> BusinessException")
    void submit_tooLarge() throws Exception {
        stubReadyToSubmit();
        MultipartFile big = mock(MultipartFile.class);
        when(big.isEmpty()).thenReturn(false);
        when(big.getSize()).thenReturn(MAX + 1);

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, big))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("20 MB");

        verify(big, never()).getBytes();
        verify(fileStorage, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("Không đính kèm file -> BusinessException")
    void submit_noFile() {
        stubReadyToSubmit();

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Tên file chứa '../' (path traversal): tên lưu trên đĩa do server sinh, tên hiển thị chỉ còn tên file")
    void submit_pathTraversalFileName() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(openAssignment());
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(0);
        stubLatestAttempt(null);
        stubStoreAndSave();

        SubmissionResponse response = service.submit(STUDENT_ID, ASSIGNMENT_ID, null,
                file("../../../etc/passwd.pdf", PDF));

        ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
        verify(fileStorage).store(eq("assignments/30/4"), storedName.capture(), any());
        assertThat(storedName.getValue()).doesNotContain("..").doesNotContain("/").doesNotContain("passwd");
        assertThat(response.originalFileName()).isEqualTo("passwd.pdf");
    }

    @Test
    @DisplayName("Lưu database thất bại -> file vừa ghi bị xóa, không để file mồ côi")
    void submit_saveFailsCleansUpFile() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(openAssignment());
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(0);
        stubLatestAttempt(null);
        when(fileStorage.store(anyString(), anyString(), any())).thenReturn(STORED_PATH);
        when(submissionRepository.save(any(Submission.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)))
                .isInstanceOf(IllegalStateException.class);

        verify(fileStorage).delete(STORED_PATH);
    }

    @Test
    @DisplayName("Ghi file thất bại -> StorageException, không tạo bản ghi")
    void submit_storageFails() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubForSubmit(openAssignment());
        stubEnrollment(EnrollmentStatus.ENROLLED);
        stubCount(0);
        stubLatestAttempt(null);
        when(fileStorage.store(anyString(), anyString(), any())).thenThrow(new StorageException("disk full"));

        assertThatThrownBy(() -> service.submit(STUDENT_ID, ASSIGNMENT_ID, null, file("a.pdf", PDF)))
                .isInstanceOf(StorageException.class);

        verify(submissionRepository, never()).save(any());
    }

    // ---------- getById / download ----------

    @Test
    @DisplayName("Sinh viên xem bài nộp của chính mình")
    void getById_owner() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSubmission(openAssignment(), STUDENT_ID);

        assertThat(service.getById(STUDENT_ID, SUBMISSION_ID).studentId()).isEqualTo(STUDENT_ID);
    }

    @Test
    @DisplayName("Sinh viên xem bài nộp của người khác -> ForbiddenException")
    void getById_otherStudentForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        stubSubmission(openAssignment(), STUDENT_ID);

        assertThatThrownBy(() -> service.getById(OTHER_STUDENT_ID, SUBMISSION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Giảng viên phụ trách xem bài nộp của sinh viên trong lớp mình")
    void getById_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSubmission(assignment(section(SectionStatus.IN_PROGRESS, lecturer), null, true, 3, GradingPolicy.LATEST),
                STUDENT_ID);

        assertThat(service.getById(LECTURER_ID, SUBMISSION_ID).id()).isEqualTo(SUBMISSION_ID);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách xem bài nộp -> ForbiddenException")
    void getById_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSubmission(assignment(section(SectionStatus.IN_PROGRESS, assigned), null, true, 3, GradingPolicy.LATEST),
                STUDENT_ID);

        assertThatThrownBy(() -> service.getById(OTHER_LECTURER_ID, SUBMISSION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Admin xem mọi bài nộp")
    void getById_admin() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSubmission(openAssignment(), STUDENT_ID);

        assertThat(service.getById(ADMIN_ID, SUBMISSION_ID).id()).isEqualTo(SUBMISSION_ID);
    }

    @Test
    @DisplayName("Xem bài nộp không tồn tại -> ResourceNotFoundException")
    void getById_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(ADMIN_ID, SUBMISSION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên tải lại file bài nộp của mình")
    void download_owner() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSubmission(openAssignment(), STUDENT_ID);
        Resource resource = new ByteArrayResource(PDF);
        when(fileStorage.open(STORED_PATH)).thenReturn(resource);

        SubmissionFile result = service.download(STUDENT_ID, SUBMISSION_ID);

        assertThat(result.resource()).isSameAs(resource);
        assertThat(result.fileName()).isEqualTo("bai-lam.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Sinh viên tải file bài nộp của người khác -> ForbiddenException, không mở file")
    void download_otherStudentForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        stubSubmission(openAssignment(), STUDENT_ID);

        assertThatThrownBy(() -> service.download(OTHER_STUDENT_ID, SUBMISSION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(fileStorage, never()).open(any());
    }

    @Test
    @DisplayName("Giảng viên phụ trách tải file bài nộp")
    void download_assignedLecturer() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        stubSubmission(assignment(section(SectionStatus.IN_PROGRESS, lecturer), null, true, 3, GradingPolicy.LATEST),
                STUDENT_ID);
        when(fileStorage.open(STORED_PATH)).thenReturn(new ByteArrayResource(PDF));

        assertThat(service.download(LECTURER_ID, SUBMISSION_ID).fileName()).isEqualTo("bai-lam.pdf");
    }

    @Test
    @DisplayName("Bản ghi còn nhưng file đã mất trên máy chủ -> ResourceNotFoundException")
    void download_fileMissing() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSubmission(openAssignment(), STUDENT_ID);
        when(fileStorage.open(STORED_PATH)).thenThrow(new ResourceNotFoundException("Tệp tài liệu", STORED_PATH));

        assertThatThrownBy(() -> service.download(ADMIN_ID, SUBMISSION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- listByAssignment / listHistory ----------

    @Test
    @DisplayName("Giảng viên phụ trách xem tất cả bài nộp của bài tập")
    void listByAssignment_manager() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        Assignment assignment = stubFind(assignment(section(SectionStatus.IN_PROGRESS, lecturer), null, true, 3,
                GradingPolicy.LATEST));
        when(submissionRepository.findByAssignmentId(ASSIGNMENT_ID)).thenReturn(List.of(
                submission(41L, assignment, STUDENT_ID, 1), submission(42L, assignment, OTHER_STUDENT_ID, 1)));

        assertThat(service.listByAssignment(LECTURER_ID, ASSIGNMENT_ID)).hasSize(2);
    }

    @Test
    @DisplayName("Sinh viên xem tất cả bài nộp của bài tập -> ForbiddenException")
    void listByAssignment_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubFind(openAssignment());

        assertThatThrownBy(() -> service.listByAssignment(STUDENT_ID, ASSIGNMENT_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(submissionRepository, never()).findByAssignmentId(any());
    }

    @Test
    @DisplayName("Liệt kê bài nộp của bài tập không tồn tại -> ResourceNotFoundException")
    void listByAssignment_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByAssignment(ADMIN_ID, ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên xem lịch sử nộp của chính mình, lần mới nhất trước")
    void listHistory_self() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        Assignment assignment = stubFind(openAssignment());
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(List.of(submission(42L, assignment, STUDENT_ID, 2), submission(41L, assignment, STUDENT_ID, 1)));

        List<SubmissionResponse> history = service.listHistory(STUDENT_ID, ASSIGNMENT_ID, STUDENT_ID);

        assertThat(history).extracting(SubmissionResponse::attemptNumber).containsExactly(2, 1);
    }

    @Test
    @DisplayName("Sinh viên xem lịch sử nộp của người khác -> ForbiddenException")
    void listHistory_otherStudentForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        stubFind(openAssignment());

        assertThatThrownBy(() -> service.listHistory(OTHER_STUDENT_ID, ASSIGNMENT_ID, STUDENT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Admin xem lịch sử nộp của một sinh viên")
    void listHistory_admin() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubFind(openAssignment());
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(List.of(submission(41L, assignment, STUDENT_ID, 1)));

        assertThat(service.listHistory(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID)).hasSize(1);
    }

    // ---------- selectForGrading ----------

    @Test
    @DisplayName("Chính sách MANUAL: giảng viên chọn bài nộp, bài được chọn trước đó bị bỏ chọn")
    void selectForGrading_manual() {
        User lecturer = asUser(LECTURER_ID, RoleName.LECTURER);
        Assignment assignment = assignment(section(SectionStatus.IN_PROGRESS, lecturer), null, true, 3,
                GradingPolicy.MANUAL);
        Submission first = submission(41L, assignment, STUDENT_ID, 1);
        Submission second = submission(SUBMISSION_ID, assignment, STUDENT_ID, 2);
        Submission third = submission(43L, assignment, STUDENT_ID, 3);
        third.setSelectedForGrading(true);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(second));
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(List.of(third, second, first));

        SubmissionResponse response = service.selectForGrading(LECTURER_ID, SUBMISSION_ID);

        assertThat(response.selectedForGrading()).isTrue();
        assertThat(first.isSelectedForGrading()).isFalse();
        assertThat(second.isSelectedForGrading()).isTrue();
        assertThat(third.isSelectedForGrading()).isFalse();
    }

    @Test
    @DisplayName("Chính sách LATEST không cho chọn bài thủ công -> BusinessException")
    void selectForGrading_policyNotManual() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Submission submission = stubSubmission(openAssignment(), STUDENT_ID);

        assertThatThrownBy(() -> service.selectForGrading(ADMIN_ID, SUBMISSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("LATEST");

        assertThat(submission.isSelectedForGrading()).isFalse();
    }

    @Test
    @DisplayName("Sinh viên tự chọn bài nộp để chấm -> ForbiddenException")
    void selectForGrading_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubSubmission(assignment(section(SectionStatus.IN_PROGRESS, null), null, true, 3, GradingPolicy.MANUAL),
                STUDENT_ID);

        assertThatThrownBy(() -> service.selectForGrading(STUDENT_ID, SUBMISSION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách chọn bài nộp -> ForbiddenException")
    void selectForGrading_otherLecturerForbidden() {
        User assigned = user(LECTURER_ID, true, RoleName.LECTURER);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        stubSubmission(assignment(section(SectionStatus.IN_PROGRESS, assigned), null, true, 3, GradingPolicy.MANUAL),
                STUDENT_ID);

        assertThatThrownBy(() -> service.selectForGrading(OTHER_LECTURER_ID, SUBMISSION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Chọn bài nộp không tồn tại -> ResourceNotFoundException")
    void selectForGrading_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.selectForGrading(ADMIN_ID, SUBMISSION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- getSubmissionToGrade ----------

    @Test
    @DisplayName("LATEST: bài được chấm là lần nộp mới nhất")
    void toGrade_latest() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubFind(openAssignment());
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(Optional.of(submission(43L, assignment, STUDENT_ID, 3)));

        assertThat(service.getSubmissionToGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID).attemptNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("FIRST: bài được chấm là lần nộp đầu tiên")
    void toGrade_first() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubFind(assignment(section(SectionStatus.IN_PROGRESS, null), null, true, 3,
                GradingPolicy.FIRST));
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberAsc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(Optional.of(submission(41L, assignment, STUDENT_ID, 1)));

        assertThat(service.getSubmissionToGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID).attemptNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("MANUAL: bài được chấm là bài giảng viên đã chọn")
    void toGrade_manualSelected() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubFind(assignment(section(SectionStatus.IN_PROGRESS, null), null, true, 3,
                GradingPolicy.MANUAL));
        Submission first = submission(41L, assignment, STUDENT_ID, 1);
        first.setSelectedForGrading(true);
        Submission second = submission(42L, assignment, STUDENT_ID, 2);
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(List.of(second, first));

        assertThat(service.getSubmissionToGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID).id()).isEqualTo(41L);
    }

    @Test
    @DisplayName("MANUAL nhưng chưa chọn bài nào -> ResourceNotFoundException")
    void toGrade_manualNothingSelected() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubFind(assignment(section(SectionStatus.IN_PROGRESS, null), null, true, 3,
                GradingPolicy.MANUAL));
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(List.of(submission(41L, assignment, STUDENT_ID, 1)));

        assertThatThrownBy(() -> service.getSubmissionToGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Grade grade(Submission submission, double score) {
        Grade grade = new Grade();
        grade.setSubmission(submission);
        grade.setScore(score);
        return grade;
    }

    private Assignment stubHighestAssignment() {
        return stubFind(assignment(section(SectionStatus.IN_PROGRESS, null), null, true, 3, GradingPolicy.HIGHEST));
    }

    @Test
    @DisplayName("HIGHEST: bài được chấm là bài nộp có điểm cao nhất, không nhất thiết là bài mới nhất")
    void toGrade_highest() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubHighestAssignment();
        Submission first = submission(41L, assignment, STUDENT_ID, 1);
        Submission second = submission(42L, assignment, STUDENT_ID, 2);
        Submission third = submission(43L, assignment, STUDENT_ID, 3);
        when(gradeRepository.findBySubmissionOfAssignmentAndStudent(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(List.of(grade(first, 6.0), grade(second, 9.5), grade(third, 8.0)));

        assertThat(service.getSubmissionToGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID).id()).isEqualTo(42L);
    }

    @Test
    @DisplayName("HIGHEST: điểm bằng nhau thì lấy lần nộp sau")
    void toGrade_highestTieTakesLaterAttempt() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Assignment assignment = stubHighestAssignment();
        Submission first = submission(41L, assignment, STUDENT_ID, 1);
        Submission second = submission(42L, assignment, STUDENT_ID, 2);
        when(gradeRepository.findBySubmissionOfAssignmentAndStudent(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(List.of(grade(second, 8.0), grade(first, 8.0)));

        assertThat(service.getSubmissionToGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID).attemptNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("HIGHEST nhưng chưa có bài nộp nào được chấm điểm -> ResourceNotFoundException")
    void toGrade_highestNothingGraded() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubHighestAssignment();
        when(gradeRepository.findBySubmissionOfAssignmentAndStudent(ASSIGNMENT_ID, STUDENT_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getSubmissionToGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("HIGHEST không cho chọn bài nộp thủ công -> BusinessException")
    void selectForGrading_highestNotManual() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubSubmission(assignment(section(SectionStatus.IN_PROGRESS, null), null, true, 3, GradingPolicy.HIGHEST),
                STUDENT_ID);

        assertThatThrownBy(() -> service.selectForGrading(ADMIN_ID, SUBMISSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HIGHEST");
    }

    @Test
    @DisplayName("Sinh viên chưa nộp bài nào -> ResourceNotFoundException")
    void toGrade_noSubmissions() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubFind(openAssignment());
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(ASSIGNMENT_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubmissionToGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sinh viên hỏi bài nộp được chấm -> ForbiddenException")
    void toGrade_studentForbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        stubFind(openAssignment());

        assertThatThrownBy(() -> service.getSubmissionToGrade(STUDENT_ID, ASSIGNMENT_ID, STUDENT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Hỏi bài nộp được chấm của bài tập không tồn tại -> ResourceNotFoundException")
    void toGrade_assignmentNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubmissionToGrade(ADMIN_ID, ASSIGNMENT_ID, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
