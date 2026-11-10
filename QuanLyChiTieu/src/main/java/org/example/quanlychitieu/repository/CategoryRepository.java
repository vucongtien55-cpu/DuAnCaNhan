package org.example.quanlychitieu.repository;

import org.example.quanlychitieu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserEmail(String userEmail);
    List<Category> findByUserEmailAndType(String userEmail, String type);
}
