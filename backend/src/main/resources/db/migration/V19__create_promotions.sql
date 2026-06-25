create table promotions (
        daily_usage_limit_global integer,
        daily_usage_limit_per_user integer,
        discount_value decimal(10,2) not null,
        first_time_user bit,
        id integer not null auto_increment,
        is_global bit not null,
        max_discount_amount integer,
        max_usage_per_user integer,
        max_weight decimal(10,2),
        min_order_value decimal(10,2),
        min_orders_count integer,
        min_weight decimal(10,2),
        usage_limit integer,
        used_count integer not null,
        valid_months_after_join integer,
        valid_years_after_join integer,
        created_at datetime(6),
        end_date datetime(6) not null,
        start_date datetime(6) not null,
        updated_at datetime(6),
        code varchar(50) not null,
        description varchar(255),
        title varchar(255),
        discount_type enum ('FIXED','PERCENTAGE') not null,
        status enum ('ACTIVE','EXPIRED','INACTIVE') not null,
        primary key (id)
    ) engine=InnoDB;

alter table promotions 
       add constraint UKjdho73ymbyu46p2hh562dk4kk unique (code);
