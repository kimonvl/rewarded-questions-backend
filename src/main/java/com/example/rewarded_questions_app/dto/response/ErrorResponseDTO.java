package com.example.rewarded_questions_app.dto.response;

public record ErrorResponseDTO(String code, String description) {

    public ErrorResponseDTO(String code) {
        this(code, "");
    }
}