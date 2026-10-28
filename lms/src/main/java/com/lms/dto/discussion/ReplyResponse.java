package com.lms.dto.discussion;

import com.lms.entity.DiscussionReply;

import java.time.LocalDateTime;

public record ReplyResponse(
        Long id,
        Long discussionId,
        Long parentReplyId,
        Long authorId,
        String authorName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ReplyResponse from(DiscussionReply reply) {
        return new ReplyResponse(
                reply.getId(),
                reply.getDiscussion().getId(),
                reply.getParent() == null ? null : reply.getParent().getId(),
                reply.getAuthor().getId(),
                reply.getAuthor().getFullName(),
                reply.getContent(),
                reply.getCreatedAt(),
                reply.getUpdatedAt());
    }
}
