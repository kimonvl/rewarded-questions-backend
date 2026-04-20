package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {
    boolean existsByUserIdAndTitle(Long id, String title);
    boolean existsByUserEmailAndTitle(String email, String title);

    @EntityGraph(attributePaths = {
            "questions",
            "questions.possibleChoices"
    })
    Optional<Questionnaire> findWithQuestionsByUuid(UUID uuid);
    Optional<Questionnaire> findByUuid(UUID uuid);
    Optional<Questionnaire> findByUserIdAndTitle(Long userId, String title);
}
