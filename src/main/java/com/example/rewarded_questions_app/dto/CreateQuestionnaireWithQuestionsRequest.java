package com.example.rewarded_questions_app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuestionnaireWithQuestionsRequest(
        @NotBlank
        @Size(min = 3, max = 200)
        String title,

        @Size(max = 200)
        String description,

        @Valid
        @NotNull
        @Size(min = 1)
        List<CreateQuestionRequest> questions
) {
}
