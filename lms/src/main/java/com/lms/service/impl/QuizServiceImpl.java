package com.lms.service.impl;

import com.lms.dto.quiz.CreateQuizRequest;
import com.lms.dto.quiz.QuizOptionRequest;
import com.lms.dto.quiz.QuizQuestionRequest;
import com.lms.dto.quiz.QuizResponse;
import com.lms.dto.quiz.QuizSummaryResponse;
import com.lms.dto.quiz.UpdateQuizRequest;
import com.lms.entity.AnswerOption;
import com.lms.entity.CourseSection;
import com.lms.entity.Question;
import com.lms.entity.Quiz;
import com.lms.entity.User;
import com.lms.entity.enums.QuestionType;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.QuizAttemptRepository;
import com.lms.repository.QuizRepository;
import com.lms.service.QuizService;
import com.lms.service.support.SectionAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizServiceImpl implements QuizService {

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 6;

    private final QuizRepository quizRepository;
    private final CourseSectionRepository sectionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final SectionAccessChecker access;

    @Override
    @Transactional
    public QuizResponse create(Long actorId, CreateQuizRequest request) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", request.sectionId()));
        access.requireManager(actor, section);
        requireSectionNotEnded(section);
        validateWindow(request.startTime(), request.endTime());
        if (request.endTime() != null && request.endTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Thời điểm đóng bài kiểm tra phải ở tương lai");
        }

        Quiz quiz = new Quiz();
        quiz.setSection(section);
        quiz.setCreatedBy(actor);
        quiz.setTitle(request.title().trim());
        quiz.setDescription(request.description());
        quiz.setDurationMinutes(request.durationMinutes());
        quiz.setStartTime(request.startTime());
        quiz.setEndTime(request.endTime());
        quiz.setMaxAttempts(request.maxAttempts() == null ? 1 : request.maxAttempts());
        quiz.setShuffleQuestions(request.shuffleQuestions());
        quiz.setPublished(false);
        if (request.questions() != null) {
            quiz.getQuestions().addAll(buildQuestions(quiz, request.questions()));
        }
        return QuizResponse.from(quizRepository.save(quiz));
    }

    @Override
    @Transactional
    public QuizResponse update(Long actorId, Long quizId, UpdateQuizRequest request) {
        User actor = access.findActiveUser(actorId);
        Quiz quiz = findQuiz(quizId);
        access.requireManager(actor, quiz.getSection());
        requireSectionNotEnded(quiz.getSection());
        validateWindow(request.startTime(), request.endTime());

        if (attemptRepository.existsByQuizId(quizId)) {
            if (!request.durationMinutes().equals(quiz.getDurationMinutes())) {
                throw new BusinessException("Bài kiểm tra đã có lượt làm bài, không thể đổi thời lượng");
            }
            if (!request.maxAttempts().equals(quiz.getMaxAttempts())) {
                throw new BusinessException("Bài kiểm tra đã có lượt làm bài, không thể đổi số lần được làm");
            }
        }

        quiz.setTitle(request.title().trim());
        quiz.setDescription(request.description());
        quiz.setDurationMinutes(request.durationMinutes());
        quiz.setStartTime(request.startTime());
        quiz.setEndTime(request.endTime());
        quiz.setMaxAttempts(request.maxAttempts());
        quiz.setShuffleQuestions(request.shuffleQuestions());
        return QuizResponse.from(quiz);
    }

    @Override
    @Transactional
    public QuizResponse replaceQuestions(Long actorId, Long quizId, List<QuizQuestionRequest> questions) {
        User actor = access.findActiveUser(actorId);
        Quiz quiz = findQuiz(quizId);
        access.requireManager(actor, quiz.getSection());
        requireSectionNotEnded(quiz.getSection());
        if (attemptRepository.existsByQuizId(quizId)) {
            throw new BusinessException("Bài kiểm tra đã có lượt làm bài, không thể thay đổi câu hỏi");
        }
        List<Question> rebuilt = buildQuestions(quiz, questions == null ? List.of() : questions);
        if (quiz.isPublished() && rebuilt.isEmpty()) {
            throw new BusinessException("Bài kiểm tra đã công bố phải có ít nhất một câu hỏi");
        }

        quiz.getQuestions().clear();
        quiz.getQuestions().addAll(rebuilt);
        return QuizResponse.from(quiz);
    }

    @Override
    @Transactional
    public QuizResponse publish(Long actorId, Long quizId) {
        User actor = access.findActiveUser(actorId);
        Quiz quiz = findQuiz(quizId);
        access.requireManager(actor, quiz.getSection());
        requireSectionNotEnded(quiz.getSection());
        if (quiz.isPublished()) {
            throw new BusinessException("Bài kiểm tra đã được công bố");
        }
        if (quiz.getQuestions().isEmpty()) {
            throw new BusinessException("Bài kiểm tra cần có ít nhất một câu hỏi trước khi công bố");
        }
        quiz.setPublished(true);
        return QuizResponse.from(quiz);
    }

    @Override
    @Transactional
    public QuizResponse unpublish(Long actorId, Long quizId) {
        User actor = access.findActiveUser(actorId);
        Quiz quiz = findQuiz(quizId);
        access.requireManager(actor, quiz.getSection());
        if (!quiz.isPublished()) {
            throw new BusinessException("Bài kiểm tra chưa được công bố");
        }
        if (attemptRepository.existsByQuizId(quizId)) {
            throw new BusinessException("Bài kiểm tra đã có lượt làm bài, không thể hủy công bố");
        }
        quiz.setPublished(false);
        return QuizResponse.from(quiz);
    }

    @Override
    @Transactional
    public void delete(Long actorId, Long quizId) {
        User actor = access.findActiveUser(actorId);
        Quiz quiz = findQuiz(quizId);
        access.requireManager(actor, quiz.getSection());
        if (attemptRepository.existsByQuizId(quizId)) {
            throw new BusinessException("Bài kiểm tra đã có lượt làm bài, không thể xóa để giữ lịch sử");
        }
        quizRepository.delete(quiz);
    }

    @Override
    public QuizResponse getForManagement(Long actorId, Long quizId) {
        User actor = access.findActiveUser(actorId);
        Quiz quiz = findQuiz(quizId);
        access.requireManager(actor, quiz.getSection());
        return QuizResponse.from(quiz);
    }

    @Override
    public List<QuizSummaryResponse> listBySection(Long actorId, Long sectionId) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
        List<Quiz> quizzes;
        if (access.isManager(actor, section)) {
            quizzes = quizRepository.findBySectionId(sectionId);
        } else if (access.isEnrolledStudent(actor, section)) {
            quizzes = quizRepository.findBySectionIdAndPublishedTrue(sectionId);
        } else {
            throw new ForbiddenException("Bạn không có quyền xem bài kiểm tra của lớp học phần này");
        }
        return quizzes.stream().map(QuizSummaryResponse::from).toList();
    }

    // ---------- helpers ----------

    private Quiz findQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài kiểm tra", quizId));
    }

    private void requireSectionNotEnded(CourseSection section) {
        if (section.getStatus() == SectionStatus.ENDED) {
            throw new BusinessException("Lớp học phần đã kết thúc, không thể thay đổi bài kiểm tra");
        }
    }

    private void validateWindow(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new BusinessException("Thời điểm đóng phải sau thời điểm mở");
        }
    }

    /** Dựng câu hỏi và lựa chọn; mỗi câu 2-6 lựa chọn và đúng một đáp án đúng. */
    private List<Question> buildQuestions(Quiz quiz, List<QuizQuestionRequest> requests) {
        List<Question> questions = new ArrayList<>();
        int questionIndex = 1;
        for (QuizQuestionRequest request : requests) {
            if (request.content() == null || request.content().isBlank()) {
                throw new BusinessException("Câu hỏi " + questionIndex + " không có nội dung");
            }
            double score = request.score() == null ? 1.0 : request.score();
            if (Double.isNaN(score) || Double.isInfinite(score) || score <= 0) {
                throw new BusinessException("Điểm của câu hỏi " + questionIndex + " phải lớn hơn 0");
            }
            List<QuizOptionRequest> options = request.options();
            if (options == null || options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
                throw new BusinessException("Câu hỏi " + questionIndex + " cần từ " + MIN_OPTIONS + " đến "
                        + MAX_OPTIONS + " lựa chọn");
            }
            if (options.stream().filter(QuizOptionRequest::correct).count() != 1) {
                throw new BusinessException("Câu hỏi " + questionIndex + " phải có đúng một đáp án đúng");
            }

            Question question = new Question();
            question.setQuiz(quiz);
            question.setContent(request.content().trim());
            question.setType(QuestionType.SINGLE_CHOICE);
            question.setScore(score);
            question.setOrderIndex(questionIndex);
            question.setExplanation(request.explanation());

            int optionIndex = 1;
            for (QuizOptionRequest optionRequest : options) {
                if (optionRequest.content() == null || optionRequest.content().isBlank()) {
                    throw new BusinessException("Câu hỏi " + questionIndex + " có lựa chọn trống");
                }
                AnswerOption option = new AnswerOption();
                option.setQuestion(question);
                option.setContent(optionRequest.content().trim());
                option.setCorrect(optionRequest.correct());
                option.setOrderIndex(optionIndex++);
                question.getOptions().add(option);
            }
            questions.add(question);
            questionIndex++;
        }
        return questions;
    }
}
