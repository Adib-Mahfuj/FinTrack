package com.adib.fintrack.controller;

import com.adib.fintrack.dto.IncomeDto;
import com.adib.fintrack.entity.ProfileEntity;
import com.adib.fintrack.service.EmailService;
import com.adib.fintrack.service.ExcelService;
import com.adib.fintrack.service.IncomeService;
import com.adib.fintrack.service.ProfileService;
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
}