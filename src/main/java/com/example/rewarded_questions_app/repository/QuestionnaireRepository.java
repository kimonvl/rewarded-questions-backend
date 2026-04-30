package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import jakarta.annotation.Nullable;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long>, JpaSpecificationExecutor<Questionnaire> {
    boolean existsByUserIdAndTitleAndDeletedFalse(Long id, String title);
    boolean existsByUserEmailAndTitleAndDeletedFalse(String email, String title);

    @EntityGraph(attributePaths = {
            "questions",
            "questions.possibleChoices"
    })
    Optional<Questionnaire> findWithQuestionsByUuidAndDeletedFalse(UUID uuid);
    Optional<Questionnaire> findByUuidAndDeletedFalse(UUID uuid);
    Optional<Questionnaire> findByUserIdAndTitleAndDeletedFalse(Long userId, String title);

    @Override
    @NullMarked
    @EntityGraph(attributePaths = "user")
    Page<Questionnaire> findAll(@Nullable Specification<Questionnaire> spec, Pageable pageable);

}
