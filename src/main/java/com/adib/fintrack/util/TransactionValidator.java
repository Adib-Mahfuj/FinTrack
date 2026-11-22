package com.adib.fintrack.util;

import com.adib.fintrack.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Shared input rules for income and expense requests.
 */
public final class TransactionValidator {

    private static final int MAX_NAME_LENGTH = 255;

    private TransactionValidator() {
    }

    // Returns the trimmed name or rejects blank / oversized values
    public static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name is required");
        }

        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    public static void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }
    }

    // A null date is allowed: the entity defaults it to today on persist
    public static void rejectFutureDate(LocalDate date) {
        // One day of tolerance covers timezone differences between client and server
        if (date != null && date.isAfter(LocalDate.now().plusDays(1))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Date cannot be in the future");
        }
    }
}