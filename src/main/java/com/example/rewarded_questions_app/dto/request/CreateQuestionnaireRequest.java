package com.example.rewarded_questions_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuestionnaireRequest(
        @NotBlank
        @Size(min = 3, max = 200)
        String title,

        @NotBlank
        @Size(max = 200)
        String description
) {
}
