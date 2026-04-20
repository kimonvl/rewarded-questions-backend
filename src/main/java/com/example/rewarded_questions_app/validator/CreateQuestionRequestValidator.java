package com.example.rewarded_questions_app.validator;

import com.example.rewarded_questions_app.dto.request.CreatePossibleChoiceRequest;
import com.example.rewarded_questions_app.dto.request.CreateQuestionRequest;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.service.QuestionService;
import com.example.rewarded_questions_app.service.QuestionnaireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateQuestionRequestValidator implements Validator {

    private final QuestionService questionService;
    private final QuestionnaireService questionnaireService;

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return CreateQuestionRequest.class == clazz;
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        CreateQuestionRequest request = (CreateQuestionRequest) target;

        if (!errors.hasFieldErrors("isFreeText")) {
            if (request.isFreeText()) {
                if (!errors.hasFieldErrors("possibleChoices")) {
                    if (!request.possibleChoices().isEmpty()) {
                        errors.rejectValue(
                                "possibleChoices",
                                "PossibleChoicesNotEmptyInFreeTextValidator",
                                "Free text questions cannot have possible choices."
                        );
                        log.warn("Question creation failed. Free text question provided with possible choices.");
                    }
                }
                if (!errors.hasFieldErrors("selectMin")) {
                    if (request.selectMin() != 0) {
                        errors.rejectValue(
                                "selectMin",
                                "SelectMinNotZeroInFreeTextValidator",
                                "Free text questions must have selectMin value equal to 0."
                        );
                        log.warn("Question creation failed. Free text question provided with selectMin value not equal to 0.");
                    }
                }
                if (!errors.hasFieldErrors("selectMax")) {
                    if (request.selectMax() != 0) {
                        errors.rejectValue(
                                "selectMax",
                                "SelectMaxNotZeroInFreeTextValidator",
                                "Free text questions must have selectMax value equal to 0."
                        );
                        log.warn("Question creation failed. Free text question provided with selectMax value not equal to 0.");
                    }
                }
            } else {
                if (!errors.hasFieldErrors("possibleChoices")) {
                    if (request.possibleChoices().size() < 2) {
                        errors.rejectValue(
                                "possibleChoices",
                                "PossibleChoicesLessThanTwoInMultipleChoiceValidator",
                                "Multiple choices questions must have at least 2 possible choices."
                        );
                        log.warn("Question creation failed. Multiple choices question provided with less than 2 possible choices.");
                    }
                }
                if (!errors.hasFieldErrors("selectMin")) {
                    if (request.selectMin() < 1) {
                        errors.rejectValue(
                                "selectMin",
                                "SelectMinLessThanOneInMultipleChoiceValidator",
                                "Multiple choices questions must have selectMin equal or greater than 1."
                        );
                        log.warn("Question creation failed. Multiple choices question provided with selectMin less than 1.");
                    }
                }
                if (!errors.hasFieldErrors("selectMin")) {
                    if (request.selectMin() > request.selectMax()) {
                        errors.rejectValue(
                                "selectMin",
                                "SelectMinGreaterThanSelectMaxInMultipleChoiceValidator",
                                "Multiple choices questions must have selectMin equal or lesser than selectMax."
                        );
                        log.warn("Question creation failed. Multiple choices question provided with selectMin greater than selectMax.");
                    }
                }
                if (!errors.hasFieldErrors("selectMax")) {
                    if (request.selectMax() > request.possibleChoices().size()) {
                        errors.rejectValue(
                                "selectMax",
                                "SelectMaxGreaterThanChoicesSizeInMultipleChoiceValidator",
                                "Multiple choices questions must have selectMax equal or lesser than choices size."
                        );
                        log.warn("Question creation failed. Multiple choices question provided with selectMax greater than choices size.");
                    }
                }
                // Check for duplicate possible choice texts within the same question
                if (!errors.hasFieldErrors("possibleChoices")) {
                    HashSet<String> choices = new HashSet<>();
                    for (CreatePossibleChoiceRequest choice : request.possibleChoices()) {
                        if (choices.contains(choice.text())) {
                            errors.rejectValue(
                                    "possibleChoices",
                                    "DuplicatePossibleChoiceInMultipleChoiceValidator",
                                    "Multiple choices questions must have unique possible choices."
                            );
                            log.warn("Question creation failed. Multiple choices question provided with duplicate possible choice.");
                        } else {
                            choices.add(choice.text());
                        }
                    }
                }

            }

        }

    }

    public void validate(CreateQuestionRequest request, Errors errors, UUID questionnaireId) {
        Optional<Questionnaire> questionnaire = questionnaireService.findQuestionnaireByUuid(questionnaireId);
        if (questionnaire.isEmpty()) {
            errors.reject(
                    "QuestionnaireNotFoundValidator",
                    "Questionnaire with id=" + questionnaireId + " not found"
            );
            log.warn("Question creation failed. Questionnaire with id={} not found", questionnaireId);
            return;
        }

        if (questionService.existsByTextAndQuestionnaireId(request.text(), questionnaire.get().getId()) && !errors.hasFieldErrors("text")) {
            errors.rejectValue(
                    "text",
                    "QuestionTextUniqueValidator",
                    "A question with this text already exists in this questionnaire"
            );
            log.warn("Question creation failed. A question with the same text already exists in questionnaire with id={}", questionnaireId);
        }

        validate(request, errors);
    }
}