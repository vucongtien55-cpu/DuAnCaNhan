package com.lms.controller;

import com.lms.dto.section.CreateSectionRequest;
import com.lms.dto.section.SectionResponse;
import com.lms.dto.section.UpdateSectionInfoRequest;
import com.lms.dto.section.UpdateSectionRequest;
import com.lms.entity.enums.SectionStatus;
import com.lms.security.CurrentUser;
import com.lms.service.CourseSectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Course Sections")
@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class CourseSectionController {

    private final CourseSectionService sectionService;
    private final CurrentUser currentUser;

    public record AssignTeacherRequest(@NotNull Long teacherId) {
    }

    public record ChangeStatusRequest(@NotNull SectionStatus status) {
    }

    @PostMapping
    public ResponseEntity<SectionResponse> create(@Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.create(currentUser.id(), request));
    }

    @GetMapping
    public Page<SectionResponse> list(@RequestParam(required = false) Long courseId,
                                      @RequestParam(required = false) SectionStatus status,
                                      @ParameterObject Pageable pageable) {
        return sectionService.list(currentUser.id(), courseId, status, PageRequests.restrict(pageable,
                Sort.by("id"), "id", "sectionCode", "name", "semester", "startDate", "endDate", "maxStudents",
                "status", "createdAt"));
    }

    @GetMapping("/by-code/{sectionCode}")
    public SectionResponse getByCode(@PathVariable String sectionCode) {
        return sectionService.getByCode(currentUser.id(), sectionCode);
    }

    @GetMapping("/{sectionId}")
    public SectionResponse get(@PathVariable Long sectionId) {
        return sectionService.getById(currentUser.id(), sectionId);
    }

    @PutMapping("/{sectionId}")
    public SectionResponse update(@PathVariable Long sectionId, @Valid @RequestBody UpdateSectionRequest request) {
        return sectionService.update(currentUser.id(), sectionId, request);
    }

    @PatchMapping("/{sectionId}/info")
    public SectionResponse updateInfo(@PathVariable Long sectionId,
                                      @Valid @RequestBody UpdateSectionInfoRequest request) {
        return sectionService.updateInfo(currentUser.id(), sectionId, request);
    }

    @PutMapping("/{sectionId}/teacher")
    public SectionResponse assignTeacher(@PathVariable Long sectionId,
                                         @Valid @RequestBody AssignTeacherRequest request) {
        return sectionService.assignTeacher(currentUser.id(), sectionId, request.teacherId());
    }

    @PutMapping("/{sectionId}/status")
    public SectionResponse changeStatus(@PathVariable Long sectionId,
                                        @Valid @RequestBody ChangeStatusRequest request) {
        return sectionService.changeStatus(currentUser.id(), sectionId, request.status());
    }

    @PostMapping("/{sectionId}/regenerate-code")
    public SectionResponse regenerateCode(@PathVariable Long sectionId) {
        return sectionService.regenerateSectionCode(currentUser.id(), sectionId);
    }
}
