package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Tìm danh mục theo slug.
    Optional<Category> findBySlug(String slug);

    // Lấy các danh mục gốc không có danh mục cha.
    List<Category> findByParentCategoryIsNull();

    // Lấy danh mục gốc đang hoạt động để không trả về danh mục đã ngừng sử dụng.
    List<Category> findByActiveTrueAndParentCategoryIsNull();

    // Lấy danh sách danh mục con của một danh mục.
    List<Category> findByParentCategory_Id(Long parentCategoryId);

    // Lấy danh mục con đang hoạt động của một danh mục.
    List<Category> findByParentCategory_IdAndActiveTrue(Long parentCategoryId);

    // Tìm danh mục có tên chứa từ khóa, không phân biệt chữ hoa/chữ thường.
    List<Category> findByNameContainingIgnoreCase(String name);

    // Tìm theo tên và chỉ trả về danh mục đang hoạt động.
    List<Category> findByNameContainingIgnoreCaseAndActiveTrue(String name);
}
