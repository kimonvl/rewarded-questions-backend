package com.example.rewarded_questions_app.api;

import com.example.rewarded_questions_app.dto.request.*;
import com.example.rewarded_questions_app.dto.GenericResponse;
import com.example.rewarded_questions_app.dto.response.QuestionDTO;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDetailsDTO;
import com.example.rewarded_questions_app.dto.response.QuestionnaireWithQuestionsDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.exceptions.ValidationException;
import com.example.rewarded_questions_app.service.QuestionService;
import com.example.rewarded_questions_app.service.QuestionnaireService;
import com.example.rewarded_questions_app.validator.CreateQuestionRequestValidator;
import com.example.rewarded_questions_app.validator.EditQuestionnaireDetailsRequestValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/questions")
public class QuestionRestController {

    private final QuestionService questionService;

    @PutMapping("{id}")
    public ResponseEntity<@NonNull GenericResponse<QuestionDTO>> editQuestion(
            @Valid @RequestBody EditQuestionRequest req,
            BindingResult bindingResult,
            Principal principal,
            @PathVariable UUID id
    ) throws ValidationException, EntityInvalidArgumentException, EntityNotFoundException {

        if (bindingResult.hasErrors()) {
            throw new ValidationException("EditQuestionRequest", "Question editing failed during validation", bindingResult);
        }
        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionService.editQuestion(req, id, principal.getName()),
                        "EditQuestionSucceeded",
                        "Question edited successfully",
                        true
                ),
                HttpStatus.OK
        );
    }

    @DeleteMapping("{id}")
    public ResponseEntity<@NonNull GenericResponse<?>> deleteQuestion(
            Principal principal,
            @PathVariable UUID id
    ) throws EntityInvalidArgumentException, EntityNotFoundException {
        questionService.deleteQuestion(id, principal.getName());
        //questionService.deleteQuestion(id, principal.getName());
        return new ResponseEntity<>(
                new GenericResponse<>(
                        null,
                        "DeleteQuestionSucceeded",
                        "Question deleted successfully",
                        true
                ),
                HttpStatus.OK
        );
    }
}