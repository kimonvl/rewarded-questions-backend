package com.example.rewarded_questions_app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EditQuestionRequest(
    @NotNull
    @NotBlank
    @Size(min = 5, max = 500)
    String  text,

    @Min(1)
    Long selectMin,

    @Min(1)
    Long selectMax,

    @Valid
    List<EditPossibleChoiceRequest> possibleChoices
) { }
