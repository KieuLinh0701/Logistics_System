create table shipment_orders (
        eta_minutes_from_start integer,
        leg_distance_km decimal(10,2),
        leg_duration_minutes integer,
        order_id integer not null,
        shipment_id integer not null,
        stop_sequence integer,
        eta_time varchar(10),
        stop_type enum ('DELIVERY','PICKUP','RETURN_TO_OFFICE'),
        primary key (order_id, shipment_id)
    ) engine=InnoDB;

create index idx_shipment_orders_order_id on shipment_orders (order_id);

create index idx_shipment_orders_shipment_id on shipment_orders (shipment_id);

create index idx_shipment_orders_shipment_id_stop_sequence on shipment_orders (shipment_id, stop_sequence);
