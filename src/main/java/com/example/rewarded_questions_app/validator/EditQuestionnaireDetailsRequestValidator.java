package com.example.rewarded_questions_app.validator;

import com.example.rewarded_questions_app.dto.request.EditQuestionnaireDetailsRequest;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.service.QuestionnaireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EditQuestionnaireDetailsRequestValidator implements Validator {

    private final QuestionnaireService questionnaireService;

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return EditQuestionnaireDetailsRequest.class == clazz;
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        EditQuestionnaireDetailsRequest request = (EditQuestionnaireDetailsRequest) target;

        if (!errors.hasFieldErrors("title") && !errors.hasFieldErrors("description")) {
            if (request.title() == null && request.description() == null) {
                errors.reject(
                        "EditQuestionnaireDetailsRequestEmptyFieldsValidator",
                        "At least one of the fields (title or description) must be provided for editing."
                );
                log.warn("Questionnaire details editing failed. Both title and description fields are null in the request.");
            }
        }
    }

    public void validate(EditQuestionnaireDetailsRequest request, Errors errors, UUID questionnaireId, String userEmail) {
        validate(request, errors);

        if (!errors.hasFieldErrors("title") && !errors.hasFieldErrors("description")) {
            Optional<Questionnaire> questionnaireOpt = questionnaireService.findQuestionnaireByUuid(questionnaireId);
            if (questionnaireOpt.isEmpty()) {
                // TODO: find a way to send generic errors in the response
                errors.reject(
                        "QuestionnaireNotFoundValidator",
                        "Questionnaire with id=" + questionnaireId + " not found"
                );
                log.warn("Questionnaire details editing failed. Questionnaire with id={} not found", questionnaireId);
                return;
            }

            if (questionnaireService.existsQuestionnaireByTitleAndUserEmail(request.title(), userEmail)) {
                errors.rejectValue(
                        "title",
                        "EditQuestionnaireDetailsRequestTitleNotUniqueValidator",
                        "Title of the questionnaire already exists for user."
                );
                log.warn("Questionnaire details editing failed. Questionnaire title={} already exists while editing questionnaire with id={}", questionnaireOpt.get().getTitle(), questionnaireId);
            }
        }
    }
}