package com.example.rewarded_questions_app.api;

import com.example.rewarded_questions_app.dto.CreateQuestionRequest;
import com.example.rewarded_questions_app.dto.CreateQuestionnaireRequest;
import com.example.rewarded_questions_app.dto.GenericResponse;
import com.example.rewarded_questions_app.dto.response.QuestionDTO;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.exceptions.ValidationException;
import com.example.rewarded_questions_app.service.QuestionService;
import com.example.rewarded_questions_app.service.QuestionnaireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/questionnaires")
public class QuestionnaireRestController {

    private final QuestionnaireService questionnaireService;
    private final QuestionService questionService;

    @PostMapping()
    public ResponseEntity<@NonNull GenericResponse<QuestionnaireDTO>> createQuestionnaire(
            @Valid @RequestBody CreateQuestionnaireRequest req,
            BindingResult bindingResult,
            Principal principal
    ) throws ValidationException, EntityNotFoundException, EntityInvalidArgumentException {

        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error -> {
                System.out.println(error.getCode() + error.getDefaultMessage());
            });
            throw new ValidationException("CreateQuestionnaireRequest", "Questionnaire creation failed during validation", bindingResult);
        }
        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionnaireService.createQuestionnaire(req, principal.getName()),
                        "CreateQuestionnaireSucceeded",
                        "Questionnaire created successfully",
                        true
                ),
                HttpStatus.CREATED);
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<@NonNull GenericResponse<QuestionDTO>> createQuestion(
            @Valid @RequestBody CreateQuestionRequest req,
            BindingResult bindingResult,
            Principal principal,
            @PathVariable UUID id
    ) throws ValidationException, EntityInvalidArgumentException, EntityNotFoundException {

        // business validator for create question request
        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error -> {
                System.out.println(error.getCode() + error.getDefaultMessage());
            });
            throw new ValidationException("CreateQuestionRequest", "Question creation failed during validation", bindingResult);
        }
        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionService.createQuestion(req,  id, principal.getName()),
                        "CreateQuestionSucceeded",
                        "Question created successfully",
                        true
                ),
                HttpStatus.CREATED);
    }
}
