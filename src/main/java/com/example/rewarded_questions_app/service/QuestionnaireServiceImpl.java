package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.CreateQuestionnaireRequest;
import com.example.rewarded_questions_app.dto.request.EditQuestionnaireDetailsRequest;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDetailsDTO;
import com.example.rewarded_questions_app.dto.response.QuestionnaireWithQuestionsDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.mapper.QuestionnaireMapper;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.model.user.User;
import com.example.rewarded_questions_app.repository.QuestionnaireRepository;
import com.example.rewarded_questions_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class QuestionnaireServiceImpl implements QuestionnaireService{

    private final UserRepository userRepository;
    private final QuestionnaireRepository questionnaireRepository;

    private final QuestionnaireMapper questionnaireMapper;

    @Override
    @PreAuthorize("hasAuthority('CREATE_QUESTIONNAIRE')")
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityInvalidArgumentException.class})
    public QuestionnaireWithQuestionsDTO createQuestionnaire(CreateQuestionnaireRequest request, String email) throws EntityNotFoundException, EntityInvalidArgumentException {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("CreateQuestionnaireUser", "User with email=" + email + " not found"));

            if (questionnaireRepository.existsByUserIdAndTitle(user.getId(), request.title())) {
                throw new EntityInvalidArgumentException("CreateQuestionnaireTitle", "Questionnaire with title=" + request.title() + " already exists for user with email=" + email);
            }

            Questionnaire saved = questionnaireRepository.save(questionnaireMapper.createQuestionnaireReqToQuestionnaire(request, user));
            log.info("Questionnaire created successfully for email={}", email);
            return questionnaireMapper.toQuestionnaireWithQuestionsDTO(saved);
        } catch (EntityNotFoundException | EntityInvalidArgumentException e) {
            log.warn("Questionnaire creation failed. Message={}", e.getMessage());
            throw e;
        }
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_QUESTIONNAIRE')")
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityInvalidArgumentException.class})
    public QuestionnaireDetailsDTO editQuestionnaireDetails(EditQuestionnaireDetailsRequest request, UUID questionnaireId, String email) throws EntityNotFoundException, EntityInvalidArgumentException {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("EditQuestionnaireDetailsUser", "User with email=" + email + " not found"));
            Questionnaire questionnaire = questionnaireRepository.findByUuid(questionnaireId)
                    .orElseThrow(() -> new EntityNotFoundException("EditQuestionnaireDetailsQuestionnaire", "Questionnaire with id=" + questionnaireId + " not found"));
            if(!questionnaire.getUser().equals(user)) {
                throw new EntityInvalidArgumentException("EditQuestionnaireDetailsUserQuestionnaire", "User with email=" + email + " is not the owner of the questionnaire with id=" + questionnaireId);
            }
            if (request.title() == null && request.description() == null) {
                throw new EntityInvalidArgumentException("EditQuestionnaireDetailsBothFieldsNull", "At least one of the fields (title or description) must be provided for editing.");
            }
            if (request.title() != null && request.title().trim().isBlank()) {
                throw new EntityInvalidArgumentException("EditQuestionnaireDetailsBlankTitle", "Blank title provided for editing.");
            }
            if (request.description() != null && request.description().trim().isBlank()) {
                throw new EntityInvalidArgumentException("EditQuestionnaireDetailsBlankDescription", "Blank description provided for editing.");
            }
            if(request.title() != null) {
                if (request.title().trim().length() < 3 || request.title().trim().length() > 30) {
                    throw new EntityInvalidArgumentException("EditQuestionnaireDetailsTitleLength", "Title length must be between 3 and 30 characters.");
                }
                if (questionnaireRepository.existsByUserIdAndTitle(user.getId(), request.title().trim()) && !questionnaire.getTitle().equals(request.title())) {
                    throw new EntityInvalidArgumentException("EditQuestionnaireDetailsTitleNotUnique", "Questionnaire with title=" + request.title() + " already exists for user with email=" + email);
                }
                questionnaire.setTitle(request.title().trim());
            }
            if(request.description() != null) {
                if (request.description().trim().length() < 5 || request.description().trim().length() > 200) {
                    throw new EntityInvalidArgumentException("EditQuestionnaireDetailsDescriptionsLength", "Descriptions length must be between 3 and 30 characters.");
                }
                questionnaire.setDescription(request.description().trim());
            }

            QuestionnaireDetailsDTO questionnaireDetailsDTO = questionnaireMapper.toQuestionnaireDetailsDTO(questionnaireRepository.save(questionnaire));
            log.info("Questionnaire details with id={} edited successfully by user with email={}",questionnaireId, email);
            return questionnaireDetailsDTO;
        } catch (EntityNotFoundException | EntityInvalidArgumentException e) {
            log.warn("Questionnaire details edit failed. Message={}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<Questionnaire> findQuestionnaireByUuid(UUID uuid) {
        return questionnaireRepository.findByUuid(uuid);
    }

    @Override
    public boolean existsQuestionnaireByTitleAndUserEmail(String title, String email) {
        return questionnaireRepository.existsByUserEmailAndTitle(email, title);
    }
}
