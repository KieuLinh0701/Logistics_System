create table fee_configurations (
        active bit not null,
        fee_value decimal(10,2) not null,
        id integer not null auto_increment,
        max_order_fee decimal(12,2),
        min_order_fee decimal(12,2),
        service_type_id integer,
        created_at datetime(6) not null,
        updated_at datetime(6),
        calculation_type enum ('FIXED','PERCENTAGE') not null,
        fee_type enum ('COD','INSURANCE','PACKAGING','VAT','VOLUMETRIC_DIVISOR') not null,
        notes longtext,
        primary key (id)
    ) engine=InnoDB;

create index idx_fee_configurations_service_type_id on fee_configurations (service_type_id);
