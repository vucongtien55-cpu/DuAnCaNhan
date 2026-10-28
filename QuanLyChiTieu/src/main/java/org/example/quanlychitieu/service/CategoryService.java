package org.example.quanlychitieu.service;

import org.example.quanlychitieu.entity.Category;
import org.example.quanlychitieu.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    //lấy danh sách danh mục thuộc về người dùng email

    // Lấy danh sách danh mục thuộc về người dùng theo email.

    public List<Category> getCategoriesByUser(String email) {
        return categoryRepository.findByUserEmail(email);
    }

    //Thêm một danh sac mới vào db
    public Category addCategory(Category category) {
        return categoryRepository.save(category);
    }
    //Xóa danh mục theo id


    // Thêm một danh mục mới vào database.
    public Category addCategory(Category category) {
        return categoryRepository.save(category);
    }

    // Xóa danh mục theo ID sau khi kiểm tra quyền sở hữu.

    public void deleteCategory(Long id, String email) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));
        if (!c.getUserEmail().equals(email)) {
            throw new SecurityException("Không có quyền xóa danh mục này.");
        }
        categoryRepository.delete(c);
    }
}

