package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {
    boolean existsByUserIdAndTitleAndDeletedFalse(Long id, String title);
    boolean existsByUserEmailAndTitleAndDeletedFalse(String email, String title);

    @EntityGraph(attributePaths = {
            "questions",
            "questions.possibleChoices"
    })
    Optional<Questionnaire> findWithQuestionsByUuidAndDeletedFalse(UUID uuid);
    Optional<Questionnaire> findByUuidAndDeletedFalse(UUID uuid);
    Optional<Questionnaire> findByUserIdAndTitleAndDeletedFalse(Long userId, String title);
}
