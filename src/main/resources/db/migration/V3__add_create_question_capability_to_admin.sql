INSERT INTO capabilities (id, name, description)
VALUES (2, 'CREATE_QUESTION', 'Create questions')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO roles_capabilities (role_id, capability_id)
VALUES (1, 2)
ON CONFLICT (role_id, capability_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('capabilities', 'id'), COALESCE((SELECT MAX(id) FROM capabilities), 1), true);
