package com.lms.controller;

import com.lms.dto.enrollment.EnrollmentResponse;
import com.lms.dto.user.UserSummaryResponse;
import com.lms.security.CurrentUser;
import com.lms.service.EnrollmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Enrollments")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final CurrentUser currentUser;

    public record EnrollRequest(@NotBlank String sectionCode) {
    }

    public record AddStudentRequest(@NotNull Long studentId) {
    }

    public record WithdrawRequest(@Size(max = 255) String reason) {
    }

    /** Sinh viên tự đăng ký bằng mã lớp. */
    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentResponse> enroll(@Valid @RequestBody EnrollRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(currentUser.id(), request.sectionCode()));
    }

    @GetMapping("/enrollments/me")
    public List<EnrollmentResponse> myEnrollments() {
        return enrollmentService.listMyEnrollments(currentUser.id());
    }

    @GetMapping("/sections/{sectionId}/enrollments")
    public List<EnrollmentResponse> listBySection(@PathVariable Long sectionId) {
        return enrollmentService.listBySection(currentUser.id(), sectionId);
    }

    /**
     * Tìm sinh viên để thêm vào lớp (Admin hoặc giảng viên phụ trách). Chỉ trả id, username, họ tên của sinh viên đang
     * hoạt động và chưa đăng ký lớp này; cần từ khóa tối thiểu 2 ký tự.
     */
    @GetMapping("/sections/{sectionId}/student-candidates")
    public Page<UserSummaryResponse> studentCandidates(@PathVariable Long sectionId,
                                                       @RequestParam String keyword, @ParameterObject Pageable pageable) {
        return enrollmentService.searchStudentCandidates(currentUser.id(), sectionId, keyword,
                PageRequests.restrict(pageable, Sort.by("fullName", "id"), "id", "username", "fullName"));
    }

    @PostMapping("/sections/{sectionId}/enrollments")
    public ResponseEntity<EnrollmentResponse> addStudent(@PathVariable Long sectionId,
                                                         @Valid @RequestBody AddStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.addStudent(currentUser.id(), sectionId, request.studentId()));
    }

    /** Rút/gỡ sinh viên: bản ghi chuyển sang DROPPED, không xóa dữ liệu. Body (lý do) là tuỳ chọn. */
    @PostMapping("/sections/{sectionId}/enrollments/{studentId}/withdraw")
    public EnrollmentResponse withdraw(@PathVariable Long sectionId, @PathVariable Long studentId,
                                       @Valid @RequestBody(required = false) WithdrawRequest request) {
        return enrollmentService.withdraw(currentUser.id(), sectionId, studentId,
                request == null ? null : request.reason());
    }
}
