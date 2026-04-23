package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.CreateQuestionnaireRequest;
import com.example.rewarded_questions_app.dto.request.EditQuestionnaireDetailsRequest;
import com.example.rewarded_questions_app.dto.response.QuestionnaireDetailsDTO;
import com.example.rewarded_questions_app.dto.response.QuestionnaireWithQuestionsDTO;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
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
@WithMockUser(authorities = {"CREATE_QUESTIONNAIRE", "EDIT_QUESTIONNAIRE"})
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
        Questionnaire saved = questionnaireRepository.findByUserIdAndTitle(owner.getId(), request.title()).orElseThrow();

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
        Questionnaire saved = questionnaireRepository.findByUuid(questionnaire.getUuid()).orElseThrow();

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
        Questionnaire saved = questionnaireRepository.findByUuid(questionnaire.getUuid()).orElseThrow();

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
        Questionnaire saved = questionnaireRepository.findByUuid(questionnaire.getUuid()).orElseThrow();

        assertThat(saved.getTitle()).isEqualTo(request.title());
        assertThat(saved.getDescription()).isEqualTo(request.description());

        assertThat(result.uuid()).isEqualTo(saved.getUuid());
        assertThat(result.title()).isEqualTo(saved.getTitle());
        assertThat(result.description()).isEqualTo(saved.getDescription());
    }
}
