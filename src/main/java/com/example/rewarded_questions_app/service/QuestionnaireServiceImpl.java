package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.CreateQuestionnaireRequest;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDTO;
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
    public QuestionnaireDTO createQuestionnaire(CreateQuestionnaireRequest request, String email) throws EntityNotFoundException, EntityInvalidArgumentException {
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
}
