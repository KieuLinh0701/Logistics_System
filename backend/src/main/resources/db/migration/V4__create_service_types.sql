create table service_types (
        id integer not null auto_increment,
        created_at datetime(6),
        updated_at datetime(6),
        name VARCHAR(100) not null,
        delivery_time TEXT,
        description TEXT,
        status enum ('ACTIVE','INACTIVE') not null,
        primary key (id)
    ) engine=InnoDB;

alter table service_types 
       add constraint UKmfkdxilrbtfhxlm9xy58o93mq unique (name);
