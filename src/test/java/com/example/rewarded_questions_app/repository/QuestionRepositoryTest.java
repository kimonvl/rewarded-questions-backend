package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

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
    private UUID questionUuid;
    private UUID deletedQuestionUuid;

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
        questionUuid = question.getUuid();
        PossibleChoice possibleChoice = new PossibleChoice();
        possibleChoice.setText("Sample choice");
        possibleChoice.setOrder(0L);
        question.addPossibleChoice(possibleChoice);
        questionnaire.addQuestion(question);

        Question deletedQuestion = new Question();
        deletedQuestion.setText("Deleted question?");
        deletedQuestion.setIsFreeText(true);
        deletedQuestion.setOrder(1L);
        deletedQuestionUuid = deletedQuestion.getUuid();
        deletedQuestion.softDelete();
        questionnaire.addQuestion(deletedQuestion);

        entityManager.persist(adminRole);
        entityManager.persist(owner);
        entityManager.persist(questionnaire);
        entityManager.flush();
        entityManager.clear();
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

    @Test
    void findWithChoicesByUuidAndDeletedFalseReturnsQuestionWithChoices() {
        Question foundQuestion = questionRepository.findWithChoicesByUuidAndDeletedFalse(questionUuid).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        PossibleChoice foundChoice = foundQuestion.getAllPossibleChoices().iterator().next();

        assertThat(foundQuestion.getUuid()).isEqualTo(questionUuid);
        assertThat(foundQuestion.getText()).isEqualTo("Sample question?");
        assertThat(foundQuestion.isDeleted()).isFalse();
        assertThat(persistenceUnitUtil.isLoaded(foundQuestion, "possibleChoices")).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(foundQuestion, "questionnaire")).isFalse();
        assertThat(foundQuestion.getAllPossibleChoices()).hasSize(1);
        assertThat(foundChoice.getText()).isEqualTo("Sample choice");
        assertThat(foundChoice.isDeleted()).isFalse();
    }

    @Test
    void findWithQuestionnaireChoicesByUuidAndDeletedFalseReturnsQuestionWithChoicesAndQuestionnaire() {
        Question foundQuestion = questionRepository.findWithQuestionnaireChoicesByUuidAndDeletedFalse(questionUuid).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        PossibleChoice foundChoice = foundQuestion.getAllPossibleChoices().iterator().next();

        assertThat(foundQuestion.getUuid()).isEqualTo(questionUuid);
        assertThat(foundQuestion.getText()).isEqualTo("Sample question?");
        assertThat(foundQuestion.isDeleted()).isFalse();
        assertThat(persistenceUnitUtil.isLoaded(foundQuestion, "possibleChoices")).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(foundQuestion, "questionnaire")).isTrue();
        assertThat(foundQuestion.getAllPossibleChoices()).hasSize(1);
        assertThat(foundChoice.getText()).isEqualTo("Sample choice");
        assertThat(foundChoice.isDeleted()).isFalse();
        assertThat(foundQuestion.getQuestionnaire().getUuid()).isEqualTo(questionnaire.getUuid());
        assertThat(foundQuestion.getQuestionnaire().getTitle()).isEqualTo("Sample Questionnaire");
        assertThat(foundQuestion.getQuestionnaire().isDeleted()).isFalse();
    }

    @Test
    void findAllWithChoicesByQuestionnaireIdAndDeletedFalseOrderByOrderAscReturnsOnlyActiveQuestionsForQuestionnaireWithChoicesOrdered() {
        User owner = entityManager.getReference(User.class, questionnaire.getUser().getId());

        Questionnaire targetQuestionnaire = new Questionnaire();
        targetQuestionnaire.setTitle("Target Questionnaire");
        targetQuestionnaire.setDescription("Target Questionnaire description");
        targetQuestionnaire.setUser(owner);

        Question secondQuestion = question("Second target question?", 2L, "Second choice");
        Question firstQuestion = question("First target question?", 0L, "First choice");
        Question deletedQuestion = question("Deleted target question?", 1L, "Deleted choice");
        deletedQuestion.softDelete();
        targetQuestionnaire.addQuestion(secondQuestion);
        targetQuestionnaire.addQuestion(firstQuestion);
        targetQuestionnaire.addQuestion(deletedQuestion);

        Questionnaire otherQuestionnaire = new Questionnaire();
        otherQuestionnaire.setTitle("Other Questionnaire");
        otherQuestionnaire.setDescription("Other Questionnaire description");
        otherQuestionnaire.setUser(owner);
        otherQuestionnaire.addQuestion(question("Other questionnaire question?", 0L, "Other choice"));

        entityManager.persist(targetQuestionnaire);
        entityManager.persist(otherQuestionnaire);
        entityManager.flush();
        entityManager.clear();

        Page<Question> page = questionRepository.findAllWithChoicesByQuestionnaireIdAndDeletedFalseOrderByOrderAsc(
                targetQuestionnaire.getId(),
                PageRequest.of(0, 10)
        );
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        List<Question> questions = page.getContent();

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(questions)
                .extracting(Question::getText)
                .containsExactly("First target question?", "Second target question?");
        assertThat(questions)
                .extracting(question -> question.getQuestionnaire().getId())
                .containsOnly(targetQuestionnaire.getId());
        assertThat(questions)
                .extracting(Question::isDeleted)
                .containsOnly(false);
        assertThat(questions)
                .allSatisfy(question -> {
                    assertThat(persistenceUnitUtil.isLoaded(question, "possibleChoices")).isTrue();
                    assertThat(question.getAllPossibleChoices())
                            .hasSize(1)
                            .allSatisfy(possibleChoice -> assertThat(possibleChoice.isDeleted()).isFalse());
                });
    }

    private Question question(String text, Long order, String choiceText) {
        Question question = new Question();
        question.setText(text);
        question.setIsFreeText(false);
        question.setOrder(order);

        PossibleChoice possibleChoice = new PossibleChoice();
        possibleChoice.setText(choiceText);
        possibleChoice.setOrder(0L);
        question.addPossibleChoice(possibleChoice);

        return question;
    }
}
