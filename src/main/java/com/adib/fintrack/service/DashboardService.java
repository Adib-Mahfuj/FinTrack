package com.adib.fintrack.service;

import com.adib.fintrack.dto.ExpenseDto;
import com.adib.fintrack.dto.IncomeDto;
import com.adib.fintrack.dto.RecentTransactionDto;
import com.adib.fintrack.entity.ProfileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    // Newest transaction first; ties are broken by creation time
    private static final Comparator<RecentTransactionDto> NEWEST_FIRST = Comparator
            .comparing(RecentTransactionDto::getDate, Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()))
            .thenComparing(RecentTransactionDto::getCreatedAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));

    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final ProfileService profileService;

    public Map<String, Object> getDashboardData() {
        ProfileEntity profile = profileService.getCurrentProfile();

        List<IncomeDto> latestIncomes = incomeService.getLatest5IncomesForCurrentUser();
        List<ExpenseDto> latestExpenses = expenseService.getLatest5ExpensesForCurrentUser();

        // Each total is read once and reused for the balance
        BigDecimal totalIncome = incomeService.getTotalIncomesForCurrentUser();
        BigDecimal totalExpense = expenseService.getTotalExpensesForCurrentUser();

        List<RecentTransactionDto> recentTransactions = Stream.concat(
                        latestIncomes.stream().map(income -> fromIncome(income, profile)),
                        latestExpenses.stream().map(expense -> fromExpense(expense, profile)))
                .sorted(NEWEST_FIRST)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalBalance", totalIncome.subtract(totalExpense));
        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("recent5Expenses", latestExpenses);
        result.put("recent5Incomes", latestIncomes);
        result.put("recentTransactions", recentTransactions);
        return result;
    }

    // Mapping helpers
    private RecentTransactionDto fromIncome(IncomeDto income, ProfileEntity profile) {
        return RecentTransactionDto.builder()
                .id(income.getId())
                .profileId(profile.getId())
                .icon(income.getIcon())
                .name(income.getName())
                .amount(income.getAmount())
                .date(income.getDate())
                .createdAt(income.getCreatedAt())
                .updatedAt(income.getUpdatedAt())
                .type("income")
                .build();
    }

    private RecentTransactionDto fromExpense(ExpenseDto expense, ProfileEntity profile) {
        return RecentTransactionDto.builder()
                .id(expense.getId())
                .profileId(profile.getId())
                .icon(expense.getIcon())
                .name(expense.getName())
                .amount(expense.getAmount())
                .date(expense.getDate())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .type("expense")
                .build();
    }
}