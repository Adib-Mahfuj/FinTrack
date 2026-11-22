package com.adib.fintrack.service;

import com.adib.fintrack.dto.IncomeDto;
import com.adib.fintrack.entity.CategoryEntity;
import com.adib.fintrack.entity.IncomeEntity;
import com.adib.fintrack.entity.ProfileEntity;
import com.adib.fintrack.exception.ApiException;
import com.adib.fintrack.repository.CategoryRepository;
import com.adib.fintrack.repository.IncomeRepository;
import com.adib.fintrack.util.TransactionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final ProfileService profileService;
    private final CategoryRepository categoryRepository;
    private final IncomeRepository incomeRepository;

    // Validates and saves a new income
    public IncomeDto addIncome(IncomeDto dto) {
        ProfileEntity profile = profileService.getCurrentProfile();

        dto.setName(TransactionValidator.requireName(dto.getName()));
        TransactionValidator.requirePositiveAmount(dto.getAmount());
        TransactionValidator.rejectFutureDate(dto.getDate());

        if (dto.getCategoryId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category is required");
        }

        CategoryEntity category = categoryRepository.findByIdAndProfileId(dto.getCategoryId(), profile.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found for this profile"));

        if (!"income".equalsIgnoreCase(category.getType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selected category is not an income category");
        }

        IncomeEntity newIncome = toEntity(dto, profile, category);
        newIncome = incomeRepository.save(newIncome);
        return toDto(newIncome);
    }

    // Incomes of the current calendar month
    public List<IncomeDto> getCurrentMonthIncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());
        List<IncomeEntity> list = incomeRepository.findByProfileIdAndDateBetween(profile.getId(), startDate, endDate);
        return list.stream().map(this::toDto).toList();
    }

    // Deletes an income owned by the current user
    public void deleteIncome(Long incomeId) {
        ProfileEntity profile = profileService.getCurrentProfile();
        IncomeEntity entity = incomeRepository.findByIdAndProfileId(incomeId, profile.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Income not found"));
        incomeRepository.delete(entity);
    }

    // Latest 5 incomes of the current user
    public List<IncomeDto> getLatest5IncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<IncomeEntity> list = incomeRepository.findTop5ByProfileIdOrderByDateDesc(profile.getId());
        return list.stream().map(this::toDto).toList();
    }

    // All-time income total of the current user
    public BigDecimal getTotalIncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        BigDecimal total = incomeRepository.findTotalIncomeByProfileId(profile.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    // Filtered and sorted incomes
    public List<IncomeDto> filterIncomes(LocalDate startDate, LocalDate endDate, String keyword, Sort sort) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<IncomeEntity> list = incomeRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
                profile.getId(), startDate, endDate, keyword, sort);
        return list.stream().map(this::toDto).toList();
    }

    // Helper methods
    private IncomeEntity toEntity(IncomeDto dto, ProfileEntity profile, CategoryEntity category) {
        return IncomeEntity.builder()
                .name(dto.getName())
                .icon(dto.getIcon())
                .amount(dto.getAmount())
                .date(dto.getDate())
                .profile(profile)
                .category(category)
                .build();
    }

    private IncomeDto toDto(IncomeEntity entity) {
        return IncomeDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .icon(entity.getIcon())
                .amount(entity.getAmount())
                .date(entity.getDate())
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : "N/A")
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}