create table vehicles (
        capacity decimal(10,2) not null,
        id integer not null auto_increment,
        latitude decimal(10,6),
        longitude decimal(10,6),
        office_id integer not null,
        created_at datetime(6) not null,
        last_maintenance_at datetime(6),
        next_maintenance_due datetime(6),
        updated_at datetime(6),
        license_plate varchar(20) not null,
        gps_device_id varchar(64),
        description NVARCHAR(255),
        status enum ('ARCHIVED','AVAILABLE','IN_USE','MAINTENANCE') not null,
        type enum ('CONTAINER','TRUCK','VAN') not null,
        primary key (id)
    ) engine=InnoDB;

alter table vehicles 
       add constraint UK9vovnbiegxevdhqfcwvp2g8pj unique (license_plate);

create index idx_vehicles_office_id on vehicles (office_id);
