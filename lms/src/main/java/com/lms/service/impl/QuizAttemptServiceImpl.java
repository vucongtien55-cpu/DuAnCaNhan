package com.lms.service.impl;

import com.lms.dto.quiz.QuizAttemptResponse;
import com.lms.dto.quiz.QuizAttemptResponse.OptionView;
import com.lms.dto.quiz.QuizAttemptResponse.QuestionView;
import com.lms.dto.quiz.QuizAttemptStaffResponse;
import com.lms.dto.quiz.QuizResponse;
import com.lms.dto.quiz.QuizResultResponse;
import com.lms.dto.quiz.SaveAnswerRequest;
import com.lms.dto.quiz.SaveAnswerResponse;
import com.lms.entity.AnswerOption;
import com.lms.entity.CourseSection;
import com.lms.entity.Enrollment;
import com.lms.entity.Grade;
import com.lms.entity.Question;
import com.lms.entity.Quiz;
import com.lms.entity.QuizAnswer;
import com.lms.entity.QuizAttempt;
import com.lms.entity.User;
import com.lms.entity.enums.AttemptStatus;
import com.lms.entity.enums.GradeType;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.AttemptExpiredException;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.GradeRepository;
import com.lms.repository.QuizAnswerRepository;
import com.lms.repository.QuizAttemptRepository;
import com.lms.repository.QuizRepository;
import com.lms.service.QuizAttemptService;
import com.lms.service.support.SectionAccessChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final GradeRepository gradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SectionAccessChecker access;
    private final TransactionOperations tx;

    // ---------- bắt đầu / tiếp tục ----------

    @Override
    // Việc chốt lượt quá hạn cũ phải được lưu dù sau đó yêu cầu bị từ chối vì lý do nghiệp vụ.
    @Transactional(noRollbackFor = BusinessException.class)
    public QuizAttemptResponse startAttempt(Long actorId, Long quizId) {
        User student = requireStudent(actorId);
        // Khóa dòng bài kiểm tra: hai yêu cầu đồng thời của cùng sinh viên không thể vượt số lần làm.
        Quiz quiz = quizRepository.findByIdForUpdate(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài kiểm tra", quizId));
        CourseSection section = quiz.getSection();
        if (!access.isEnrolledStudent(student, section)) {
            throw new ForbiddenException("Bạn chưa đăng ký hoặc đã rút khỏi lớp học phần này");
        }
        if (!quiz.isPublished()) {
            throw new ResourceNotFoundException("Bài kiểm tra", quizId);
        }
        if (section.getStatus() == SectionStatus.ENDED) {
            throw new BusinessException("Lớp học phần đã kết thúc, không thể làm bài kiểm tra");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<QuizAttempt> open = attemptRepository
                .findFirstByQuizIdAndStudentIdAndStatus(quizId, student.getId(), AttemptStatus.IN_PROGRESS);
        if (open.isPresent()) {
            QuizAttempt attempt = open.get();
            if (!now.isAfter(attempt.getDeadlineAt())) {
                return toAttemptView(attempt, now);
            }
            finalizeAttempt(attempt, AttemptStatus.EXPIRED, attempt.getDeadlineAt());
        }

        long used = attemptRepository.countByQuizIdAndStudentId(quizId, student.getId());
        if (used >= quiz.getMaxAttempts()) {
            throw new BusinessException("Bạn đã làm bài kiểm tra này " + used + " lần (tối đa "
                    + quiz.getMaxAttempts() + " lần)");
        }
        if (quiz.getStartTime() != null && now.isBefore(quiz.getStartTime())) {
            throw new BusinessException("Bài kiểm tra chưa mở");
        }
        if (quiz.getEndTime() != null && now.isAfter(quiz.getEndTime())) {
            throw new BusinessException("Bài kiểm tra đã đóng");
        }
        if (quiz.getQuestions().isEmpty() || quiz.getDurationMinutes() == null) {
            throw new BusinessException("Bài kiểm tra chưa được cấu hình đầy đủ");
        }

        LocalDateTime deadline = now.plusMinutes(quiz.getDurationMinutes());
        if (quiz.getEndTime() != null && quiz.getEndTime().isBefore(deadline)) {
            deadline = quiz.getEndTime();
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setAttemptNumber((int) used + 1);
        attempt.setStartedAt(now);
        attempt.setDeadlineAt(deadline);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        return toAttemptView(attemptRepository.save(attempt), now);
    }

    @Override
    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public QuizAttemptResponse getAttempt(Long actorId, Long attemptId) {
        User student = requireStudent(actorId);
        QuizAttempt attempt = findOwnedForUpdate(student, attemptId);
        LocalDateTime now = LocalDateTime.now();
        requireInProgress(attempt, now);
        return toAttemptView(attempt, now);
    }

    // ---------- lưu câu trả lời ----------

    @Override
    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public SaveAnswerResponse saveAnswer(Long actorId, Long attemptId, SaveAnswerRequest request) {
        User student = requireStudent(actorId);
        QuizAttempt attempt = findOwnedForUpdate(student, attemptId);
        LocalDateTime now = LocalDateTime.now();
        requireInProgress(attempt, now);

        Question question = attempt.getQuiz().getQuestions().stream()
                .filter(q -> q.getId().equals(request.questionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Câu hỏi không thuộc bài kiểm tra này"));
        Optional<QuizAnswer> existing = answerRepository.findByAttemptIdAndQuestionId(attemptId, question.getId());

        if (request.selectedOptionId() == null) {
            existing.ifPresent(answer -> {
                answer.setSelectedOptions(new HashSet<>());
                answerRepository.save(answer);
            });
            return new SaveAnswerResponse(question.getId(), null, remainingSeconds(attempt, now));
        }

        AnswerOption option = question.getOptions().stream()
                .filter(o -> o.getId().equals(request.selectedOptionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Lựa chọn không thuộc câu hỏi này"));
        QuizAnswer answer = existing.orElseGet(() -> {
            QuizAnswer created = new QuizAnswer();
            created.setAttempt(attempt);
            created.setQuestion(question);
            return created;
        });
        answer.setSelectedOptions(new HashSet<>(Set.of(option)));
        answer.setCorrect(null);
        answer.setScoreAwarded(null);
        answerRepository.save(answer);
        return new SaveAnswerResponse(question.getId(), option.getId(), remainingSeconds(attempt, now));
    }

    // ---------- nộp bài / kết quả ----------

    @Override
    @Transactional
    public QuizResultResponse submit(Long actorId, Long attemptId) {
        User student = requireStudent(actorId);
        QuizAttempt attempt = findOwnedForUpdate(student, attemptId);
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(attempt.getDeadlineAt())) {
                // Nộp muộn: chỉ tính các câu đã lưu trước hạn, thời điểm nộp ghi nhận là hạn chót.
                finalizeAttempt(attempt, AttemptStatus.EXPIRED, attempt.getDeadlineAt());
            } else {
                finalizeAttempt(attempt, AttemptStatus.SUBMITTED, now);
            }
        }
        return toResult(attempt);
    }

    @Override
    @Transactional
    public QuizResultResponse getMyResult(Long actorId, Long attemptId) {
        User student = requireStudent(actorId);
        QuizAttempt attempt = findOwnedForUpdate(student, attemptId);
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            if (!LocalDateTime.now().isAfter(attempt.getDeadlineAt())) {
                throw new BusinessException("Bài làm chưa được nộp");
            }
            finalizeAttempt(attempt, AttemptStatus.EXPIRED, attempt.getDeadlineAt());
        }
        return toResult(attempt);
    }

    @Override
    public List<QuizResultResponse> listMyAttempts(Long actorId, Long quizId) {
        User student = requireStudent(actorId);
        quizRepository.findById(quizId).orElseThrow(() -> new ResourceNotFoundException("Bài kiểm tra", quizId));
        return attemptRepository.findByQuizIdAndStudentIdOrderByAttemptNumberDesc(quizId, student.getId()).stream()
                .map(this::toResult)
                .toList();
    }

    // ---------- Admin / giảng viên ----------

    @Override
    public List<QuizAttemptStaffResponse> listAttempts(Long actorId, Long quizId) {
        User actor = access.findActiveUser(actorId);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài kiểm tra", quizId));
        access.requireManager(actor, quiz.getSection());

        Map<Long, Grade> gradeByAttempt = gradeRepository.findByQuizAttemptOfQuiz(quizId).stream()
                .collect(Collectors.toMap(g -> g.getQuizAttempt().getId(), Function.identity(), (a, b) -> a));
        double maxScore = QuizResponse.maxScoreOf(quiz);
        return attemptRepository.findByQuizId(quizId).stream()
                .map(attempt -> {
                    Grade grade = gradeByAttempt.get(attempt.getId());
                    return new QuizAttemptStaffResponse(
                            attempt.getId(),
                            attempt.getStudent().getId(),
                            attempt.getStudent().getFullName(),
                            attempt.getAttemptNumber(),
                            attempt.getStatus(),
                            attempt.getStartedAt(),
                            attempt.getSubmittedAt(),
                            attempt.getScore(),
                            maxScore,
                            grade == null ? null : grade.getId(),
                            grade != null && grade.isPublished());
                })
                .toList();
    }

    @Override
    @Transactional
    public int publishQuizGrades(Long actorId, Long quizId) {
        User actor = access.findActiveUser(actorId);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài kiểm tra", quizId));
        access.requireManager(actor, quiz.getSection());

        List<Grade> pending = gradeRepository.findByQuizAttemptOfQuiz(quizId).stream()
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

    // ---------- tự động chốt bài quá hạn ----------

    @Override
    // Không bao bọc cả vòng lặp trong một giao dịch: mỗi lượt được chốt trong giao dịch riêng, một lượt lỗi không kéo
    // theo các lượt khác.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int autoSubmitExpiredAttempts() {
        List<Long> ids = attemptRepository.findIdsByStatusAndDeadlineAtBefore(
                AttemptStatus.IN_PROGRESS, LocalDateTime.now());
        int finalized = 0;
        for (Long id : ids) {
            try {
                if (Boolean.TRUE.equals(tx.execute(status -> finalizeIfExpired(id)))) {
                    finalized++;
                }
            } catch (RuntimeException e) {
                log.error("Không thể tự động chốt lượt làm bài {}", id, e);
            }
        }
        return finalized;
    }

    private boolean finalizeIfExpired(Long attemptId) {
        QuizAttempt attempt = attemptRepository.findByIdForUpdate(attemptId).orElse(null);
        if (attempt == null || attempt.getStatus() != AttemptStatus.IN_PROGRESS
                || !LocalDateTime.now().isAfter(attempt.getDeadlineAt())) {
            return false;
        }
        finalizeAttempt(attempt, AttemptStatus.EXPIRED, attempt.getDeadlineAt());
        return true;
    }

    // ---------- chấm điểm ----------

    /** Chốt bài và tự chấm bằng các câu đã lưu. Câu chưa chọn là bỏ trống (0 điểm, không có bản ghi). */
    private void finalizeAttempt(QuizAttempt attempt, AttemptStatus status, LocalDateTime submittedAt) {
        Quiz quiz = attempt.getQuiz();
        Map<Long, QuizAnswer> answers = new HashMap<>();
        for (QuizAnswer answer : answerRepository.findByAttemptId(attempt.getId())) {
            answers.put(answer.getQuestion().getId(), answer);
        }

        double total = 0;
        for (Question question : quiz.getQuestions()) {
            QuizAnswer answer = answers.get(question.getId());
            if (answer == null || answer.getSelectedOptions().isEmpty()) {
                continue;
            }
            boolean correct = answer.getSelectedOptions().size() == 1
                    && answer.getSelectedOptions().iterator().next().isCorrect();
            answer.setCorrect(correct);
            answer.setScoreAwarded(correct ? question.getScore() : 0.0);
            total += correct ? question.getScore() : 0.0;
        }

        attempt.setScore(total);
        attempt.setStatus(status);
        attempt.setSubmittedAt(submittedAt);
        createGradeIfAbsent(attempt, quiz, total);
    }

    private void createGradeIfAbsent(QuizAttempt attempt, Quiz quiz, double score) {
        if (gradeRepository.findByQuizAttemptId(attempt.getId()).isPresent()) {
            return;
        }
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndSectionId(attempt.getStudent().getId(), quiz.getSection().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy đăng ký của sinh viên " + attempt.getStudent().getId()));
        Grade grade = new Grade();
        grade.setEnrollment(enrollment);
        grade.setQuizAttempt(attempt);
        grade.setType(GradeType.QUIZ);
        grade.setTitle(quiz.getTitle());
        grade.setScore(score);
        grade.setMaxScore(QuizResponse.maxScoreOf(quiz));
        grade.setPublished(false);
        gradeRepository.save(grade);
    }

    // ---------- helpers ----------

    private User requireStudent(Long actorId) {
        User user = access.findActiveUser(actorId);
        if (!access.hasRole(user, RoleName.STUDENT)) {
            throw new ForbiddenException("Chức năng này chỉ dành cho sinh viên");
        }
        return user;
    }

    private QuizAttempt findOwnedForUpdate(User student, Long attemptId) {
        QuizAttempt attempt = attemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt làm bài", attemptId));
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("Bạn không có quyền truy cập lượt làm bài này");
        }
        return attempt;
    }

    /** Lượt phải đang làm và còn thời gian; nếu đã quá hạn thì chốt bài rồi báo hết giờ. */
    private void requireInProgress(QuizAttempt attempt, LocalDateTime now) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("Bài làm đã kết thúc");
        }
        if (now.isAfter(attempt.getDeadlineAt())) {
            finalizeAttempt(attempt, AttemptStatus.EXPIRED, attempt.getDeadlineAt());
            throw new AttemptExpiredException("Đã hết giờ làm bài, bài làm đã được tự động nộp");
        }
    }

    private long remainingSeconds(QuizAttempt attempt, LocalDateTime now) {
        return Math.max(0, Duration.between(now, attempt.getDeadlineAt()).getSeconds());
    }

    /** Đề cho sinh viên: không có cờ đáp án đúng và lời giải. Thứ tự câu xáo trộn ổn định theo mã lượt làm bài. */
    private QuizAttemptResponse toAttemptView(QuizAttempt attempt, LocalDateTime now) {
        Quiz quiz = attempt.getQuiz();
        List<Question> questions = new ArrayList<>(quiz.getQuestions());
        if (quiz.isShuffleQuestions()) {
            Collections.shuffle(questions, new Random(attempt.getId()));
        }
        List<QuestionView> questionViews = questions.stream()
                .map(q -> new QuestionView(q.getId(), q.getContent(), q.getScore(), q.getOptions().stream()
                        .map(o -> new OptionView(o.getId(), o.getContent()))
                        .toList()))
                .toList();

        Map<Long, Long> saved = new HashMap<>();
        for (QuizAnswer answer : answerRepository.findByAttemptId(attempt.getId())) {
            if (answer.getSelectedOptions().size() == 1) {
                saved.put(answer.getQuestion().getId(), answer.getSelectedOptions().iterator().next().getId());
            }
        }
        return new QuizAttemptResponse(attempt.getId(), quiz.getId(), quiz.getTitle(), attempt.getAttemptNumber(),
                attempt.getStatus(), attempt.getStartedAt(), attempt.getDeadlineAt(),
                remainingSeconds(attempt, now), questionViews, saved);
    }

    /** Điểm chỉ lộ ra khi Grade đã công bố. */
    private QuizResultResponse toResult(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        Optional<Grade> grade = attempt.getStatus() == AttemptStatus.IN_PROGRESS
                ? Optional.empty()
                : gradeRepository.findByQuizAttemptId(attempt.getId());
        boolean published = grade.map(Grade::isPublished).orElse(false);
        return new QuizResultResponse(attempt.getId(), quiz.getId(), quiz.getTitle(), attempt.getAttemptNumber(),
                attempt.getStatus(), attempt.getStartedAt(), attempt.getSubmittedAt(), published,
                published ? grade.get().getScore() : null,
                published ? grade.get().getMaxScore() : null);
    }
}
