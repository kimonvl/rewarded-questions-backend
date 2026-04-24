package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @EntityGraph(attributePaths = {"possibleChoices"})
    Optional<Question> findByUuidAndDeletedFalse(UUID uuid);

    boolean existsByTextAndQuestionnaireIdAndDeletedFalse(String text, Long questionnaireId);
}
