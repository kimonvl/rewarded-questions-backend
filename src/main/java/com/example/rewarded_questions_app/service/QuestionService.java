package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.CreateQuestionRequest;
import com.example.rewarded_questions_app.dto.request.EditQuestionRequest;
import com.example.rewarded_questions_app.dto.request.ReorderQuestionsRequest;
import com.example.rewarded_questions_app.dto.response.QuestionDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface QuestionService {
    QuestionDTO createQuestion(CreateQuestionRequest request, UUID questionnaireId, String email) throws EntityNotFoundException, EntityInvalidArgumentException;
    List<QuestionDTO> reorderQuestions(ReorderQuestionsRequest request, UUID questionnaireId, String email) throws EntityInvalidArgumentException, EntityNotFoundException;
    QuestionDTO editQuestion(EditQuestionRequest request, UUID questionId, String email) throws EntityNotFoundException, EntityInvalidArgumentException;
    void deleteQuestion(UUID questionId, String email) throws EntityNotFoundException, EntityInvalidArgumentException;
    Page<QuestionDTO> getPaginatedQuestionsForQuestionnaire(Pageable pageable, UUID questionnaireId) throws EntityNotFoundException;

    boolean existsByTextAndQuestionnaireId(String text, Long questionnaireId);
}
