create table ai_route_plan_stops (
        cod_amount integer,
        eta_minutes_from_start integer,
        is_inserted bit,
        leg_distance_km decimal(38,2),
        leg_duration_minutes integer,
        order_id integer,
        original_sequence integer,
        recipient_latitude float(53),
        recipient_longitude float(53),
        service_time_minutes integer,
        stop_sequence integer not null,
        actual_arrived_at datetime(6),
        actual_completed_at datetime(6),
        id bigint not null auto_increment,
        route_id bigint not null,
        eta_time varchar(10),
        priority varchar(20),
        recipient_phone varchar(50),
        tracking_number varchar(50),
        inserted_reason varchar(100),
        recipient_address NVARCHAR(500),
        recipient_name NVARCHAR(255),
        stop_status enum ('ARRIVED','COMPLETED','FAILED','PENDING','SKIPPED'),
        stop_type enum ('DELIVERY','PICKUP','RETURN_TO_OFFICE') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_ai_route_plan_stops_order_id on ai_route_plan_stops (order_id);

create index idx_ai_route_plan_stops_route_id on ai_route_plan_stops (route_id);

create index idx_ai_route_plan_stops_route_id_stop_sequence on ai_route_plan_stops (route_id, stop_sequence);

create index idx_ai_route_plan_stops_tracking_number on ai_route_plan_stops (tracking_number);
