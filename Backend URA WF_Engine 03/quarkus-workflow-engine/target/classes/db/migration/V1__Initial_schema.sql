-- URA Workflow Engine: initial schema
-- Every table carries created_at/updated_at because all JPA entities
-- extend BaseEntity (org.ura.workflow.entity.BaseEntity), which maps
-- those two audit columns via @PrePersist/@PreUpdate.

CREATE TABLE departments
(
    id                    BIGSERIAL PRIMARY KEY,
    code                  VARCHAR(30)  UNIQUE NOT NULL,
    name                  VARCHAR(150) NOT NULL,
    parent_department_id  BIGINT,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP
);

ALTER TABLE departments
    ADD CONSTRAINT fk_parent_department
    FOREIGN KEY (parent_department_id)
    REFERENCES departments (id);

-- Fixed rank ladder used by every department (concept paper, 2.1):
-- Commissioner General > Commissioner > Assistant Commissioner >
-- Manager > Supervisor > Officer.
CREATE TABLE ranks
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL,
    level       INTEGER UNIQUE NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE persons
(
    id            BIGSERIAL PRIMARY KEY,
    staff_number  VARCHAR(30) UNIQUE NOT NULL,
    first_name    VARCHAR(80),
    last_name     VARCHAR(80),
    email         VARCHAR(150),
    department_id BIGINT NOT NULL,
    rank_id       BIGINT NOT NULL,
    reports_to    BIGINT,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP
);

ALTER TABLE persons
    ADD CONSTRAINT fk_person_department
    FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE persons
    ADD CONSTRAINT fk_person_rank
    FOREIGN KEY (rank_id) REFERENCES ranks (id);

ALTER TABLE persons
    ADD CONSTRAINT fk_reports_to
    FOREIGN KEY (reports_to) REFERENCES persons (id);

-- Reusable process recipes (architecture doc: "Workflow Definition Registry").
-- Instances pin the definition version at start, so in-flight items keep
-- the rules they started under even if the definition is edited later.
CREATE TABLE workflow_definitions
(
    id           BIGSERIAL PRIMARY KEY,
    workflow_key VARCHAR(100) NOT NULL,
    name         VARCHAR(200),
    version      INTEGER NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    description  TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP,
    CONSTRAINT uq_workflow_definition_key_version UNIQUE (workflow_key, version)
);

-- Ordered steps for a definition. target_rank_id is resolved against the
-- organisation model (reports_to chain) at runtime, never hard-coded to a
-- specific person.
CREATE TABLE workflow_steps
(
    id                      BIGSERIAL PRIMARY KEY,
    workflow_definition_id  BIGINT NOT NULL,
    step_number             INTEGER NOT NULL,
    step_name               VARCHAR(100),
    target_rank_id          BIGINT NOT NULL,
    allowed_action          VARCHAR(30),
    return_step             INTEGER,
    is_final                BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    CONSTRAINT uq_workflow_step_number UNIQUE (workflow_definition_id, step_number)
);

ALTER TABLE workflow_steps
    ADD CONSTRAINT fk_step_definition
    FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions (id);

ALTER TABLE workflow_steps
    ADD CONSTRAINT fk_step_target_rank
    FOREIGN KEY (target_rank_id) REFERENCES ranks (id);

-- A running (or completed/cancelled) instance of a workflow definition.
-- business_reference is an opaque string owned by the consumer system
-- (e.g. the memo id in ura-memo-demo); the engine never opens it.
CREATE TABLE workflow_instances
(
    id                      BIGSERIAL PRIMARY KEY,
    workflow_definition_id  BIGINT NOT NULL,
    business_reference      VARCHAR(100) NOT NULL,
    current_step            INTEGER NOT NULL,
    status                  VARCHAR(30) NOT NULL,
    started_by              BIGINT NOT NULL,
    started_at              TIMESTAMP,
    completed_at            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP
);

ALTER TABLE workflow_instances
    ADD CONSTRAINT fk_instance_definition
    FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions (id);

ALTER TABLE workflow_instances
    ADD CONSTRAINT fk_started_by
    FOREIGN KEY (started_by) REFERENCES persons (id);

-- Stage 1: single active assignment per in-progress instance
-- (architecture doc, Assignment / Inbox Service).
CREATE TABLE workflow_assignments
(
    id                     BIGSERIAL PRIMARY KEY,
    workflow_instance_id   BIGINT NOT NULL,
    assigned_to            BIGINT NOT NULL,
    assigned_at            TIMESTAMP,
    completed_at           TIMESTAMP,
    status                 VARCHAR(30) NOT NULL,
    comments               TEXT,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP
);

ALTER TABLE workflow_assignments
    ADD CONSTRAINT fk_assignment_instance
    FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances (id);

ALTER TABLE workflow_assignments
    ADD CONSTRAINT fk_assignment_person
    FOREIGN KEY (assigned_to) REFERENCES persons (id);

-- Append-only authoritative log (architecture doc: Audit History Service).
CREATE TABLE workflow_history
(
    id                     BIGSERIAL PRIMARY KEY,
    workflow_instance_id   BIGINT NOT NULL,
    actor_id               BIGINT,
    step_number            INTEGER,
    action                 VARCHAR(30) NOT NULL,
    comments               TEXT,
    action_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP
);

ALTER TABLE workflow_history
    ADD CONSTRAINT fk_history_instance
    FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances (id);

ALTER TABLE workflow_history
    ADD CONSTRAINT fk_history_actor
    FOREIGN KEY (actor_id) REFERENCES persons (id);

CREATE INDEX idx_person_department ON persons (department_id);
CREATE INDEX idx_person_rank ON persons (rank_id);
CREATE INDEX idx_person_reports_to ON persons (reports_to);
CREATE INDEX idx_assignment_person ON workflow_assignments (assigned_to);
CREATE INDEX idx_instance_status ON workflow_instances (status);
CREATE INDEX idx_history_instance ON workflow_history (workflow_instance_id);
