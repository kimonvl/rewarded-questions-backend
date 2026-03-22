package com.example.rewarded_questions_app.api;

import com.example.rewarded_questions_app.dto.*;
import com.example.rewarded_questions_app.dto.response.AuthResponseDTO;
import com.example.rewarded_questions_app.dto.response.UserDTO;
import com.example.rewarded_questions_app.exceptions.*;

import com.example.rewarded_questions_app.service.AuthService;
import com.example.rewarded_questions_app.validator.RegisterValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController {

    private final AuthService authService;
    private final RegisterValidator registerValidator;


    @PostMapping("/register")
    public ResponseEntity<@NonNull GenericResponse<UserDTO>> register(
            @Valid @RequestBody RegisterRequest req,
            BindingResult bindingResult
    ) throws EntityInvalidArgumentException, EntityAlreadyExistsException, ValidationException {
        registerValidator.validate(req, bindingResult);
        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error -> {
                System.out.println(error.getCode() + error.getDefaultMessage());
            });
            throw new ValidationException("RegisterRequest", "Validation failed for registration request", bindingResult);
        }
        return new ResponseEntity<>(
                new GenericResponse<>(
                        authService.register(req),
                        "RegisterSucceeded",
                        "User registered successfully",
                        true
                ),
                HttpStatus.CREATED);

    }

    @PostMapping("/login")
    public ResponseEntity<@NonNull GenericResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequest req,
            BindingResult bindingResult
    ) throws EntityInvalidArgumentException, InternalErrorException, EntityNotFoundException, ValidationException {
        if (bindingResult.hasErrors()) {
            throw new ValidationException("LoginRequest", "Validation failed for login request", bindingResult);
        }

        return new ResponseEntity<>(
                new GenericResponse<>(
                        authService.login(req),
                        "LoginSucceeded",
                        "User logged in successfully",
                        true
                ),
                HttpStatus.OK);
    }

//    @GetMapping("/test")
//    public ResponseEntity<@NonNull GenericResponse<String>> test() {
//
//        return new ResponseEntity<>(
//                new GenericResponse<>(
//                        "Test",
//                        "TestSuccess",
//                        "Test reached",
//                        true
//                ),
//                HttpStatus.OK);
//    }
}