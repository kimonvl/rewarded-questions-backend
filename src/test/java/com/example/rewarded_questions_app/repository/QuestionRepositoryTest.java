package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.Question;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.model.user.Capability;
import com.example.rewarded_questions_app.model.user.Role;
import com.example.rewarded_questions_app.model.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class QuestionRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private EntityManager entityManager;

    private Questionnaire questionnaire;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        Capability createQuestionnaire = new Capability();
        createQuestionnaire.setName("CREATE_QUESTIONNAIRE");
        createQuestionnaire.setDescription("Create questionnaires");

        User owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPassword("password");
        owner.setOrganization("Example Org");
        adminRole.addUser(owner);

        questionnaire = new Questionnaire();
        questionnaire.setTitle("Sample Questionnaire");
        questionnaire.setDescription("Sample Questionnaire description");
        questionnaire.setUser(owner);

        Question question = new Question();
        question.setText("Sample question?");
        question.setIsFreeText(true);
        question.setOrder(0L);
        questionnaire.addQuestion(question);

        Question deletedQuestion = new Question();
        deletedQuestion.setText("Deleted question?");
        deletedQuestion.setIsFreeText(true);
        deletedQuestion.setOrder(1L);
        deletedQuestion.softDelete();
        questionnaire.addQuestion(deletedQuestion);

        entityManager.persist(adminRole);
        entityManager.persist(owner);
        entityManager.persist(questionnaire);
        entityManager.flush();
    }

    @Test
    void existsByTextAndQuestionnaireIdAndDeletedFalseSameTextDifferentQuestionnaireReturnsFalse() {
        boolean exists = questionRepository.existsByTextAndQuestionnaireIdAndDeletedFalse(
                "Sample question?",
                questionnaire.getId() + 1
        );

        assertThat(exists).isFalse();
    }

    @Test
    void existsByTextAndQuestionnaireIdAndDeletedFalseSameQuestionnaireDifferentTextReturnsFalse() {
        boolean exists = questionRepository.existsByTextAndQuestionnaireIdAndDeletedFalse(
                "Different question?",
                questionnaire.getId()
        );

        assertThat(exists).isFalse();
    }

    @Test
    void existsByTextAndQuestionnaireIdAndDeletedFalseSameQuestionnaireSameTextReturnsTrue() {
        boolean exists = questionRepository.existsByTextAndQuestionnaireIdAndDeletedFalse(
                "Sample question?",
                questionnaire.getId()
        );

        assertThat(exists).isTrue();
    }

    @Test
    void existsByTextAndQuestionnaireIdAndDeletedFalseDeletedQuestionReturnsFalse() {
        boolean exists = questionRepository.existsByTextAndQuestionnaireIdAndDeletedFalse(
                "Deleted question?",
                questionnaire.getId()
        );

        assertThat(exists).isFalse();
    }
}
