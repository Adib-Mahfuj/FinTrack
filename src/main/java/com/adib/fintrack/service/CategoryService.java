package com.adib.fintrack.service;

import com.adib.fintrack.dto.CategoryDto;
import com.adib.fintrack.entity.CategoryEntity;
import com.adib.fintrack.entity.ProfileEntity;
import com.adib.fintrack.exception.ApiException;
import com.adib.fintrack.repository.CategoryRepository;
import com.adib.fintrack.repository.ExpenseRepository;
import com.adib.fintrack.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Set<String> VALID_TYPES = Set.of("income", "expense");
    private static final int MAX_NAME_LENGTH = 50;
    private static final String DUPLICATE_MESSAGE = "A category with this name already exists for this type";

    private final CategoryRepository categoryRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfileService profileService;

    // Creates a category for the current user
    public CategoryDto save(CategoryDto categoryDto) {
        ProfileEntity profile = profileService.getCurrentProfile();
        String name = normalizeName(categoryDto.getName());
        String type = normalizeType(categoryDto.getType());

        if (categoryRepository.existsByProfileIdAndTypeIgnoreCaseAndNameIgnoreCase(profile.getId(), type, name)) {
            throw new ApiException(HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
        }

        CategoryEntity newCategory = CategoryEntity.builder()
                .name(name)
                .type(type)
                .icon(categoryDto.getIcon())
                .profile(profile)
                .build();

        try {
            newCategory = categoryRepository.saveAndFlush(newCategory);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent requests can both pass the check; the DB constraint catches the second
            throw new ApiException(HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
        }
        return toDto(newCategory);
    }

    // Lists all categories of the current user
    public List<CategoryDto> getCategoriesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> categories = categoryRepository.findByProfileId(profile.getId());
        return categories.stream().map(this::toDto).toList();
    }

    // Lists the current user's categories of one type
    public List<CategoryDto> getCategoriesByTypeForCurrentUser(String type) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> categories = categoryRepository.findByTypeAndProfileId(type, profile.getId());
        return categories.stream().map(this::toDto).toList();
    }

    // Updates name, icon and type of an owned category
    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity existing = categoryRepository.findByIdAndProfileId(categoryId, profile.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));

        String name = normalizeName(categoryDto.getName());
        String type = normalizeType(categoryDto.getType());

        if (categoryRepository.existsByProfileIdAndTypeIgnoreCaseAndNameIgnoreCaseAndIdNot(
                profile.getId(), type, name, categoryId)) {
            throw new ApiException(HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
        }

        // Changing the type of a category that already has transactions would corrupt its data
        if (!type.equalsIgnoreCase(existing.getType())) {
            long usage = countTransactions(categoryId);
            if (usage > 0) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Type cannot be changed because this category already has "
                                + usage + (usage == 1 ? " transaction" : " transactions"));
            }
        }

        existing.setName(name);
        existing.setType(type);
        existing.setIcon(categoryDto.getIcon());

        try {
            existing = categoryRepository.saveAndFlush(existing);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
        }
        return toDto(existing);
    }

    // Deletes an owned category that has no transactions
    @Transactional
    public void deleteCategory(Long categoryId) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findByIdAndProfileId(categoryId, profile.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));

        long usage = countTransactions(categoryId);
        if (usage > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This category is used by " + usage
                            + (usage == 1 ? " transaction" : " transactions")
                            + " and cannot be deleted");
        }

        try {
            categoryRepository.delete(category);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // A transaction was added between the usage check and the delete
            throw new ApiException(HttpStatus.CONFLICT, "This category is in use and cannot be deleted");
        }
    }

    // Helper methods
    private long countTransactions(Long categoryId) {
        return incomeRepository.countByCategoryId(categoryId)
                + expenseRepository.countByCategoryId(categoryId);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category name is required");
        }

        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Category name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!VALID_TYPES.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category type must be either income or expense");
        }
        return normalized;
    }

    private CategoryDto toDto(CategoryEntity category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .type(category.getType())
                .profileId(category.getProfile() != null ? category.getProfile().getId() : null)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

}