package com.lms.repository;

import com.lms.entity.Discussion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    List<Discussion> findBySectionIdOrderByPinnedDescCreatedAtDesc(Long sectionId);
}
