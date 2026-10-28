package com.lms.service;

import com.lms.dto.quiz.QuizAttemptResponse;
import com.lms.dto.quiz.QuizAttemptStaffResponse;
import com.lms.dto.quiz.QuizResultResponse;
import com.lms.dto.quiz.SaveAnswerRequest;
import com.lms.dto.quiz.SaveAnswerResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Làm bài kiểm tra. {@code actorId} là người đã đăng nhập (bắt buộc).
 * <ul>
 *   <li>Sinh viên đang theo học (ENROLLED) lớp có bài kiểm tra đã công bố, trong khung giờ mở/đóng, và chưa dùng hết
 *       số lần làm (mặc định 1): bắt đầu, lưu câu trả lời, nộp bài, xem kết quả của chính mình.</li>
 *   <li>Giờ giấc do server kiểm soát: hạn chót tính lúc bắt đầu = min(bắt đầu + thời lượng, giờ đóng).
 *       Quá hạn, bài được chốt bằng các câu đã lưu (câu chưa chọn là bỏ trống), trạng thái EXPIRED.</li>
 *   <li>Server tự chấm khi chốt bài và tạo Grade (loại QUIZ) chưa công bố. Sinh viên không bao giờ nhận đáp án đúng
 *       hay lời giải; chỉ thấy điểm sau khi điểm được công bố.</li>
 *   <li>Admin và giảng viên phụ trách: xem các lượt làm bài, công bố điểm.</li>
 * </ul>
 */
public interface QuizAttemptService {

    /** Bắt đầu lượt mới, hoặc trả lại lượt đang làm dở còn thời gian (để tiếp tục khi mất kết nối). */
    QuizAttemptResponse startAttempt(@NotNull Long actorId, @NotNull Long quizId);

    /** Lấy lại đề và các câu đã lưu của lượt đang làm. Nếu đã hết giờ, chốt bài rồi báo AttemptExpiredException. */
    QuizAttemptResponse getAttempt(@NotNull Long actorId, @NotNull Long attemptId);

    /** Lưu (hoặc bỏ chọn) câu trả lời. Nếu đã hết giờ, chốt bài rồi báo AttemptExpiredException. */
    SaveAnswerResponse saveAnswer(@NotNull Long actorId, @NotNull Long attemptId,
                                  @Valid @NotNull SaveAnswerRequest request);

    /** Nộp bài. Gọi lại khi bài đã nộp thì trả lại kết quả cũ (an toàn khi mạng chập chờn). */
    QuizResultResponse submit(@NotNull Long actorId, @NotNull Long attemptId);

    QuizResultResponse getMyResult(@NotNull Long actorId, @NotNull Long attemptId);

    List<QuizResultResponse> listMyAttempts(@NotNull Long actorId, @NotNull Long quizId);

    /** Các lượt làm bài của bài kiểm tra, kèm điểm; chỉ Admin và giảng viên phụ trách. */
    List<QuizAttemptStaffResponse> listAttempts(@NotNull Long actorId, @NotNull Long quizId);

    /** Công bố mọi điểm chưa công bố của bài kiểm tra; trả về số điểm vừa công bố. */
    int publishQuizGrades(@NotNull Long actorId, @NotNull Long quizId);

    /** Chốt các lượt quá hạn mà sinh viên chưa nộp (tác vụ định kỳ của hệ thống). Trả về số lượt đã chốt. */
    int autoSubmitExpiredAttempts();
}
