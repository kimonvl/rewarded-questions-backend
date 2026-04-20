package com.example.rewarded_questions_app.dto.response;

import java.util.UUID;

public record QuestionnaireDetailsDTO(
        UUID uuid,
        String title,
        String description
) {
}
