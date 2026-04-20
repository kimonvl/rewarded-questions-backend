package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.CreatePossibleChoiceRequest;
import com.example.rewarded_questions_app.dto.CreateQuestionRequest;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.model.user.Capability;
import com.example.rewarded_questions_app.model.user.Role;
import com.example.rewarded_questions_app.model.user.User;
import com.example.rewarded_questions_app.repository.QuestionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "app.security.secret-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.security.jwt-expiration=10800000"
})
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@WithMockUser(authorities = "CREATE_QUESTION")
class QuestionServiceImplTest {
    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private User otherOwner;
    private Questionnaire questionnaire;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setName("ADMIN");

        Capability createQuestion = new Capability();
        createQuestion.setName("CREATE_QUESTION");
        createQuestion.setDescription("Create questions");
        adminRole.addCapability(createQuestion);

        owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPassword("password");
        owner.setOrganization("Example Org");
        adminRole.addUser(owner);

        otherOwner = new User();
        otherOwner.setEmail("other@example.com");
        otherOwner.setPassword("password");
        otherOwner.setOrganization("Other Org");
        adminRole.addUser(otherOwner);

        questionnaire = new Questionnaire();
        questionnaire.setTitle("Sample Questionnaire");
        questionnaire.setDescription("Sample description");
        questionnaire.setUser(owner);

        Question existingQuestion = new Question();
        existingQuestion.setText("Existing question?");
        existingQuestion.setIsFreeText(true);
        existingQuestion.setSelectMin(0L);
        existingQuestion.setSelectMax(0L);
        existingQuestion.setOrder(0L);
        questionnaire.addQuestion(existingQuestion);

        entityManager.persist(createQuestion);
        entityManager.persist(adminRole);
        entityManager.persist(owner);
        entityManager.persist(otherOwner);
        entityManager.persist(questionnaire);
        entityManager.flush();
    }

    @Test
    void createQuestionUserNotFound() {
        CreateQuestionRequest request = freeTextRequest("New question?");
        assertThrows(EntityNotFoundException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), "missing@email.com"));
    }

    @Test
    void createQuestionQuestionnaireNotFound() {
        CreateQuestionRequest request = freeTextRequest("New question?");
        assertThrows(EntityNotFoundException.class, () -> questionService.createQuestion(request, UUID.randomUUID(), owner.getEmail()));
    }

    @Test
    void createQuestionQuestionnaireDoesNotBelongToUser() {
        CreateQuestionRequest request = freeTextRequest("New question?");
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), otherOwner.getEmail()));
    }

    @Test
    void createQuestionDuplicateQuestionText() {
        CreateQuestionRequest request = freeTextRequest("Existing question?");
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void createQuestionEmptyText() {
        CreateQuestionRequest request = freeTextRequest("");
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void createQuestionTextLessThan5Chars() {
        CreateQuestionRequest request = freeTextRequest("New?");
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void createQuestionFreeTextChoicesNotEmpty() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "New question?",
                true,
                0L,
                0L,
                List.of(new CreatePossibleChoiceRequest("Choice 1"))
        );
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void createQuestionFreeTextMinChoicesNotZero() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "New question?",
                true,
                1L,
                0L,
                List.of()
        );
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void createQuestionFreeTextFalseChoicesLessThanTwo() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "New question?",
                false,
                1L,
                1L,
                List.of(new CreatePossibleChoiceRequest("Choice 1"))
        );
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void createQuestionFreeTextFalseMinChoicesLessThanOne() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "New question?",
                false,
                0L,
                1L,
                List.of(
                        new CreatePossibleChoiceRequest("Red"),
                        new CreatePossibleChoiceRequest("Green"),
                        new CreatePossibleChoiceRequest("Blue")
                )
        );
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void createQuestionFreeTextFalseMaxChoicesLessThanMinChoices() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "New question?",
                false,
                1L,
                0L,
                List.of(
                        new CreatePossibleChoiceRequest("Red"),
                        new CreatePossibleChoiceRequest("Green"),
                        new CreatePossibleChoiceRequest("Blue")
                )
        );
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void createQuestionFreeTextFalseMaxChoicesGreaterThanTotalChoices() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "New question?",
                false,
                1L,
                4L,
                List.of(
                        new CreatePossibleChoiceRequest("Red"),
                        new CreatePossibleChoiceRequest("Green"),
                        new CreatePossibleChoiceRequest("Blue")
                )
        );
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void createQuestionFreeTextFalseChoiceEmptyText() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "New question?",
                false,
                1L,
                2L,
                List.of(
                        new CreatePossibleChoiceRequest("Green"),
                        new CreatePossibleChoiceRequest("Blue"),
                        new CreatePossibleChoiceRequest("")
                )
        );
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void createQuestionFreeTextFalseChoiceDuplicateText() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "New question?",
                false,
                1L,
                2L,
                List.of(
                        new CreatePossibleChoiceRequest("Green"),
                        new CreatePossibleChoiceRequest("Green"),
                        new CreatePossibleChoiceRequest("Blue")
                )
        );
        assertThrows(EntityInvalidArgumentException.class, () -> questionService.createQuestion(request, questionnaire.getUuid(), owner.getEmail()));
    }

    private static CreateQuestionRequest freeTextRequest(String text) {
        return new CreateQuestionRequest(text, true, 0L, 0L, List.of());
    }

    private static CreateQuestionRequest multipleChoiceRequest(String text) {
        return new CreateQuestionRequest(
                text,
                false,
                1L,
                2L,
                List.of(
                        new CreatePossibleChoiceRequest("Red"),
                        new CreatePossibleChoiceRequest("Green"),
                        new CreatePossibleChoiceRequest("Blue")
                )
        );
    }
}