-- The Stage 1 pilot process from the concept paper (Example A / "Suggested
-- Way Forward" Stage 1): an internal memo climbing Officer -> Supervisor ->
-- Manager -> Assistant Commissioner -> Commissioner -> COMPLETED.
INSERT INTO workflow_definitions (workflow_key, name, version, active, description)
VALUES (
    'internal-memo',
    'Internal Memo Approval',
    1,
    TRUE,
    'Upward approval path for an internal memo: prepare, review, endorse, approve, approve.'
);

INSERT INTO workflow_steps
    (workflow_definition_id, step_number, step_name, target_rank_id, allowed_action, return_step, is_final)
VALUES
    ((SELECT id FROM workflow_definitions WHERE workflow_key = 'internal-memo' AND version = 1),
        1, 'Prepare', (SELECT id FROM ranks WHERE name = 'Officer'), 'SUBMIT', NULL, FALSE),
    ((SELECT id FROM workflow_definitions WHERE workflow_key = 'internal-memo' AND version = 1),
        2, 'Review', (SELECT id FROM ranks WHERE name = 'Supervisor'), 'APPROVE', 1, FALSE),
    ((SELECT id FROM workflow_definitions WHERE workflow_key = 'internal-memo' AND version = 1),
        3, 'Endorse', (SELECT id FROM ranks WHERE name = 'Manager'), 'APPROVE', 2, FALSE),
    ((SELECT id FROM workflow_definitions WHERE workflow_key = 'internal-memo' AND version = 1),
        4, 'Approve (Assistant Commissioner)', (SELECT id FROM ranks WHERE name = 'Assistant Commissioner'), 'APPROVE', 3, FALSE),
    ((SELECT id FROM workflow_definitions WHERE workflow_key = 'internal-memo' AND version = 1),
        5, 'Approve (Commissioner)', (SELECT id FROM ranks WHERE name = 'Commissioner'), 'APPROVE', 4, TRUE);
