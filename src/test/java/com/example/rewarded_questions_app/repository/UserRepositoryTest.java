package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.user.Capability;
import com.example.rewarded_questions_app.model.user.Role;
import com.example.rewarded_questions_app.model.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByEmailReturnsUserWithRoleAndCapabilities() {
        Role adminRole = new Role();
        adminRole.setName("ADMIN");

        Capability createQuestionnaire = new Capability();
        createQuestionnaire.setName("CREATE_QUESTIONNAIRE");
        createQuestionnaire.setDescription("Create questionnaires");

        adminRole.addCapability(createQuestionnaire);

        entityManager.persist(createQuestionnaire);
        entityManager.persist(adminRole);

        User user = new User();
        user.setEmail("admin@example.com");
        user.setPassword("password");
        user.setOrganization("Example Org");
        user.setRole(adminRole);

        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepository.findByEmail("admin@example.com").orElseThrow();

        // first check lazy loaded fields are loaded before accessing them and triggering lazy loading
        assertThat(foundUser.getEmail()).isEqualTo("admin@example.com");
        assertThat(Persistence.getPersistenceUtil().isLoaded(foundUser, "role")).isTrue();
        Role role = foundUser.getRole();
        assertThat(Persistence.getPersistenceUtil().isLoaded(role, "capabilities")).isTrue();

        assertThat(role.getName()).isEqualTo("ADMIN");
        assertThat(role.getAllCapabilities())
                .extracting(Capability::getName)
                .containsExactly("CREATE_QUESTIONNAIRE");
    }

    @Test
    void existsByEmailIgnoreCaseReturnsTrueForDifferentCase() {
        Role userRole = new Role();
        userRole.setName("USER");
        entityManager.persist(userRole);

        User user = new User();
        user.setEmail("person@example.com");
        user.setPassword("password");
        user.setOrganization("Example Org");
        user.setRole(userRole);

        entityManager.persist(user);
        entityManager.flush();

        boolean exists = userRepository.existsByEmailIgnoreCase("PERSON@EXAMPLE.COM");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailIgnoreCaseReturnsFalseWhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmailIgnoreCase("missing@example.com");

        assertThat(exists).isFalse();
    }
}
