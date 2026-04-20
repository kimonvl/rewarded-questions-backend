package com.example.rewarded_questions_app.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderQuestionsRequest(

        @NotNull @NotEmpty List<UUID> questionUUIDs
) { }
