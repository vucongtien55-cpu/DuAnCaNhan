package com.lms.controller;

import com.lms.dto.course.CourseResponse;
import com.lms.dto.course.CreateCourseRequest;
import com.lms.dto.course.UpdateCourseRequest;
import com.lms.entity.enums.CourseStatus;
import com.lms.security.CurrentUser;
import com.lms.service.CourseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Courses")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CurrentUser currentUser;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(currentUser.id(), request));
    }

    @GetMapping
    public Page<CourseResponse> list(@RequestParam(required = false) CourseStatus status,
                                    @ParameterObject Pageable pageable) {
        return courseService.list(currentUser.id(), status, PageRequests.restrict(pageable, Sort.by("id"),
                "id", "code", "title", "credits", "status", "createdAt", "updatedAt"));
    }

    @GetMapping("/{courseId}")
    public CourseResponse get(@PathVariable Long courseId) {
        return courseService.getById(currentUser.id(), courseId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{courseId}")
    public CourseResponse update(@PathVariable Long courseId, @Valid @RequestBody UpdateCourseRequest request) {
        return courseService.update(currentUser.id(), courseId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(@PathVariable Long courseId) {
        courseService.delete(currentUser.id(), courseId);
        return ResponseEntity.noContent().build();
    }
}
