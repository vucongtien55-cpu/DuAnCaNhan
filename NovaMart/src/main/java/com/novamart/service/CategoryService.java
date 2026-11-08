package com.novamart.service;

import com.novamart.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    // Lấy danh sách danh mục sản phẩm đang hoạt động
    List<CategoryResponse> getAllActiveCategories();

    // Lấy thông tin chi tiết danh mục theo slug
    CategoryResponse getCategoryBySlug(String slug);

    // Admin tạo danh mục sản phẩm mới (hỗ trợ danh mục cha - con)
    CategoryResponse createCategory(String name, String description, Long parentId);
}
