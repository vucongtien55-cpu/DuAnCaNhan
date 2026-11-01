package com.lms.controller;

import com.lms.dto.assignment.AssignmentAttachment;
import com.lms.dto.assignment.AssignmentResponse;
import com.lms.dto.assignment.CreateAssignmentRequest;
import com.lms.dto.assignment.UpdateAssignmentRequest;
import com.lms.security.CurrentUser;
import com.lms.service.AssignmentAttachmentService;
import com.lms.service.AssignmentService;
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

@Tag(name = "Assignments")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AssignmentAttachmentService attachmentService;
    private final CurrentUser currentUser;

    @PostMapping("/assignments")
    public ResponseEntity<AssignmentResponse> create(@Valid @RequestBody CreateAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.create(currentUser.id(), request));
    }

    @GetMapping("/sections/{sectionId}/assignments")
    public List<AssignmentResponse> listBySection(@PathVariable Long sectionId) {
        return assignmentService.listBySection(currentUser.id(), sectionId);
    }

    @GetMapping("/assignments/{assignmentId}")
    public AssignmentResponse get(@PathVariable Long assignmentId) {
        return assignmentService.getById(currentUser.id(), assignmentId);
    }

    @PutMapping("/assignments/{assignmentId}")
    public AssignmentResponse update(@PathVariable Long assignmentId,
                                     @Valid @RequestBody UpdateAssignmentRequest request) {
        return assignmentService.update(currentUser.id(), assignmentId, request);
    }

    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long assignmentId) {
        assignmentService.delete(currentUser.id(), assignmentId);
        return ResponseEntity.noContent().build();
    }

    /** multipart/form-data với file {@code file}; thay thế file đính kèm cũ nếu có. */
    @PutMapping(value = "/assignments/{assignmentId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssignmentResponse uploadAttachment(@PathVariable Long assignmentId,
                                               @RequestPart("file") MultipartFile file) {
        return attachmentService.upload(currentUser.id(), assignmentId, file);
    }

    @GetMapping("/assignments/{assignmentId}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long assignmentId) {
        AssignmentAttachment f = attachmentService.download(currentUser.id(), assignmentId);
        return FileResponses.attachment(f.resource(), f.fileName(), f.contentType(), f.size());
    }

    @DeleteMapping("/assignments/{assignmentId}/attachment")
    public AssignmentResponse removeAttachment(@PathVariable Long assignmentId) {
        return attachmentService.remove(currentUser.id(), assignmentId);
    }
}
