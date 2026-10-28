package com.lms.service.impl;

import com.lms.dto.submission.SubmissionFile;
import com.lms.dto.submission.SubmissionResponse;
import com.lms.dto.submission.SubmitRequest;
import com.lms.entity.Assignment;
import com.lms.entity.CourseSection;
import com.lms.entity.Grade;
import com.lms.entity.Submission;
import com.lms.entity.User;
import com.lms.entity.enums.GradingPolicy;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.AssignmentRepository;
import com.lms.repository.GradeRepository;
import com.lms.repository.SubmissionRepository;
import com.lms.service.SubmissionService;
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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;
    private final FileStorage fileStorage;
    private final UploadValidator uploadValidator;
    private final SectionAccessChecker access;

    @Override
    @Transactional
    public SubmissionResponse submit(Long actorId, Long assignmentId, SubmitRequest request, MultipartFile file) {
        User student = access.findActiveUser(actorId);
        if (!access.hasRole(student, RoleName.STUDENT)) {
            throw new ForbiddenException("Chỉ sinh viên mới được nộp bài");
        }
        // Khóa dòng bài tập: hai lần nộp đồng thời của cùng sinh viên không thể vượt số lần nộp hay trùng attemptNumber.
        Assignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập", assignmentId));
        CourseSection section = assignment.getSection();
        if (!access.isEnrolledStudent(student, section)) {
            throw new ForbiddenException("Bạn chưa đăng ký hoặc đã rút khỏi lớp học phần này");
        }
        if (section.getStatus() == SectionStatus.ENDED) {
            throw new BusinessException("Lớp học phần đã kết thúc, không thể nộp bài");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean late = assignment.getDueDate() != null && now.isAfter(assignment.getDueDate());
        if (late && !assignment.isAllowLateSubmission()) {
            throw new BusinessException("Đã quá hạn nộp và bài tập không cho phép nộp muộn");
        }

        long used = submissionRepository.countByAssignmentIdAndStudentId(assignmentId, student.getId());
        if (assignment.getMaxSubmissions() != null && used >= assignment.getMaxSubmissions()) {
            throw new BusinessException("Bạn đã dùng hết " + assignment.getMaxSubmissions() + " lần nộp cho bài tập này");
        }

        ValidatedUpload upload = uploadValidator.validate(file);
        int attemptNumber = submissionRepository
                .findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignmentId, student.getId())
                .map(s -> s.getAttemptNumber() + 1)
                .orElse(1);

        // Tên lưu trên đĩa do server sinh nên các lần nộp không bao giờ ghi đè lên nhau.
        String storedName = UUID.randomUUID() + "." + upload.extension();
        String path = fileStorage.store("assignments/" + assignmentId + "/" + student.getId(), storedName,
                upload.content());

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setAttemptNumber(attemptNumber);
        submission.setContent(request == null ? null : request.content());
        submission.setFilePath(path);
        submission.setOriginalFileName(upload.originalFileName());
        submission.setContentType(upload.contentType());
        submission.setFileSize((long) upload.content().length);
        submission.setSubmittedAt(now);
        submission.setLate(late);

        Submission saved;
        try {
            saved = submissionRepository.save(submission);
        } catch (RuntimeException e) {
            StorageCleanup.deleteQuietly(fileStorage, path);
            throw e;
        }
        StorageCleanup.deleteOnRollback(fileStorage, path);
        return SubmissionResponse.from(saved);
    }

    @Override
    public SubmissionResponse getById(Long actorId, Long submissionId) {
        User actor = access.findActiveUser(actorId);
        Submission submission = findSubmission(submissionId);
        requireOwnerOrManager(actor, submission);
        return SubmissionResponse.from(submission);
    }

    @Override
    public SubmissionFile download(Long actorId, Long submissionId) {
        User actor = access.findActiveUser(actorId);
        Submission submission = findSubmission(submissionId);
        requireOwnerOrManager(actor, submission);
        return new SubmissionFile(
                fileStorage.open(submission.getFilePath()),
                submission.getOriginalFileName(),
                submission.getContentType(),
                submission.getFileSize());
    }

    @Override
    public List<SubmissionResponse> listByAssignment(Long actorId, Long assignmentId) {
        User actor = access.findActiveUser(actorId);
        Assignment assignment = findAssignment(assignmentId);
        access.requireManager(actor, assignment.getSection());
        return submissionRepository.findByAssignmentId(assignmentId).stream()
                .map(SubmissionResponse::from)
                .toList();
    }

    @Override
    public List<SubmissionResponse> listHistory(Long actorId, Long assignmentId, Long studentId) {
        User actor = access.findActiveUser(actorId);
        Assignment assignment = findAssignment(assignmentId);
        if (!actor.getId().equals(studentId)) {
            access.requireManager(actor, assignment.getSection());
        }
        return submissionRepository.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignmentId, studentId)
                .stream()
                .map(SubmissionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SubmissionResponse selectForGrading(Long actorId, Long submissionId) {
        User actor = access.findActiveUser(actorId);
        Submission submission = findSubmission(submissionId);
        Assignment assignment = submission.getAssignment();
        access.requireManager(actor, assignment.getSection());

        if (assignment.getGradingPolicy() != GradingPolicy.MANUAL) {
            throw new BusinessException("Bài tập dùng chính sách " + assignment.getGradingPolicy()
                    + ", bài nộp được chấm đã được xác định tự động");
        }

        List<Submission> attempts = submissionRepository.findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(
                assignment.getId(), submission.getStudent().getId());
        attempts.forEach(s -> s.setSelectedForGrading(s.getId().equals(submission.getId())));
        return SubmissionResponse.from(submission);
    }

    @Override
    public SubmissionResponse getSubmissionToGrade(Long actorId, Long assignmentId, Long studentId) {
        User actor = access.findActiveUser(actorId);
        Assignment assignment = findAssignment(assignmentId);
        access.requireManager(actor, assignment.getSection());

        Submission chosen = switch (assignment.getGradingPolicy()) {
            case LATEST -> submissionRepository
                    .findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignmentId, studentId)
                    .orElse(null);
            case FIRST -> submissionRepository
                    .findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberAsc(assignmentId, studentId)
                    .orElse(null);
            case MANUAL -> submissionRepository
                    .findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignmentId, studentId).stream()
                    .filter(Submission::isSelectedForGrading)
                    .findFirst()
                    .orElse(null);
            // Điểm lưu theo từng bài nộp ở Grade.submission; điểm bằng nhau thì lấy lần nộp sau.
            case HIGHEST -> gradeRepository.findBySubmissionOfAssignmentAndStudent(assignmentId, studentId).stream()
                    .max(Comparator.comparingDouble(Grade::getScore)
                            .thenComparingInt(g -> g.getSubmission().getAttemptNumber()))
                    .map(Grade::getSubmission)
                    .orElse(null);
        };
        if (chosen == null) {
            throw new ResourceNotFoundException("Bài nộp để chấm của sinh viên " + studentId
                    + " cho bài tập", assignmentId);
        }
        return SubmissionResponse.from(chosen);
    }

    // ---------- helpers ----------

    private Submission findSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài nộp", submissionId));
    }

    private Assignment findAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập", assignmentId));
    }

    /** Sinh viên chỉ xem bài nộp của chính mình; Admin và giảng viên phụ trách xem theo lớp. */
    private void requireOwnerOrManager(User actor, Submission submission) {
        if (!submission.getStudent().getId().equals(actor.getId())) {
            access.requireManager(actor, submission.getAssignment().getSection());
        }
    }
}
