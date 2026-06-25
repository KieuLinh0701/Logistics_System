create table incident_reports (
        handled_by integer,
        id integer not null auto_increment,
        office_id integer not null,
        order_id integer not null,
        shipper_id integer not null,
        created_at datetime(6),
        handled_at datetime(6),
        updated_at datetime(6),
        code varchar(50),
        resolution NVARCHAR(1000),
        title NVARCHAR(255) not null,
        description longtext,
        incident_type enum ('OTHER','PACKAGE_DAMAGED','RECIPIENT_NOT_AVAILABLE','RECIPIENT_REFUSED','SECURITY_ISSUE','WRONG_ADDRESS') not null,
        priority enum ('HIGH','LOW','MEDIUM') not null,
        status enum ('PENDING','PROCESSING','REJECTED','RESOLVED') not null,
        primary key (id)
    ) engine=InnoDB;

alter table incident_reports 
       add constraint UKfp9gvx93vk4ofhn4lpmm5xpvp unique (code);

create index idx_incident_reports_handled_by on incident_reports (handled_by);

create index idx_incident_reports_office_id on incident_reports (office_id);

create index idx_incident_reports_order_id on incident_reports (order_id);

create index idx_incident_reports_shipper_id on incident_reports (shipper_id);

create index idx_incident_reports_office_id_status_created_at on incident_reports (office_id, status, created_at);
