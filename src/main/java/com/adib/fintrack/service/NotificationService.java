package com.adib.fintrack.service;

import com.adib.fintrack.dto.ExpenseDto;
import com.adib.fintrack.entity.ProfileEntity;
import com.adib.fintrack.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final ExpenseService expenseService;

    @Value("${finance.tracker.frontend.url}")
    private String frontendUrl;

    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Dhaka")  // AT 10PM DAILY
    public void sendDailyIncomeExpenseReminder() {
        log.info("Job started: sendDailyIncomeExpenseReminder()");
        List<ProfileEntity> profiles = profileRepository.findAll();
        int successCount = 0;
        int failureCount = 0;

        for (ProfileEntity profile : profiles) {
            try {
                String body = "Hi " + profile.getFullName() + ",<br><br>"
                        + "This is a friendly reminder to add your income and expenses for today in FinTrack.<br><br>"
                        + "<a href=\"" + frontendUrl + "\" style=\"display:inline-block;padding:10px 20px;background-color:#4CAF50;color:#fff;text-decoration:none;border-radius:5px;font-weight:bold;\">Go to FinTrack</a>"
                        + "<br><br>Best regards,<br>FinTrack Team";

                String subject = "FinTrack Daily Reminder - " + LocalDate.now();
                emailService.sendEmail(profile.getEmail(), subject, body);
                successCount++;
                log.info("Reminder email sent successfully to: {}", profile.getEmail());

            } catch (Exception e) {
                failureCount++;
                log.error("Failed to send reminder email to user: {} - Error: {}", profile.getEmail(), e.getMessage(), e);
                // Continue to next user
            }
        }
        log.info("Job completed: sendDailyIncomeExpenseReminder() - Success: {}, Failed: {}", successCount, failureCount);
    }


    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Dhaka")  // AT 11PM DAILY
    public void sendDailyExpenseSummary() {
        log.info("Job started: sendDailyExpenseSummary()");
        List<ProfileEntity> profiles = profileRepository.findAll();
        int successCount = 0;
        int failureCount = 0;

        for (ProfileEntity profile : profiles) {
            try {
                List<ExpenseDto> todayExpenses = expenseService.getExpensesForUserOnDate(profile.getId(), LocalDate.now());
                if (!todayExpenses.isEmpty()) {
                    // Calculate total
                    BigDecimal total = todayExpenses.stream()
                            .map(ExpenseDto::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    StringBuilder table = new StringBuilder();
                    table.append("<table style='border-collapse:collapse;width:100%;'>");
                    table.append("<tr style='background-color:#f2f2f2;'><th style='border:1px solid #ddd;padding:8px;'>S.No</th><th style='border:1px solid #ddd;padding:8px;'>Name</th><th style='border:1px solid #ddd;padding:8px;'>Amount</th><th style='border:1px solid #ddd;padding:8px;'>Category</th></tr>");
                    int i = 1;
                    for (ExpenseDto expense : todayExpenses) {
                        table.append("<tr>");
                        table.append("<td style='border:1px solid #ddd;padding:8px;'>").append(i++).append("</td>");
                        table.append("<td style='border:1px solid #ddd;padding:8px;'>").append(expense.getName()).append("</td>");
                        table.append("<td style='border:1px solid #ddd;padding:8px;'>৳").append(expense.getAmount()).append("</td>");
                        table.append("<td style='border:1px solid #ddd;padding:8px;'>").append(expense.getCategoryId() != null ? expense.getCategoryName() : "N/A").append("</td>");
                        table.append("</tr>");
                    }
                    table.append("</table>");

                    String body = "Hi " + profile.getFullName() + ",<br/><br/> Here is a summary of your expenses for today:<br/><br/> " + table + "<br/><br/><strong>Total: ৳" + total + "</strong><br/><br/>Best regards,<br/>FinTrack Team";
                    String subject = "Your daily Expense Summary - " + LocalDate.now();
                    emailService.sendEmail(profile.getEmail(), subject, body);
                    successCount++;
                    log.info("Email sent successfully to: {}", profile.getEmail());
                }
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to send email to user: {} - Error: {}", profile.getEmail(), e.getMessage(), e);
                // Continue to next user
            }
        }

        log.info("Job Completed: sendDailyExpenseSummary() - Success: {}, Failed: {}", successCount, failureCount);
    }

}
