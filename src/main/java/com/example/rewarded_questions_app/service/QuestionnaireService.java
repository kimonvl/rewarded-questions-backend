package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.CreateQuestionnaireRequest;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;

import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireService {
    QuestionnaireDTO createQuestionnaire(CreateQuestionnaireRequest request, String email) throws EntityNotFoundException, EntityInvalidArgumentException;
    Optional<Questionnaire> findQuestionnaireByUuid(UUID uuid);
}
