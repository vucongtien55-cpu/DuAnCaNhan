package com.lms.service;

import com.lms.dto.discussion.CreateDiscussionRequest;
import com.lms.dto.discussion.DiscussionDetailResponse;
import com.lms.dto.discussion.DiscussionSummaryResponse;
import com.lms.dto.discussion.ReplyContentRequest;
import com.lms.dto.discussion.ReplyRequest;
import com.lms.dto.discussion.ReplyResponse;
import com.lms.dto.discussion.UpdateDiscussionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Thảo luận trong lớp học phần. {@code actorId} là người đã đăng nhập (bắt buộc).
 * <ul>
 *   <li>Admin, giảng viên phụ trách và sinh viên đang theo học (ENROLLED): xem, tạo chủ đề, trả lời.</li>
 *   <li>Sửa/xóa: chỉ tác giả hoặc Admin. Giảng viên phụ trách và Admin được ghim và khóa chủ đề.</li>
 *   <li>Lớp ENDED hoặc chủ đề đã khóa: chỉ đọc (trừ Admin).</li>
 *   <li>Giữ lịch sử: tác giả chỉ xóa chủ đề chưa có phản hồi, và phản hồi chưa có phản hồi con; Admin xóa được cả nhánh.</li>
 *   <li>Trả lời lồng nhau tối đa hai cấp.</li>
 * </ul>
 */
public interface DiscussionService {

    DiscussionDetailResponse create(@NotNull Long actorId, @Valid @NotNull CreateDiscussionRequest request);

    /** Ghim lên đầu, sau đó mới nhất trước. */
    List<DiscussionSummaryResponse> listBySection(@NotNull Long actorId, @NotNull Long sectionId);

    DiscussionDetailResponse get(@NotNull Long actorId, @NotNull Long discussionId);

    DiscussionDetailResponse update(@NotNull Long actorId, @NotNull Long discussionId,
                                    @Valid @NotNull UpdateDiscussionRequest request);

    void delete(@NotNull Long actorId, @NotNull Long discussionId);

    DiscussionSummaryResponse setPinned(@NotNull Long actorId, @NotNull Long discussionId, boolean pinned);

    DiscussionSummaryResponse setLocked(@NotNull Long actorId, @NotNull Long discussionId, boolean locked);

    ReplyResponse reply(@NotNull Long actorId, @NotNull Long discussionId, @Valid @NotNull ReplyRequest request);

    ReplyResponse updateReply(@NotNull Long actorId, @NotNull Long replyId,
                              @Valid @NotNull ReplyContentRequest request);

    void deleteReply(@NotNull Long actorId, @NotNull Long replyId);
}
