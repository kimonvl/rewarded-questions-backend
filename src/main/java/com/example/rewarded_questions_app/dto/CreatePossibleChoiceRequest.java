package com.example.rewarded_questions_app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePossibleChoiceRequest(
        @NotBlank
        @Size(min = 1, max = 200)
        String text
) {
}
