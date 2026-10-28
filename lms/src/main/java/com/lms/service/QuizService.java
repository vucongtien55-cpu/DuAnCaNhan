package com.lms.service;

import com.lms.dto.quiz.CreateQuizRequest;
import com.lms.dto.quiz.QuizQuestionRequest;
import com.lms.dto.quiz.QuizResponse;
import com.lms.dto.quiz.QuizSummaryResponse;
import com.lms.dto.quiz.UpdateQuizRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Quản lý bài kiểm tra trắc nghiệm. {@code actorId} là người đã đăng nhập (bắt buộc).
 * <ul>
 *   <li>Admin: mọi lớp. Giảng viên: chỉ lớp mình phụ trách.</li>
 *   <li>Sinh viên đang theo học: chỉ xem danh sách bài kiểm tra đã công bố (không có câu hỏi/đáp án).</li>
 *   <li>Mỗi câu hỏi có 2-6 lựa chọn và đúng một đáp án đúng. Điểm tối đa của bài là tổng điểm các câu.</li>
 *   <li>Đã có lượt làm bài: không đổi câu hỏi, thời lượng, số lần làm; không hủy công bố; không xóa.</li>
 * </ul>
 */
public interface QuizService {

    QuizResponse create(@NotNull Long actorId, @Valid @NotNull CreateQuizRequest request);

    QuizResponse update(@NotNull Long actorId, @NotNull Long quizId, @Valid @NotNull UpdateQuizRequest request);

    /** Thay toàn bộ câu hỏi; chỉ khi chưa có lượt làm bài nào. */
    QuizResponse replaceQuestions(@NotNull Long actorId, @NotNull Long quizId,
                                  @Valid List<@Valid QuizQuestionRequest> questions);

    QuizResponse publish(@NotNull Long actorId, @NotNull Long quizId);

    QuizResponse unpublish(@NotNull Long actorId, @NotNull Long quizId);

    void delete(@NotNull Long actorId, @NotNull Long quizId);

    /** Chi tiết đầy đủ kèm đáp án đúng, chỉ Admin và giảng viên phụ trách. */
    QuizResponse getForManagement(@NotNull Long actorId, @NotNull Long quizId);

    /** Người quản lý thấy mọi bài; sinh viên đang theo học chỉ thấy bài đã công bố. */
    List<QuizSummaryResponse> listBySection(@NotNull Long actorId, @NotNull Long sectionId);
}
