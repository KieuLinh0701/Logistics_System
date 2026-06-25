create table vehicle_trackings (
        id integer not null auto_increment,
        latitude decimal(10,6) not null,
        longitude decimal(10,6) not null,
        shipment_id integer not null,
        speed decimal(6,2) not null,
        vehicle_id integer not null,
        recorded_at datetime(6) not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_vehicle_trackings_shipment_id on vehicle_trackings (shipment_id);

create index idx_vehicle_trackings_vehicle_id on vehicle_trackings (vehicle_id);

create index idx_vehicle_trackings_vehicle_id_recorded_at on vehicle_trackings (vehicle_id, recorded_at);
