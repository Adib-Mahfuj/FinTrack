package com.adib.fintrack.controller;

import com.adib.fintrack.dto.ExpenseDto;
import com.adib.fintrack.dto.FilterDto;
import com.adib.fintrack.dto.IncomeDto;
import com.adib.fintrack.service.ExpenseService;
import com.adib.fintrack.service.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/filter")
public class FilterController {

    private static final List<String> ALLOWED_SORT_FIELDS = List.of("date", "amount", "name");

    private final ExpenseService expenseService;
    private final IncomeService incomeService;

    @PostMapping
    public ResponseEntity<?> filterTransactions(@RequestBody FilterDto filter) {

        // preparing the data or validation
        LocalDate startDate = filter.getStartDate() != null ? filter.getStartDate() : LocalDate.of(1970, 1, 1);
        LocalDate endDate = filter.getEndDate() != null ? filter.getEndDate() : LocalDate.now();
        String keyword = filter.getKeyword() != null ? filter.getKeyword() : "";

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body("Start date cannot be after end date");
        }

        String sortField = ALLOWED_SORT_FIELDS.contains(filter.getSortField()) ? filter.getSortField() : "date";
        Sort.Direction direction = "desc".equalsIgnoreCase(filter.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField);

        if ("income".equalsIgnoreCase(filter.getType())) {
            List<IncomeDto> incomes = incomeService.filterIncomes(startDate, endDate, keyword, sort);
            return ResponseEntity.ok(incomes);
        } else if ("expense".equalsIgnoreCase(filter.getType())) {
            List<ExpenseDto> expenses = expenseService.filterExpenses(startDate, endDate, keyword, sort);
            return ResponseEntity.ok(expenses);
        } else {
            return ResponseEntity.badRequest().body("Invalid type. Must be 'income' or 'expense'");
        }
    }
}