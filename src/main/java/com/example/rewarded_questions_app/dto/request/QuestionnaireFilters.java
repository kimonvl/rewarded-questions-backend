package com.example.rewarded_questions_app.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QuestionnaireFilters {
    private String title;
    private String businessName;
}
