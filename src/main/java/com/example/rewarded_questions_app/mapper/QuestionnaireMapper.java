package com.example.rewarded_questions_app.mapper;

import com.example.rewarded_questions_app.dto.response.QuestionnaireDTO;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionnaireMapper {

    private final QuestionMapper questionMapper;

    public QuestionnaireDTO toQuestionnaireDTO(Questionnaire questionnaire) {
        return new QuestionnaireDTO(
                questionnaire.getUuid(),
                questionnaire.getTitle(),
                questionnaire.getDescription(),
                questionnaire.getAllQuestions().stream()
                        .map(questionMapper::toDto)
                        .toList()
        );
    }
}
