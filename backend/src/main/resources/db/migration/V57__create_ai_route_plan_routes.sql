create table ai_route_plan_routes (
        current_latitude float(53),
        current_longitude float(53),
        estimated_distance_km decimal(38,2),
        estimated_duration_minutes decimal(38,2),
        fuel_cost decimal(38,2),
        is_active bit not null,
        return_to_office bit,
        route_sequence integer,
        route_version integer,
        shipment_id integer,
        shipper_employee_id integer not null,
        shipper_user_id integer not null,
        stop_count integer,
        actual_completed_at datetime(6),
        actual_started_at datetime(6),
        id bigint not null auto_increment,
        parent_route_id bigint,
        plan_id bigint not null,
        reoptimized_at datetime(6),
        total_cod bigint,
        start_time varchar(10),
        reoptimize_reason varchar(50),
        encoded_polyline TEXT,
        shipper_name NVARCHAR(255),
        route_mode enum ('CLOSED_LOOP','OPEN_ROUTE'),
        primary key (id)
    ) engine=InnoDB;

create index idx_ai_route_plan_routes_plan_id on ai_route_plan_routes (plan_id);

create index idx_ai_route_plan_routes_plan_id_route_sequence on ai_route_plan_routes (plan_id, route_sequence);

create index idx_ai_route_plan_routes_shipper_employee_id_is_active on ai_route_plan_routes (shipper_employee_id, is_active);
