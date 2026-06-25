create table pickup_attempts (
        attempt_number integer not null,
        order_id integer not null,
        shipper_id integer not null,
        attempted_at datetime(6) not null,
        id bigint not null auto_increment,
        note NVARCHAR(1000),
        fail_reason enum ('CUSTOMER_CANCELLED','NOT_READY','NO_RESPONSE','OTHER','SHOP_CLOSED'),
        status enum ('FAILED','SUCCESS') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_pickup_attempts_order_id on pickup_attempts (order_id);

create index idx_pickup_attempts_shipper_id on pickup_attempts (shipper_id);

create index idx_pickup_attempts_order_id_attempted_at on pickup_attempts (order_id, attempted_at);
