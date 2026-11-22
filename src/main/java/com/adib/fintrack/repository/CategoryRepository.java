package com.adib.fintrack.repository;

import com.adib.fintrack.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByProfileId(Long profileId);

    Optional<CategoryEntity> findByIdAndProfileId(Long id, Long profileId);

    List<CategoryEntity> findByTypeAndProfileId(String type, Long profileId);

    // Legacy name-only check; superseded by the type-aware methods below
    boolean existsByNameAndProfileId(String name, Long profileId);

    // Duplicate check: same user, same type, same name (case-insensitive)
    boolean existsByProfileIdAndTypeIgnoreCaseAndNameIgnoreCase(Long profileId, String type, String name);

    // Same check for updates, excluding the category being edited
    boolean existsByProfileIdAndTypeIgnoreCaseAndNameIgnoreCaseAndIdNot(Long profileId, String type, String name, Long id);
}