package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @EntityGraph(attributePaths = {"possibleChoices"})
    Optional<Question> findWithChoicesByUuidAndDeletedFalse(UUID uuid);

    @EntityGraph(attributePaths = {
            "possibleChoices",
            "questionnaire"
    })
    Optional<Question> findWithQuestionnaireChoicesByUuidAndDeletedFalse(UUID uuid);

    @EntityGraph(attributePaths = {"possibleChoices"})
    Page<Question> findAllWithChoicesByQuestionnaireIdAndDeletedFalseOrderByOrderAsc(Long questionnaireId, Pageable pageable);

    boolean existsByTextAndQuestionnaireIdAndDeletedFalse(String text, Long questionnaireId);
}
