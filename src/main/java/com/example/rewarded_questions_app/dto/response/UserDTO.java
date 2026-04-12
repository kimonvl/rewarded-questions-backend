package com.example.rewarded_questions_app.dto.response;

import java.util.UUID;

public record UserDTO (
        UUID id,
        String email,
        Long roleId
) {

}