package com.lms.service.impl;

import com.lms.dto.assignment.AssignmentResponse;
import com.lms.dto.assignment.CreateAssignmentRequest;
import com.lms.dto.assignment.UpdateAssignmentRequest;
import com.lms.entity.Assignment;
import com.lms.entity.CourseSection;
import com.lms.entity.User;
import com.lms.entity.enums.GradingPolicy;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.AssignmentRepository;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.SubmissionRepository;
import com.lms.service.AssignmentService;
import com.lms.service.support.SectionAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseSectionRepository sectionRepository;
    private final SubmissionRepository submissionRepository;
    private final SectionAccessChecker access;

    @Override
    @Transactional
    public AssignmentResponse create(Long actorId, CreateAssignmentRequest request) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = findSection(request.sectionId());
        access.requireManager(actor, section);
        requireNotEnded(section);

        if (request.dueDate() != null && request.dueDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Hạn nộp phải ở tương lai");
        }

        Assignment assignment = new Assignment();
        assignment.setSection(section);
        assignment.setCreatedBy(actor);
        assignment.setTitle(request.title().trim());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        assignment.setMaxScore(request.maxScore());
        assignment.setMaxSubmissions(request.maxSubmissions());
        assignment.setAllowLateSubmission(request.allowLateSubmission() == null || request.allowLateSubmission());
        assignment.setGradingPolicy(request.gradingPolicy() == null ? GradingPolicy.LATEST : request.gradingPolicy());
        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public AssignmentResponse update(Long actorId, Long assignmentId, UpdateAssignmentRequest request) {
        User actor = access.findActiveUser(actorId);
        // Khóa dòng bài tập để không chạy đua với việc nộp bài đang diễn ra.
        Assignment assignment = findForUpdate(assignmentId);
        access.requireManager(actor, assignment.getSection());
        requireNotEnded(assignment.getSection());

        if (submissionRepository.existsByAssignmentId(assignmentId)) {
            if (Double.compare(request.maxScore(), assignment.getMaxScore()) != 0) {
                throw new BusinessException("Bài tập đã có bài nộp, không thể thay đổi điểm tối đa");
            }
            if (request.gradingPolicy() != assignment.getGradingPolicy()) {
                throw new BusinessException("Bài tập đã có bài nộp, không thể thay đổi chính sách chọn bài chấm");
            }
            if (request.maxSubmissions() != null) {
                int used = submissionRepository.findMaxAttemptNumberByAssignmentId(assignmentId);
                if (request.maxSubmissions() < used) {
                    throw new BusinessException("Số lần nộp tối đa (" + request.maxSubmissions()
                            + ") không được nhỏ hơn số lần sinh viên đã nộp (" + used + ")");
                }
            }
        }

        assignment.setTitle(request.title().trim());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        assignment.setMaxScore(request.maxScore());
        assignment.setMaxSubmissions(request.maxSubmissions());
        assignment.setAllowLateSubmission(request.allowLateSubmission());
        assignment.setGradingPolicy(request.gradingPolicy());
        return AssignmentResponse.from(assignment);
    }

    @Override
    public AssignmentResponse getById(Long actorId, Long assignmentId) {
        User actor = access.findActiveUser(actorId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập", assignmentId));
        access.requireReader(actor, assignment.getSection());
        return AssignmentResponse.from(assignment);
    }

    @Override
    public List<AssignmentResponse> listBySection(Long actorId, Long sectionId) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = findSection(sectionId);
        access.requireReader(actor, section);
        return assignmentRepository.findBySectionIdOrderByDueDateAsc(sectionId).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long actorId, Long assignmentId) {
        User actor = access.findActiveUser(actorId);
        Assignment assignment = findForUpdate(assignmentId);
        access.requireManager(actor, assignment.getSection());
        requireNotEnded(assignment.getSection());

        if (submissionRepository.existsByAssignmentId(assignmentId)) {
            throw new BusinessException("Bài tập đã có bài nộp, không thể xóa để giữ lịch sử nộp bài");
        }
        assignmentRepository.delete(assignment);
    }

    private Assignment findForUpdate(Long assignmentId) {
        return assignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập", assignmentId));
    }

    private CourseSection findSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
    }

    private void requireNotEnded(CourseSection section) {
        if (section.getStatus() == SectionStatus.ENDED) {
            throw new BusinessException("Lớp học phần đã kết thúc, không thể thay đổi bài tập");
        }
    }
}
