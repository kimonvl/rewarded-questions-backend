package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.CreateQuestionRequest;
import com.example.rewarded_questions_app.dto.ReorderQuestionsRequest;
import com.example.rewarded_questions_app.dto.response.QuestionDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;

import java.util.List;
import java.util.UUID;

public interface QuestionService {
    QuestionDTO createQuestion(CreateQuestionRequest request, UUID questionnaireId, String email) throws EntityNotFoundException, EntityInvalidArgumentException;
    List<QuestionDTO> reorderQuestions(ReorderQuestionsRequest request, UUID questionnaireId, String email) throws EntityInvalidArgumentException, EntityNotFoundException;
    boolean existsByTextAndQuestionnaireId(String text, Long questionnaireId);
}
