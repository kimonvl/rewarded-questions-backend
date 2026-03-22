package com.example.rewarded_questions_app.dto.response;

import java.util.List;
import java.util.UUID;

public record QuestionnaireDTO(
        UUID uuid,
        String title,
        String description,
        List<QuestionDTO> questions
) {
}