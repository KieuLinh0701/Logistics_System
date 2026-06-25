create table addresses (
        city_code integer not null,
        id integer not null auto_increment,
        is_default bit,
        latitude float(53) not null,
        longitude float(53) not null,
        user_id integer,
        ward_code integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6),
        city_name NVARCHAR(255) not null,
        detail NVARCHAR(255) not null,
        full_address NVARCHAR(255) not null,
        name NVARCHAR(50),
        phone_number NVARCHAR(11),
        ward_name NVARCHAR(255) not null,
        type enum ('RECIPIENT','SENDER') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_addresses_user_id on addresses (user_id);

create index idx_addresses_user_id_is_default on addresses (user_id, is_default);
