package com.example.rewarded_questions_app.mapper;

import com.example.rewarded_questions_app.dto.CreateQuestionRequest;
import com.example.rewarded_questions_app.dto.response.QuestionDTO;
import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class QuestionMapper {

    private final PossibleChoiceMapper possibleChoiceMapper;

    public Question createQuestionRequestToQuestion(CreateQuestionRequest request) {
        Question question = new Question();
        question.setText(request.text());
        question.setIsFreeText(request.isFreeText());
        question.setSelectMin(request.selectMin());
        question.setSelectMax(request.selectMax());

        return question;
    }

    public QuestionDTO toDto(Question question) {
        return new QuestionDTO(
                question.getUuid(),
                question.getQuestionnaire().getUuid(),
                question.getText(),
                question.getIsFreeText(),
                question.getSelectMin(),
                question.getSelectMax(),
                question.getAllPossibleChoices().stream()
                        .sorted(Comparator.comparing(PossibleChoice::getOrder))
                        .map(possibleChoiceMapper::toDto)
                        .toList()
        );
    }
}
