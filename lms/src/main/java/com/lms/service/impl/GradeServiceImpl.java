package com.lms.service.impl;

import com.lms.dto.grade.GradeResponse;
import com.lms.dto.grade.GradeSubmissionRequest;
import com.lms.dto.grade.StudentGradeResponse;
import com.lms.entity.Assignment;
import com.lms.entity.Enrollment;
import com.lms.entity.Grade;
import com.lms.entity.Submission;
import com.lms.entity.User;
import com.lms.entity.enums.GradeType;
import com.lms.entity.enums.RoleName;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.AssignmentRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.GradeRepository;
import com.lms.repository.SubmissionRepository;
import com.lms.service.GradeService;
import com.lms.service.support.OfficialGradeResolver;
import com.lms.service.support.SectionAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SectionAccessChecker access;
    private final OfficialGradeResolver officialGrades;

    // ---------- chấm điểm ----------

    @Override
    @Transactional
    public GradeResponse gradeSubmission(Long actorId, Long submissionId, GradeSubmissionRequest request) {
        User actor = access.findActiveUser(actorId);
        Submission submission = findSubmission(submissionId);
        Assignment assignment = submission.getAssignment();
        access.requireManager(actor, assignment.getSection());
        validateScore(request.score(), assignment.getMaxScore());

        Optional<Grade> existing = gradeRepository.findBySubmissionId(submissionId);
        if (existing.isPresent() && existing.get().isPublished()) {
            throw new BusinessException("Điểm đã công bố, hãy hủy công bố trước khi chỉnh sửa");
        }

        Grade grade = existing.orElseGet(() -> newGrade(submission, assignment));
        grade.setScore(request.score());
        grade.setMaxScore(assignment.getMaxScore());
        grade.setFeedback(blankToNull(request.feedback()));
        grade.setInternalNote(blankToNull(request.internalNote()));
        grade.setGradedBy(actor);
        grade.setGradedAt(LocalDateTime.now());
        return GradeResponse.from(gradeRepository.save(grade));
    }

    private Grade newGrade(Submission submission, Assignment assignment) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndSectionId(submission.getStudent().getId(), assignment.getSection().getId())
                .orElseThrow(() -> new BusinessException("Sinh viên không có đăng ký trong lớp học phần này"));
        Grade grade = new Grade();
        grade.setEnrollment(enrollment);
        grade.setSubmission(submission);
        grade.setType(GradeType.ASSIGNMENT);
        grade.setTitle(assignment.getTitle());
        return grade;
    }

    private void validateScore(Double score, Double maxScore) {
        if (score == null || score.isNaN() || score.isInfinite()) {
            throw new BusinessException("Điểm không hợp lệ");
        }
        if (score < 0) {
            throw new BusinessException("Điểm không được âm");
        }
        if (score > maxScore) {
            throw new BusinessException("Điểm (" + score + ") không được vượt quá điểm tối đa của bài tập (" + maxScore + ")");
        }
    }

    // ---------- xem điểm (giảng viên / Admin) ----------

    @Override
    public GradeResponse getBySubmission(Long actorId, Long submissionId) {
        User actor = access.findActiveUser(actorId);
        Submission submission = findSubmission(submissionId);
        access.requireManager(actor, submission.getAssignment().getSection());
        return gradeRepository.findBySubmissionId(submissionId)
                .map(GradeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm của bài nộp", submissionId));
    }

    @Override
    public List<GradeResponse> listByAssignment(Long actorId, Long assignmentId) {
        User actor = access.findActiveUser(actorId);
        Assignment assignment = findAssignment(assignmentId);
        access.requireManager(actor, assignment.getSection());
        return gradeRepository.findBySubmissionOfAssignment(assignmentId).stream()
                .map(GradeResponse::from)
                .toList();
    }

    @Override
    public GradeResponse getOfficialGrade(Long actorId, Long assignmentId, Long studentId) {
        User actor = access.findActiveUser(actorId);
        Assignment assignment = findAssignment(assignmentId);
        access.requireManager(actor, assignment.getSection());
        return resolveOfficialGrade(assignment, studentId)
                .map(GradeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Điểm chính thức của sinh viên " + studentId + " cho bài tập", assignmentId));
    }

    // ---------- công bố điểm ----------

    @Override
    @Transactional
    public GradeResponse publishGrade(Long actorId, Long gradeId) {
        User actor = access.findActiveUser(actorId);
        Grade grade = findGrade(gradeId);
        access.requireManager(actor, grade.getEnrollment().getSection());
        if (grade.isPublished()) {
            throw new BusinessException("Điểm đã được công bố");
        }
        publish(grade, actor);
        return GradeResponse.from(grade);
    }

    @Override
    @Transactional
    public GradeResponse unpublishGrade(Long actorId, Long gradeId) {
        User actor = access.findActiveUser(actorId);
        Grade grade = findGrade(gradeId);
        access.requireManager(actor, grade.getEnrollment().getSection());
        if (!grade.isPublished()) {
            throw new BusinessException("Điểm chưa được công bố");
        }
        grade.setPublished(false);
        grade.setPublishedAt(null);
        grade.setPublishedBy(null);
        return GradeResponse.from(grade);
    }

    @Override
    @Transactional
    public int publishAssignmentGrades(Long actorId, Long assignmentId) {
        User actor = access.findActiveUser(actorId);
        Assignment assignment = findAssignment(assignmentId);
        access.requireManager(actor, assignment.getSection());

        List<Grade> pending = gradeRepository.findBySubmissionOfAssignment(assignmentId).stream()
                .filter(g -> !g.isPublished())
                .toList();
        pending.forEach(g -> publish(g, actor));
        return pending.size();
    }

    private void publish(Grade grade, User actor) {
        grade.setPublished(true);
        grade.setPublishedAt(LocalDateTime.now());
        grade.setPublishedBy(actor);
    }

    // ---------- xem điểm (sinh viên) ----------

    @Override
    public StudentGradeResponse getMyGrade(Long actorId, Long assignmentId) {
        User student = requireStudent(actorId);
        Assignment assignment = findAssignment(assignmentId);
        requireOwnEnrollment(student, assignment);
        return toStudentView(assignment, student.getId());
    }

    @Override
    public List<StudentGradeResponse> listMyGrades(Long actorId, Long sectionId) {
        User student = requireStudent(actorId);
        enrollmentRepository.findByStudentIdAndSectionId(student.getId(), sectionId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có đăng ký trong lớp học phần này"));
        return assignmentRepository.findBySectionIdOrderByDueDateAsc(sectionId).stream()
                .map(assignment -> toStudentView(assignment, student.getId()))
                .toList();
    }

    /** Sinh viên chỉ nhận điểm chính thức đã công bố; mọi trường hợp khác chỉ là "chưa công bố". */
    private StudentGradeResponse toStudentView(Assignment assignment, Long studentId) {
        return resolveOfficialGrade(assignment, studentId)
                .filter(Grade::isPublished)
                .map(grade -> StudentGradeResponse.of(assignment, grade))
                .orElseGet(() -> StudentGradeResponse.notPublished(assignment));
    }

    private User requireStudent(Long actorId) {
        User user = access.findActiveUser(actorId);
        if (!access.hasRole(user, RoleName.STUDENT)) {
            throw new ForbiddenException("Chức năng này chỉ dành cho sinh viên");
        }
        return user;
    }

    private void requireOwnEnrollment(User student, Assignment assignment) {
        if (enrollmentRepository.findByStudentIdAndSectionId(student.getId(), assignment.getSection().getId()).isEmpty()) {
            throw new ForbiddenException("Bạn không có đăng ký trong lớp học phần này");
        }
    }

    // ---------- chính sách chọn bài nộp được tính điểm ----------

    /** Điểm chính thức theo chính sách của bài tập (xem {@link OfficialGradeResolver}). */
    private Optional<Grade> resolveOfficialGrade(Assignment assignment, Long studentId) {
        return officialGrades.resolve(assignment, studentId);
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

    private Grade findGrade(Long gradeId) {
        return gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm", gradeId));
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
