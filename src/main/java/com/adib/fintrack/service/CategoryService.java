package com.adib.fintrack.service;

import com.adib.fintrack.dto.CategoryDto;
import com.adib.fintrack.entity.CategoryEntity;
import com.adib.fintrack.entity.ProfileEntity;
import com.adib.fintrack.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;

    //save category methods
    public CategoryDto save(CategoryDto categoryDto) {
        ProfileEntity profile = profileService.getCurrentProfile();
        if (categoryRepository.existsByNameAndProfileId(categoryDto.getName(), profile.getId())) {
            throw new RuntimeException("Category with this name already exists");
        }
        CategoryEntity newCategory = toEntity(categoryDto, profile);
        newCategory = categoryRepository.save(newCategory);
        return toDto(newCategory);
    }

    //get categories for current user
    public List<CategoryDto> getCategoriesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> categories = categoryRepository.findByProfileId(profile.getId());
        return categories.stream().map(this::toDto).toList();
    }

    //get categories by type for current user
    public List<CategoryDto> getCategoriesByTypeForCurrentUser(String type) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> categories = categoryRepository.findByTypeAndProfileId(type, profile.getId());
        return categories.stream().map(this::toDto).toList();
    }

    //update categories
    public CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity existingCategory = categoryRepository.findByIdAndProfileId(categoryId, profile.getId())
                .orElseThrow(() -> new RuntimeException("Category not found or not accessible"));
        existingCategory.setName(categoryDto.getName());
        existingCategory.setIcon(categoryDto.getIcon());
        existingCategory = categoryRepository.save(existingCategory);
        return toDto(existingCategory);
    }

    // helper methods
    private CategoryEntity toEntity(CategoryDto categoryDto, ProfileEntity profile) {
        return CategoryEntity.builder()
                .name(categoryDto.getName())
                .profile(profile)
                .icon(categoryDto.getIcon())
                .type(categoryDto.getType())
                .build();
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
