package com.example.rewarded_questions_app.api;

import com.example.rewarded_questions_app.dto.request.CreateQuestionRequest;
import com.example.rewarded_questions_app.dto.request.CreateQuestionnaireRequest;
import com.example.rewarded_questions_app.dto.GenericResponse;
import com.example.rewarded_questions_app.dto.request.EditQuestionnaireDetailsRequest;
import com.example.rewarded_questions_app.dto.request.QuestionnaireFilters;
import com.example.rewarded_questions_app.dto.request.ReorderQuestionsRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/questionnaires")
public class QuestionnaireRestController {

    private final QuestionnaireService questionnaireService;
    private final QuestionService questionService;

    private final CreateQuestionRequestValidator createQuestionRequestValidator;
    private final EditQuestionnaireDetailsRequestValidator editQuestionnaireDetailsRequestValidator;

    @GetMapping()
    public ResponseEntity<@NonNull GenericResponse<Page<QuestionnaireDetailsDTO>>> getFilteredAndPaginatedQuestionnaires(
            @PageableDefault(page = 0, size = 10) Pageable pageable,
            @ModelAttribute QuestionnaireFilters filters
    ) {
        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionnaireService.getFilteredAndPaginatedQuestionnaires(pageable, filters),
                        "GetQuestionnairesSucceeded",
                        "Questionnaires retrieved successfully",
                        true
                ),
                HttpStatus.OK
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<@NonNull GenericResponse<QuestionnaireDetailsDTO>> getQuestionnaireDetails(
            @PathVariable UUID id
    ) throws EntityNotFoundException {
        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionnaireService.getQuestionnaireDetails(id),
                        "GetQuestionnaireDetailsSucceeded",
                        "Questionnaire retrieved successfully",
                        true
                ),
                HttpStatus.OK
        );
    }

    @GetMapping("{id}/questions")
    public ResponseEntity<@NonNull GenericResponse<Page<QuestionDTO>>> getQuestionnaireQuestions(
            @PageableDefault(page = 0, size = 10) Pageable pageable,
            @PathVariable UUID id
    ) throws EntityNotFoundException {
        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionService.getPaginatedQuestionsForQuestionnaire(pageable, id),
                        "GetQuestionnaireQuestionsSucceeded",
                        "Questionnaire's questions retrieved successfully",
                        true
                ),
                HttpStatus.OK
        );
    }


    @PostMapping()
    public ResponseEntity<@NonNull GenericResponse<QuestionnaireWithQuestionsDTO>> createQuestionnaire(
            @Valid @RequestBody CreateQuestionnaireRequest req,
            BindingResult bindingResult,
            Principal principal
    ) throws ValidationException, EntityNotFoundException, EntityInvalidArgumentException {

        if (bindingResult.hasErrors()) {
            throw new ValidationException("CreateQuestionnaireRequest", "Questionnaire creation failed during validation", bindingResult);
        }
        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionnaireService.createQuestionnaire(req, principal.getName()),
                        "CreateQuestionnaireSucceeded",
                        "Questionnaire created successfully",
                        true
                ),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<@NonNull GenericResponse<QuestionDTO>> createQuestion(
            @Valid @RequestBody CreateQuestionRequest req,
            BindingResult bindingResult,
            Principal principal,
            @PathVariable UUID id
    ) throws ValidationException, EntityInvalidArgumentException, EntityNotFoundException {

        createQuestionRequestValidator.validate(req, bindingResult, id);
        if (bindingResult.hasErrors()) {
            throw new ValidationException("CreateQuestionRequest", "Question creation failed during validation", bindingResult);
        }
        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionService.createQuestion(req,  id, principal.getName()),
                        "CreateQuestionSucceeded",
                        "Question created successfully",
                        true
                ),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/{id}/questions/order")
    public ResponseEntity<@NonNull GenericResponse<List<QuestionDTO>>> reorderQuestions(
            @Valid @RequestBody ReorderQuestionsRequest req,
            BindingResult bindingResult,
            Principal principal,
            @PathVariable UUID id
    ) throws ValidationException, EntityInvalidArgumentException, EntityNotFoundException {

        // TODO: ReorderQuestionsRequest validator
        if (bindingResult.hasErrors()) {
            throw new ValidationException("ReorderQuestionsRequest", "Question reordering failed during validation", bindingResult);
        }
        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionService.reorderQuestions(req,  id, principal.getName()),
                        "ReorderQuestionsSucceeded",
                        "Questions reordered successfully",
                        true
                ),
                HttpStatus.OK
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<@NonNull GenericResponse<QuestionnaireDetailsDTO>> editQuestionnaireDetails(
            @Valid @RequestBody EditQuestionnaireDetailsRequest req,
            BindingResult bindingResult,
            Principal principal,
            @PathVariable UUID id
    ) throws ValidationException, EntityInvalidArgumentException, EntityNotFoundException {
        editQuestionnaireDetailsRequestValidator.validate(req, bindingResult, id, principal.getName());
        if (bindingResult.hasErrors()) {
            throw new ValidationException("EditQuestionnaireDetailsRequest", "Questionnaire details editing failed during validation", bindingResult);
        }

        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionnaireService.editQuestionnaireDetails(req,  id, principal.getName()),
                        "EditQuestionnaireDetailsSucceeded",
                        "Questionnaire details edited successfully",
                        true
                ),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<@NonNull GenericResponse<UUID>> deleteQuestionnaire(
            Principal principal,
            @PathVariable UUID id
    ) throws EntityInvalidArgumentException, EntityNotFoundException {
        return new ResponseEntity<>(
                new GenericResponse<>(
                        questionnaireService.deleteQuestionnaire(id, principal.getName()),
                        "DeleteQuestionnaireSucceeded",
                        "Questionnaire deleted successfully",
                        true
                ),
                HttpStatus.NO_CONTENT
        );
    }
}
