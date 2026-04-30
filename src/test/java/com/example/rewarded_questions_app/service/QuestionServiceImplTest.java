package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.CreatePossibleChoiceRequest;
import com.example.rewarded_questions_app.dto.request.CreateQuestionRequest;
import com.example.rewarded_questions_app.dto.request.EditPossibleChoiceRequest;
import com.example.rewarded_questions_app.dto.request.EditQuestionRequest;
import com.example.rewarded_questions_app.dto.request.ReorderQuestionsRequest;
import com.example.rewarded_questions_app.dto.response.QuestionDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "app.security.secret-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.security.jwt-expiration=10800000"
})
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@WithMockUser(authorities = {"CREATE_QUESTION", "EDIT_QUESTION", "DELETE_QUESTION"})
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
    private UUID multipleChoiceQuestionUuid;
    private UUID firstChoiceUuid;
    private UUID secondChoiceUuid;
    private UUID thirdChoiceUuid;
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
        Question multipleChoiceQuestion = createMultipleChoiceQuestion("Favorite color?", 3L,
                "Red", "Green", "Blue");
        questionnaire.addQuestion(firstQuestion);
        questionnaire.addQuestion(secondQuestion);
        questionnaire.addQuestion(thirdQuestion);
        questionnaire.addQuestion(multipleChoiceQuestion);
        firstQuestionUuid = firstQuestion.getUuid();
        secondQuestionUuid = secondQuestion.getUuid();
        thirdQuestionUuid = thirdQuestion.getUuid();
        multipleChoiceQuestionUuid = multipleChoiceQuestion.getUuid();
        List<PossibleChoice> multipleChoiceQuestionChoices = multipleChoiceQuestion.getAllPossibleChoices().stream()
                .sorted(Comparator.comparing(PossibleChoice::getOrder))
                .toList();
        firstChoiceUuid = multipleChoiceQuestionChoices.get(0).getUuid();
        secondChoiceUuid = multipleChoiceQuestionChoices.get(1).getUuid();
        thirdChoiceUuid = multipleChoiceQuestionChoices.get(2).getUuid();

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
        ReorderQuestionsRequest request = reorderRequest(thirdQuestionUuid, multipleChoiceQuestionUuid, firstQuestionUuid, secondQuestionUuid);

        List<QuestionDTO> reorderedQuestions = questionService.reorderQuestions(request, questionnaire.getUuid(), owner.getEmail());

        assertEquals(List.of(thirdQuestionUuid, multipleChoiceQuestionUuid, firstQuestionUuid, secondQuestionUuid),
                reorderedQuestions.stream().map(QuestionDTO::uuid).toList());

        entityManager.flush();
        entityManager.clear();

        List<Question> savedQuestions = questionRepository.findAll().stream()
                .filter(question -> question.getQuestionnaire().getUuid().equals(questionnaire.getUuid()))
                .sorted(Comparator.comparing(Question::getOrder))
                .toList();

        assertEquals(List.of(thirdQuestionUuid, multipleChoiceQuestionUuid, firstQuestionUuid, secondQuestionUuid),
                savedQuestions.stream().map(Question::getUuid).toList());
        assertEquals(List.of(thirdQuestionUuid, multipleChoiceQuestionUuid, firstQuestionUuid, secondQuestionUuid),
                reorderedQuestions.stream().map(QuestionDTO::uuid).toList());
        assertEquals(List.of(0L, 1L, 2L, 3L), savedQuestions.stream().map(Question::getOrder).toList());
    }

    @Test
    void editQuestionUserInvalidEmail() {
        EditQuestionRequest request = freeTextEditRequest("Edited question text");
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> questionService.editQuestion(request, firstQuestionUuid, "missing@email.com"));
        assertEquals("EditQuestionUserNotFound", exception.getCode());
    }

    @Test
    void editQuestionQuestionInvalidId() {
        EditQuestionRequest request = freeTextEditRequest("Edited question text");
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> questionService.editQuestion(request, UUID.randomUUID(), owner.getEmail()));
        assertEquals("EditQuestionQuestionNotFound", exception.getCode());
    }

    @Test
    void editQuestionUserNotOwner() {
        EditQuestionRequest request = freeTextEditRequest("Edited question text");
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, firstQuestionUuid, otherOwner.getEmail()));
        assertEquals("EditQuestionQuestionUserInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionTextEmpty() {
        EditQuestionRequest request = freeTextEditRequest("");
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, firstQuestionUuid, owner.getEmail()));
        assertEquals("EditQuestionTextEmptyInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionTextInvalidLength() {
        EditQuestionRequest request = freeTextEditRequest("abcd");
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, firstQuestionUuid, owner.getEmail()));
        assertEquals("EditQuestionTextLengthInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionFreeTextSelectMinProvided() {
        EditQuestionRequest request = new EditQuestionRequest("Edited question text", 1L, null, null);
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, firstQuestionUuid, owner.getEmail()));
        assertEquals("EditQuestionFTRequestInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionFreeTextSuccess() throws EntityInvalidArgumentException, EntityNotFoundException {
        EditQuestionRequest request = freeTextEditRequest("Edited free text question");

        QuestionDTO result = questionService.editQuestion(request, firstQuestionUuid, owner.getEmail());

        assertEquals(firstQuestionUuid, result.uuid());
        assertEquals("Edited free text question", result.text());
        assertTrue(result.isFreeText());
        assertEquals(0L, result.selectMin());
        assertEquals(0L, result.selectMax());
        assertTrue(result.possibleChoices().isEmpty());

        entityManager.flush();
        entityManager.clear();

        Question savedQuestion = questionRepository.findWithChoicesByUuidAndDeletedFalse(firstQuestionUuid).orElseThrow();
        assertEquals("Edited free text question", savedQuestion.getText());
        assertTrue(savedQuestion.getIsFreeText());
        assertEquals(0L, savedQuestion.getSelectMin());
        assertEquals(0L, savedQuestion.getSelectMax());
        assertTrue(savedQuestion.getAllPossibleChoices().isEmpty());
    }

    @Test
    void editQuestionMultipleChoicePossibleChoicesInvalidSize() {
        EditQuestionRequest request = new EditQuestionRequest(
                "Edited multiple choice question",
                1L,
                1L,
                List.of(new EditPossibleChoiceRequest(firstChoiceUuid, "Red edited"))
        );
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, multipleChoiceQuestionUuid, owner.getEmail()));
        assertEquals("EditQuestionMCChoicesSizeInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionMultipleChoiceSelectMinNull() {
        EditQuestionRequest request = new EditQuestionRequest(
                "Edited multiple choice question",
                null,
                2L,
                List.of(
                        new EditPossibleChoiceRequest(firstChoiceUuid, "Red edited"),
                        new EditPossibleChoiceRequest(secondChoiceUuid, "Green edited")
                )
        );
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, multipleChoiceQuestionUuid, owner.getEmail()));
        assertEquals("EditQuestionMCSelectMinMaxNullInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionMultipleChoiceSelectMinGreaterThanSelectMax() {
        EditQuestionRequest request = new EditQuestionRequest(
                "Edited multiple choice question",
                3L,
                2L,
                List.of(
                        new EditPossibleChoiceRequest(firstChoiceUuid, "Red edited"),
                        new EditPossibleChoiceRequest(secondChoiceUuid, "Green edited"),
                        new EditPossibleChoiceRequest(thirdChoiceUuid, "Blue edited")
                )
        );
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, multipleChoiceQuestionUuid, owner.getEmail()));
        assertEquals("EditQuestionMCSelectMinInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionMultipleChoiceSelectMaxGreaterThanChoicesSize() {
        EditQuestionRequest request = new EditQuestionRequest(
                "Edited multiple choice question",
                1L,
                4L,
                List.of(
                        new EditPossibleChoiceRequest(firstChoiceUuid, "Red edited"),
                        new EditPossibleChoiceRequest(secondChoiceUuid, "Green edited"),
                        new EditPossibleChoiceRequest(thirdChoiceUuid, "Blue edited")
                )
        );
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, multipleChoiceQuestionUuid, owner.getEmail()));
        assertEquals("EditQuestionMCSelectMaxInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionMultipleChoicePossibleChoiceTextBlank() {
        EditQuestionRequest request = new EditQuestionRequest(
                "Edited multiple choice question",
                1L,
                2L,
                List.of(
                        new EditPossibleChoiceRequest(firstChoiceUuid, "Red edited"),
                        new EditPossibleChoiceRequest(secondChoiceUuid, ""),
                        new EditPossibleChoiceRequest(thirdChoiceUuid, "Blue edited")
                )
        );
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, multipleChoiceQuestionUuid, owner.getEmail()));
        assertEquals("EditQuestionMCChoiceTextBlankInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionMultipleChoicePossibleChoiceDuplicateUuid() {
        EditQuestionRequest request = new EditQuestionRequest(
                "Edited multiple choice question",
                1L,
                2L,
                List.of(
                        new EditPossibleChoiceRequest(firstChoiceUuid, "Red edited"),
                        new EditPossibleChoiceRequest(firstChoiceUuid, "Green edited"),
                        new EditPossibleChoiceRequest(thirdChoiceUuid, "Blue edited")
                )
        );
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, multipleChoiceQuestionUuid, owner.getEmail()));
        assertEquals("EditQuestionMCChoiceUUIDDuplicateInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionMultipleChoicePossibleChoiceDuplicateText() {
        EditQuestionRequest request = new EditQuestionRequest(
                "Edited multiple choice question",
                1L,
                2L,
                List.of(
                        new EditPossibleChoiceRequest(firstChoiceUuid, "Same text"),
                        new EditPossibleChoiceRequest(secondChoiceUuid, "Same text"),
                        new EditPossibleChoiceRequest(thirdChoiceUuid, "Different text")
                )
        );
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.editQuestion(request, multipleChoiceQuestionUuid, owner.getEmail()));
        assertEquals("EditQuestionMCChoiceTextDuplicateInvalidArgument", exception.getCode());
    }

    @Test
    void editQuestionMultipleChoiceSuccess() throws EntityInvalidArgumentException, EntityNotFoundException {
        EditQuestionRequest request = new EditQuestionRequest(
                "Edited multiple choice question",
                1L,
                2L,
                List.of(
                        new EditPossibleChoiceRequest(secondChoiceUuid, "Green updated"),
                        new EditPossibleChoiceRequest(firstChoiceUuid, "Red updated"),
                        new EditPossibleChoiceRequest(null, "Yellow")
                )
        );

        QuestionDTO result = questionService.editQuestion(request, multipleChoiceQuestionUuid, owner.getEmail());

        assertEquals(multipleChoiceQuestionUuid, result.uuid());
        assertEquals("Edited multiple choice question", result.text());
        assertFalse(result.isFreeText());
        assertEquals(1L, result.selectMin());
        assertEquals(2L, result.selectMax());
        assertEquals(3, result.possibleChoices().size());
        assertEquals(List.of("Green updated", "Red updated", "Yellow"),
                result.possibleChoices().stream().map(choice -> choice.text()).toList());

        entityManager.flush();
        entityManager.clear();

        Question savedQuestion = questionRepository.findWithChoicesByUuidAndDeletedFalse(multipleChoiceQuestionUuid).orElseThrow();
        List<PossibleChoice> savedChoices = savedQuestion.getAllPossibleChoices().stream()
                .sorted(Comparator.comparing(PossibleChoice::getOrder))
                .toList();

        assertEquals("Edited multiple choice question", savedQuestion.getText());
        assertFalse(savedQuestion.getIsFreeText());
        assertEquals(1L, savedQuestion.getSelectMin());
        assertEquals(2L, savedQuestion.getSelectMax());
        assertEquals(3, savedChoices.size());
        assertEquals(List.of(secondChoiceUuid, firstChoiceUuid),
                savedChoices.stream().limit(2).map(PossibleChoice::getUuid).toList());
        assertEquals(List.of("Green updated", "Red updated", "Yellow"),
                savedChoices.stream().map(PossibleChoice::getText).toList());
        assertEquals(List.of(0L, 1L, 2L),
                savedChoices.stream().map(PossibleChoice::getOrder).toList());
        assertFalse(savedChoices.stream().map(PossibleChoice::getUuid).toList().contains(thirdChoiceUuid));
    }

    @Test
    void deleteQuestionUserNotFound() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> questionService.deleteQuestion(firstQuestionUuid, "missing@email.com"));
        assertEquals("DeleteQuestionUserNotFound", exception.getCode());
    }

    @Test
    void deleteQuestionQuestionNotFound() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> questionService.deleteQuestion(UUID.randomUUID(), owner.getEmail()));
        assertEquals("DeleteQuestionQuestionNotFound", exception.getCode());
    }

    @Test
    void deleteQuestionUserNotOwner() {
        EntityInvalidArgumentException exception = assertThrows(EntityInvalidArgumentException.class,
                () -> questionService.deleteQuestion(firstQuestionUuid, otherOwner.getEmail()));
        assertEquals("DeleteQuestionUserQuestionInvalidArgument", exception.getCode());
    }

    @Test
    void deleteQuestionSuccess() throws EntityInvalidArgumentException, EntityNotFoundException {
        questionService.deleteQuestion(multipleChoiceQuestionUuid, owner.getEmail());

        entityManager.flush();
        entityManager.clear();

        Question deletedQuestion = entityManager.createQuery(
                        "select q from Question q where q.uuid = :uuid",
                        Question.class
                )
                .setParameter("uuid", multipleChoiceQuestionUuid)
                .getSingleResult();
        List<PossibleChoice> deletedChoices = entityManager.createQuery(
                        "select pc from PossibleChoice pc where pc.question.uuid = :questionUuid",
                        PossibleChoice.class
                )
                .setParameter("questionUuid", multipleChoiceQuestionUuid)
                .getResultList();

        assertTrue(deletedQuestion.isDeleted());
        assertNotNull(deletedQuestion.getDeletedAt());
        assertThat(questionRepository.findWithChoicesByUuidAndDeletedFalse(multipleChoiceQuestionUuid)).isEmpty();

        assertEquals(3, deletedChoices.size());
        assertTrue(deletedChoices.stream().allMatch(PossibleChoice::isDeleted));
        assertTrue(deletedChoices.stream().allMatch(choice -> choice.getDeletedAt() != null));
    }

    @Test
    void getPaginatedQuestionsForQuestionnaireQuestionnaireNotFound() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> questionService.getPaginatedQuestionsForQuestionnaire(PageRequest.of(0, 10), UUID.randomUUID()));

        assertEquals("GetPaginatedQuestionsQuestionnaireNotFound", exception.getCode());
    }

    @Test
    void getPaginatedQuestionsForQuestionnaireSuccess() throws EntityNotFoundException {
        Page<QuestionDTO> result = questionService.getPaginatedQuestionsForQuestionnaire(
                PageRequest.of(0, 2),
                questionnaire.getUuid()
        );

        assertEquals(4, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(2, result.getContent().size());
        assertEquals(List.of(firstQuestionUuid, secondQuestionUuid),
                result.getContent().stream().map(QuestionDTO::uuid).toList());
        assertEquals(List.of(0L, 1L),
                result.getContent().stream().map(QuestionDTO::order).toList());
        assertTrue(result.getContent().stream().allMatch(question -> question.possibleChoices().isEmpty()));

        Page<QuestionDTO> secondPage = questionService.getPaginatedQuestionsForQuestionnaire(
                PageRequest.of(1, 2),
                questionnaire.getUuid()
        );
        QuestionDTO multipleChoiceQuestion = secondPage.getContent().get(1);

        assertEquals(List.of(thirdQuestionUuid, multipleChoiceQuestionUuid),
                secondPage.getContent().stream().map(QuestionDTO::uuid).toList());
        assertEquals(multipleChoiceQuestionUuid, multipleChoiceQuestion.uuid());
        assertEquals("Favorite color?", multipleChoiceQuestion.text());
        assertFalse(multipleChoiceQuestion.isFreeText());
        assertThat(multipleChoiceQuestion.possibleChoices())
                .hasSize(3)
                .extracting(choice -> choice.text())
                .containsExactlyInAnyOrder("Red", "Green", "Blue");
    }

    private static CreateQuestionRequest freeTextRequest(String text) {
        return new CreateQuestionRequest(text, true, 0L, 0L, List.of());
    }

    private static EditQuestionRequest freeTextEditRequest(String text) {
        return new EditQuestionRequest(text, null, null, null);
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

    private static Question createMultipleChoiceQuestion(String text, Long order, String... choices) {
        Question question = new Question();
        question.setText(text);
        question.setIsFreeText(false);
        question.setSelectMin(1L);
        question.setSelectMax(2L);
        question.setOrder(order);
        for (int i = 0; i < choices.length; i++) {
            PossibleChoice possibleChoice = new PossibleChoice();
            possibleChoice.setText(choices[i]);
            possibleChoice.setOrder((long) i);
            question.addPossibleChoice(possibleChoice);
        }
        return question;
    }
    
}
