package com.example.rewarded_questions_app.repository;

import com.example.rewarded_questions_app.model.submission.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
}
