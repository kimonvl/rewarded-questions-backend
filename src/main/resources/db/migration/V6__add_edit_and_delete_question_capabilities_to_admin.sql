INSERT INTO capabilities (id, name, description)
VALUES
    (4, 'DELETE_QUESTIONNAIRE', 'Delete questionnaires'),
    (5, 'EDIT_QUESTION', 'Edit questions'),
    (6, 'DELETE_QUESTION', 'Delete questions')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO roles_capabilities (role_id, capability_id)
VALUES
    (1, 4),
    (1, 5),
    (1, 6)
ON CONFLICT (role_id, capability_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('capabilities', 'id'), COALESCE((SELECT MAX(id) FROM capabilities), 1), true);
