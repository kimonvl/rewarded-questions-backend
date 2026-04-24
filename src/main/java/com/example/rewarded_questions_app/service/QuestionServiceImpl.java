package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.*;
import com.example.rewarded_questions_app.dto.response.QuestionDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.mapper.PossibleChoiceMapper;
import com.example.rewarded_questions_app.mapper.QuestionMapper;
import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.model.user.User;
import com.example.rewarded_questions_app.repository.QuestionRepository;
import com.example.rewarded_questions_app.repository.QuestionnaireRepository;
import com.example.rewarded_questions_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class QuestionServiceImpl implements QuestionService{

    private final PossibleChoiceService possibleChoiceService;

    private final UserRepository userRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;
    private final PossibleChoiceMapper possibleChoiceMapper;

    @Override
    @PreAuthorize("hasAuthority('CREATE_QUESTION')")
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityInvalidArgumentException.class})
    public QuestionDTO createQuestion(
            CreateQuestionRequest request, UUID questionnaireId, String email
    ) throws EntityNotFoundException, EntityInvalidArgumentException {
        try {
            validateCreateQuestionRequest(request);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("CreateQuestionUser", "User with email=" + email + " not found"));
            Questionnaire questionnaire = questionnaireRepository.findByUuidAndDeletedFalse(questionnaireId)
                    .orElseThrow(() -> new EntityNotFoundException("CreateQuestionQuestionnaire", "Questionnaire with id=" + questionnaireId + " not found"));
            if (!questionnaire.getUser().equals(user)) {
                throw new EntityInvalidArgumentException("CreateQuestionQuestionnaireUser", "Questionnaire with id=" + questionnaireId + " does not belong to user with email=" + email);
            }
            if (existsByTextAndQuestionnaireId(request.text(), questionnaire.getId())) {
                throw new EntityInvalidArgumentException("CreateQuestionTextUnique", "Question text must be unique within the questionnaire");
            }

            Question question = questionMapper.createQuestionRequestToQuestion(request);
            question.setOrder((long) questionnaire.getAllQuestions().size());
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

    @Override
    @PreAuthorize("hasAuthority('EDIT_QUESTION')")
    @Transactional(rollbackFor = {EntityInvalidArgumentException.class, EntityNotFoundException.class})
    public List<QuestionDTO> reorderQuestions(
            ReorderQuestionsRequest request, UUID questionnaireId, String email
    ) throws EntityInvalidArgumentException, EntityNotFoundException {
        try {
            if (request.questionUUIDs() == null || request.questionUUIDs().isEmpty()) {
                throw new EntityInvalidArgumentException("ReorderQuestionsEmptyList", "Question UUID list cannot be null or empty");
            }
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("ReorderQuestionsUser", "User with email=" + email + " not found"));
            Questionnaire questionnaire = questionnaireRepository.findWithQuestionsByUuidAndDeletedFalse(questionnaireId)
                    .orElseThrow(() -> new EntityNotFoundException("ReorderQuestionsQuestionnaire", "Questionnaire with id=" + questionnaireId + " not found"));
            if (!questionnaire.getUser().equals(user)) {
                throw new EntityInvalidArgumentException("ReorderQuestionsQuestionnaireUser", "Questionnaire with id=" + questionnaireId + " does not belong to user with email=" + email);
            }
            HashSet<UUID> questionIds = new HashSet<>(request.questionUUIDs());
            if (questionIds.size() != questionnaire.getAllQuestions().size()) {
                throw new EntityInvalidArgumentException("ReorderQuestionsSize", "Question UUID list size must match the number of questions in the questionnaire");
            }

            for (Question question : questionnaire.getAllQuestions()) {
                int order = request.questionUUIDs().indexOf(question.getUuid());
                if (order == -1) {
                    throw new EntityInvalidArgumentException("ReorderQuestionsUUID", "Question UUID " + question.getUuid() + " is not in the provided list");
                }
                question.setOrder((long) order);
            }
            List<Question> savedQuestions = questionRepository.saveAll(questionnaire.getAllQuestions());

            log.info("Questions reordered successfully by user with email={} for questionnaire with id={}", email, questionnaireId);
            return savedQuestions.stream()
                    .map(questionMapper::toDto)
                    .sorted(Comparator.comparing(QuestionDTO::order))
                    .toList();
        } catch (EntityInvalidArgumentException | EntityNotFoundException e) {
            log.warn("Reorder questions failed, by user with email={} for questionnaire with id={}. Message={}", email, questionnaireId, e.getMessage());
            throw e;
        }

    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_QUESTION')")
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityInvalidArgumentException.class})
    public QuestionDTO editQuestion(
            EditQuestionRequest request, UUID questionId, String email
    ) throws EntityNotFoundException, EntityInvalidArgumentException {
        // TODO: Define 2 edit paths depending on the type of question.
        // TODO: Validation of request will happen in each path.
        // TODO: Validation of possible choices in this service and then delegate the possible choice edit to the Possible Choice Service.

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("EditQuestionUser", "User with email=" + email + " not found"));
            Question question = questionRepository.findByUuidAndDeletedFalse(questionId)
                    .orElseThrow(() -> new EntityNotFoundException("EditQuestionQuestion", "Question with id=" + questionId + " not found"));
            if (!question.getQuestionnaire().getUser().equals(user)) {
                throw new EntityInvalidArgumentException("EditQuestionQuestionUser", "Question with id=" + questionId + " does not belong to user with email=" + email);
            }
            if (request.text() == null || request.text().isEmpty()) {
                throw new EntityInvalidArgumentException("EditQuestionTextEmpty", "Question text cannot be blank.");
            }
            if (request.text().length() < 5 || request.text().length() > 200) {
                throw new EntityInvalidArgumentException("EditQuestionTextLength", "Question text must be between 5 and 200 characters.");
            }

            if (question.getIsFreeText()) {
                handleEditFreeTextQuestion(request, question);
            } else {
                handleEditMultipleChoiceQuestion(request, question);
            }

            questionRepository.save(question);
            QuestionDTO result = questionMapper.toDto(question);

            log.info("Edit question succeeded by user with email={} for question with id={}", email, questionId);
            return result;
        } catch (EntityNotFoundException | EntityInvalidArgumentException e) {
            log.warn("Edit question failed, by user with email={} for question with id={}. Message={}", email, questionId, e.getMessage());
            throw e;
        }
    }

    private void handleEditFreeTextQuestion(EditQuestionRequest request, Question question) throws EntityInvalidArgumentException {
        if (request.selectMin() != null ||  request.selectMax() != null || request.possibleChoices() != null) {
            throw new EntityInvalidArgumentException("EditQuestionFTRequest", "For free text question request can contain only text field.");
        }
        question.setText(request.text());
    }

    private void handleEditMultipleChoiceQuestion(EditQuestionRequest request, Question question) throws EntityInvalidArgumentException {
        if (request.possibleChoices() == null || request.possibleChoices().size() < 2) {
            throw new EntityInvalidArgumentException("EditQuestionMCChoicesSize", "For multiple choice questions, at least 2 possible choices must be provided.");
        }
        if (request.selectMin() == null || request.selectMax() == null) {
            throw new EntityInvalidArgumentException("EditQuestionMCSelectMinMaxNull", "For multiple choice questions, selectMin and selectMax values must be provided.");
        }
        if (request.selectMin() < 1 || request.selectMin() > request.selectMax()) {
            throw new EntityInvalidArgumentException("EditQuestionMCSelectMin", "For multiple choice questions, selectMin must be greater than 0 and less than or equal to selectMax.");
        }
        if (request.selectMax() > request.possibleChoices().size()) {
            throw new EntityInvalidArgumentException("EditQuestionMCSelectMax", "For multiple choice questions, selectMax must be less than or equal to the number of possible choices.");
        }

        // Check for duplicate possible choice texts and UUIDs in the request
        Set<UUID> possibleChoiceIds = getPossibleChoiceUuidsFromRequest(request);

        // Check that all edited possible choices in request belong to the question
        Set<UUID> possibleChoiceIdsDB = question.getAllPossibleChoices()
                .stream()
                .map(PossibleChoice::getUuid)
                .collect(Collectors.toSet());
        for (UUID uuid: possibleChoiceIds) {
            if (!possibleChoiceIdsDB.contains(uuid)) {
                throw new EntityInvalidArgumentException("EditQuestionMCChoiceUUIDNotInQuestion", "Possible choice UUID: " + uuid + "does not belong to question with id=" + question.getId());
            }
        }

        question.setText(request.text());
        question.setSelectMin(request.selectMin());
        question.setSelectMax(request.selectMax());
        possibleChoiceService.editPossibleChoices(question, request.possibleChoices());
    }

    private static @NonNull Set<UUID> getPossibleChoiceUuidsFromRequest(EditQuestionRequest request) throws EntityInvalidArgumentException {
        Set<UUID> possibleChoiceIds = new HashSet<>();
        Set<String> possibleChoiceTexts = new HashSet<>();
        for (EditPossibleChoiceRequest choice : request.possibleChoices()) {
            if (choice.text() == null || choice.text().isBlank()) {
                throw new EntityInvalidArgumentException("EditQuestionMCChoiceTextBlank", "Possible choice text cannot be blank.");
            }
            if (choice.uuid() != null) {
                if (possibleChoiceIds.contains(choice.uuid())) {
                    throw new EntityInvalidArgumentException("EditQuestionMCChoiceUUIDDuplicate", "Duplicate possible choice UUID: " + choice.uuid());
                } else {
                    possibleChoiceIds.add(choice.uuid());
                }
            }
            if (possibleChoiceTexts.contains(choice.text())) {
                throw new EntityInvalidArgumentException("EditQuestionMCChoiceTextDuplicate", "Duplicate possible choice text: " + choice.text() + " in request.");
            } else {
                possibleChoiceTexts.add(choice.text());
            }
        }
        return possibleChoiceIds;
    }

    @Override
    public boolean existsByTextAndQuestionnaireId(String text, Long questionnaireId) {
        return questionRepository.existsByTextAndQuestionnaireIdAndDeletedFalse(text, questionnaireId);
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
