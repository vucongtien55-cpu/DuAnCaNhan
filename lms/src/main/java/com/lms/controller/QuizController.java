package com.lms.controller;

import com.lms.dto.quiz.CreateQuizRequest;
import com.lms.dto.quiz.QuizAttemptResponse;
import com.lms.dto.quiz.QuizAttemptStaffResponse;
import com.lms.dto.quiz.QuizQuestionRequest;
import com.lms.dto.quiz.QuizResponse;
import com.lms.dto.quiz.QuizResultResponse;
import com.lms.dto.quiz.QuizSummaryResponse;
import com.lms.dto.quiz.SaveAnswerRequest;
import com.lms.dto.quiz.SaveAnswerResponse;
import com.lms.dto.quiz.UpdateQuizRequest;
import com.lms.security.CurrentUser;
import com.lms.service.QuizAttemptService;
import com.lms.service.QuizService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bài kiểm tra. Quản lý quiz (có đáp án đúng) chỉ dành cho Admin và giảng viên phụ trách; sinh viên chỉ làm bài và xem
 * kết quả của chính mình, không bao giờ nhận đáp án đúng. Danh tính và quyền theo lớp lấy từ JWT và được service kiểm tra.
 */
@Tag(name = "Quizzes")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final QuizAttemptService attemptService;
    private final CurrentUser currentUser;

    public record CountResponse(int count) {
    }

    // ---------- quản lý quiz: Admin / giảng viên phụ trách ----------

    @PostMapping("/quizzes")
    public ResponseEntity<QuizResponse> create(@Valid @RequestBody CreateQuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.create(currentUser.id(), request));
    }

    @GetMapping("/quizzes/{quizId}")
    public QuizResponse getForManagement(@PathVariable Long quizId) {
        return quizService.getForManagement(currentUser.id(), quizId);
    }

    @PutMapping("/quizzes/{quizId}")
    public QuizResponse update(@PathVariable Long quizId, @Valid @RequestBody UpdateQuizRequest request) {
        return quizService.update(currentUser.id(), quizId, request);
    }

    @PutMapping("/quizzes/{quizId}/questions")
    public QuizResponse replaceQuestions(@PathVariable Long quizId,
                                         @Valid @RequestBody List<@Valid QuizQuestionRequest> questions) {
        return quizService.replaceQuestions(currentUser.id(), quizId, questions);
    }

    @PostMapping("/quizzes/{quizId}/publish")
    public QuizResponse publish(@PathVariable Long quizId) {
        return quizService.publish(currentUser.id(), quizId);
    }

    @PostMapping("/quizzes/{quizId}/unpublish")
    public QuizResponse unpublish(@PathVariable Long quizId) {
        return quizService.unpublish(currentUser.id(), quizId);
    }

    @DeleteMapping("/quizzes/{quizId}")
    public ResponseEntity<Void> delete(@PathVariable Long quizId) {
        quizService.delete(currentUser.id(), quizId);
        return ResponseEntity.noContent().build();
    }

    /** Người quản lý lớp thấy mọi quiz; sinh viên ghi danh chỉ thấy quiz đã công bố (không có câu hỏi). */
    @GetMapping("/sections/{sectionId}/quizzes")
    public List<QuizSummaryResponse> listBySection(@PathVariable Long sectionId) {
        return quizService.listBySection(currentUser.id(), sectionId);
    }

    @GetMapping("/quizzes/{quizId}/attempts")
    public List<QuizAttemptStaffResponse> listAttempts(@PathVariable Long quizId) {
        return attemptService.listAttempts(currentUser.id(), quizId);
    }

    @PostMapping("/quizzes/{quizId}/grades/publish")
    public CountResponse publishGrades(@PathVariable Long quizId) {
        return new CountResponse(attemptService.publishQuizGrades(currentUser.id(), quizId));
    }

    // ---------- làm bài: sinh viên ghi danh ----------

    /** Bắt đầu lượt mới hoặc tiếp tục lượt đang làm dở còn thời gian. */
    @PostMapping("/quizzes/{quizId}/attempts")
    public QuizAttemptResponse start(@PathVariable Long quizId) {
        return attemptService.startAttempt(currentUser.id(), quizId);
    }

    @GetMapping("/quizzes/{quizId}/my-attempts")
    public List<QuizResultResponse> myAttempts(@PathVariable Long quizId) {
        return attemptService.listMyAttempts(currentUser.id(), quizId);
    }

    @GetMapping("/quiz-attempts/{attemptId}")
    public QuizAttemptResponse getAttempt(@PathVariable Long attemptId) {
        return attemptService.getAttempt(currentUser.id(), attemptId);
    }

    @PutMapping("/quiz-attempts/{attemptId}/answers")
    public SaveAnswerResponse saveAnswer(@PathVariable Long attemptId, @Valid @RequestBody SaveAnswerRequest request) {
        return attemptService.saveAnswer(currentUser.id(), attemptId, request);
    }

    @PostMapping("/quiz-attempts/{attemptId}/submit")
    public QuizResultResponse submit(@PathVariable Long attemptId) {
        return attemptService.submit(currentUser.id(), attemptId);
    }

    @GetMapping("/quiz-attempts/{attemptId}/result")
    public QuizResultResponse result(@PathVariable Long attemptId) {
        return attemptService.getMyResult(currentUser.id(), attemptId);
    }
}
