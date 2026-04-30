package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.dto.request.QuestionnaireFilters;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
import com.example.rewarded_questions_app.model.user.Capability;
import com.example.rewarded_questions_app.model.user.Role;
import com.example.rewarded_questions_app.model.user.User;
import com.example.rewarded_questions_app.repository.specification.QuestionnaireSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
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
    private User anotherOwner;
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

        anotherOwner = new User();
        anotherOwner.setEmail("another-owner@example.com");
        anotherOwner.setPassword("password");
        anotherOwner.setOrganization("Another Business");
        adminRole.addUser(anotherOwner);

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

        Question deletedQuestion = new Question();
        deletedQuestion.setText("Deleted question?");
        deletedQuestion.setIsFreeText(true);
        deletedQuestion.setSelectMin(0L);
        deletedQuestion.setSelectMax(0L);
        deletedQuestion.setOrder(1L);
        deletedQuestion.softDelete();
        questionnaire.addQuestion(deletedQuestion);

        Questionnaire deletedQuestionnaire = new Questionnaire();
        deletedQuestionnaire.setTitle("Deleted Questionnaire");
        deletedQuestionnaire.setDescription("Deleted Questionnaire description");
        deletedQuestionnaire.setUser(owner);
        deletedQuestionnaireUuid = deletedQuestionnaire.getUuid();
        deletedQuestionnaire.softDelete();

        entityManager.persist(adminRole);
        entityManager.persist(owner);
        entityManager.persist(anotherOwner);

        questionnaireRepository.save(questionnaire);
        questionnaireRepository.save(createQuestionnaire("Customer Feedback", "Customer feedback description", owner));
        questionnaireRepository.save(createQuestionnaire("Employee Survey", "Employee survey description", anotherOwner));
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
        assertThat(persistenceUnitUtil.isLoaded(foundQuestionnaire, "user")).isTrue();
        assertThat(foundQuestionnaire.getUser()).isEqualTo(owner);
        assertThat(persistenceUnitUtil.isLoaded(foundQuestionnaire, "questions")).isFalse();
    }

    @Test
    void findByUuidAndDeletedFalseDeletedQuestionnaireReturnsEmpty() {
        assertThat(questionnaireRepository.findByUuidAndDeletedFalse(deletedQuestionnaireUuid)).isEmpty();
    }

    @Test
    void findAllFiltersByTitleAndLoadsUsers() {
        QuestionnaireFilters filters = new QuestionnaireFilters();
        filters.setTitle("customer");
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        Page<Questionnaire> page = questionnaireRepository.findAll(
                QuestionnaireSpecification.build(filters),
                PageRequest.of(0, 10, Sort.by("title"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
                .singleElement()
                .satisfies(questionnaire -> {
                    assertThat(questionnaire.getTitle()).isEqualTo("Customer Feedback");
                    assertThat(questionnaire.isDeleted()).isFalse();
                    assertThat(persistenceUnitUtil.isLoaded(questionnaire, "user")).isTrue();
                    assertThat(questionnaire.getUser().getOrganization()).isEqualTo("Example Org");
                });
    }

    @Test
    void findAllFiltersByBusinessNameAndLoadsUsers() {
        QuestionnaireFilters filters = new QuestionnaireFilters();
        filters.setBusinessName("another business");
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        Page<Questionnaire> page = questionnaireRepository.findAll(
                QuestionnaireSpecification.build(filters),
                PageRequest.of(0, 10, Sort.by("title"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
                .singleElement()
                .satisfies(questionnaire -> {
                    assertThat(questionnaire.getTitle()).isEqualTo("Employee Survey");
                    assertThat(questionnaire.isDeleted()).isFalse();
                    assertThat(persistenceUnitUtil.isLoaded(questionnaire, "user")).isTrue();
                    assertThat(questionnaire.getUser()).isEqualTo(anotherOwner);
                });
    }

    @Test
    void findAllPaginatesLoadsUsersAndExcludesDeletedQuestionnaires() {
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        Page<Questionnaire> firstPage = questionnaireRepository.findAll(
                QuestionnaireSpecification.build(null),
                PageRequest.of(0, 2, Sort.by("title"))
        );
        Page<Questionnaire> secondPage = questionnaireRepository.findAll(
                QuestionnaireSpecification.build(null),
                PageRequest.of(1, 2, Sort.by("title"))
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent())
                .extracting(Questionnaire::getTitle)
                .containsExactly("Customer Feedback", "Employee Survey");
        assertThat(secondPage.getContent())
                .extracting(Questionnaire::getTitle)
                .containsExactly("Sample Questionnaire");

        List<Questionnaire> allLoadedQuestionnaires = List.of(
                firstPage.getContent().get(0),
                firstPage.getContent().get(1),
                secondPage.getContent().get(0)
        );

        assertThat(allLoadedQuestionnaires)
                .allSatisfy(questionnaire -> {
                    assertThat(questionnaire.isDeleted()).isFalse();
                    assertThat(questionnaire.getTitle()).isNotEqualTo("Deleted Questionnaire");
                    assertThat(persistenceUnitUtil.isLoaded(questionnaire, "user")).isTrue();
                });
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
        assertThat(foundQuestion.getText()).isEqualTo("Sample question?");
        assertThat(foundQuestion.isDeleted()).isFalse();
        assertThat(persistenceUnitUtil.isLoaded(foundQuestion, "possibleChoices")).isTrue();
        assertThat(foundQuestion.getAllPossibleChoices()).hasSize(1);
    }

    @Test
    void findWithQuestionsByUuidAndDeletedFalseDeletedQuestionnaireReturnsEmpty() {
        assertThat(questionnaireRepository.findWithQuestionsByUuidAndDeletedFalse(deletedQuestionnaireUuid)).isEmpty();
    }

    private Questionnaire createQuestionnaire(String title, String description, User user) {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setTitle(title);
        questionnaire.setDescription(description);
        questionnaire.setUser(user);
        return questionnaire;
    }

}
