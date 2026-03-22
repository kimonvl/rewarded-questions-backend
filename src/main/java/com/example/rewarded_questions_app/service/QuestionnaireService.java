package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.CreateQuestionnaireWithQuestionsRequest;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDTO;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;

public interface QuestionnaireService {
    QuestionnaireDTO createQuestionnaire(CreateQuestionnaireWithQuestionsRequest request, String username) throws EntityNotFoundException;
}
