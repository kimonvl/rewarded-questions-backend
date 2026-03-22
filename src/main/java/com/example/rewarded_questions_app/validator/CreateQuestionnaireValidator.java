package com.example.rewarded_questions_app.validator;

import com.example.rewarded_questions_app.dto.CreateQuestionRequest;
import com.example.rewarded_questions_app.dto.CreateQuestionnaireWithQuestionsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateQuestionnaireValidator implements Validator {

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return CreateQuestionnaireWithQuestionsRequest.class == clazz;
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        CreateQuestionnaireWithQuestionsRequest request = (CreateQuestionnaireWithQuestionsRequest) target;

        if (errors.hasFieldErrors("questions") || request.questions() == null) {
            return;
        }

        for (int i = 0; i < request.questions().size(); i++) {
            var question = request.questions().get(i);

            if (question == null) {
                errors.rejectValue(
                        "questions[" + i + "]",
                        "NotNull",
                        "Question entry cannot be null"
                );
                continue;
            }

            boolean hasChoices = question.possibleChoices() != null && !question.possibleChoices().isEmpty();

            validateSelectMinMax(errors, question, hasChoices, i);

            if (hasChoices) {
                int choicesSize = question.possibleChoices().size();

                validateSelectMinMaxAgainstChoicesSize(errors, question, choicesSize, i);

                validateChoicesOrder(errors, question, choicesSize, i);
            }
        }
    }

    private static void validateChoicesOrder(@NonNull Errors errors, CreateQuestionRequest question, int choicesSize, int i) {
        Set<Long> uniqueOrders = new HashSet<>();
        for (var choice : question.possibleChoices()) {
            if (choice.order() <= choicesSize && choice.order() > 0) {
                uniqueOrders.add(choice.order());
            }
        }
        if (uniqueOrders.size() < choicesSize) {
            errors.rejectValue(
                    "questions[" + i + "].possibleChoices",
                    "InvalidOrder",
                    "Invalid order of possible choices provided"
            );
            log.warn("Question validation failed. Question at index {} has invalid order of possible choices", i);
        }
    }

    private static void validateSelectMinMaxAgainstChoicesSize(@NonNull Errors errors, CreateQuestionRequest question, int choicesSize, int i) {
        if (question.selectMin() != null && question.selectMin() > choicesSize) {
            errors.rejectValue(
                    "questions[" + i + "].selectMin",
                    "InvalidSelectMin",
                    "selectMin cannot be greater than the number of possible choices"
            );
            log.warn("Question validation failed. Question at index {} has selectMin greater than possible choices size", i);
        }

        if (question.selectMax() != null && question.selectMax() > choicesSize) {
            errors.rejectValue(
                    "questions[" + i + "].selectMax",
                    "InvalidSelectMax",
                    "selectMax cannot be greater than the number of possible choices"
            );
            log.warn("Question validation failed. Question at index {} has selectMax greater than possible choices size", i);
        }
    }

    private static void validateSelectMinMax(@NonNull Errors errors, CreateQuestionRequest question, boolean hasChoices, int i) {
        if (Boolean.TRUE.equals(question.isFreeText()) && hasChoices) {
            errors.rejectValue(
                    "questions[" + i + "].isFreeText",
                    "InvalidFreeTextQuestion",
                    "Free text question cannot have possible choices"
            );
            log.warn("Question validation failed. Free text question at index {} has possible choices", i);
        }

        if (Boolean.FALSE.equals(question.isFreeText()) && !hasChoices) {
            errors.rejectValue(
                    "questions[" + i + "].possibleChoices",
                    "MissingPossibleChoices",
                    "Multiple choice question must have possible choices"
            );
            log.warn("Question validation failed. Multiple choice question at index {} has no possible choices", i);
        }

        if (question.selectMin() != null && question.selectMax() != null && question.selectMin() > question.selectMax()) {
            errors.rejectValue(
                    "questions[" + i + "].selectMin",
                    "InvalidSelectRange",
                    "selectMin cannot be greater than selectMax"
            );
            log.warn("Question validation failed. Question at index {} has selectMin greater than selectMax", i);
        }
    }
}
