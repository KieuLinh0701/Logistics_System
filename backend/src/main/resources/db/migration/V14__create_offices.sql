create table offices (
        capacity integer,
        city_code integer not null,
        closing_time time(6) not null,
        id integer not null auto_increment,
        latitude decimal(12,7) not null,
        longitude decimal(12,7) not null,
        manager_id integer,
        opening_time time(6) not null,
        ward_code integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6),
        postal_code varchar(10),
        phone_number varchar(15) not null,
        code varchar(50) not null,
        email varchar(100) not null,
        detail NVARCHAR(255) not null,
        name NVARCHAR(255) not null,
        notes longtext,
        status enum ('ACTIVE','INACTIVE','MAINTENANCE') not null,
        type enum ('HEAD_OFFICE','POST_OFFICE') not null,
        primary key (id)
    ) engine=InnoDB;

alter table offices 
       add constraint UKb5bupohtlqg0evvb1xvw66fog unique (manager_id);

alter table offices 
       add constraint UKivdy09twxv5nmrtaxml6wcdrt unique (phone_number);

alter table offices 
       add constraint UK6vj8bq31kumenvpgla0rucf89 unique (code);

alter table offices 
       add constraint UKedjms83xmpm0fdqiqya1a6qwt unique (name);
