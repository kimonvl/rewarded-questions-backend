package com.example.rewarded_questions_app.dto;

public record AuthResponseDTO(
        String token,
        UserDTO user
) {
}
