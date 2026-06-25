create table shipper_vehicles (
        battery_level integer,
        current_orders integer not null,
        current_weight_kg decimal(10,2) not null,
        id integer not null auto_increment,
        max_orders integer not null,
        max_weight_kg integer not null,
        shipper_id integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6),
        notes TEXT,
        status enum ('ACTIVE','INACTIVE','MAINTENANCE') not null,
        vehicle_type enum ('ELECTRIC_BIKE','MOTORBIKE') not null,
        primary key (id)
    ) engine=InnoDB;

alter table shipper_vehicles 
       add constraint UK6rvqaaxrc9pqng20fdgrb5g5t unique (shipper_id);
