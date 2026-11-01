package com.lms.controller;

import com.lms.dto.discussion.CreateDiscussionRequest;
import com.lms.dto.discussion.DiscussionDetailResponse;
import com.lms.dto.discussion.DiscussionSummaryResponse;
import com.lms.dto.discussion.ReplyContentRequest;
import com.lms.dto.discussion.ReplyRequest;
import com.lms.dto.discussion.ReplyResponse;
import com.lms.dto.discussion.UpdateDiscussionRequest;
import com.lms.security.CurrentUser;
import com.lms.service.DiscussionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Thảo luận trong lớp học phần. Tác giả luôn là người đăng nhập (JWT); quyền xem/viết theo lớp, quyền sửa/xóa theo tác
 * giả và quyền ghim/khóa theo vai trò đều do service kiểm tra.
 */
@Tag(name = "Discussions")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;
    private final CurrentUser currentUser;

    public record SetPinnedRequest(@NotNull Boolean pinned) {
    }

    public record SetLockedRequest(@NotNull Boolean locked) {
    }

    @PostMapping("/discussions")
    public ResponseEntity<DiscussionDetailResponse> create(@Valid @RequestBody CreateDiscussionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discussionService.create(currentUser.id(), request));
    }

    @GetMapping("/sections/{sectionId}/discussions")
    public List<DiscussionSummaryResponse> listBySection(@PathVariable Long sectionId) {
        return discussionService.listBySection(currentUser.id(), sectionId);
    }

    @GetMapping("/discussions/{discussionId}")
    public DiscussionDetailResponse get(@PathVariable Long discussionId) {
        return discussionService.get(currentUser.id(), discussionId);
    }

    @PutMapping("/discussions/{discussionId}")
    public DiscussionDetailResponse update(@PathVariable Long discussionId,
                                           @Valid @RequestBody UpdateDiscussionRequest request) {
        return discussionService.update(currentUser.id(), discussionId, request);
    }

    @DeleteMapping("/discussions/{discussionId}")
    public ResponseEntity<Void> delete(@PathVariable Long discussionId) {
        discussionService.delete(currentUser.id(), discussionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/discussions/{discussionId}/pinned")
    public DiscussionSummaryResponse setPinned(@PathVariable Long discussionId,
                                               @Valid @RequestBody SetPinnedRequest request) {
        return discussionService.setPinned(currentUser.id(), discussionId, request.pinned());
    }

    @PutMapping("/discussions/{discussionId}/locked")
    public DiscussionSummaryResponse setLocked(@PathVariable Long discussionId,
                                               @Valid @RequestBody SetLockedRequest request) {
        return discussionService.setLocked(currentUser.id(), discussionId, request.locked());
    }

    @PostMapping("/discussions/{discussionId}/replies")
    public ResponseEntity<ReplyResponse> reply(@PathVariable Long discussionId,
                                               @Valid @RequestBody ReplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discussionService.reply(currentUser.id(), discussionId, request));
    }

    @PutMapping("/discussion-replies/{replyId}")
    public ReplyResponse updateReply(@PathVariable Long replyId, @Valid @RequestBody ReplyContentRequest request) {
        return discussionService.updateReply(currentUser.id(), replyId, request);
    }

    @DeleteMapping("/discussion-replies/{replyId}")
    public ResponseEntity<Void> deleteReply(@PathVariable Long replyId) {
        discussionService.deleteReply(currentUser.id(), replyId);
        return ResponseEntity.noContent().build();
    }
}
