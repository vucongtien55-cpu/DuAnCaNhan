package com.lms.dto.discussion;

import com.lms.entity.Discussion;
import com.lms.entity.DiscussionReply;

import java.time.LocalDateTime;
import java.util.List;

/** Chủ đề kèm toàn bộ phản hồi (xếp theo thời gian; {@code parentReplyId} cho biết phản hồi nào trả lời phản hồi nào). */
public record DiscussionDetailResponse(
        Long id,
        Long sectionId,
        String title,
        String content,
        Long authorId,
        String authorName,
        boolean pinned,
        boolean locked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ReplyResponse> replies) {

    public static DiscussionDetailResponse from(Discussion discussion, List<DiscussionReply> replies) {
        return new DiscussionDetailResponse(
                discussion.getId(),
                discussion.getSection().getId(),
                discussion.getTitle(),
                discussion.getContent(),
                discussion.getAuthor().getId(),
                discussion.getAuthor().getFullName(),
                discussion.isPinned(),
                discussion.isLocked(),
                discussion.getCreatedAt(),
                discussion.getUpdatedAt(),
                replies.stream().map(ReplyResponse::from).toList());
    }
}
