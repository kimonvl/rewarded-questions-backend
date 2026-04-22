INSERT INTO capabilities (id, name, description)
VALUES (3, 'EDIT_QUESTIONNAIRE', 'Edit questionnaires')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO roles_capabilities (role_id, capability_id)
VALUES (1, 3)
ON CONFLICT (role_id, capability_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('capabilities', 'id'), COALESCE((SELECT MAX(id) FROM capabilities), 1), true);
