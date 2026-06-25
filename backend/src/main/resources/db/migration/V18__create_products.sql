create table products (
        id integer not null auto_increment,
        price integer not null,
        sold_quantity integer not null,
        stock integer not null,
        user_id integer not null,
        weight decimal(10,2) not null,
        created_at datetime(6) not null,
        updated_at datetime(6),
        code varchar(20),
        image varchar(255),
        name NVARCHAR(255) not null,
        status enum ('ACTIVE','INACTIVE') not null,
        type enum ('FRESH','GOODS','LETTER') not null,
        primary key (id)
    ) engine=InnoDB;

alter table products 
       add constraint unique_product_per_user unique (user_id, name);

alter table products 
       add constraint UK57ivhy5aj3qfmdvl6vxdfjs4p unique (code);

create index idx_products_user_id on products (user_id);
