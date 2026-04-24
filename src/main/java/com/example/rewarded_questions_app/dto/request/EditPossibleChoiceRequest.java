package com.example.rewarded_questions_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EditPossibleChoiceRequest(
        UUID uuid,

        @NotNull
        @NotBlank
        @Size(min = 5, max = 200)
        String text
) {
}
