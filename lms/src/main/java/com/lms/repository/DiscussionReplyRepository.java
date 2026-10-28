package com.lms.repository;

import com.lms.entity.DiscussionReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, Long> {

    List<DiscussionReply> findByDiscussionIdOrderByCreatedAtAsc(Long discussionId);

    long countByDiscussionId(Long discussionId);

    /** Các phản hồi trực tiếp trả lời một phản hồi khác. */
    List<DiscussionReply> findByParentId(Long parentId);
}
