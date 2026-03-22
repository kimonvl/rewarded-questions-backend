package com.example.rewarded_questions_app.dto;

import java.util.UUID;

public record UserDTO (
        UUID id,
        String username,
        Long roleId
) {

}