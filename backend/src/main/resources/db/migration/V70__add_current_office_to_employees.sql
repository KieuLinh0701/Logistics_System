ALTER TABLE employees
    ADD COLUMN current_office_id INT NULL,
ADD CONSTRAINT fk_employee_current_office
    FOREIGN KEY (current_office_id) REFERENCES offices(id);

UPDATE employees SET current_office_id = office_id;