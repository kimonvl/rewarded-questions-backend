package com.example.rewarded_questions_app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePossibleChoiceRequest(
        @NotNull
        @Min(1)
        Long order,

        @NotBlank
        @Size(min = 1, max = 200)
        String text
) {
}
