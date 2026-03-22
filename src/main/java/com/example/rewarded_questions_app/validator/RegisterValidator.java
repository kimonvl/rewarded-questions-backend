package com.example.rewarded_questions_app.validator;


import com.example.rewarded_questions_app.dto.RegisterRequest;
import com.example.rewarded_questions_app.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterValidator implements Validator {

    private final AuthService authService;


    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return RegisterRequest.class == clazz;
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        RegisterRequest registerRequest = (RegisterRequest) target;
        if (!errors.hasFieldErrors("username")) {
            if (authService.isUserExists(registerRequest.username())) {
                errors.rejectValue("username", "RegisterUsernameExists");
                log.warn("Registration failed. User with username={} already exists", registerRequest.username());
            }
        }
    }
}