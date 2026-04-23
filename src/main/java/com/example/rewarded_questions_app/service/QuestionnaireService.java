package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.CreateQuestionnaireRequest;
import com.example.rewarded_questions_app.dto.request.EditQuestionnaireDetailsRequest;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDetailsDTO;
import com.example.rewarded_questions_app.dto.response.QuestionnaireWithQuestionsDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;

import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireService {
    QuestionnaireWithQuestionsDTO createQuestionnaire(CreateQuestionnaireRequest request, String email) throws EntityNotFoundException, EntityInvalidArgumentException;
    QuestionnaireDetailsDTO editQuestionnaireDetails(EditQuestionnaireDetailsRequest request, UUID questionnaireId, String email) throws EntityNotFoundException, EntityInvalidArgumentException;
    UUID deleteQuestionnaire(UUID questionnaireId, String email) throws EntityNotFoundException, EntityInvalidArgumentException;

    Optional<Questionnaire> findQuestionnaireByUuid(UUID uuid);
    boolean existsQuestionnaireByTitleAndUserEmail(String title, String email);
}
