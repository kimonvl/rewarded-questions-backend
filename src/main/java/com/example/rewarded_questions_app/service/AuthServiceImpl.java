package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.authentication.JwtService;
import com.example.rewarded_questions_app.dto.response.AuthResponseDTO;
import com.example.rewarded_questions_app.dto.LoginRequest;
import com.example.rewarded_questions_app.dto.RegisterRequest;
import com.example.rewarded_questions_app.dto.response.UserDTO;
import com.example.rewarded_questions_app.exceptions.EntityAlreadyExistsException;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.exceptions.InternalErrorException;
import com.example.rewarded_questions_app.mapper.UserMapper;
import com.example.rewarded_questions_app.model.user.Role;
import com.example.rewarded_questions_app.model.user.User;
import com.example.rewarded_questions_app.repository.RoleRepository;
import com.example.rewarded_questions_app.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtService;

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;

    @Override
    @Transactional(rollbackFor = {EntityAlreadyExistsException.class, EntityInvalidArgumentException.class})
    public UserDTO register(RegisterRequest req) throws EntityAlreadyExistsException, EntityInvalidArgumentException {
        try {
            String normalized = req.username().trim().toLowerCase();
            if (userRepo.existsByUsernameIgnoreCase(normalized)) {
                throw new EntityAlreadyExistsException("RegisterUser", "User with username=" + normalized + " already exists");
            }

            Role role = roleRepo.findById(req.roleId())
                    .orElseThrow(() -> new EntityInvalidArgumentException("RegisterRole", "Role id=" + req.roleId() + " invalid"));
            User u = userMapper.registerRequestToUser(req, passwordEncoder.encode(req.password()));
            role.addUser(u);

            log.info("User with username={} registered successfully", normalized);
            return userMapper.userToUserDTO(userRepo.save(u));
        } catch (EntityAlreadyExistsException e) {
            log.error("Registration failed for username={}. Username already exists", req.username(), e);
            throw e;
        } catch (EntityInvalidArgumentException e) {
            log.error("Registration failed for username={}. Message={}", req.username(), e.getMessage(), e);
            throw e;
        }
    }
    @Override
    @Transactional(rollbackFor = {AuthenticationException.class, EntityInvalidArgumentException.class, InternalErrorException.class})
    public AuthResponseDTO login(LoginRequest request) throws EntityInvalidArgumentException, InternalErrorException, EntityNotFoundException {
        try {
            var auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            var principal = (User) auth.getPrincipal();
            if (principal == null) {
                log.error("Login failed for username={}, principal not found", request.username());
                throw new InternalErrorException("Login", "Login failed for username=" + request.username() + " due to unexpected system error");
            }

            if (!principal.getRole().getId().equals(request.roleId())) {
                log.error("Login failed due to role mismatch for username={}, role={}", request.username(), request.roleId());
                throw new EntityInvalidArgumentException("LoginRole", "Login failed. Role mismatch for username=" + request.username());
            }
            String token = jwtService.generateToken(
                    principal.getId(),
                    principal.getUsername(),
                    principal.getRole().getName()
            );

            log.info("Login succeeded: user with username={} authenticated", request.username());
            return new AuthResponseDTO(token, userMapper.userToUserDTO(principal));
        } catch (AuthenticationException e) {
            log.warn("Login failed: bad credentials username={}", request.username(), e);
            throw e;
        } catch (EntityInvalidArgumentException e) {
            log.warn("Login failed: role mismatch for username={}", request.username(), e);
            throw e;
        } catch (InternalErrorException e) {
            log.error("Login failed: unexpected system error for username={}", request.username(), e);
            throw e;
        }
    }

    @Override
    public boolean isUserExists(String username) {
        return userRepo.existsByUsernameIgnoreCase(username);
    }

    // For refresh token
//    @Override
//    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityInvalidArgumentException.class})
//    public AuthResult refresh(String refreshTokenValue)
//            throws EntityNotFoundException, EntityInvalidArgumentException
//    {
//        try {
//            RefreshToken existing = refreshRepo.findByToken(refreshTokenValue)
//                    .orElseThrow(() -> new EntityNotFoundException("RefreshToken", "Refresh token not found"));
//
//            if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
//                throw new EntityInvalidArgumentException("RefreshToken", "Refresh token is expired or revoked");
//            }
//
//            // rotate refresh token
//            existing.setRevoked(true);
//            refreshRepo.save(existing);
//
//            User user = existing.getUser();
//            String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().getName());
//            RefreshToken newRefresh = issueRefreshToken(user.getEmail());
//
//            log.info("Refresh and access token issued for user with email={}", user.getEmail());
//            return new AuthResult(access, newRefresh.getToken(), userMapper.toDto(user));
//        } catch (EntityNotFoundException e) {
//            log.warn("Refresh failed: Refresh token not found", e);
//            throw e;
//        } catch (EntityInvalidArgumentException e) {
//            log.warn("Refresh failed: Refresh token is expired", e);
//            throw e;
//        }
//    }
//    @Override
//    @Transactional(rollbackFor = {EntityNotFoundException.class})
//    public void logout(String refreshTokenValue) throws EntityNotFoundException {
//        try {
//            RefreshToken token = refreshRepo.findByToken(refreshTokenValue)
//                    .orElseThrow(() -> new EntityNotFoundException("LogoutToken", "Refresh token not found"));
//
//            token.setRevoked(true);
//            refreshRepo.save(token);
//            log.info("Logout succeeded: User with email={} logged out", token.getUser().getEmail());
//        } catch (EntityNotFoundException e) {
//            log.warn("Logout failed: Refresh token not found", e);
//            throw e;
//        }
}