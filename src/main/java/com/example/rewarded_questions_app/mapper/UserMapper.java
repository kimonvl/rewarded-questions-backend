package com.example.rewarded_questions_app.mapper;

import com.example.rewarded_questions_app.dto.RegisterRequest;
import com.example.rewarded_questions_app.dto.response.UserDTO;
import com.example.rewarded_questions_app.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {
    public User registerRequestToUser(RegisterRequest req, String encodedPassword) {
        User user = new User();
        user.setEmail(req.email());
        user.setPassword(encodedPassword);
        user.setOrganization(req.organization());
        return user;
    }

    public UserDTO userToUserDTO(User user) {
        return new UserDTO(
                user.getUuid(),
                user.getEmail(),
                user.getRole().getId()
        );
    }
}
