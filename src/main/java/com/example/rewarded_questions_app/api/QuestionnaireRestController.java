package com.example.rewarded_questions_app.api;

import com.example.rewarded_questions_app.dto.CreateQuestionnaireWithQuestionsRequest;
import com.example.rewarded_questions_app.dto.GenericResponse;
import com.example.rewarded_questions_app.dto.RegisterRequest;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDTO;
import com.example.rewarded_questions_app.exceptions.EntityAlreadyExistsException;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.exceptions.ValidationException;
import com.example.rewarded_questions_app.service.QuestionnaireService;
import com.example.rewarded_questions_app.validator.CreateQuestionnaireValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/questionnaires")
public class QuestionnaireRestController {

    private final QuestionnaireService questionnaireService;
    private final CreateQuestionnaireValidator createQuestionnaireValidator;

    @PostMapping()
    public ResponseEntity<@NonNull GenericResponse<QuestionnaireDTO>> createQuestionnaireWithQuestions(
            @Valid @RequestBody CreateQuestionnaireWithQuestionsRequest req,
            BindingResult bindingResult,
            Principal principal
    ) throws ValidationException, EntityNotFoundException {

        createQuestionnaireValidator.validate(req, bindingResult);
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
}
