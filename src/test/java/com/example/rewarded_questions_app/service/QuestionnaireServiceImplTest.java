package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.CreateQuestionnaireRequest;
import com.example.rewarded_questions_app.dto.request.EditQuestionnaireDetailsRequest;
import com.example.rewarded_questions_app.dto.request.QuestionnaireFilters;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDetailsDTO;
import com.example.rewarded_questions_app.dto.response.QuestionnaireWithQuestionsDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import com.example.rewarded_questions_app.model.user.Capability;
import com.example.rewarded_questions_app.model.user.Role;
import com.example.rewarded_questions_app.model.user.User;
import com.example.rewarded_questions_app.repository.QuestionnaireRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "app.security.secret-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.security.jwt-expiration=10800000"
})
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@WithMockUser(authorities = {"CREATE_QUESTIONNAIRE", "EDIT_QUESTIONNAIRE", "DELETE_QUESTIONNAIRE"})
class QuestionnaireServiceImplTest {

    @Autowired
    private QuestionnaireService questionnaireService;

    @Autowired
    private QuestionnaireRepository questionnaireRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private Questionnaire questionnaire;

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

        questionnaire = new Questionnaire();
        questionnaire.setTitle("Sample Questionnaire");
        questionnaire.setDescription("Sample Questionnaire description");
        questionnaire.setUser(owner);

        Question question = new Question();
        question.setText("Sample question?");
        question.setIsFreeText(true);
        question.setOrder(0L);
        questionnaire.addQuestion(question);

        entityManager.persist(adminRole);
        entityManager.persist(owner);

        entityManager.persist(questionnaire);
    }

    @Test
    void createQuestionnaireInvalidEmailThrowsEntityNotFound() {
        CreateQuestionnaireRequest request = new CreateQuestionnaireRequest("New Questionnaire", "Description");

        assertThrows(EntityNotFoundException.class, () -> questionnaireService.createQuestionnaire(request, "owner1@email.com"));
    }

    @Test
    void createQuestionnaireDuplicateTitleThrowsEntityInvalidArgument() {
        CreateQuestionnaireRequest request = new CreateQuestionnaireRequest("Sample Questionnaire", "Description");

        assertThrows(EntityInvalidArgumentException.class, () -> questionnaireService.createQuestionnaire(request, owner.getEmail()));
    }

    @Test
    void createQuestionnaireSuccess() throws EntityInvalidArgumentException, EntityNotFoundException {
        CreateQuestionnaireRequest request = new CreateQuestionnaireRequest("New Questionnaire", "Description");

        QuestionnaireWithQuestionsDTO result = questionnaireService.createQuestionnaire(request, owner.getEmail());
        Questionnaire saved = questionnaireRepository.findByUserIdAndTitleAndDeletedFalse(owner.getId(), request.title()).orElseThrow();

        assertThat(saved.getUser()).isEqualTo(owner);
        assertThat(saved.getTitle()).isEqualTo(request.title());
        assertThat(saved.getDescription()).isEqualTo(request.description());
        assertThat(saved.getAllQuestions()).isNotNull();
        assertThat(saved.getAllQuestions().size()).isEqualTo(0);

        assertThat(result.uuid()).isEqualTo(saved.getUuid());
        assertThat(result.title()).isEqualTo(saved.getTitle());
        assertThat(result.description()).isEqualTo(saved.getDescription());
        assertThat(result.questions().size()).isEqualTo(saved.getAllQuestions().size());
    }

    @Test
    void editQuestionnaireDetailsUserNotFound() {
        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest("New Title", null);

        assertThrows(EntityNotFoundException.class, () -> questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), "missing@example.com"));
    }

    @Test
    void editQuestionnaireDetailsQuestionnaireNotFound() {
        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest("New Title", null);

        assertThrows(EntityNotFoundException.class, () -> questionnaireService.editQuestionnaireDetails(request, java.util.UUID.randomUUID(), owner.getEmail()));
    }

    @Test
    void editQuestionnaireDetailsUserNotOwner() {
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setOrganization("Other Org");
        owner.getRole().addUser(otherUser);
        entityManager.persist(otherUser);

        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest("New Title", null);

        assertThrows(EntityInvalidArgumentException.class, () -> questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), otherUser.getEmail()));
    }

    @Test
    void editQuestionnaireDetailsTitleAndDescriptionNull() {
        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest(null, null);

        assertThrows(EntityInvalidArgumentException.class, () -> questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void editQuestionnaireDetailsTitleEmpty() {
        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest("", "Description");

        assertThrows(EntityInvalidArgumentException.class, () -> questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void editQuestionnaireDetailsDescriptionEmpty() {
        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest(null, "");

        assertThrows(EntityInvalidArgumentException.class, () -> questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void editQuestionnaireDetailsTitleExists() {
        Questionnaire existingQuestionnaire = new Questionnaire();
        existingQuestionnaire.setTitle("Existing Questionnaire");
        existingQuestionnaire.setDescription("Existing Questionnaire description");
        existingQuestionnaire.setUser(owner);
        entityManager.persist(existingQuestionnaire);

        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest(existingQuestionnaire.getTitle(), null);

        assertThrows(EntityInvalidArgumentException.class, () -> questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void editQuestionnaireDetailsTitleLessThan3Chars() {
        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest("ab", null);

        assertThrows(EntityInvalidArgumentException.class, () -> questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void editQuestionnaireDetailsDescriptionLessThan5Chars() {
        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest(null, "abcd");

        assertThrows(EntityInvalidArgumentException.class, () -> questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), owner.getEmail()));
    }

    @Test
    void editQuestionnaireDetailsOnlyTitleSuccess() throws EntityInvalidArgumentException, EntityNotFoundException {
        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest("New Title", null);

        QuestionnaireDetailsDTO result = questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), owner.getEmail());
        Questionnaire saved = questionnaireRepository.findByUuidAndDeletedFalse(questionnaire.getUuid()).orElseThrow();

        assertThat(saved.getTitle()).isEqualTo(request.title());
        assertThat(saved.getDescription()).isEqualTo("Sample Questionnaire description");

        assertThat(result.uuid()).isEqualTo(saved.getUuid());
        assertThat(result.title()).isEqualTo(saved.getTitle());
        assertThat(result.description()).isEqualTo(saved.getDescription());
    }

    @Test
    void editQuestionnaireDetailsOnlyDescriptionSuccess() throws EntityInvalidArgumentException, EntityNotFoundException {
        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest(null, "New Questionnaire description");

        QuestionnaireDetailsDTO result = questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), owner.getEmail());
        Questionnaire saved = questionnaireRepository.findByUuidAndDeletedFalse(questionnaire.getUuid()).orElseThrow();

        assertThat(saved.getTitle()).isEqualTo("Sample Questionnaire");
        assertThat(saved.getDescription()).isEqualTo(request.description());

        assertThat(result.uuid()).isEqualTo(saved.getUuid());
        assertThat(result.title()).isEqualTo(saved.getTitle());
        assertThat(result.description()).isEqualTo(saved.getDescription());
    }

    @Test
    void editQuestionnaireDetailsTitleAndDescriptionSuccess() throws EntityInvalidArgumentException, EntityNotFoundException {
        EditQuestionnaireDetailsRequest request = new EditQuestionnaireDetailsRequest("New Title", "New Questionnaire description");

        QuestionnaireDetailsDTO result = questionnaireService.editQuestionnaireDetails(request, questionnaire.getUuid(), owner.getEmail());
        Questionnaire saved = questionnaireRepository.findByUuidAndDeletedFalse(questionnaire.getUuid()).orElseThrow();

        assertThat(saved.getTitle()).isEqualTo(request.title());
        assertThat(saved.getDescription()).isEqualTo(request.description());

        assertThat(result.uuid()).isEqualTo(saved.getUuid());
        assertThat(result.title()).isEqualTo(saved.getTitle());
        assertThat(result.description()).isEqualTo(saved.getDescription());
    }

    @Test
    void deleteQuestionnaireUserNotFound() {
        assertThrows(EntityNotFoundException.class, () -> questionnaireService.deleteQuestionnaire(questionnaire.getUuid(), "missing@example.com"));
    }

    @Test
    void deleteQuestionnaireQuestionnaireNotFound() {
        assertThrows(EntityNotFoundException.class, () -> questionnaireService.deleteQuestionnaire(java.util.UUID.randomUUID(), owner.getEmail()));
    }

    @Test
    void deleteQuestionnaireUserNotOwner() {
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setOrganization("Other Org");
        owner.getRole().addUser(otherUser);
        entityManager.persist(otherUser);

        assertThrows(EntityInvalidArgumentException.class, () -> questionnaireService.deleteQuestionnaire(questionnaire.getUuid(), otherUser.getEmail()));
    }

    @Test
    void deleteQuestionnaireSuccess() throws EntityInvalidArgumentException, EntityNotFoundException {
        Question existingQuestion = questionnaire.getAllQuestions().iterator().next();

        PossibleChoice firstChoice = new PossibleChoice();
        firstChoice.setText("First choice");
        firstChoice.setOrder(0L);
        existingQuestion.addPossibleChoice(firstChoice);

        PossibleChoice secondChoice = new PossibleChoice();
        secondChoice.setText("Second choice");
        secondChoice.setOrder(1L);
        existingQuestion.addPossibleChoice(secondChoice);

        entityManager.persist(firstChoice);
        entityManager.persist(secondChoice);

        java.util.UUID deletedUuid = questionnaireService.deleteQuestionnaire(questionnaire.getUuid(), owner.getEmail());
        entityManager.flush();
        entityManager.clear();

        Questionnaire deletedQuestionnaire = entityManager.createQuery(
                        "select q from Questionnaire q where q.uuid = :uuid",
                        Questionnaire.class
                )
                .setParameter("uuid", deletedUuid)
                .getSingleResult();
        var deletedQuestions = entityManager.createQuery(
                        "select q from Question q where q.questionnaire.uuid = :uuid",
                        Question.class
                )
                .setParameter("uuid", deletedUuid)
                .getResultList();
        var deletedPossibleChoices = entityManager.createQuery(
                        "select pc from PossibleChoice pc where pc.question.questionnaire.uuid = :uuid",
                        PossibleChoice.class
                )
                .setParameter("uuid", deletedUuid)
                .getResultList();

        assertThat(deletedQuestionnaire.isDeleted()).isTrue();
        assertThat(deletedQuestionnaire.getDeletedAt()).isNotNull();
        assertThat(questionnaireRepository.findByUuidAndDeletedFalse(deletedUuid)).isEmpty();

        assertThat(deletedQuestions).hasSize(1);
        assertThat(deletedQuestions)
                .allSatisfy(question -> {
                    assertThat(question.isDeleted()).isTrue();
                    assertThat(question.getDeletedAt()).isNotNull();
                });

        assertThat(deletedPossibleChoices).hasSize(2);
        assertThat(deletedPossibleChoices)
                .allSatisfy(possibleChoice -> {
                    assertThat(possibleChoice.isDeleted()).isTrue();
                    assertThat(possibleChoice.getDeletedAt()).isNotNull();
                });
    }

    @Test
    void getFilteredAndPaginatedQuestionnairesSize() {
        persistQuestionnaire("Alpha Questionnaire", "Acme Ltd");
        persistQuestionnaire("Beta Questionnaire", "Beta Ltd");
        persistQuestionnaire("Gamma Questionnaire", "Gamma Ltd");
        persistQuestionnaire("Delta Questionnaire", "Delta Ltd");
        entityManager.flush();

        Page<QuestionnaireDetailsDTO> sizeTwoPage = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 2, Sort.by("title").ascending()),
                filters(null, null)
        );
        Page<QuestionnaireDetailsDTO> sizeThreePage = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 3, Sort.by("title").ascending()),
                filters(null, null)
        );

        assertThat(sizeTwoPage.getContent()).hasSize(2);
        assertThat(sizeTwoPage.getTotalElements()).isEqualTo(5);
        assertThat(sizeTwoPage.getTotalPages()).isEqualTo(3);

        assertThat(sizeThreePage.getContent()).hasSize(3);
        assertThat(sizeThreePage.getTotalElements()).isEqualTo(5);
        assertThat(sizeThreePage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void getFilteredAndPaginatedQuestionnairesPage() {
        persistQuestionnaire("Alpha Questionnaire", "Acme Ltd");
        persistQuestionnaire("Beta Questionnaire", "Beta Ltd");
        persistQuestionnaire("Gamma Questionnaire", "Gamma Ltd");
        entityManager.flush();

        Page<QuestionnaireDetailsDTO> firstPage = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 2, Sort.by("title").ascending()),
                filters(null, null)
        );
        Page<QuestionnaireDetailsDTO> secondPage = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(1, 2, Sort.by("title").ascending()),
                filters(null, null)
        );

        assertThat(titles(firstPage)).containsExactly("Alpha Questionnaire", "Beta Questionnaire");
        assertThat(firstPage.getNumber()).isEqualTo(0);
        assertThat(firstPage.hasNext()).isTrue();

        assertThat(titles(secondPage)).containsExactly("Gamma Questionnaire", "Sample Questionnaire");
        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void getFilteredAndPaginatedQuestionnairesTitle() {
        persistQuestionnaire("Customer Survey", "Acme Ltd");
        persistQuestionnaire("Employee Survey", "Beta Ltd");
        persistQuestionnaire("Customer Feedback", "Gamma Ltd");
        entityManager.flush();

        Page<QuestionnaireDetailsDTO> customerMatches = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 10, Sort.by("title").ascending()),
                filters("customer", null)
        );
        Page<QuestionnaireDetailsDTO> surveyMatches = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 10, Sort.by("title").ascending()),
                filters("SURVEY", null)
        );
        Page<QuestionnaireDetailsDTO> missingMatches = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 10, Sort.by("title").ascending()),
                filters("missing", null)
        );

        assertThat(titles(customerMatches)).containsExactly("Customer Feedback", "Customer Survey");
        assertThat(titles(surveyMatches)).containsExactly("Customer Survey", "Employee Survey");
        assertThat(missingMatches.getContent()).isEmpty();
    }

    @Test
    void getFilteredAndPaginatedQuestionnairesBusinessName() {
        persistQuestionnaire("Acme Onboarding", "Acme Ltd");
        persistQuestionnaire("Acme Retention", "Acme Labs");
        persistQuestionnaire("Beta Onboarding", "Beta Ltd");
        entityManager.flush();

        Page<QuestionnaireDetailsDTO> acmeMatches = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 10, Sort.by("title").ascending()),
                filters(null, "acme")
        );
        Page<QuestionnaireDetailsDTO> exampleMatches = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 10, Sort.by("title").ascending()),
                filters(null, "EXAMPLE")
        );
        Page<QuestionnaireDetailsDTO> missingMatches = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 10, Sort.by("title").ascending()),
                filters(null, "missing")
        );

        assertThat(titles(acmeMatches)).containsExactly("Acme Onboarding", "Acme Retention");
        assertThat(titles(exampleMatches)).containsExactly("Sample Questionnaire");
        assertThat(missingMatches.getContent()).isEmpty();
    }

    @Test
    void getFilteredAndPaginatedQuestionnairesBusinessNameTitle() {
        persistQuestionnaire("Customer Survey", "Acme Ltd");
        persistQuestionnaire("Customer Feedback", "Beta Ltd");
        persistQuestionnaire("Employee Survey", "Acme Ltd");
        entityManager.flush();

        Page<QuestionnaireDetailsDTO> customerAcmeMatches = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 10, Sort.by("title").ascending()),
                filters("customer", "acme")
        );
        Page<QuestionnaireDetailsDTO> surveyAcmeMatches = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 10, Sort.by("title").ascending()),
                filters("survey", "acme")
        );
        Page<QuestionnaireDetailsDTO> sampleAcmeMatches = questionnaireService.getFilteredAndPaginatedQuestionnaires(
                PageRequest.of(0, 10, Sort.by("title").ascending()),
                filters("sample", "acme")
        );

        assertThat(titles(customerAcmeMatches)).containsExactly("Customer Survey");
        assertThat(titles(surveyAcmeMatches)).containsExactly("Customer Survey", "Employee Survey");
        assertThat(sampleAcmeMatches.getContent()).isEmpty();
    }

    private void persistQuestionnaire(String title, String organization) {
        User user = new User();
        user.setEmail(title.toLowerCase().replaceAll("[^a-z0-9]", "") + "@example.com");
        user.setPassword("password");
        user.setOrganization(organization);
        owner.getRole().addUser(user);

        Questionnaire newQuestionnaire = new Questionnaire();
        newQuestionnaire.setTitle(title);
        newQuestionnaire.setDescription(title + " description");
        newQuestionnaire.setUser(user);

        entityManager.persist(user);
        entityManager.persist(newQuestionnaire);

    }

    private QuestionnaireFilters filters(String title, String businessName) {
        QuestionnaireFilters filters = new QuestionnaireFilters();
        filters.setTitle(title);
        filters.setBusinessName(businessName);
        return filters;
    }

    private java.util.List<String> titles(Page<QuestionnaireDetailsDTO> page) {
        return page.getContent().stream()
                .map(QuestionnaireDetailsDTO::title)
                .toList();
    }
}
