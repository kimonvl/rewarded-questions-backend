package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.CreatePossibleChoiceRequest;
import com.example.rewarded_questions_app.dto.request.CreateQuestionRequest;
import com.example.rewarded_questions_app.dto.request.ReorderQuestionsRequest;
import com.example.rewarded_questions_app.dto.response.QuestionDTO;
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

import java.util.Comparator;
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
@WithMockUser(authorities = {"CREATE_QUESTION", "EDIT_QUESTION"})
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
    private UUID firstQuestionUuid;
    private UUID secondQuestionUuid;
    private UUID thirdQuestionUuid;
    private UUID otherQuestionUuid;

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

        Question firstQuestion = createFreeTextQuestion("Existing question?", 0L);
        Question secondQuestion = createFreeTextQuestion("Second question?", 1L);
        Question thirdQuestion = createFreeTextQuestion("Third question?", 2L);
        questionnaire.addQuestion(firstQuestion);
        questionnaire.addQuestion(secondQuestion);
        questionnaire.addQuestion(thirdQuestion);
        firstQuestionUuid = firstQuestion.getUuid();
        secondQuestionUuid = secondQuestion.getUuid();
        thirdQuestionUuid = thirdQuestion.getUuid();

        Questionnaire otherQuestionnaire = new Questionnaire();
        otherQuestionnaire.setTitle("Other Questionnaire");
        otherQuestionnaire.setDescription("Other description");
        otherQuestionnaire.setUser(owner);

        Question otherQuestion = createFreeTextQuestion("Other questionnaire question?", 0L);
        otherQuestionnaire.addQuestion(otherQuestion);
        otherQuestionUuid = otherQuestion.getUuid();

        entityManager.persist(createQuestion);
        entityManager.persist(adminRole);
        entityManager.persist(owner);
        entityManager.persist(otherOwner);
        entityManager.persist(questionnaire);
        entityManager.persist(otherQuestionnaire);
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

    @Test
    void reorderQuestionsQuestionnaireIdInvalid() {
        ReorderQuestionsRequest request = reorderRequest(firstQuestionUuid, secondQuestionUuid, thirdQuestionUuid);

        assertThrows(EntityNotFoundException.class,
                () -> questionService.reorderQuestions(request, UUID.randomUUID(), owner.getEmail()));
    }

    @Test
    void reorderQuestionsUserDoesntOwnQuestionnaire() {
        ReorderQuestionsRequest request = reorderRequest(firstQuestionUuid, secondQuestionUuid, thirdQuestionUuid);

        assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.reorderQuestions(request, questionnaire.getUuid(), otherOwner.getEmail()));
    }

    @Test
    void reorderQuestionsQuestionIdsListEmpty() {
        ReorderQuestionsRequest request = new ReorderQuestionsRequest(List.of());

        assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.reorderQuestions(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void reorderQuestionsDuplicateQuestionIds() {
        ReorderQuestionsRequest request = reorderRequest(firstQuestionUuid, firstQuestionUuid, secondQuestionUuid);

        assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.reorderQuestions(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void reorderQuestionsQuestionIdFromAnotherQuestionnaire() {
        ReorderQuestionsRequest request = reorderRequest(firstQuestionUuid, secondQuestionUuid, otherQuestionUuid);

        assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.reorderQuestions(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void reorderQuestionsMissingQuestionIds() {
        ReorderQuestionsRequest request = reorderRequest(firstQuestionUuid, secondQuestionUuid);

        assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.reorderQuestions(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void reorderQuestionsSuccess() throws EntityInvalidArgumentException, EntityNotFoundException {
        ReorderQuestionsRequest request = reorderRequest(thirdQuestionUuid, firstQuestionUuid, secondQuestionUuid);

        List<QuestionDTO> reorderedQuestions = questionService.reorderQuestions(request, questionnaire.getUuid(), owner.getEmail());

        assertEquals(List.of(thirdQuestionUuid, firstQuestionUuid, secondQuestionUuid),
                reorderedQuestions.stream().map(QuestionDTO::uuid).toList());

        entityManager.flush();
        entityManager.clear();

        List<Question> savedQuestions = questionRepository.findAll().stream()
                .filter(question -> question.getQuestionnaire().getUuid().equals(questionnaire.getUuid()))
                .sorted(Comparator.comparing(Question::getOrder))
                .toList();

        assertEquals(List.of(thirdQuestionUuid, firstQuestionUuid, secondQuestionUuid),
                savedQuestions.stream().map(Question::getUuid).toList());
        assertEquals(List.of(thirdQuestionUuid, firstQuestionUuid, secondQuestionUuid),
                reorderedQuestions.stream().map(QuestionDTO::uuid).toList());
        assertEquals(List.of(0L, 1L, 2L), savedQuestions.stream().map(Question::getOrder).toList());
    }

    private static CreateQuestionRequest freeTextRequest(String text) {
        return new CreateQuestionRequest(text, true, 0L, 0L, List.of());
    }

    private static ReorderQuestionsRequest reorderRequest(UUID... questionUuids) {
        return new ReorderQuestionsRequest(List.of(questionUuids));
    }

    private static Question createFreeTextQuestion(String text, Long order) {
        Question question = new Question();
        question.setText(text);
        question.setIsFreeText(true);
        question.setSelectMin(0L);
        question.setSelectMax(0L);
        question.setOrder(order);
        return question;
    }
    
}
