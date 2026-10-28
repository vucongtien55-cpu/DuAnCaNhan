package com.lms.repository;

import com.lms.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findBySectionId(Long sectionId);

    List<Material> findBySectionIdOrderByCreatedAtDesc(Long sectionId);
}
