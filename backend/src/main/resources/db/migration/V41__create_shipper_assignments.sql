create table shipper_assignments (
        city_code integer not null,
        shipper_id integer not null,
        ward_code integer not null,
        created_at datetime(6) not null,
        end_at datetime(6),
        id bigint not null auto_increment,
        start_at datetime(6) not null,
        updated_at datetime(6),
        notes NVARCHAR(255),
        primary key (id)
    ) engine=InnoDB;

create index idx_shipper_assignments_shipper_id on shipper_assignments (shipper_id);
