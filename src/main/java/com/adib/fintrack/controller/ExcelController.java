package com.adib.fintrack.controller;

import com.adib.fintrack.dto.IncomeDto;
import com.adib.fintrack.service.ExcelService;
import com.adib.fintrack.service.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/excel")
public class ExcelController {

    private final ExcelService excelService;
    private final IncomeService incomeService;

    @GetMapping("/download/income")
    public ResponseEntity<byte[]> downloadIncomeExcel() throws IOException {
        List<IncomeDto> incomes = incomeService.getCurrentMonthIncomesForCurrentUser();
        byte[] file = excelService.generateIncomeExcel(incomes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=income_details.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}