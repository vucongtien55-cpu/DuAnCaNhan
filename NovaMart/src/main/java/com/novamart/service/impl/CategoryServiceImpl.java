package com.novamart.service.impl;

import com.novamart.dto.response.CategoryResponse;
import com.novamart.entity.Category;
import com.novamart.exception.BadRequestException;
import com.novamart.exception.ResourceNotFoundException;
import com.novamart.repository.CategoryRepository;
import com.novamart.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục sản phẩm"));
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(String name, String description, Long parentId) {
        String slug = generateSlug(name);
        if (categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Danh mục này đã tồn tại trên hệ thống!");
        }

        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha"));
        }

        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .parent(parent)
                .active(true)
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .active(category.isActive())
                .build();
    }

    private String generateSlug(String input) {
        if (input == null) return "";
        String normalized = input.replace("đ", "d").replace("Đ", "D");
        String nowhitespace = Pattern.compile("[\\s+]").matcher(normalized).replaceAll("-");
        String decomposed = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(decomposed).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-");
    }
}
