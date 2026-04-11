CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE capabilities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE roles_capabilities (
    role_id BIGINT NOT NULL,
    capability_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, capability_id),
    CONSTRAINT fk_roles_capabilities_role
        FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_roles_capabilities_capability
        FOREIGN KEY (capability_id) REFERENCES capabilities (id)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL,
    deleted_at TIMESTAMPTZ,
    uuid UUID NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE questionnaires (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL,
    deleted_at TIMESTAMPTZ,
    uuid UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(200),
    business VARCHAR(200),
    CONSTRAINT fk_questionnaires_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_questionnaires_business
    ON questionnaires (business);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL,
    deleted_at TIMESTAMPTZ,
    uuid UUID NOT NULL UNIQUE,
    questionnaire_id BIGINT NOT NULL,
    text VARCHAR(255) NOT NULL,
    is_free_text BOOLEAN,
    select_min BIGINT DEFAULT 1,
    select_max BIGINT,
    CONSTRAINT fk_questions_questionnaire
        FOREIGN KEY (questionnaire_id) REFERENCES questionnaires (id)
);

CREATE INDEX idx_questions_questionnaires
    ON questions (questionnaire_id);

CREATE TABLE possible_choices (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL,
    deleted_at TIMESTAMPTZ,
    uuid UUID NOT NULL UNIQUE,
    question_id BIGINT NOT NULL,
    text VARCHAR(255) NOT NULL,
    choice_order BIGINT,
    CONSTRAINT fk_possible_choices_question
        FOREIGN KEY (question_id) REFERENCES questions (id)
);

CREATE INDEX idx_possible_choices_questions
    ON possible_choices (question_id);

CREATE TABLE submissions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL,
    deleted_at TIMESTAMPTZ,
    uuid UUID NOT NULL UNIQUE,
    questionnaire_id BIGINT NOT NULL,
    CONSTRAINT fk_submissions_questionnaire
        FOREIGN KEY (questionnaire_id) REFERENCES questionnaires (id)
);

CREATE INDEX idx_questionnaire_id
    ON submissions (questionnaire_id);

CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL,
    deleted_at TIMESTAMPTZ,
    uuid UUID NOT NULL UNIQUE,
    question_id BIGINT NOT NULL,
    free_text VARCHAR(255),
    possible_choice_id BIGINT,
    submission_id BIGINT NOT NULL,
    CONSTRAINT fk_answers_question
        FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_answers_possible_choice
        FOREIGN KEY (possible_choice_id) REFERENCES possible_choices (id),
    CONSTRAINT fk_answers_submission
        FOREIGN KEY (submission_id) REFERENCES submissions (id),
    CONSTRAINT uk_submission_question_choice
        UNIQUE (submission_id, question_id, possible_choice_id)
);

CREATE INDEX idx_submission_id
    ON answers (submission_id);
