-- Backend assessment §3 (HIGH/MEDIUM): concurrency control, deterministic
-- active-definition resolution, and status/action integrity at the
-- database level rather than relying solely on application code.

-- Optimistic locking backing WorkflowInstance.version (@Version).
ALTER TABLE workflow_instances
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- findActiveByKey() picks "the" active definition for a workflow_key; the
-- old unique constraint was only (workflow_key, version), so two versions
-- of the same key could both be active at once, making the pick
-- non-deterministic. At most one active version per key, enforced here
-- rather than only in application code.
CREATE UNIQUE INDEX uq_one_active_definition_per_key
    ON workflow_definitions (workflow_key)
    WHERE active = TRUE;

-- Status/action values are persisted as free-form strings (matched by
-- Java enums WorkflowState / WorkflowAction at the application layer);
-- CHECK constraints stop bad data from entering directly via SQL even if
-- application-layer validation is bypassed.
ALTER TABLE workflow_instances
    ADD CONSTRAINT chk_instance_status
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RETURNED', 'COMPLETED', 'CANCELLED'));

ALTER TABLE workflow_assignments
    ADD CONSTRAINT chk_assignment_status
    CHECK (status IN ('ACTIVE', 'COMPLETED', 'RETURNED', 'CANCELLED', 'DELEGATED'));

ALTER TABLE workflow_history
    ADD CONSTRAINT chk_history_action
    CHECK (action IN ('START', 'APPROVE', 'RETURN', 'CANCEL', 'DELEGATE', 'COMPLETED'));
