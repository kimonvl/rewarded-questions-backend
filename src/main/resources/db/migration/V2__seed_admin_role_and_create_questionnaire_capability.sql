INSERT INTO roles (id, name)
VALUES (1, 'ADMIN')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name;

INSERT INTO capabilities (id, name, description)
VALUES (1, 'CREATE_QUESTIONNAIRE', 'Create questionnaires')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO roles_capabilities (role_id, capability_id)
VALUES (1, 1)
ON CONFLICT (role_id, capability_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('roles', 'id'), COALESCE((SELECT MAX(id) FROM roles), 1), true);
SELECT setval(pg_get_serial_sequence('capabilities', 'id'), COALESCE((SELECT MAX(id) FROM capabilities), 1), true);
