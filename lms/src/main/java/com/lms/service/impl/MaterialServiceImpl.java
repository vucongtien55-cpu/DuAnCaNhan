package com.lms.service.impl;

import com.lms.dto.material.MaterialFile;
import com.lms.dto.material.MaterialResponse;
import com.lms.dto.material.UploadMaterialRequest;
import com.lms.entity.CourseSection;
import com.lms.entity.Material;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.MaterialType;
import com.lms.entity.enums.RoleName;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.MaterialRepository;
import com.lms.repository.UserRepository;
import com.lms.service.MaterialService;
import com.lms.storage.FileStorage;
import com.lms.storage.StorageCleanup;
import com.lms.storage.UploadValidator;
import com.lms.storage.UploadValidator.ValidatedUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final CourseSectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;
    private final UploadValidator uploadValidator;

    @Override
    @Transactional
    public MaterialResponse upload(Long actorId, Long sectionId, UploadMaterialRequest request, MultipartFile file) {
        User actor = findActiveUser(actorId);
        CourseSection section = findSection(sectionId);
        requireManager(actor, section);

        ValidatedUpload upload = uploadValidator.validate(file);

        // Tên lưu trên đĩa do server sinh: không dùng tên từ client nên không thể trùng hay thoát thư mục.
        String storedName = UUID.randomUUID() + "." + upload.extension();
        String path = fileStorage.store("sections/" + sectionId, storedName, upload.content());

        Material material = new Material();
        material.setSection(section);
        material.setUploadedBy(actor);
        material.setTitle(request.title().trim());
        material.setDescription(request.description());
        material.setType(typeOf(upload.extension()));
        material.setFilePath(path);
        material.setOriginalFileName(upload.originalFileName());
        material.setContentType(upload.contentType());
        material.setFileSize((long) upload.content().length);

        Material saved;
        try {
            saved = materialRepository.save(material);
        } catch (RuntimeException e) {
            StorageCleanup.deleteQuietly(fileStorage, path);
            throw e;
        }
        // Nếu giao dịch rollback sau đó (ví dụ commit lỗi), file vừa ghi không được để mồ côi.
        StorageCleanup.deleteOnRollback(fileStorage, path);
        return MaterialResponse.from(saved);
    }

    @Override
    public List<MaterialResponse> listBySection(Long actorId, Long sectionId) {
        User actor = findActiveUser(actorId);
        CourseSection section = findSection(sectionId);
        requireReader(actor, section);
        return materialRepository.findBySectionIdOrderByCreatedAtDesc(sectionId).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @Override
    public MaterialResponse getById(Long actorId, Long materialId) {
        User actor = findActiveUser(actorId);
        Material material = findMaterial(materialId);
        requireReader(actor, material.getSection());
        return MaterialResponse.from(material);
    }

    @Override
    public MaterialFile download(Long actorId, Long materialId) {
        User actor = findActiveUser(actorId);
        Material material = findMaterial(materialId);
        requireReader(actor, material.getSection());
        return new MaterialFile(
                fileStorage.open(material.getFilePath()),
                material.getOriginalFileName(),
                material.getContentType(),
                material.getFileSize());
    }

    @Override
    @Transactional
    public void delete(Long actorId, Long materialId) {
        User actor = findActiveUser(actorId);
        Material material = findMaterial(materialId);
        requireManager(actor, material.getSection());

        String path = material.getFilePath();
        materialRepository.delete(material);
        // Xóa file sau khi bản ghi đã commit: nếu xóa bản ghi thất bại thì file vẫn còn nguyên.
        StorageCleanup.runAfterCommit(() -> StorageCleanup.deleteQuietly(fileStorage, path));
    }

    private MaterialType typeOf(String extension) {
        return switch (extension) {
            case "pdf", "docx" -> MaterialType.DOCUMENT;
            case "pptx" -> MaterialType.SLIDE;
            default -> MaterialType.OTHER;
        };
    }

    // ---------- lookup & access ----------

    private Material findMaterial(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài liệu", materialId));
    }

    private CourseSection findSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
        if (!user.isEnabled()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }
        return user;
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == roleName);
    }

    private boolean isManager(User actor, CourseSection section) {
        boolean assignedLecturer = hasRole(actor, RoleName.LECTURER)
                && section.getTeacher() != null
                && section.getTeacher().getId().equals(actor.getId());
        return hasRole(actor, RoleName.ADMIN) || assignedLecturer;
    }

    /** Tải lên, xóa: Admin hoặc giảng viên phụ trách lớp. */
    private void requireManager(User actor, CourseSection section) {
        if (!isManager(actor, section)) {
            throw new ForbiddenException("Bạn không có quyền quản lý tài liệu của lớp học phần này");
        }
    }

    /** Xem, tải: người quản lý lớp, hoặc sinh viên đang theo học (ENROLLED) lớp này. */
    private void requireReader(User actor, CourseSection section) {
        if (isManager(actor, section)) {
            return;
        }
        boolean enrolledStudent = hasRole(actor, RoleName.STUDENT)
                && enrollmentRepository.findByStudentIdAndSectionId(actor.getId(), section.getId())
                .map(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .orElse(false);
        if (!enrolledStudent) {
            throw new ForbiddenException("Bạn không có quyền truy cập tài liệu của lớp học phần này");
        }
    }
}
