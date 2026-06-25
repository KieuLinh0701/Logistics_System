create table shipping_rates (
        extra_price decimal(10,2),
        id integer not null auto_increment,
        price decimal(10,2) not null,
        service_type_id integer not null,
        unit decimal(10,2),
        weight_from decimal(10,2) not null,
        weight_to decimal(10,2),
        created_at datetime(6),
        updated_at datetime(6),
        region_type enum ('INTER_REGION','INTRA_CITY','INTRA_REGION','NEAR_REGION') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_shipping_rates_service_type_id on shipping_rates (service_type_id);
