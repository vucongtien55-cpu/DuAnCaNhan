package com.lms.service.support;

import com.lms.entity.Assignment;
import com.lms.entity.Grade;
import com.lms.entity.Submission;
import com.lms.repository.GradeRepository;
import com.lms.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

/** Xác định điểm chính thức của một sinh viên cho một bài tập theo {@code Assignment.gradingPolicy}. */
@Component
@RequiredArgsConstructor
public class OfficialGradeResolver {

    private final SubmissionRepository submissionRepository;
    private final GradeRepository gradeRepository;

    /**
     * Với LATEST/FIRST/MANUAL, nếu bài nộp được chọn chưa có điểm thì chưa có điểm chính thức (không tự lấy bài
     * khác). Với HIGHEST: điểm lớn nhất, bằng nhau thì lần nộp sau. Có thể trả về điểm chưa công bố; người gọi tự
     * lọc theo {@code Grade.published}.
     */
    public Optional<Grade> resolve(Assignment assignment, Long studentId) {
        Long assignmentId = assignment.getId();
        return switch (assignment.getGradingPolicy()) {
            case LATEST -> submissionRepository
                    .findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignmentId, studentId)
                    .flatMap(s -> gradeRepository.findBySubmissionId(s.getId()));
            case FIRST -> submissionRepository
                    .findFirstByAssignmentIdAndStudentIdOrderByAttemptNumberAsc(assignmentId, studentId)
                    .flatMap(s -> gradeRepository.findBySubmissionId(s.getId()));
            case MANUAL -> submissionRepository
                    .findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(assignmentId, studentId).stream()
                    .filter(Submission::isSelectedForGrading)
                    .findFirst()
                    .flatMap(s -> gradeRepository.findBySubmissionId(s.getId()));
            case HIGHEST -> gradeRepository.findBySubmissionOfAssignmentAndStudent(assignmentId, studentId).stream()
                    .max(Comparator.comparingDouble(Grade::getScore)
                            .thenComparingInt(g -> g.getSubmission().getAttemptNumber()));
        };
    }
}
