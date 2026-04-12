package com.example.rewarded_questions_app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuestionRequest(

        @NotBlank
        @Size(min = 5, max = 500)
        String text,

        @NotNull
        Boolean isFreeText,

        @NotNull
        @Min(1)
        Long selectMin,

        @Min(1)
        Long selectMax,

        @Valid
        List<CreatePossibleChoiceRequest> possibleChoices
) {
}
