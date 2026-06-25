create table ai_route_plans (
        active bit not null,
        created_by_employee_id integer,
        manager_employee_id integer not null,
        office_id integer not null,
        return_to_office bit not null,
        total_distance_km decimal(38,2),
        total_duration_minutes decimal(38,2),
        total_fuel_cost decimal(38,2),
        unassigned_count integer,
        version_number integer,
        base_plan_id bigint,
        completed_at datetime(6),
        confirmed_at datetime(6),
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        parent_plan_id bigint,
        started_at datetime(6),
        total_cod bigint,
        updated_at datetime(6),
        created_by_role varchar(20),
        plan_code varchar(50),
        optimization_note NVARCHAR(1000),
        optimization_scope enum ('MANAGER_GLOBAL','PICKUP_INSERTION','SHIPPER_LOCAL'),
        route_mode enum ('CLOSED_LOOP','OPEN_ROUTE'),
        status enum ('CANCELLED','COMPLETED','CONFIRMED','DRAFT','RUNNING') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_ai_route_plans_manager_employee_id on ai_route_plans (manager_employee_id);

create index idx_ai_route_plans_office_id on ai_route_plans (office_id);

create index idx_ai_route_plans_office_id_status_created_at on ai_route_plans (office_id, status, created_at);
