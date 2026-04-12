package com.example.rewarded_questions_app.mapper;

import com.example.rewarded_questions_app.dto.CreatePossibleChoiceRequest;
import com.example.rewarded_questions_app.dto.response.PossibleChoiceDTO;
import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PossibleChoiceMapper {
    public PossibleChoice createPossibleChoiceReqToPossibleChoice(CreatePossibleChoiceRequest request, Long order) {
        PossibleChoice possibleChoice = new PossibleChoice();
        possibleChoice.setOrder(order);
        possibleChoice.setText(request.text());

        return possibleChoice;
    }

    public PossibleChoiceDTO toDto(PossibleChoice possibleChoice) {
        return new PossibleChoiceDTO(
                possibleChoice.getUuid(),
                possibleChoice.getText(),
                possibleChoice.getOrder()
        );
    }
}
