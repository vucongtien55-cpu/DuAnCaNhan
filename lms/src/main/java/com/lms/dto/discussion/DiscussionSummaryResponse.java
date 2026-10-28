package com.lms.dto.discussion;

import com.lms.entity.Discussion;

import java.time.LocalDateTime;

public record DiscussionSummaryResponse(
        Long id,
        Long sectionId,
        String title,
        Long authorId,
        String authorName,
        boolean pinned,
        boolean locked,
        long replyCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static DiscussionSummaryResponse from(Discussion discussion, long replyCount) {
        return new DiscussionSummaryResponse(
                discussion.getId(),
                discussion.getSection().getId(),
                discussion.getTitle(),
                discussion.getAuthor().getId(),
                discussion.getAuthor().getFullName(),
                discussion.isPinned(),
                discussion.isLocked(),
                replyCount,
                discussion.getCreatedAt(),
                discussion.getUpdatedAt());
    }
}
