package com.adib.fintrack.controller;

import com.adib.fintrack.dto.ExpenseDto;
import com.adib.fintrack.dto.IncomeDto;
import com.adib.fintrack.entity.ProfileEntity;
import com.adib.fintrack.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/email")
public class EmailController {

    private final IncomeService incomeService;
    private final ExcelService excelService;
    private final EmailService emailService;
    private final ProfileService profileService;
    private final ExpenseService expenseService;

    @GetMapping("/income-excel")
    public ResponseEntity<Void> emailIncomeExcel() throws IOException {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<IncomeDto> incomes = incomeService.getCurrentMonthIncomesForCurrentUser();
        byte[] file = excelService.generateIncomeExcel(incomes);

        String subject = "Your FinTrack Income Report";
        String body = "<p>Hi " + profile.getFullName() + ",</p>"
                + "<p>Please find your income report attached.</p>"
                + "<p>Regards,<br/>FinTrack</p>";

        emailService.sendEmailWithAttachment(
                profile.getEmail(), subject, body, file, "income_details.xlsx");

        return ResponseEntity.ok().build();
    }

    @GetMapping("/expense-excel")
    public ResponseEntity<Void> emailExpenseExcel() throws IOException {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<ExpenseDto> expenses = expenseService.getCurrentMonthExpensesForCurrentUser();
        byte[] file = excelService.generateExpenseExcel(expenses);

        String subject = "Your FinTrack Expense Report";
        String body = "<p>Hi " + profile.getFullName() + ",</p>"
                + "<p>Please find your expense report attached.</p>"
                + "<p>Regards,<br/>FinTrack</p>";

        emailService.sendEmailWithAttachment(
                profile.getEmail(), subject, body, file, "expense_details.xlsx");

        return ResponseEntity.ok().build();
    }
}