package com.lms.service.impl;

import com.lms.dto.assignment.AssignmentAttachment;
import com.lms.dto.assignment.AssignmentResponse;
import com.lms.entity.Assignment;
import com.lms.entity.User;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.AssignmentRepository;
import com.lms.service.AssignmentAttachmentService;
import com.lms.service.support.SectionAccessChecker;
import com.lms.storage.FileStorage;
import com.lms.storage.StorageCleanup;
import com.lms.storage.UploadValidator;
import com.lms.storage.UploadValidator.ValidatedUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentAttachmentServiceImpl implements AssignmentAttachmentService {

    private final AssignmentRepository assignmentRepository;
    private final FileStorage fileStorage;
    private final UploadValidator uploadValidator;
    private final SectionAccessChecker access;

    @Override
    @Transactional
    public AssignmentResponse upload(Long actorId, Long assignmentId, MultipartFile file) {
        User actor = access.findActiveUser(actorId);
        // Khóa dòng bài tập để hai lần tải lên đồng thời không để lại file mồ côi.
        Assignment assignment = findForUpdate(assignmentId);
        access.requireManager(actor, assignment.getSection());
        requireNotEnded(assignment);

        ValidatedUpload upload = uploadValidator.validate(file);

        // Tên lưu trên đĩa do server sinh: không dùng tên từ client nên không thể trùng hay thoát thư mục.
        String storedName = UUID.randomUUID() + "." + upload.extension();
        String path = fileStorage.store("assignment-attachments/" + assignmentId, storedName, upload.content());

        String oldPath = assignment.getAttachmentPath();
        assignment.setAttachmentPath(path);
        assignment.setAttachmentFileName(upload.originalFileName());
        assignment.setAttachmentContentType(upload.contentType());
        assignment.setAttachmentSize((long) upload.content().length);

        Assignment saved;
        try {
            saved = assignmentRepository.save(assignment);
        } catch (RuntimeException e) {
            StorageCleanup.deleteQuietly(fileStorage, path);
            throw e;
        }
        StorageCleanup.deleteOnRollback(fileStorage, path);
        if (oldPath != null) {
            // File cũ chỉ bị xóa sau khi bản ghi trỏ sang file mới đã commit.
            StorageCleanup.runAfterCommit(() -> StorageCleanup.deleteQuietly(fileStorage, oldPath));
        }
        return AssignmentResponse.from(saved);
    }

    @Override
    public AssignmentAttachment download(Long actorId, Long assignmentId) {
        User actor = access.findActiveUser(actorId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập", assignmentId));
        access.requireReader(actor, assignment.getSection());
        if (assignment.getAttachmentPath() == null) {
            throw new ResourceNotFoundException("Tệp đính kèm của bài tập", assignmentId);
        }
        return new AssignmentAttachment(
                fileStorage.open(assignment.getAttachmentPath()),
                assignment.getAttachmentFileName(),
                assignment.getAttachmentContentType(),
                assignment.getAttachmentSize());
    }

    @Override
    @Transactional
    public AssignmentResponse remove(Long actorId, Long assignmentId) {
        User actor = access.findActiveUser(actorId);
        Assignment assignment = findForUpdate(assignmentId);
        access.requireManager(actor, assignment.getSection());
        requireNotEnded(assignment);

        String oldPath = assignment.getAttachmentPath();
        if (oldPath == null) {
            throw new ResourceNotFoundException("Tệp đính kèm của bài tập", assignmentId);
        }
        assignment.setAttachmentPath(null);
        assignment.setAttachmentFileName(null);
        assignment.setAttachmentContentType(null);
        assignment.setAttachmentSize(null);
        Assignment saved = assignmentRepository.save(assignment);
        StorageCleanup.runAfterCommit(() -> StorageCleanup.deleteQuietly(fileStorage, oldPath));
        return AssignmentResponse.from(saved);
    }

    private Assignment findForUpdate(Long assignmentId) {
        return assignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập", assignmentId));
    }

    private void requireNotEnded(Assignment assignment) {
        if (assignment.getSection().getStatus() == SectionStatus.ENDED) {
            throw new BusinessException("Lớp học phần đã kết thúc, không thể thay đổi file đính kèm");
        }
    }
}
