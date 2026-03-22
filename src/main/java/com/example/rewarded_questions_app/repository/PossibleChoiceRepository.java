package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PossibleChoiceRepository extends JpaRepository<PossibleChoice, Long> {
}
