package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.CreatePossibleChoiceRequest;
import com.example.rewarded_questions_app.dto.CreateQuestionRequest;
import com.example.rewarded_questions_app.dto.CreateQuestionnaireWithQuestionsRequest;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDTO;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.mapper.PossibleChoiceMapper;
import com.example.rewarded_questions_app.mapper.QuestionMapper;
import com.example.rewarded_questions_app.mapper.QuestionnaireMapper;
import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
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

@Slf4j
@RequiredArgsConstructor
@Service
public class QuestionnaireServiceImpl implements QuestionnaireService{

    private final UserRepository userRepository;
    private final QuestionnaireRepository questionnaireRepository;

    private final QuestionnaireMapper questionnaireMapper;
    private final QuestionMapper questionMapper;
    private final PossibleChoiceMapper possibleChoiceMapper;

    @Override
    @PreAuthorize("hasAuthority('CREATE_QUESTIONNAIRE')")
    @Transactional(rollbackFor = {EntityNotFoundException.class})
    public QuestionnaireDTO createQuestionnaire(CreateQuestionnaireWithQuestionsRequest request, String username) throws EntityNotFoundException {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("CreateQuestionnaireUser", "User with username=" + username + " not found"));

            // TODO: do validations
            Questionnaire questionnaire = new Questionnaire();
            questionnaire.setUser(user);
            questionnaire.setTitle(request.title());
            questionnaire.setDescription(request.description());

            for (CreateQuestionRequest questionRequest : request.questions()) {
                Question question = questionMapper.createQuestionRequestToQuestion(questionRequest);
                questionnaire.addQuestion(question);

                if (!questionRequest.possibleChoices().isEmpty()) {
                    for (CreatePossibleChoiceRequest possibleChoiceRequest : questionRequest.possibleChoices()) {
                        PossibleChoice possibleChoice = possibleChoiceMapper.createPossibleChoiceReqToPossibleChoice(possibleChoiceRequest);
                        question.addPossibleChoice(possibleChoice);
                    }
                }
            }

            Questionnaire saved = questionnaireRepository.save(questionnaire);
            log.info("Questionnaire created successfully for username={}", username);
            return questionnaireMapper.toQuestionnaireDTO(saved);
        } catch (EntityNotFoundException e) {
            log.warn("Questionnaire creation failed. Message={}", e.getMessage());
            throw e;
        }
    }
}
