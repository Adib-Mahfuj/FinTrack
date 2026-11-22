package com.adib.fintrack.service;

import com.adib.fintrack.dto.ExpenseDto;
import com.adib.fintrack.entity.CategoryEntity;
import com.adib.fintrack.entity.ExpenseEntity;
import com.adib.fintrack.entity.ProfileEntity;
import com.adib.fintrack.exception.ApiException;
import com.adib.fintrack.repository.CategoryRepository;
import com.adib.fintrack.repository.ExpenseRepository;
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
public class ExpenseService {

    private final ProfileService profileService;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    // Validates and saves a new expense
    public ExpenseDto addExpense(ExpenseDto dto) {
        ProfileEntity profile = profileService.getCurrentProfile();

        dto.setName(TransactionValidator.requireName(dto.getName()));
        TransactionValidator.requirePositiveAmount(dto.getAmount());
        TransactionValidator.rejectFutureDate(dto.getDate());

        if (dto.getCategoryId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category is required");
        }

        CategoryEntity category = categoryRepository.findByIdAndProfileId(dto.getCategoryId(), profile.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found for this profile"));

        if (!"expense".equalsIgnoreCase(category.getType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selected category is not an expense category");
        }

        ExpenseEntity newExpense = toEntity(dto, profile, category);
        newExpense = expenseRepository.save(newExpense);
        return toDto(newExpense);
    }

    // Expenses of the current calendar month
    public List<ExpenseDto> getCurrentMonthExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());
        List<ExpenseEntity> list = expenseRepository.findByProfileIdAndDateBetween(profile.getId(), startDate, endDate);
        return list.stream().map(this::toDto).toList();
    }

    // Deletes an expense owned by the current user
    public void deleteExpense(Long expenseId) {
        ProfileEntity profile = profileService.getCurrentProfile();
        ExpenseEntity entity = expenseRepository.findByIdAndProfileId(expenseId, profile.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
        expenseRepository.delete(entity);
    }

    // Latest 5 expenses of the current user
    public List<ExpenseDto> getLatest5ExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<ExpenseEntity> list = expenseRepository.findTop5ByProfileIdOrderByDateDesc(profile.getId());
        return list.stream().map(this::toDto).toList();
    }

    // All-time expense total of the current user
    public BigDecimal getTotalExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        BigDecimal total = expenseRepository.findTotalExpenseByProfileId(profile.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    // Filtered and sorted expenses
    public List<ExpenseDto> filterExpenses(LocalDate startDate, LocalDate endDate, String keyword, Sort sort) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<ExpenseEntity> list = expenseRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
                profile.getId(), startDate, endDate, keyword, sort);
        return list.stream().map(this::toDto).toList();
    }

    // Used by the notification job (runs outside a web request)
    public List<ExpenseDto> getExpensesForUserOnDate(Long profileId, LocalDate date) {
        List<ExpenseEntity> list = expenseRepository.findByProfileIdAndDate(profileId, date);
        return list.stream().map(this::toDto).toList();
    }

    // Helper methods
    private ExpenseEntity toEntity(ExpenseDto dto, ProfileEntity profile, CategoryEntity category) {
        return ExpenseEntity.builder()
                .name(dto.getName())
                .icon(dto.getIcon())
                .amount(dto.getAmount())
                .date(dto.getDate())
                .profile(profile)
                .category(category)
                .build();
    }

    private ExpenseDto toDto(ExpenseEntity entity) {
        return ExpenseDto.builder()
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