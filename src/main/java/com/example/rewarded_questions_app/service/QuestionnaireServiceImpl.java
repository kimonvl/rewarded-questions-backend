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
            return questionnaireMapper.toQuestionnaireDTO(saved);
        } catch (EntityNotFoundException | EntityInvalidArgumentException e) {
            log.warn("Questionnaire creation failed. Message={}", e.getMessage());
            throw e;
        }
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_QUESTIONNAIRE')")
    @Transactional(rollbackFor = {})
    public QuestionnaireDetailsDTO editQuestionnaireDetails(EditQuestionnaireDetailsRequest request, UUID questionnaireId, String email) {
        return null;
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
