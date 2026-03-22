package com.example.rewarded_questions_app.dto.response;


import java.util.UUID;

public record PossibleChoiceDTO(
        UUID uuid,
        String text,
        Long order
) {
}
