create table order_products (
        delivered_quantity integer not null,
        id integer not null auto_increment,
        order_id integer not null,
        price integer not null,
        product_id integer not null,
        quantity integer not null,
        returned_quantity integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6),
        version BIGINT NOT NULL DEFAULT 0 not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_order_products_order_id on order_products (order_id);

create index idx_order_products_product_id on order_products (product_id);
