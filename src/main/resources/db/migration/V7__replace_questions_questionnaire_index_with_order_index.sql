DROP INDEX IF EXISTS idx_questions_questionnaires;

CREATE INDEX idx_questions_questionnaires
    ON questions (questionnaire_id, question_order);
