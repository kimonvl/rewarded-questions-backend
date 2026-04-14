package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.authentication.JwtService;
import com.example.rewarded_questions_app.dto.LoginRequest;
import com.example.rewarded_questions_app.dto.RegisterRequest;
import com.example.rewarded_questions_app.dto.response.AuthResponseDTO;
import com.example.rewarded_questions_app.dto.response.UserDTO;
import com.example.rewarded_questions_app.exceptions.EntityAlreadyExistsException;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.exceptions.InternalErrorException;
import com.example.rewarded_questions_app.model.user.Capability;
import com.example.rewarded_questions_app.model.user.Role;
import com.example.rewarded_questions_app.model.user.User;
import com.example.rewarded_questions_app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.security.secret-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.security.jwt-expiration=10800000"
})
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AuthServiceImplTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    private Long adminRoleId;

    @BeforeEach
    void setUp() {
        Capability createQuestionnaire = new Capability();
        createQuestionnaire.setName("CREATE_QUESTIONNAIRE");
        createQuestionnaire.setDescription("Create questionnaires");

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.addCapability(createQuestionnaire);

        entityManager.persist(createQuestionnaire);
        entityManager.persist(adminRole);

        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword(passwordEncoder.encode("password"));
        existingUser.setOrganization("Existing Org");
        adminRole.addUser(existingUser);

        entityManager.persist(existingUser);
        entityManager.flush();

        adminRoleId = adminRole.getId();
    }

    @Test
    void registerWithExistingEmailThrowsException() {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "password",
                "Another Org",
                adminRoleId
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EntityAlreadyExistsException.class);
    }

    @Test
    void registerWithInvalidRoleIdThrowsException() {
        RegisterRequest request = new RegisterRequest(
                "new-user@example.com",
                "password",
                "Example Org",
                -1L
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EntityInvalidArgumentException.class);
    }

    @Test
    void registerSucceeds() throws EntityAlreadyExistsException, EntityInvalidArgumentException {
        RegisterRequest request = new RegisterRequest(
                "registered@example.com",
                "password",
                "Registered Org",
                adminRoleId
        );

        UserDTO result = authService.register(request);

        User savedUser = userRepository.findByEmail(request.email()).orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo(request.email());
        assertThat(savedUser.getOrganization()).isEqualTo(request.organization());
        assertThat(savedUser.getRole().getId()).isEqualTo(adminRoleId);
        assertThat(savedUser.getRole().getName()).isEqualTo("ADMIN");
        assertThat(savedUser.getPassword()).isNotEqualTo(request.password());
        assertThat(passwordEncoder.matches(request.password(), savedUser.getPassword())).isTrue();

        assertThat(result.id()).isEqualTo(savedUser.getUuid());
        assertThat(result.email()).isEqualTo(request.email());
        assertThat(result.roleId()).isEqualTo(adminRoleId);
    }

    @Test
    void loginSucceeds() throws EntityInvalidArgumentException, InternalErrorException, EntityNotFoundException {
        LoginRequest request = new LoginRequest(
                "existing@example.com",
                "password",
                adminRoleId
        );

        AuthResponseDTO result = authService.login(request);
        User savedUser = userRepository.findByEmail(request.email()).orElseThrow();

        assertThat(result.token()).isNotBlank();
        assertThat(jwtService.extractSubject(result.token())).isEqualTo(request.email());
        assertThat(jwtService.getStringClaim(result.token(), "role")).isEqualTo("ADMIN");
        assertThat(jwtService.isTokenValid(result.token(), savedUser)).isTrue();

        assertThat(result.user().id()).isEqualTo(savedUser.getUuid());
        assertThat(result.user().email()).isEqualTo(request.email());
        assertThat(result.user().roleId()).isEqualTo(savedUser.getRole().getId());
    }

    @Test
    void loginWithBadPasswordThrowsAuthenticationException() {
        LoginRequest request = new LoginRequest(
                "existing@example.com",
                "wrong-password",
                adminRoleId
        );

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void loginWithUnknownEmailThrowsAuthenticationException() {
        LoginRequest request = new LoginRequest(
                "missing@example.com",
                "password",
                adminRoleId
        );

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void loginWithMismatchedRoleThrowsException() {
        Role teacherRole = new Role();
        teacherRole.setName("TEACHER");

        entityManager.persist(teacherRole);
        entityManager.flush();

        LoginRequest request = new LoginRequest(
                "existing@example.com",
                "password",
                teacherRole.getId()
        );

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(EntityInvalidArgumentException.class);
    }
}
