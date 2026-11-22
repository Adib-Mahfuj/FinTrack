package com.adib.fintrack.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deliberately not @Data: no generated toString(), so passwords can never end up in logs.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
}