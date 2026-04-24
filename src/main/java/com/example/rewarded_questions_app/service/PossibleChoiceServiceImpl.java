package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.EditPossibleChoiceRequest;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PossibleChoiceServiceImpl implements PossibleChoiceService {

    @Override
    public void editPossibleChoices(Question question, List<EditPossibleChoiceRequest> choices) throws EntityInvalidArgumentException {
        try {
            if (choices == null || choices.size() < 2) {
                throw new EntityInvalidArgumentException("EditPossibleChoicesChoicesSize", "At least 2 possible choices are required for a question.");
            }

            Set<UUID> editedPossibleChoicesFromRequest = new HashSet<>();
            for (EditPossibleChoiceRequest choice : choices) {
                if (choice.uuid() != null) {
                    editedPossibleChoicesFromRequest.add(choice.uuid());
                }
            }

            // Delete existing possible choices that are not included in the edit request
            for (PossibleChoice choice : new HashSet<>(question.getAllPossibleChoices())) {
                if (!editedPossibleChoicesFromRequest.contains(choice.getUuid())) {
                    question.removePossibleChoice(choice);
                }
            }

            // Edit or create choices
            for (int i = 0; i < choices.size(); i++) {
                EditPossibleChoiceRequest choice = choices.get(i);
                if (choice.uuid() != null) {
                    for (PossibleChoice possibleChoice : question.getAllPossibleChoices()) {
                        if (possibleChoice.getUuid().equals(choice.uuid())) {
                            possibleChoice.setText(choice.text());
                            possibleChoice.setOrder((long) i);
                            break;
                        }
                    }
                } else {
                    PossibleChoice newChoice = new PossibleChoice();
                    newChoice.setText(choice.text());
                    newChoice.setOrder((long) i);
                    question.addPossibleChoice(newChoice);
                }
            }

        } catch (EntityInvalidArgumentException e) {
            log.error("Error editing possible choices for question {}: Message={}", question.getId(), e.getMessage());
            throw e;
        }
    }
}
