package com.example.rewarded_questions_app.exceptions;

import com.example.rewarded_questions_app.dto.GenericResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler responsible for gathering and translating application-specific
 * exceptions into standardized http responses.
 *
 * <p>This class ensures consistent error responses by wrapping the
 * exception messages into {@link GenericResponse} which is contained inside {@link ResponseEntity}
 * with appropriate http status codes.</p>
 * */
@RequiredArgsConstructor
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<GenericResponse<?>> handleValidationException(ValidationException e) {
        log.warn("Validation Failed. Message={}", e.getMessage());

        BindingResult bindingResult = e.getBindingResult();

        Map<String, String> errors = new HashMap<>();
        for (FieldError filedError : bindingResult.getFieldErrors()) {
            errors.put(filedError.getField(), filedError.getCode());
        }

        return new ResponseEntity<>(new GenericResponse<>(errors, e.getCode(), e.getMessage(), false),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<GenericResponse<?>> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("Entity not found. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)               // 404 Not Found
                .body(new GenericResponse<>(null, e.getCode(), e.getMessage(), false));
    }

    @ExceptionHandler(EntityInvalidArgumentException.class)
    public ResponseEntity<GenericResponse<?>> handleInvalidArgumentException(EntityInvalidArgumentException e) {
        log.warn("Invalid Argument. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)             // 400 Bad Request
                .body(new GenericResponse<>(null, e.getCode(), e.getMessage(), false));
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<GenericResponse<?>> handleEntityAlreadyExistsException(EntityAlreadyExistsException e) {
        log.warn("Entity already exits. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)                // 409 Conflict
                .body(new GenericResponse<>(null, e.getCode(), e.getMessage(), false));
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<GenericResponse<?>> handleFileUploadException(FileUploadException e) {
        log.warn("File upload failed. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)       // 500 Internal Server Error
                .body(new GenericResponse<>(null, e.getCode(), e.getMessage(), false));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<GenericResponse<?>> handleDatabaseException(DataAccessException e) {
        log.warn("Database error. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>(null, "DATABASE_ERROR", "A database error occurred.", false));
    }


    // Generic fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<?>> handleGenericException(Exception e) {
        log.warn("Unexpected error. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>(null, "INTERNAL_SERVER_ERROR", "A unexpected error occurred.", false));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GenericResponse<?>> handleAuthenticationException(AuthenticationException e,
                                                                          HttpServletRequest request) {
        log.warn("Failed login for IP={}", request.getRemoteAddr());

        String errorCode = switch (e) {
            case BadCredentialsException ex -> "INVALID_CREDENTIALS";
            case DisabledException ex -> "ACCOUNT_DISABLED";
            case LockedException ex -> "ACCOUNT_LOCKED";
            case AccountExpiredException ex -> "ACCOUNT_EXPIRED";
            case CredentialsExpiredException ex -> "CREDENTIALS_EXPIRED";
            default -> "AUTHENTICATION_ERROR";
        };

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)            // 401 Unauthorized
                .body(new GenericResponse<>(null, errorCode, e.getMessage(), false));
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GenericResponse<?>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)       // 403 Forbidden
                .body(new GenericResponse<>(null, "ACCESS_DENIED", e.getMessage(), false));
    }

//    /**
//     * Handles cases where a user attempts to create an entity that already exists.
//     *
//     * @param exception the thrown {@link EntityAlreadyExistsException}
//     * @return a response with a message indicating that the entity cannot be created because it already exists.
//     * */
//    @ExceptionHandler(EntityAlreadyExistsException.class)
//    ResponseEntity<@NonNull GenericResponse<?>> handleEntityAlreadyExists(EntityAlreadyExistsException exception){
//        return new ResponseEntity<>(new GenericResponse<>(null, exception.getMessage(), false), HttpStatus.CONFLICT);
//    }
//
//    /**
//     * Handles cases where a user attempts to fetch an entity that doesn't exist.
//     *
//     * @param exception the thrown {@link EntityNotFoundException}
//     * @return a response with a message indicating that the entity cannot be fetched because it doesn't exist.
//     * */
//    @ExceptionHandler(EntityNotFoundException.class)
//    ResponseEntity<@NonNull GenericResponse<?>> handleEntityNotFound(
//            EntityNotFoundException exception,
//            Locale locale
//    ){
//        String localized = messageSource.getMessage(exception.getMessage(), null, exception.getMessage(), locale);
//        return new ResponseEntity<>(new GenericResponse<>(null, localized, false), HttpStatus.NOT_FOUND);
//    }
//
//    @ExceptionHandler(EntityInvalidArgumentException.class)
//    ResponseEntity<@NonNull GenericResponse<?>> handleEntityInvalidArgument(
//            EntityInvalidArgumentException exception,
//            Locale locale
//    ){
//        String localized = messageSource.getMessage(exception.getMessage(), null, exception.getMessage(), locale);
//        return new ResponseEntity<>(new GenericResponse<>(null, localized, false), HttpStatus.NOT_ACCEPTABLE);
//    }
//
//    /**
//     * Handles cases where a user tries to log in with wrong
//     * email, password or role.
//     *
//     * @param exception the thrown {@link WrongCredentialsException}
//     * @return a response with a message indicating authentication failure
//     * */
//    @ExceptionHandler(WrongCredentialsException.class)
//    public ResponseEntity<@NonNull GenericResponse<?>> handleWrongCredentials(
//            WrongCredentialsException exception,
//            Locale locale
//    ) {
//        String localized = messageSource.getMessage(exception.getMessage(), null, exception.getMessage(), locale);
//        return new ResponseEntity<>(new GenericResponse<>(null, localized, false), HttpStatus.UNAUTHORIZED);
//
//    }
//
//    @ExceptionHandler(MediaUploadFailedException.class)
//    ResponseEntity<@NonNull GenericResponse<?>> handleMediaUpload(
//            MediaUploadFailedException exception,
//            Locale locale
//    ){
//        String localized = messageSource.getMessage(exception.getMessage(), null, exception.getMessage(), locale);
//        return new ResponseEntity<>(new GenericResponse<>(null, localized, false), HttpStatus.INTERNAL_SERVER_ERROR);
//    }

}