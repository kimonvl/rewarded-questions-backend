package com.example.rewarded_questions_app.dto.response;

import java.util.List;
import java.util.UUID;

public record QuestionDTO(
        UUID uuid,
        UUID questionnaireId,
        String text,
        Boolean isFreeText,
        Long selectMin,
        Long selectMax,
        Long order,
        List<PossibleChoiceDTO> possibleChoices
) {
}