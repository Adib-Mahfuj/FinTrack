package com.adib.fintrack.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Editable profile fields. A blank profileImageUrl removes the photo.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {
    private String fullName;
    private String profileImageUrl;
}