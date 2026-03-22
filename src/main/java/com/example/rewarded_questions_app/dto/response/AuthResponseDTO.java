package com.example.rewarded_questions_app.dto.response;

public record AuthResponseDTO(
        String token,
        UserDTO user
) {
}
