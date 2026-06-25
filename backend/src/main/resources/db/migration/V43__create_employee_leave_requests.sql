create table employee_leave_requests (
        approved_by integer,
        employee_id integer not null,
        id integer not null auto_increment,
        leave_date date not null,
        office_id integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6),
        custom_reason NVARCHAR(500),
        employee_note NVARCHAR(1000),
        reason_type enum ('EMERGENCY','FAMILY','OTHER','PERSONAL','SICK') not null,
        shift enum ('AFTERNOON','EVENING','FULL_DAY','MORNING') not null,
        status enum ('APPROVED','CANCELLED','PENDING','REJECTED') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_employee_leave_requests_approved_by on employee_leave_requests (approved_by);

create index idx_employee_leave_requests_employee_id on employee_leave_requests (employee_id);

create index idx_employee_leave_requests_office_id on employee_leave_requests (office_id);

create index idx_employee_leave_requests_employee_id_status_leave_date on employee_leave_requests (employee_id, status, leave_date);
