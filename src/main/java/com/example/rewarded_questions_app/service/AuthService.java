package com.example.rewarded_questions_app.service;

import com.example.rewarded_questions_app.dto.AuthResponseDTO;
import com.example.rewarded_questions_app.dto.LoginRequest;
import com.example.rewarded_questions_app.dto.RegisterRequest;
import com.example.rewarded_questions_app.dto.UserDTO;
import com.example.rewarded_questions_app.exceptions.EntityAlreadyExistsException;
import com.example.rewarded_questions_app.exceptions.EntityInvalidArgumentException;
import com.example.rewarded_questions_app.exceptions.EntityNotFoundException;
import com.example.rewarded_questions_app.exceptions.InternalErrorException;

public interface AuthService {
    UserDTO register(RegisterRequest request) throws EntityAlreadyExistsException, EntityInvalidArgumentException;
    AuthResponseDTO login(LoginRequest request) throws EntityInvalidArgumentException, InternalErrorException, EntityNotFoundException;
    boolean isUserExists(String username);
}
