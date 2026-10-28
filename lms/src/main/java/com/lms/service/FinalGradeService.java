package com.lms.service;

import com.lms.dto.grade.FinalGradeResponse;
import com.lms.dto.grade.GradeResponse;
import com.lms.dto.grade.GradeWeightRequest;
import com.lms.dto.grade.GradeWeightResponse;
import com.lms.dto.grade.StudentFinalGradeResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Cấu hình trọng số và điểm tổng kết của lớp học phần. {@code actorId} là người đã đăng nhập (bắt buộc).
 * <ul>
 *   <li>Admin và giảng viên phụ trách: cấu hình trọng số, tính, xem, công bố điểm tổng kết.</li>
 *   <li>Sinh viên: xem trọng số của lớp mình đang học; chỉ xem điểm tổng kết của chính mình khi đã công bố.</li>
 * </ul>
 * Quy ước: trọng số là phần trăm, tổng đúng 100; hỗ trợ ASSIGNMENT và QUIZ. Điểm thành phần chỉ lấy từ điểm đã
 * công bố: bài tập theo điểm chính thức của từng bài tập, quiz theo lượt có điểm cao nhất của từng bài; mỗi thành
 * phần là trung bình tỷ lệ điểm/điểm tối đa. Thành phần có trọng số mà sinh viên chưa có điểm công bố nào thì không
 * có điểm tổng kết (INCOMPLETE), không coi là 0 và không chia lại trọng số. Điểm tổng kết thang 10, làm tròn một lần
 * ở cuối theo HALF_UP đến 2 chữ số thập phân, lưu vào Grade loại COURSE_TOTAL.
 */
public interface FinalGradeService {

    /** Thay toàn bộ cấu hình trọng số của lớp. Không đổi được khi đã có điểm tổng kết được công bố. */
    List<GradeWeightResponse> setWeights(@NotNull Long actorId, @NotNull Long sectionId,
                                         @Valid List<@Valid GradeWeightRequest> weights);

    List<GradeWeightResponse> getWeights(@NotNull Long actorId, @NotNull Long sectionId);

    /** Kiểm tra lại cấu hình rồi tính (hoặc tính lại) điểm tổng kết cho mọi sinh viên đang theo học của lớp. */
    List<FinalGradeResponse> calculate(@NotNull Long actorId, @NotNull Long sectionId);

    /** Điểm tổng kết đã lưu của cả lớp, kể cả chưa công bố. */
    List<GradeResponse> listFinalGrades(@NotNull Long actorId, @NotNull Long sectionId);

    /** Công bố mọi điểm tổng kết chưa công bố; trả về số điểm vừa công bố. */
    int publishFinalGrades(@NotNull Long actorId, @NotNull Long sectionId);

    /** Hủy công bố mọi điểm tổng kết đã công bố; trả về số điểm vừa hủy công bố. */
    int unpublishFinalGrades(@NotNull Long actorId, @NotNull Long sectionId);

    StudentFinalGradeResponse getMyFinalGrade(@NotNull Long actorId, @NotNull Long sectionId);
}
