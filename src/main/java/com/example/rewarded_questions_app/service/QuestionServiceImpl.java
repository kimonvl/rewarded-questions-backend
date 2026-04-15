package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.CreatePossibleChoiceRequest;
import com.example.rewarded_questions_app.dto.CreateQuestionRequest;
import com.example.rewarded_questions_app.dto.response.QuestionDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.mapper.PossibleChoiceMapper;
import com.example.rewarded_questions_app.mapper.QuestionMapper;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.model.user.User;
import com.example.rewarded_questions_app.repository.QuestionRepository;
import com.example.rewarded_questions_app.repository.QuestionnaireRepository;
import com.example.rewarded_questions_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class QuestionServiceImpl implements QuestionService{

    private final UserRepository userRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;
    private final PossibleChoiceMapper possibleChoiceMapper;

    @Override
    @PreAuthorize("hasAuthority('CREATE_QUESTION')")
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityInvalidArgumentException.class})
    public QuestionDTO createQuestion(CreateQuestionRequest request, UUID questionnaireId, String email) throws EntityNotFoundException, EntityInvalidArgumentException {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("CreateQuestionUser", "User with email=" + email + " not found"));

            Questionnaire questionnaire = questionnaireRepository.findByUuid(questionnaireId)
                    .orElseThrow(() -> new EntityNotFoundException("CreateQuestionQuestionnaire", "Questionnaire with id=" + questionnaireId + " not found"));

            if (!questionnaire.getUser().equals(user)) {
                throw new EntityInvalidArgumentException("CreateQuestionQuestionnaire", "Questionnaire with id=" + questionnaireId + " does not belong to user with email=" + email);
            }

            if (questionRepository.existsByTextAndQuestionnaireId(request.text(), questionnaire.getId())) {
                throw new EntityInvalidArgumentException("CreateQuestionTextUnique", "Question text must be unique within the questionnaire");
            }

            validateCreateQuestionRequest(request);

            Question question = questionMapper.createQuestionRequestToQuestion(request);
            questionnaire.addQuestion(question);
            for (int i = 0; i < request.possibleChoices().size(); i++) {
                question.addPossibleChoice(possibleChoiceMapper.createPossibleChoiceReqToPossibleChoice(request.possibleChoices().get(i), (long) i));
            }
            Question saved = questionRepository.save(question);
            log.info("Question created successfully by user with email={} for questionnaire with id={}", email, questionnaireId);
            return questionMapper.toDto(saved);
        } catch (EntityNotFoundException | EntityInvalidArgumentException e) {
            log.warn("Question creation failed, by user with email={} for questionnaire with id={}. Message={}", email, questionnaireId, e.getMessage());
            throw e;
        }

    }

    private static void validateCreateQuestionRequest(CreateQuestionRequest request) throws EntityInvalidArgumentException {
        if (request == null) {
            throw new EntityInvalidArgumentException("CreateQuestionRequestNull", "Create question request cannot be null");
        }

        if (request.text() == null || request.text().isBlank() || request.text().length() < 5) {
            throw new EntityInvalidArgumentException("CreateQuestionTextBlank", "Question text cannot be blank and must have at least 5 characters");
        }

        if (request.isFreeText() == null) {
            throw new EntityInvalidArgumentException("CreateQuestionIsFreeTextNull", "Question type is required");
        }

        if (request.selectMin() == null || request.selectMax() == null) {
            throw new EntityInvalidArgumentException("CreateQuestionSelectMinMaxNull", "selectMin and selectMax are required");
        }

        if (request.possibleChoices() == null) {
            throw new EntityInvalidArgumentException("CreateQuestionPossibleChoicesNull", "Possible choices are required");
        }

        if (request.isFreeText()) {
            if (!request.possibleChoices().isEmpty()) {
                throw new EntityInvalidArgumentException("CreateQuestionPossibleChoices", "Free text question cannot have possible choices");
            } else if (request.selectMin() != 0 || request.selectMax() != 0) {
                throw new EntityInvalidArgumentException("CreateQuestionSelectMinMax0", "Free text question must have selectMin and selectMax values equal to 0");
            }
        } else {
            if (request.possibleChoices().size() < 2) {
                throw new EntityInvalidArgumentException("CreateQuestionPossibleChoicesSize", "Non free text question must have at least 2 possible choices");
            } else if (request.selectMin() < 1) {
                throw new EntityInvalidArgumentException("CreateQuestionSelectMin", "Non free text question must have selectMin value greater than 0");
            } else if (request.selectMin() > request.selectMax()) {
                throw new EntityInvalidArgumentException("CreateQuestionSelectMax", "Non free text question must have selectMin less or equal to selectMax");
            } else if (request.selectMax() > request.possibleChoices().size()) {
                throw new EntityInvalidArgumentException("CreateQuestionSelectMaxSize", "Non free text question must have selectMax less than or equal to the number of possible choices");
            }

            // Check for duplicate possible choice texts within the same question
            HashSet<String>  choices = new HashSet<>();
            for (CreatePossibleChoiceRequest choice : request.possibleChoices()) {
                if (choice.text().isBlank()) {
                    throw new EntityInvalidArgumentException("CreateQuestionPossibleChoiceTextBlank", "Possible choice text cannot be blank");
                }
                if (choices.contains(choice.text())) {
                    throw new EntityInvalidArgumentException("CreateQuestionPossibleChoicesUnique", "Duplicate possible choice text: " + choice.text());
                } else {
                    choices.add(choice.text());
                }
            }
        }
    }
}
