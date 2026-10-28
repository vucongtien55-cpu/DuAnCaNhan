package com.lms.service.impl;

import com.lms.dto.grade.FinalGradeResponse;
import com.lms.dto.grade.FinalGradeResponse.ComponentResult;
import com.lms.dto.grade.FinalGradeStatus;
import com.lms.dto.grade.GradeResponse;
import com.lms.dto.grade.GradeWeightRequest;
import com.lms.dto.grade.GradeWeightResponse;
import com.lms.dto.grade.StudentFinalGradeResponse;
import com.lms.entity.Assignment;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Grade;
import com.lms.entity.Quiz;
import com.lms.entity.SectionGradeWeight;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.GradeType;
import com.lms.entity.enums.RoleName;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.AssignmentRepository;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.GradeRepository;
import com.lms.repository.QuizRepository;
import com.lms.repository.SectionGradeWeightRepository;
import com.lms.service.FinalGradeService;
import com.lms.service.support.OfficialGradeResolver;
import com.lms.service.support.SectionAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinalGradeServiceImpl implements FinalGradeService {

    /** Tổng trọng số bắt buộc (phần trăm). */
    static final BigDecimal TOTAL_WEIGHT = new BigDecimal("100");
    /** Điểm tổng kết tính trên thang 10. */
    static final double FINAL_MAX_SCORE = 10.0;
    /** Điểm tổng kết lưu với 2 chữ số thập phân. */
    static final int SCORE_DECIMALS = 2;
    static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    /** Độ chính xác của các phép chia trung gian; chỉ làm tròn kết quả ở bước cuối. */
    private static final int RATIO_SCALE = 10;
    private static final int WEIGHT_DECIMALS = 2;
    private static final Set<GradeType> WEIGHTED_TYPES = EnumSet.of(GradeType.ASSIGNMENT, GradeType.QUIZ);

    private final SectionGradeWeightRepository weightRepository;
    private final CourseSectionRepository sectionRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final OfficialGradeResolver officialGrades;
    private final SectionAccessChecker access;

    // ---------- cấu hình trọng số ----------

    @Override
    @Transactional
    public List<GradeWeightResponse> setWeights(Long actorId, Long sectionId, List<GradeWeightRequest> weights) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = findSectionForUpdate(sectionId);
        access.requireManager(actor, section);

        Map<GradeType, BigDecimal> wanted = validateRequest(weights);
        boolean finalGradesPublished = gradeRepository.findBySectionIdAndType(sectionId, GradeType.COURSE_TOTAL)
                .stream().anyMatch(Grade::isPublished);
        if (finalGradesPublished) {
            throw new BusinessException("Đã có điểm tổng kết được công bố, hãy hủy công bố trước khi đổi trọng số");
        }

        // Cập nhật tại chỗ để không đụng ràng buộc unique (lớp, loại điểm) khi xóa rồi thêm lại cùng loại.
        Map<GradeType, SectionGradeWeight> existing = new EnumMap<>(GradeType.class);
        weightRepository.findBySectionId(sectionId).forEach(w -> existing.put(w.getGradeType(), w));
        List<SectionGradeWeight> saved = new ArrayList<>();
        wanted.forEach((type, value) -> {
            SectionGradeWeight row = existing.remove(type);
            if (row == null) {
                row = new SectionGradeWeight();
                row.setSection(section);
                row.setGradeType(type);
            }
            row.setWeight(value);
            saved.add(weightRepository.save(row));
        });
        weightRepository.deleteAll(existing.values());
        return saved.stream().map(GradeWeightResponse::from).toList();
    }

    @Override
    public List<GradeWeightResponse> getWeights(Long actorId, Long sectionId) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = findSection(sectionId);
        access.requireReader(actor, section);
        return weightRepository.findBySectionId(sectionId).stream()
                .sorted(Comparator.comparing(SectionGradeWeight::getGradeType))
                .map(GradeWeightResponse::from)
                .toList();
    }

    private Map<GradeType, BigDecimal> validateRequest(List<GradeWeightRequest> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new BusinessException("Cần cấu hình trọng số cho ít nhất một thành phần");
        }
        Map<GradeType, BigDecimal> wanted = new EnumMap<>(GradeType.class);
        for (GradeWeightRequest request : weights) {
            if (request == null || request.gradeType() == null || request.weight() == null) {
                throw new BusinessException("Mỗi thành phần cần có loại điểm và trọng số");
            }
            if (!WEIGHTED_TYPES.contains(request.gradeType())) {
                throw new BusinessException("Thành phần " + request.gradeType() + " chưa được hỗ trợ tính điểm");
            }
            BigDecimal weight = request.weight();
            if (weight.signum() <= 0 || weight.compareTo(TOTAL_WEIGHT) > 0) {
                throw new BusinessException("Trọng số của " + request.gradeType() + " phải lớn hơn 0 và không quá 100");
            }
            if (weight.stripTrailingZeros().scale() > WEIGHT_DECIMALS) {
                throw new BusinessException("Trọng số chỉ được có tối đa " + WEIGHT_DECIMALS + " chữ số thập phân");
            }
            if (wanted.put(request.gradeType(), weight.setScale(WEIGHT_DECIMALS, RoundingMode.UNNECESSARY)) != null) {
                throw new BusinessException("Thành phần " + request.gradeType() + " bị khai báo trùng");
            }
        }
        requireTotalWeight(wanted.values());
        return wanted;
    }

    private void requireTotalWeight(Iterable<BigDecimal> weights) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal weight : weights) {
            total = total.add(weight);
        }
        if (total.compareTo(TOTAL_WEIGHT) != 0) {
            throw new BusinessException("Tổng trọng số phải bằng " + TOTAL_WEIGHT.toPlainString() + "% (hiện tại "
                    + total.stripTrailingZeros().toPlainString() + "%)");
        }
    }

    /** Kiểm tra lại cấu hình đang lưu trước khi tính, không tin vào dữ liệu đã lưu từ trước. */
    private Map<GradeType, BigDecimal> loadValidConfiguration(Long sectionId) {
        List<SectionGradeWeight> rows = weightRepository.findBySectionId(sectionId);
        if (rows.isEmpty()) {
            throw new BusinessException("Lớp học phần chưa cấu hình trọng số điểm");
        }
        Map<GradeType, BigDecimal> weights = new EnumMap<>(GradeType.class);
        for (SectionGradeWeight row : rows) {
            if (!WEIGHTED_TYPES.contains(row.getGradeType())) {
                throw new BusinessException("Cấu hình có thành phần " + row.getGradeType() + " chưa được hỗ trợ");
            }
            weights.put(row.getGradeType(), row.getWeight());
        }
        requireTotalWeight(weights.values());
        return weights;
    }

    // ---------- tính điểm tổng kết ----------

    @Override
    @Transactional
    public List<FinalGradeResponse> calculate(Long actorId, Long sectionId) {
        User actor = access.findActiveUser(actorId);
        // Khóa lớp: hai lần tính đồng thời không tạo ra hai điểm tổng kết cho cùng một sinh viên.
        CourseSection section = findSectionForUpdate(sectionId);
        access.requireManager(actor, section);
        Map<GradeType, BigDecimal> weights = loadValidConfiguration(sectionId);

        List<Assignment> assignments = assignmentRepository.findBySectionIdOrderByDueDateAsc(sectionId);
        List<List<Grade>> quizGrades = new ArrayList<>();
        for (Quiz quiz : quizRepository.findBySectionId(sectionId)) {
            quizGrades.add(gradeRepository.findByQuizAttemptOfQuiz(quiz.getId()));
        }
        Map<Long, Grade> existingTotals = new HashMap<>();
        gradeRepository.findBySectionIdAndType(sectionId, GradeType.COURSE_TOTAL)
                .forEach(g -> existingTotals.put(g.getEnrollment().getId(), g));

        List<FinalGradeResponse> results = new ArrayList<>();
        for (Enrollment enrollment : enrollmentRepository.findBySectionId(sectionId)) {
            if (enrollment.getStatus() != EnrollmentStatus.ENROLLED
                    && enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
                continue;
            }
            results.add(calculateFor(enrollment, weights, assignments, quizGrades,
                    existingTotals.get(enrollment.getId()), actor));
        }
        return results;
    }

    private FinalGradeResponse calculateFor(Enrollment enrollment, Map<GradeType, BigDecimal> weights,
                                            List<Assignment> assignments, List<List<Grade>> quizGrades,
                                            Grade existing, User actor) {
        User student = enrollment.getStudent();
        if (existing != null && existing.isPublished()) {
            return new FinalGradeResponse(enrollment.getId(), student.getId(), student.getFullName(),
                    FinalGradeStatus.LOCKED, existing.getScore(), existing.getMaxScore(), existing.getId(), true,
                    List.of(), List.of());
        }

        List<ComponentResult> components = new ArrayList<>();
        List<GradeType> missing = new ArrayList<>();
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (Map.Entry<GradeType, BigDecimal> entry : weights.entrySet()) {
            List<BigDecimal> ratios = entry.getKey() == GradeType.ASSIGNMENT
                    ? assignmentRatios(assignments, student.getId())
                    : quizRatios(quizGrades, enrollment.getId());
            if (ratios.isEmpty()) {
                missing.add(entry.getKey());
                continue;
            }
            BigDecimal mean = ratios.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(ratios.size()), RATIO_SCALE, ROUNDING);
            weightedSum = weightedSum.add(mean.multiply(entry.getValue()));
            components.add(new ComponentResult(entry.getKey(), entry.getValue(),
                    mean.multiply(TOTAL_WEIGHT).setScale(SCORE_DECIMALS, ROUNDING), ratios.size()));
        }

        if (!missing.isEmpty()) {
            // Thiếu điểm thành phần: không có điểm tổng kết; điểm tổng kết cũ (chưa công bố) không còn đúng nên bị xóa.
            if (existing != null) {
                gradeRepository.delete(existing);
            }
            return new FinalGradeResponse(enrollment.getId(), student.getId(), student.getFullName(),
                    FinalGradeStatus.INCOMPLETE, null, null, null, false, components, missing);
        }

        BigDecimal finalScore = weightedSum.multiply(BigDecimal.valueOf(FINAL_MAX_SCORE))
                .divide(TOTAL_WEIGHT, RATIO_SCALE, ROUNDING)
                .setScale(SCORE_DECIMALS, ROUNDING);
        Grade total = existing != null ? existing : newTotalGrade(enrollment);
        total.setScore(finalScore.doubleValue());
        total.setMaxScore(FINAL_MAX_SCORE);
        total.setGradedBy(actor);
        total.setGradedAt(LocalDateTime.now());
        Grade saved = gradeRepository.save(total);
        return new FinalGradeResponse(enrollment.getId(), student.getId(), student.getFullName(),
                FinalGradeStatus.COMPUTED, saved.getScore(), saved.getMaxScore(), saved.getId(), false,
                components, List.of());
    }

    private Grade newTotalGrade(Enrollment enrollment) {
        Grade grade = new Grade();
        grade.setEnrollment(enrollment);
        grade.setType(GradeType.COURSE_TOTAL);
        grade.setTitle("Điểm tổng kết");
        grade.setPublished(false);
        return grade;
    }

    /** Điểm chính thức đã công bố của từng bài tập, quy về tỷ lệ 0-1. Bài chưa có điểm công bố không được tính. */
    private List<BigDecimal> assignmentRatios(List<Assignment> assignments, Long studentId) {
        List<BigDecimal> ratios = new ArrayList<>();
        for (Assignment assignment : assignments) {
            officialGrades.resolve(assignment, studentId)
                    .filter(Grade::isPublished)
                    .flatMap(this::ratioOf)
                    .ifPresent(ratios::add);
        }
        return ratios;
    }

    /** Với mỗi bài kiểm tra: tỷ lệ của lượt làm có điểm đã công bố cao nhất. */
    private List<BigDecimal> quizRatios(List<List<Grade>> gradesPerQuiz, Long enrollmentId) {
        List<BigDecimal> ratios = new ArrayList<>();
        for (List<Grade> grades : gradesPerQuiz) {
            grades.stream()
                    .filter(g -> g.getEnrollment() != null && g.getEnrollment().getId().equals(enrollmentId))
                    .filter(Grade::isPublished)
                    .map(this::ratioOf)
                    .flatMap(Optional::stream)
                    .max(Comparator.naturalOrder())
                    .ifPresent(ratios::add);
        }
        return ratios;
    }

    private Optional<BigDecimal> ratioOf(Grade grade) {
        if (grade.getScore() == null || grade.getMaxScore() == null || grade.getMaxScore() <= 0) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(grade.getScore())
                .divide(BigDecimal.valueOf(grade.getMaxScore()), RATIO_SCALE, ROUNDING));
    }

    // ---------- xem và công bố ----------

    @Override
    public List<GradeResponse> listFinalGrades(Long actorId, Long sectionId) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = findSection(sectionId);
        access.requireManager(actor, section);
        return gradeRepository.findBySectionIdAndType(sectionId, GradeType.COURSE_TOTAL).stream()
                .map(GradeResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public int publishFinalGrades(Long actorId, Long sectionId) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = findSectionForUpdate(sectionId);
        access.requireManager(actor, section);

        List<Grade> pending = gradeRepository.findBySectionIdAndType(sectionId, GradeType.COURSE_TOTAL).stream()
                .filter(g -> !g.isPublished())
                .toList();
        LocalDateTime now = LocalDateTime.now();
        pending.forEach(grade -> {
            grade.setPublished(true);
            grade.setPublishedAt(now);
            grade.setPublishedBy(actor);
        });
        return pending.size();
    }

    @Override
    @Transactional
    public int unpublishFinalGrades(Long actorId, Long sectionId) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = findSectionForUpdate(sectionId);
        access.requireManager(actor, section);

        List<Grade> published = gradeRepository.findBySectionIdAndType(sectionId, GradeType.COURSE_TOTAL).stream()
                .filter(Grade::isPublished)
                .toList();
        published.forEach(grade -> {
            grade.setPublished(false);
            grade.setPublishedAt(null);
            grade.setPublishedBy(null);
        });
        return published.size();
    }

    @Override
    public StudentFinalGradeResponse getMyFinalGrade(Long actorId, Long sectionId) {
        User student = access.findActiveUser(actorId);
        if (!access.hasRole(student, RoleName.STUDENT)) {
            throw new ForbiddenException("Chức năng này chỉ dành cho sinh viên");
        }
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndSectionId(student.getId(), sectionId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có đăng ký trong lớp học phần này"));
        return gradeRepository.findByEnrollmentIdAndType(enrollment.getId(), GradeType.COURSE_TOTAL)
                .filter(Grade::isPublished)
                .map(g -> new StudentFinalGradeResponse(sectionId, true, g.getScore(), g.getMaxScore(),
                        g.getPublishedAt()))
                .orElseGet(() -> new StudentFinalGradeResponse(sectionId, false, null, null, null));
    }

    // ---------- helpers ----------

    private CourseSection findSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
    }

    private CourseSection findSectionForUpdate(Long sectionId) {
        return sectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
    }
}
