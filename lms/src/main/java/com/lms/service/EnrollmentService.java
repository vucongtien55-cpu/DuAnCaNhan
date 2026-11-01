package com.lms.service;

import com.lms.dto.enrollment.EnrollmentResponse;
import com.lms.dto.user.UserSummaryResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Nghiệp vụ đăng ký học. {@code actorId} là người thực hiện.
 * <ul>
 *   <li>Sinh viên: tự đăng ký bằng mã lớp, tự rút khỏi lớp, xem lịch sử đăng ký của mình.</li>
 *   <li>Admin và giảng viên phụ trách: thêm/gỡ sinh viên, xem danh sách lớp (gồm cả sinh viên đã rút).</li>
 * </ul>
 * Rút lớp không xóa dữ liệu: bản ghi chuyển sang DROPPED. Sinh viên đã rút không tự đăng ký lại được;
 * Admin hoặc giảng viên phụ trách thêm lại sẽ khôi phục bản ghi cũ.
 */
public interface EnrollmentService {

    /** Sinh viên tự đăng ký: lớp phải OPEN_REGISTRATION, còn chỗ và chưa từng đăng ký. */
    EnrollmentResponse enroll(Long actorId, @NotBlank String sectionCode);

    /** Admin hoặc giảng viên phụ trách thêm sinh viên vào lớp đang OPEN_REGISTRATION hoặc IN_PROGRESS. */
    EnrollmentResponse addStudent(Long actorId, Long sectionId, Long studentId);

    /** Sinh viên tự rút, hoặc Admin/giảng viên phụ trách gỡ sinh viên. Lớp ENDED không còn rút được. */
    EnrollmentResponse withdraw(Long actorId, Long sectionId, Long studentId, @Size(max = 255) String reason);

    /** Danh sách đăng ký của lớp (kể cả đã rút); chỉ Admin và giảng viên phụ trách. */
    List<EnrollmentResponse> listBySection(Long actorId, Long sectionId);

    /** Lịch sử đăng ký của chính sinh viên, gồm cả các lớp đã rút. */
    List<EnrollmentResponse> listMyEnrollments(Long actorId);

    /**
     * Admin hoặc giảng viên phụ trách tìm sinh viên đang hoạt động, chưa có đăng ký còn hiệu lực trong lớp, để thêm vào
     * lớp. Từ khóa tối thiểu {@link #MIN_KEYWORD_LENGTH} ký tự để không thể liệt kê toàn bộ sinh viên.
     */
    Page<UserSummaryResponse> searchStudentCandidates(Long actorId, Long sectionId, String keyword, Pageable pageable);

    int MIN_KEYWORD_LENGTH = 2;
}
