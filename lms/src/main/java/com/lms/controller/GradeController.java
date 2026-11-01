package com.lms.controller;

import com.lms.dto.grade.FinalGradeResponse;
import com.lms.dto.grade.GradeResponse;
import com.lms.dto.grade.GradeSubmissionRequest;
import com.lms.dto.grade.GradeWeightRequest;
import com.lms.dto.grade.GradeWeightResponse;
import com.lms.dto.grade.StudentFinalGradeResponse;
import com.lms.dto.grade.StudentGradeResponse;
import com.lms.security.CurrentUser;
import com.lms.service.FinalGradeService;
import com.lms.service.GradeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Điểm bài tập và điểm tổng kết. Quyền theo lớp được service kiểm tra: Admin và giảng viên phụ trách quản lý điểm,
 * sinh viên chỉ xem điểm đã công bố của chính mình (danh tính luôn lấy từ JWT).
 */
@Tag(name = "Grades")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final FinalGradeService finalGradeService;
    private final CurrentUser currentUser;

    public record CountResponse(int count) {
    }

    // ---------- điểm bài tập: giảng viên phụ trách / Admin ----------

    /** Chấm hoặc chấm lại một bài nộp (điểm đã công bố phải hủy công bố trước). */
    @PutMapping("/submissions/{submissionId}/grade")
    public GradeResponse grade(@PathVariable Long submissionId, @Valid @RequestBody GradeSubmissionRequest request) {
        return gradeService.gradeSubmission(currentUser.id(), submissionId, request);
    }

    @GetMapping("/submissions/{submissionId}/grade")
    public GradeResponse getBySubmission(@PathVariable Long submissionId) {
        return gradeService.getBySubmission(currentUser.id(), submissionId);
    }

    @GetMapping("/assignments/{assignmentId}/grades")
    public List<GradeResponse> listByAssignment(@PathVariable Long assignmentId) {
        return gradeService.listByAssignment(currentUser.id(), assignmentId);
    }

    @GetMapping("/assignments/{assignmentId}/students/{studentId}/grade")
    public GradeResponse officialGrade(@PathVariable Long assignmentId, @PathVariable Long studentId) {
        return gradeService.getOfficialGrade(currentUser.id(), assignmentId, studentId);
    }

    @PostMapping("/assignments/{assignmentId}/grades/publish")
    public CountResponse publishAssignmentGrades(@PathVariable Long assignmentId) {
        return new CountResponse(gradeService.publishAssignmentGrades(currentUser.id(), assignmentId));
    }

    @PostMapping("/grades/{gradeId}/publish")
    public GradeResponse publish(@PathVariable Long gradeId) {
        return gradeService.publishGrade(currentUser.id(), gradeId);
    }

    @PostMapping("/grades/{gradeId}/unpublish")
    public GradeResponse unpublish(@PathVariable Long gradeId) {
        return gradeService.unpublishGrade(currentUser.id(), gradeId);
    }

    // ---------- điểm bài tập: sinh viên xem điểm của chính mình ----------

    @GetMapping("/assignments/{assignmentId}/my-grade")
    public StudentGradeResponse myGrade(@PathVariable Long assignmentId) {
        return gradeService.getMyGrade(currentUser.id(), assignmentId);
    }

    @GetMapping("/sections/{sectionId}/my-grades")
    public List<StudentGradeResponse> myGrades(@PathVariable Long sectionId) {
        return gradeService.listMyGrades(currentUser.id(), sectionId);
    }

    // ---------- trọng số và điểm tổng kết ----------

    @PutMapping("/sections/{sectionId}/grade-weights")
    public List<GradeWeightResponse> setWeights(@PathVariable Long sectionId,
                                                @Valid @RequestBody List<@Valid GradeWeightRequest> weights) {
        return finalGradeService.setWeights(currentUser.id(), sectionId, weights);
    }

    @GetMapping("/sections/{sectionId}/grade-weights")
    public List<GradeWeightResponse> getWeights(@PathVariable Long sectionId) {
        return finalGradeService.getWeights(currentUser.id(), sectionId);
    }

    @PostMapping("/sections/{sectionId}/final-grades/calculate")
    public List<FinalGradeResponse> calculate(@PathVariable Long sectionId) {
        return finalGradeService.calculate(currentUser.id(), sectionId);
    }

    @GetMapping("/sections/{sectionId}/final-grades")
    public List<GradeResponse> listFinalGrades(@PathVariable Long sectionId) {
        return finalGradeService.listFinalGrades(currentUser.id(), sectionId);
    }

    @PostMapping("/sections/{sectionId}/final-grades/publish")
    public CountResponse publishFinalGrades(@PathVariable Long sectionId) {
        return new CountResponse(finalGradeService.publishFinalGrades(currentUser.id(), sectionId));
    }

    @PostMapping("/sections/{sectionId}/final-grades/unpublish")
    public CountResponse unpublishFinalGrades(@PathVariable Long sectionId) {
        return new CountResponse(finalGradeService.unpublishFinalGrades(currentUser.id(), sectionId));
    }

    @GetMapping("/sections/{sectionId}/my-final-grade")
    public StudentFinalGradeResponse myFinalGrade(@PathVariable Long sectionId) {
        return finalGradeService.getMyFinalGrade(currentUser.id(), sectionId);
    }
}
