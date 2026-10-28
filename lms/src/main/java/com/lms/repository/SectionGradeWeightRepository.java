package com.lms.repository;

import com.lms.entity.SectionGradeWeight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionGradeWeightRepository extends JpaRepository<SectionGradeWeight, Long> {

    List<SectionGradeWeight> findBySectionId(Long sectionId);
}
