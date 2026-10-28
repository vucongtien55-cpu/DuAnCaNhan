package com.lms.service.impl;

import com.lms.dto.discussion.CreateDiscussionRequest;
import com.lms.dto.discussion.DiscussionDetailResponse;
import com.lms.dto.discussion.DiscussionSummaryResponse;
import com.lms.dto.discussion.ReplyContentRequest;
import com.lms.dto.discussion.ReplyRequest;
import com.lms.dto.discussion.ReplyResponse;
import com.lms.dto.discussion.UpdateDiscussionRequest;
import com.lms.entity.CourseSection;
import com.lms.entity.Discussion;
import com.lms.entity.DiscussionReply;
import com.lms.entity.User;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.DiscussionReplyRepository;
import com.lms.repository.DiscussionRepository;
import com.lms.service.DiscussionService;
import com.lms.service.support.SectionAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscussionServiceImpl implements DiscussionService {

    private final DiscussionRepository discussionRepository;
    private final DiscussionReplyRepository replyRepository;
    private final CourseSectionRepository sectionRepository;
    private final SectionAccessChecker access;

    // ---------- chủ đề ----------

    @Override
    @Transactional
    public DiscussionDetailResponse create(Long actorId, CreateDiscussionRequest request) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", request.sectionId()));
        access.requireReader(actor, section);
        requireSectionOpen(actor, section);

        Discussion discussion = new Discussion();
        discussion.setSection(section);
        discussion.setAuthor(actor);
        discussion.setTitle(request.title().trim());
        discussion.setContent(request.content().trim());
        return DiscussionDetailResponse.from(discussionRepository.save(discussion), List.of());
    }

    @Override
    public List<DiscussionSummaryResponse> listBySection(Long actorId, Long sectionId) {
        User actor = access.findActiveUser(actorId);
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", sectionId));
        access.requireReader(actor, section);
        return discussionRepository.findBySectionIdOrderByPinnedDescCreatedAtDesc(sectionId).stream()
                .map(d -> DiscussionSummaryResponse.from(d, replyRepository.countByDiscussionId(d.getId())))
                .toList();
    }

    @Override
    public DiscussionDetailResponse get(Long actorId, Long discussionId) {
        User actor = access.findActiveUser(actorId);
        Discussion discussion = findDiscussion(discussionId);
        access.requireReader(actor, discussion.getSection());
        return detailOf(discussion);
    }

    @Override
    @Transactional
    public DiscussionDetailResponse update(Long actorId, Long discussionId, UpdateDiscussionRequest request) {
        User actor = access.findActiveUser(actorId);
        Discussion discussion = findDiscussion(discussionId);
        access.requireReader(actor, discussion.getSection());
        requireAuthorOrAdmin(actor, discussion.getAuthor());
        requireOpen(actor, discussion);

        discussion.setTitle(request.title().trim());
        discussion.setContent(request.content().trim());
        return detailOf(discussion);
    }

    @Override
    @Transactional
    public void delete(Long actorId, Long discussionId) {
        User actor = access.findActiveUser(actorId);
        Discussion discussion = findDiscussion(discussionId);
        access.requireReader(actor, discussion.getSection());
        requireAuthorOrAdmin(actor, discussion.getAuthor());
        requireOpen(actor, discussion);

        List<DiscussionReply> replies = replyRepository.findByDiscussionIdOrderByCreatedAtAsc(discussionId);
        if (!isAdmin(actor) && !replies.isEmpty()) {
            throw new BusinessException("Chủ đề đã có phản hồi, không thể xóa để giữ lại cuộc thảo luận");
        }
        // Xóa phản hồi con trước phản hồi cha để không vi phạm khóa ngoại parent_id.
        replyRepository.deleteAll(replies.stream().filter(r -> r.getParent() != null).toList());
        replyRepository.flush();
        replyRepository.deleteAll(replies.stream().filter(r -> r.getParent() == null).toList());
        replyRepository.flush();
        discussionRepository.delete(discussion);
    }

    @Override
    @Transactional
    public DiscussionSummaryResponse setPinned(Long actorId, Long discussionId, boolean pinned) {
        Discussion discussion = findForModeration(actorId, discussionId);
        discussion.setPinned(pinned);
        return DiscussionSummaryResponse.from(discussion, replyRepository.countByDiscussionId(discussionId));
    }

    @Override
    @Transactional
    public DiscussionSummaryResponse setLocked(Long actorId, Long discussionId, boolean locked) {
        Discussion discussion = findForModeration(actorId, discussionId);
        discussion.setLocked(locked);
        return DiscussionSummaryResponse.from(discussion, replyRepository.countByDiscussionId(discussionId));
    }

    private Discussion findForModeration(Long actorId, Long discussionId) {
        User actor = access.findActiveUser(actorId);
        Discussion discussion = findDiscussion(discussionId);
        if (!access.isManager(actor, discussion.getSection())) {
            throw new ForbiddenException("Chỉ Admin hoặc giảng viên phụ trách mới được ghim hoặc khóa chủ đề");
        }
        return discussion;
    }

    // ---------- phản hồi ----------

    @Override
    @Transactional
    public ReplyResponse reply(Long actorId, Long discussionId, ReplyRequest request) {
        User actor = access.findActiveUser(actorId);
        Discussion discussion = findDiscussion(discussionId);
        access.requireReader(actor, discussion.getSection());
        requireOpen(actor, discussion);

        DiscussionReply parent = null;
        if (request.parentReplyId() != null) {
            parent = findReply(request.parentReplyId());
            if (!parent.getDiscussion().getId().equals(discussionId)) {
                throw new BusinessException("Phản hồi được trả lời không thuộc chủ đề này");
            }
            if (parent.getParent() != null) {
                throw new BusinessException("Chỉ hỗ trợ trả lời tối đa hai cấp");
            }
        }

        DiscussionReply reply = new DiscussionReply();
        reply.setDiscussion(discussion);
        reply.setAuthor(actor);
        reply.setParent(parent);
        reply.setContent(request.content().trim());
        return ReplyResponse.from(replyRepository.save(reply));
    }

    @Override
    @Transactional
    public ReplyResponse updateReply(Long actorId, Long replyId, ReplyContentRequest request) {
        User actor = access.findActiveUser(actorId);
        DiscussionReply reply = findReply(replyId);
        Discussion discussion = reply.getDiscussion();
        access.requireReader(actor, discussion.getSection());
        requireAuthorOrAdmin(actor, reply.getAuthor());
        requireOpen(actor, discussion);

        reply.setContent(request.content().trim());
        return ReplyResponse.from(reply);
    }

    @Override
    @Transactional
    public void deleteReply(Long actorId, Long replyId) {
        User actor = access.findActiveUser(actorId);
        DiscussionReply reply = findReply(replyId);
        Discussion discussion = reply.getDiscussion();
        access.requireReader(actor, discussion.getSection());
        requireAuthorOrAdmin(actor, reply.getAuthor());
        requireOpen(actor, discussion);

        List<DiscussionReply> children = replyRepository.findByParentId(replyId);
        if (!children.isEmpty()) {
            if (!isAdmin(actor)) {
                throw new BusinessException("Phản hồi đã có người trả lời, không thể xóa để giữ lại cuộc thảo luận");
            }
            replyRepository.deleteAll(children);
            replyRepository.flush();
        }
        replyRepository.delete(reply);
    }

    // ---------- helpers ----------

    private Discussion findDiscussion(Long discussionId) {
        return discussionRepository.findById(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chủ đề thảo luận", discussionId));
    }

    private DiscussionReply findReply(Long replyId) {
        return replyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Phản hồi", replyId));
    }

    private DiscussionDetailResponse detailOf(Discussion discussion) {
        return DiscussionDetailResponse.from(discussion,
                replyRepository.findByDiscussionIdOrderByCreatedAtAsc(discussion.getId()));
    }

    private boolean isAdmin(User user) {
        return access.hasRole(user, RoleName.ADMIN);
    }

    private void requireAuthorOrAdmin(User actor, User author) {
        if (!isAdmin(actor) && !author.getId().equals(actor.getId())) {
            throw new ForbiddenException("Bạn chỉ được chỉnh sửa hoặc xóa bài viết của chính mình");
        }
    }

    /** Lớp ENDED là chỉ đọc; Admin được phép quản trị mọi lúc. */
    private void requireSectionOpen(User actor, CourseSection section) {
        if (!isAdmin(actor) && section.getStatus() == SectionStatus.ENDED) {
            throw new BusinessException("Lớp học phần đã kết thúc, phần thảo luận chỉ còn để xem");
        }
    }

    private void requireOpen(User actor, Discussion discussion) {
        requireSectionOpen(actor, discussion.getSection());
        if (!isAdmin(actor) && discussion.isLocked()) {
            throw new BusinessException("Chủ đề đã bị khóa");
        }
    }
}
