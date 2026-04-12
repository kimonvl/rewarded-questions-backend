package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    boolean existsByTextAndQuestionnaireId(String text, Long questionnaireId);
}
