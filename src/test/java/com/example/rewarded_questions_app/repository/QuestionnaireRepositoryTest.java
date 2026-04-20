package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import com.example.rewarded_questions_app.model.user.Capability;
import com.example.rewarded_questions_app.model.user.Role;
import com.example.rewarded_questions_app.model.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class QuestionnaireRepositoryTest {

    @Autowired
    private QuestionnaireRepository questionnaireRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private UUID questionnaireUuid;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        Capability createQuestionnaire = new Capability();
        createQuestionnaire.setName("CREATE_QUESTIONNAIRE");
        createQuestionnaire.setDescription("Create questionnaires");

        owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPassword("password");
        owner.setOrganization("Example Org");
        adminRole.addUser(owner);

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setTitle("Sample Questionnaire");
        questionnaire.setDescription("Sample Questionnaire description");
        questionnaire.setUser(owner);
        questionnaireUuid = questionnaire.getUuid();

        Question question = new Question();
        question.setText("Sample question?");
        question.setIsFreeText(true);
        question.setSelectMin(0L);
        question.setSelectMax(0L);
        question.setOrder(0L);
        questionnaire.addQuestion(question);

        entityManager.persist(adminRole);
        entityManager.persist(owner);

        questionnaireRepository.save(questionnaire);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void existsByUserIdAndTitleSameTitleDifferentUserReturnsFalse() {
        boolean exists = questionnaireRepository.existsByUserIdAndTitle(owner.getId() + 1, "Sample Questionnaire");
        assertThat(exists).isFalse();
    }

    @Test
    void existsByUserIdAndTitleSameUserDifferentTitleReturnsFalse() {
        boolean exists = questionnaireRepository.existsByUserIdAndTitle(owner.getId(), "Sample Questionnaire 1");
        assertThat(exists).isFalse();
    }

    @Test
    void existsByUserIdAndTitleSameUserSameTitleReturnsTrue() {
        boolean exists = questionnaireRepository.existsByUserIdAndTitle(owner.getId(), "Sample Questionnaire");
        assertThat(exists).isTrue();
    }

    @Test
    void findByUuidReturnsQuestionnaire() {
        Questionnaire foundQuestionnaire = questionnaireRepository.findByUuid(questionnaireUuid).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(foundQuestionnaire.getUuid()).isEqualTo(questionnaireUuid);
        assertThat(foundQuestionnaire.getTitle()).isEqualTo("Sample Questionnaire");
        assertThat(foundQuestionnaire.getDescription()).isEqualTo("Sample Questionnaire description");
        assertThat(foundQuestionnaire.getUser()).isEqualTo(owner);
        assertThat(persistenceUnitUtil.isLoaded(foundQuestionnaire, "questions")).isFalse();
    }

    @Test
    void findWithQuestionsByUuidReturnsQuestionnaireWithQuestionsLoaded() {
        Questionnaire foundQuestionnaire = questionnaireRepository.findWithQuestionsByUuid(questionnaireUuid).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(foundQuestionnaire.getUuid()).isEqualTo(questionnaireUuid);
        assertThat(persistenceUnitUtil.isLoaded(foundQuestionnaire, "questions")).isTrue();
        assertThat(foundQuestionnaire.getAllQuestions()).hasSize(1);
    }

}
