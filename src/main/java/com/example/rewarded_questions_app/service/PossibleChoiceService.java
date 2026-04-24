package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.EditPossibleChoiceRequest;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.model.questionnaire.Question;

import java.util.List;

public interface PossibleChoiceService {
    void editPossibleChoices(Question question, List<EditPossibleChoiceRequest> choices) throws EntityInvalidArgumentException;
}
