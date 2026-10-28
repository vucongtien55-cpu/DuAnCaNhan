package com.lms.dto.quiz;

import com.lms.entity.AnswerOption;
import com.lms.entity.Question;
import com.lms.entity.Quiz;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Chi tiết bài kiểm tra cho Admin và giảng viên phụ trách: CÓ đáp án đúng và lời giải.
 * Tuyệt đối không dùng cho sinh viên.
 */
public record QuizResponse(
        Long id,
        Long sectionId,
        String title,
        String description,
        Integer durationMinutes,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer maxAttempts,
        boolean shuffleQuestions,
        boolean published,
        double maxScore,
        List<QuestionDetail> questions) {

    public record QuestionDetail(Long id, String content, double score, Integer orderIndex, String explanation,
                                 List<OptionDetail> options) {
    }

    public record OptionDetail(Long id, String content, boolean correct) {
    }

    public static QuizResponse from(Quiz quiz) {
        List<QuestionDetail> questions = quiz.getQuestions().stream()
                .map(q -> new QuestionDetail(q.getId(), q.getContent(), q.getScore(), q.getOrderIndex(),
                        q.getExplanation(), optionsOf(q)))
                .toList();
        return new QuizResponse(
                quiz.getId(),
                quiz.getSection().getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getDurationMinutes(),
                quiz.getStartTime(),
                quiz.getEndTime(),
                quiz.getMaxAttempts(),
                quiz.isShuffleQuestions(),
                quiz.isPublished(),
                maxScoreOf(quiz),
                questions);
    }

    public static double maxScoreOf(Quiz quiz) {
        return quiz.getQuestions().stream().mapToDouble(Question::getScore).sum();
    }

    private static List<OptionDetail> optionsOf(Question question) {
        return question.getOptions().stream()
                .sorted(Comparator.comparing(AnswerOption::getOrderIndex,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(o -> new OptionDetail(o.getId(), o.getContent(), o.isCorrect()))
                .toList();
    }
}
