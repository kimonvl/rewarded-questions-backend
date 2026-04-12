package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{

    @EntityGraph(attributePaths = {"role", "role.capabilities"})
    Optional<User> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);
}