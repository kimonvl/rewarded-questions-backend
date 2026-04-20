package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.request.CreateQuestionnaireRequest;
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
@WithMockUser(authorities = "CREATE_QUESTIONNAIRE")
class QuestionnaireServiceImplTest {

    @Autowired
    private QuestionnaireService questionnaireService;

    @Autowired
    private QuestionnaireRepository questionnaireRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;

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
}
