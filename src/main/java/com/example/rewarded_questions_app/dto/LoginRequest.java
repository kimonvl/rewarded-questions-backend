package com.example.rewarded_questions_app.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(min = 3, max = 100) String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotNull Long roleId
) {}
