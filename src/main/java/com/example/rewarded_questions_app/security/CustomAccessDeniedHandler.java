package com.example.rewarded_questions_app.security;


import com.example.rewarded_questions_app.dto.response.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Authorization Filter. Returns 403.
 */
@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;  // Spring Boot gets this from Jackson
    // Needs to be injected since this is a filter and thus, outside Spring pipeline

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
            throws IOException {

        log.warn("Access denied for user to request={} with message={}", request.getRequestURI(), accessDeniedException.getMessage());
        // Set the response status and content type
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json; charset=UTF-8");

        response.getWriter().write(
                objectMapper.writeValueAsString(
                        new ErrorResponseDTO("ACCESS_DENIED", "User is not allowed to access this route.")
                )
        );
    }
}