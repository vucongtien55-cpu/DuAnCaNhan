package com.novamart.repository;

import com.novamart.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Tìm danh mục theo slug định danh
    Optional<Category> findBySlug(String slug);

    // Kiểm tra slug danh mục đã tồn tại chưa
    Boolean existsBySlug(String slug);

    // Lấy danh sách các danh mục đang hoạt động (dùng cho công khai/trang chủ)
    List<Category> findByActiveTrue();

    // Lấy danh sách danh mục con theo ID của danh mục cha
    List<Category> findByParentId(Long parentId);
}