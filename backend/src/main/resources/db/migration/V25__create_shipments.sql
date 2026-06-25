create table shipments (
        created_by integer,
        employee_id integer,
        from_office_id integer,
        id integer not null auto_increment,
        to_office_id integer,
        vehicle_id integer,
        created_at datetime(6) not null,
        end_time datetime(6),
        start_time datetime(6),
        updated_at datetime(6),
        code varchar(50),
        status enum ('CANCELLED','COMPLETED','IN_TRANSIT','PENDING') not null,
        type enum ('DELIVERY','TRANSFER') not null,
        primary key (id)
    ) engine=InnoDB;

alter table shipments 
       add constraint UKdedyg762hjlqlklajppu0dg69 unique (code);

create index idx_shipments_created_by on shipments (created_by);

create index idx_shipments_employee_id on shipments (employee_id);

create index idx_shipments_from_office_id on shipments (from_office_id);

create index idx_shipments_to_office_id on shipments (to_office_id);

create index idx_shipments_vehicle_id on shipments (vehicle_id);

create index idx_shipments_employee_id_status_created_at on shipments (employee_id, status, created_at);

create index idx_shipments_vehicle_id_status on shipments (vehicle_id, status);
