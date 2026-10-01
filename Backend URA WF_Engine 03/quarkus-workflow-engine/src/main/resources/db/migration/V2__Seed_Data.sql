-- Rank ladder (concept paper, 2.1)
INSERT INTO ranks (name, level, description) VALUES
    ('Officer', 1, 'Prepares and executes work'),
    ('Supervisor', 2, 'First-line review'),
    ('Manager', 3, 'Endorses and manages a unit'),
    ('Assistant Commissioner', 4, 'Approves at divisional level'),
    ('Commissioner', 5, 'Heads a department; final approval'),
    ('Commissioner General', 6, 'Executive leadership');

-- Departments (concept paper, 3.4)
INSERT INTO departments (code, name) VALUES
    ('ICT', 'Information Communication Technology'),
    ('HR', 'Human Resource'),
    ('LEGAL', 'Legal Services'),
    ('FIN', 'Finance'),
    ('DT', 'Domestic Taxes'),
    ('CUSTOMS', 'Customs');

-- Sample organisation chart for the Domestic Taxes department, matching
-- the internal-memo example path in the concept paper (2.2) and
-- architecture doc: Officer -> Supervisor -> Manager ->
-- Assistant Commissioner -> Commissioner. Used to exercise the engine's
-- reports_to resolution without needing HR sync (Stage 1 uses seeded /
-- admin-managed org data).
INSERT INTO persons (staff_number, first_name, last_name, email, department_id, rank_id, reports_to, active) VALUES
    ('DT-COMM-001', 'Grace', 'Nakato', 'grace.nakato@ura.go.ug',
        (SELECT id FROM departments WHERE code = 'DT'),
        (SELECT id FROM ranks WHERE name = 'Commissioner'),
        NULL, TRUE);

INSERT INTO persons (staff_number, first_name, last_name, email, department_id, rank_id, reports_to, active) VALUES
    ('DT-AC-001', 'Peter', 'Okello', 'peter.okello@ura.go.ug',
        (SELECT id FROM departments WHERE code = 'DT'),
        (SELECT id FROM ranks WHERE name = 'Assistant Commissioner'),
        (SELECT id FROM persons WHERE staff_number = 'DT-COMM-001'), TRUE);

INSERT INTO persons (staff_number, first_name, last_name, email, department_id, rank_id, reports_to, active) VALUES
    ('DT-MGR-001', 'Sarah', 'Kirabo', 'sarah.kirabo@ura.go.ug',
        (SELECT id FROM departments WHERE code = 'DT'),
        (SELECT id FROM ranks WHERE name = 'Manager'),
        (SELECT id FROM persons WHERE staff_number = 'DT-AC-001'), TRUE);

INSERT INTO persons (staff_number, first_name, last_name, email, department_id, rank_id, reports_to, active) VALUES
    ('DT-SUP-001', 'James', 'Mugisha', 'james.mugisha@ura.go.ug',
        (SELECT id FROM departments WHERE code = 'DT'),
        (SELECT id FROM ranks WHERE name = 'Supervisor'),
        (SELECT id FROM persons WHERE staff_number = 'DT-MGR-001'), TRUE);

INSERT INTO persons (staff_number, first_name, last_name, email, department_id, rank_id, reports_to, active) VALUES
    ('DT-OFF-001', 'Esther', 'Namuli', 'esther.namuli@ura.go.ug',
        (SELECT id FROM departments WHERE code = 'DT'),
        (SELECT id FROM ranks WHERE name = 'Officer'),
        (SELECT id FROM persons WHERE staff_number = 'DT-SUP-001'), TRUE);
