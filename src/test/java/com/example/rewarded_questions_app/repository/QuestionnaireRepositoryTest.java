package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
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
    private UUID deletedQuestionnaireUuid;

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
        PossibleChoice possibleChoice = new PossibleChoice();
        possibleChoice.setText("Sample choice");
        possibleChoice.setOrder(0L);
        question.addPossibleChoice(possibleChoice);
        questionnaire.addQuestion(question);

        Questionnaire deletedQuestionnaire = new Questionnaire();
        deletedQuestionnaire.setTitle("Deleted Questionnaire");
        deletedQuestionnaire.setDescription("Deleted Questionnaire description");
        deletedQuestionnaire.setUser(owner);
        deletedQuestionnaireUuid = deletedQuestionnaire.getUuid();
        deletedQuestionnaire.softDelete();

        entityManager.persist(adminRole);
        entityManager.persist(owner);

        questionnaireRepository.save(questionnaire);
        questionnaireRepository.save(deletedQuestionnaire);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void existsByUserIdAndTitleAndDeletedFalseSameTitleDifferentUserReturnsFalse() {
        boolean exists = questionnaireRepository.existsByUserIdAndTitleAndDeletedFalse(owner.getId() + 1, "Sample Questionnaire");
        assertThat(exists).isFalse();
    }

    @Test
    void existsByUserIdAndTitleAndDeletedFalseSameUserDifferentTitleReturnsFalse() {
        boolean exists = questionnaireRepository.existsByUserIdAndTitleAndDeletedFalse(owner.getId(), "Sample Questionnaire 1");
        assertThat(exists).isFalse();
    }

    @Test
    void existsByUserIdAndTitleAndDeletedFalseSameUserSameTitleReturnsTrue() {
        boolean exists = questionnaireRepository.existsByUserIdAndTitleAndDeletedFalse(owner.getId(), "Sample Questionnaire");
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserIdAndTitleAndDeletedFalseDeletedQuestionnaireReturnsFalse() {
        boolean exists = questionnaireRepository.existsByUserIdAndTitleAndDeletedFalse(owner.getId(), "Deleted Questionnaire");
        assertThat(exists).isFalse();
    }

    @Test
    void existsByUserEmailAndTitleAndDeletedFalseActiveQuestionnaireReturnsTrue() {
        boolean exists = questionnaireRepository.existsByUserEmailAndTitleAndDeletedFalse(owner.getEmail(), "Sample Questionnaire");
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserEmailAndTitleAndDeletedFalseDeletedQuestionnaireReturnsFalse() {
        boolean exists = questionnaireRepository.existsByUserEmailAndTitleAndDeletedFalse(owner.getEmail(), "Deleted Questionnaire");
        assertThat(exists).isFalse();
    }

    @Test
    void findByUuidAndDeletedFalseReturnsQuestionnaire() {
        Questionnaire foundQuestionnaire = questionnaireRepository.findByUuidAndDeletedFalse(questionnaireUuid).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(foundQuestionnaire.getUuid()).isEqualTo(questionnaireUuid);
        assertThat(foundQuestionnaire.getTitle()).isEqualTo("Sample Questionnaire");
        assertThat(foundQuestionnaire.getDescription()).isEqualTo("Sample Questionnaire description");
        assertThat(foundQuestionnaire.getUser()).isEqualTo(owner);
        assertThat(persistenceUnitUtil.isLoaded(foundQuestionnaire, "questions")).isFalse();
    }

    @Test
    void findByUuidAndDeletedFalseDeletedQuestionnaireReturnsEmpty() {
        assertThat(questionnaireRepository.findByUuidAndDeletedFalse(deletedQuestionnaireUuid)).isEmpty();
    }

    @Test
    void findByUserIdAndTitleAndDeletedFalseReturnsQuestionnaire() {
        Questionnaire foundQuestionnaire = questionnaireRepository.findByUserIdAndTitleAndDeletedFalse(owner.getId(), "Sample Questionnaire").orElseThrow();

        assertThat(foundQuestionnaire.getUuid()).isEqualTo(questionnaireUuid);
        assertThat(foundQuestionnaire.getTitle()).isEqualTo("Sample Questionnaire");
        assertThat(foundQuestionnaire.isDeleted()).isFalse();
    }

    @Test
    void findByUserIdAndTitleAndDeletedFalseDeletedQuestionnaireReturnsEmpty() {
        assertThat(questionnaireRepository.findByUserIdAndTitleAndDeletedFalse(owner.getId(), "Deleted Questionnaire")).isEmpty();
    }

    @Test
    void findWithQuestionsByUuidAndDeletedFalseReturnsQuestionnaireWithQuestionsLoaded() {
        Questionnaire foundQuestionnaire = questionnaireRepository.findWithQuestionsByUuidAndDeletedFalse(questionnaireUuid).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        Question foundQuestion = foundQuestionnaire.getAllQuestions().iterator().next();

        assertThat(foundQuestionnaire.getUuid()).isEqualTo(questionnaireUuid);
        assertThat(foundQuestionnaire.isDeleted()).isFalse();
        assertThat(persistenceUnitUtil.isLoaded(foundQuestionnaire, "questions")).isTrue();
        assertThat(foundQuestionnaire.getAllQuestions()).hasSize(1);
        assertThat(persistenceUnitUtil.isLoaded(foundQuestion, "possibleChoices")).isTrue();
        assertThat(foundQuestion.getAllPossibleChoices()).hasSize(1);
    }

    @Test
    void findWithQuestionsByUuidAndDeletedFalseDeletedQuestionnaireReturnsEmpty() {
        assertThat(questionnaireRepository.findWithQuestionsByUuidAndDeletedFalse(deletedQuestionnaireUuid)).isEmpty();
    }

}
