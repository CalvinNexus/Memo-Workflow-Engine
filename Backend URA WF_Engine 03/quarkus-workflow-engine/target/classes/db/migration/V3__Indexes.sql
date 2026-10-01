-- Additional indexes/constraints beyond V1's baseline set.

-- Fast lookup of "the" active definition version for a workflow_key
-- (Runtime Engine.start pins whichever version is currently active).
CREATE INDEX idx_workflow_definition_key_active
    ON workflow_definitions (workflow_key, active);

-- Enforce Stage 1's "single active assignment per in-progress instance"
-- rule at the database level, not just in application code.
CREATE UNIQUE INDEX uq_one_active_assignment_per_instance
    ON workflow_assignments (workflow_instance_id)
    WHERE status = 'ACTIVE';

-- Inbox queries filter by person and status together.
CREATE INDEX idx_assignment_person_status
    ON workflow_assignments (assigned_to, status);

-- History is always read ordered by time for a given instance.
CREATE INDEX idx_history_instance_time
    ON workflow_history (workflow_instance_id, action_time);
