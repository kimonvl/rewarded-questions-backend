package com.example.rewarded_questions_app.dto.request;

import jakarta.validation.constraints.Size;

public record EditQuestionnaireDetailsRequest(
        @Size(min = 3, max = 30, message = "Title size must be between 3 and 30 characters") String title,
        @Size(min = 5, max = 200, message = "Description size must be between 5 and 200 characters") String description
) {
}
