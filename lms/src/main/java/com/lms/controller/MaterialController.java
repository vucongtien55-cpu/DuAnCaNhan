package com.lms.controller;

import com.lms.dto.material.MaterialFile;
import com.lms.dto.material.MaterialResponse;
import com.lms.dto.material.UploadMaterialRequest;
import com.lms.security.CurrentUser;
import com.lms.service.MaterialService;
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

@Tag(name = "Materials")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final CurrentUser currentUser;

    /** multipart/form-data: các field {@code title}, {@code description} và file {@code file}. */
    @PostMapping(value = "/sections/{sectionId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> upload(@PathVariable Long sectionId,
                                                   @Valid @ModelAttribute UploadMaterialRequest request,
                                                   @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.upload(currentUser.id(), sectionId, request, file));
    }

    @GetMapping("/sections/{sectionId}/materials")
    public List<MaterialResponse> list(@PathVariable Long sectionId) {
        return materialService.listBySection(currentUser.id(), sectionId);
    }

    @GetMapping("/materials/{materialId}")
    public MaterialResponse get(@PathVariable Long materialId) {
        return materialService.getById(currentUser.id(), materialId);
    }

    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long materialId) {
        MaterialFile f = materialService.download(currentUser.id(), materialId);
        return FileResponses.attachment(f.resource(), f.fileName(), f.contentType(), f.size());
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<Void> delete(@PathVariable Long materialId) {
        materialService.delete(currentUser.id(), materialId);
        return ResponseEntity.noContent().build();
    }
}
