package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.Category;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.exception.DuplicateResourceException;
import org.example.ridehailing.banhangonline.repository.CategoryRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.CategoryService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private static final int CATEGORY_NAME_MAX_LENGTH = 120;
    private static final int CATEGORY_SLUG_MAX_LENGTH = 160;
    private static final int CATEGORY_DESCRIPTION_MAX_LENGTH = 1000;
    private static final Pattern VALID_SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final String ADMIN_ROLE = "ADMIN";

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Category getActiveCategoryById(Long categoryId) {
        requirePositiveId(categoryId, "categoryId");
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục."));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new EntityNotFoundException("Không tìm thấy danh mục đang hoạt động.");
        }
        return category;
    }

    @Override
    public Category getActiveCategoryBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Slug danh mục không được để trống.");
        }
        Category category = categoryRepository.findBySlug(normalizeSlug(slug))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục."));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new EntityNotFoundException("Không tìm thấy danh mục đang hoạt động.");
        }
        return category;
    }

    @Override
    public List<Category> getActiveRootCategories() {
        return categoryRepository.findByActiveTrueAndParentCategoryIsNull();
    }

    @Override
    public List<Category> getAllRootCategories(Long actorUserId) {
        requireAdministrator(actorUserId);
        return categoryRepository.findByParentCategoryIsNull();
    }

    @Override
    public List<Category> getActiveChildren(Long parentCategoryId) {
        requirePositiveId(parentCategoryId, "parentCategoryId");
        getActiveCategoryById(parentCategoryId);
        return categoryRepository.findByParentCategory_IdAndActiveTrue(parentCategoryId);
    }

    @Override
    public List<Category> getAllChildren(Long actorUserId, Long parentCategoryId) {
        requireAdministrator(actorUserId);
        requirePositiveId(parentCategoryId, "parentCategoryId");
        categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục."));
        return categoryRepository.findByParentCategory_Id(parentCategoryId);
    }

    @Override
    public List<Category> searchActiveCategoriesByName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Từ khóa tìm danh mục không được để trống.");
        }
        return categoryRepository.findByNameContainingIgnoreCaseAndActiveTrue(keyword.trim());
    }

    @Override
    public List<Category> searchAllCategories(Long actorUserId, String keyword) {
        requireAdministrator(actorUserId);
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Từ khóa tìm danh mục không được để trống.");
        }
        return categoryRepository.findByNameContainingIgnoreCase(keyword.trim());
    }

    @Override
    @Transactional
    public Category createCategory(Long actorUserId, Category category, Long parentCategoryId) {
        requireAdministrator(actorUserId);
        validateCategory(category);
        ensureSlugAvailable(normalizeSlug(category.getSlug()), null);

        Category parent = resolveParent(parentCategoryId, null);
        category.setId(null);
        category.setName(category.getName().trim());
        category.setSlug(normalizeSlug(category.getSlug()));
        category.setParentCategory(parent);
        category.setActive(true);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(
            Long actorUserId, Long categoryId, Category updates, Long parentCategoryId) {
        requireAdministrator(actorUserId);
        requirePositiveId(categoryId, "categoryId");
        validateCategory(updates);

        Category existing = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục."));
        String normalizedSlug = normalizeSlug(updates.getSlug());
        ensureSlugAvailable(normalizedSlug, categoryId);
        Category parent = resolveParent(parentCategoryId, categoryId);

        existing.setName(updates.getName().trim());
        existing.setSlug(normalizedSlug);
        existing.setDescription(updates.getDescription());
        existing.setImageUrl(updates.getImageUrl());
        existing.setParentCategory(parent);
        return categoryRepository.save(existing);
    }

    @Override
    @Transactional
    public void deactivateCategory(Long actorUserId, Long categoryId) {
        requireAdministrator(actorUserId);
        requirePositiveId(categoryId, "categoryId");

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục."));
        category.setActive(false);
        categoryRepository.save(category);
    }

    // Quyền quản lý danh mục toàn hệ thống chỉ dành cho tài khoản ADMIN đang hoạt động.
    private void requireAdministrator(Long actorUserId) {
        requirePositiveId(actorUserId, "actorUserId");
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người thực hiện."));
        if (!Boolean.TRUE.equals(actor.getEnabled()) || !ADMIN_ROLE.equalsIgnoreCase(actor.getRole())) {
            throw new AccessDeniedException("Chỉ quản trị viên đang hoạt động mới được quản lý danh mục.");
        }
    }

    // Danh mục cha phải đang hoạt động và không được tạo chu trình trong cây danh mục.
    private Category resolveParent(Long parentCategoryId, Long categoryBeingUpdatedId) {
        if (parentCategoryId == null) {
            return null;
        }
        requirePositiveId(parentCategoryId, "parentCategoryId");
        Category parent = getActiveCategoryById(parentCategoryId);

        Category ancestor = parent;
        while (ancestor != null) {
            if (categoryBeingUpdatedId != null && categoryBeingUpdatedId.equals(ancestor.getId())) {
                throw new IllegalArgumentException("Không thể đặt danh mục làm danh mục cha của chính nó hoặc hậu duệ.");
            }
            ancestor = ancestor.getParentCategory();
        }
        return parent;
    }

    // Bảo đảm slug duy nhất, ngoại trừ chính danh mục đang được cập nhật.
    private void ensureSlugAvailable(String slug, Long categoryBeingUpdatedId) {
        Optional<Category> existing = categoryRepository.findBySlug(slug);
        if (existing.isPresent()
                && !existing.get().getId().equals(categoryBeingUpdatedId)) {
            throw new DuplicateResourceException("Slug danh mục đã được sử dụng.");
        }
    }

    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Thông tin danh mục không được để trống.");
        }
        if (category.getName() == null || category.getName().isBlank()
                || category.getName().trim().length() > CATEGORY_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Tên danh mục phải có từ 1 đến 120 ký tự.");
        }
        if (category.getSlug() == null || category.getSlug().isBlank()
                || normalizeSlug(category.getSlug()).length() > CATEGORY_SLUG_MAX_LENGTH
                || !VALID_SLUG.matcher(normalizeSlug(category.getSlug())).matches()) {
            throw new IllegalArgumentException(
                    "Slug chỉ được chứa chữ thường không dấu, chữ số và dấu gạch ngang.");
        }
        if (category.getDescription() != null
                && category.getDescription().length() > CATEGORY_DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("Mô tả danh mục không được vượt quá 1000 ký tự.");
        }
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
