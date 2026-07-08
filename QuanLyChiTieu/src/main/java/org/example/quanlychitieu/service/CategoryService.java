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

    public List<Category> getCategoriesByUser(String email) {
        return categoryRepository.findByUserEmail(email);
    }

    public Category addCategory(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id, String email) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (!c.getUserEmail().equals(email)) {
            throw new SecurityException("Unauthorized access to delete this category");
        }
        categoryRepository.delete(c);
    }
}

