package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.Category;

import java.util.List;

public interface CategoryService {

    // Tìm danh mục đang hoạt động theo mã.
    Category getActiveCategoryById(Long categoryId);

    // Tìm danh mục đang hoạt động theo slug.
    Category getActiveCategoryBySlug(String slug);

    // Lấy các danh mục gốc đang hoạt động.
    List<Category> getActiveRootCategories();

    // Lấy mọi danh mục gốc cho ADMIN, bao gồm danh mục đã ngừng hoạt động.
    List<Category> getAllRootCategories(Long actorUserId);

    // Lấy danh mục con đang hoạt động của một danh mục.
    List<Category> getActiveChildren(Long parentCategoryId);

    // Lấy mọi danh mục con cho ADMIN, bao gồm danh mục đã ngừng hoạt động.
    List<Category> getAllChildren(Long actorUserId, Long parentCategoryId);

    // Tìm danh mục đang hoạt động theo tên.
    List<Category> searchActiveCategoriesByName(String keyword);

    // Tìm mọi danh mục theo tên cho ADMIN, kể cả danh mục đã ngừng hoạt động.
    List<Category> searchAllCategories(Long actorUserId, String keyword);

    // Tạo danh mục; actorUserId phải đến từ danh tính đã xác thực ở tầng gọi.
    Category createCategory(Long actorUserId, Category category, Long parentCategoryId);

    // Cập nhật danh mục; parentCategoryId null nghĩa là chuyển thành danh mục gốc.
    Category updateCategory(Long actorUserId, Long categoryId, Category updates, Long parentCategoryId);

    // Ngừng hoạt động danh mục mềm để giữ nguyên dữ liệu tham chiếu.
    void deactivateCategory(Long actorUserId, Long categoryId);
}
