package com.example.rewarded_questions_app.mapper;

import com.example.rewarded_questions_app.dto.request.CreateQuestionnaireRequest;
import com.example.rewarded_questions_app.dto.response.QuestionnaireWithQuestionsDTO;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionnaireMapper {

    private final QuestionMapper questionMapper;

    public Questionnaire createQuestionnaireReqToQuestionnaire(CreateQuestionnaireRequest request, User user) {
        //TODO: builder pattern
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setTitle(request.title());
        questionnaire.setDescription(request.description());
        questionnaire.setUser(user);
        return questionnaire;
    }

    public QuestionnaireWithQuestionsDTO toQuestionnaireDTO(Questionnaire questionnaire) {
        return new QuestionnaireWithQuestionsDTO(
                questionnaire.getUuid(),
                questionnaire.getTitle(),
                questionnaire.getDescription(),
                questionnaire.getAllQuestions().stream()
                        .map(questionMapper::toDto)
                        .toList()
        );
    }
}
