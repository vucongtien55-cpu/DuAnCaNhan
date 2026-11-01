package com.lms.controller;

import com.lms.dto.submission.SubmissionFile;
import com.lms.dto.submission.SubmissionResponse;
import com.lms.dto.submission.SubmitRequest;
import com.lms.security.CurrentUser;
import com.lms.service.SubmissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Submissions")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final CurrentUser currentUser;

    /** multipart/form-data: file {@code file} (bắt buộc) và ghi chú {@code content} (tuỳ chọn). Sinh viên được xác định theo JWT. */
    @PostMapping(value = "/assignments/{assignmentId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> submit(@PathVariable Long assignmentId,
                                                     @Valid @ModelAttribute SubmitRequest request,
                                                     @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submissionService.submit(currentUser.id(), assignmentId, request, file));
    }

    /** Toàn bộ bài nộp của một bài tập: Admin và giảng viên phụ trách. */
    @GetMapping("/assignments/{assignmentId}/submissions")
    public List<SubmissionResponse> listByAssignment(@PathVariable Long assignmentId) {
        return submissionService.listByAssignment(currentUser.id(), assignmentId);
    }

    /** Lịch sử nộp của một sinh viên: chính sinh viên đó, Admin hoặc giảng viên phụ trách. */
    @GetMapping("/assignments/{assignmentId}/students/{studentId}/submissions")
    public List<SubmissionResponse> history(@PathVariable Long assignmentId, @PathVariable Long studentId) {
        return submissionService.listHistory(currentUser.id(), assignmentId, studentId);
    }

    /** Bài nộp sẽ được chấm của sinh viên theo chính sách của bài tập. */
    @GetMapping("/assignments/{assignmentId}/students/{studentId}/submission-to-grade")
    public SubmissionResponse submissionToGrade(@PathVariable Long assignmentId, @PathVariable Long studentId) {
        return submissionService.getSubmissionToGrade(currentUser.id(), assignmentId, studentId);
    }

    @GetMapping("/submissions/{submissionId}")
    public SubmissionResponse get(@PathVariable Long submissionId) {
        return submissionService.getById(currentUser.id(), submissionId);
    }

    @GetMapping("/submissions/{submissionId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long submissionId) {
        SubmissionFile f = submissionService.download(currentUser.id(), submissionId);
        return FileResponses.attachment(f.resource(), f.fileName(), f.contentType(), f.size());
    }

    /** Chọn bài nộp để chấm (chỉ khi bài tập dùng chính sách MANUAL). */
    @PutMapping("/submissions/{submissionId}/select-for-grading")
    public SubmissionResponse selectForGrading(@PathVariable Long submissionId) {
        return submissionService.selectForGrading(currentUser.id(), submissionId);
    }
}
