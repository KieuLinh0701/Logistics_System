create table delivery_attempts (
        attempt_number integer not null,
        order_id integer not null,
        shipper_id integer not null,
        attempted_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        note NVARCHAR(1000),
        fail_reason enum ('NO_RESPONSE','OTHER','RECIPIENT_NOT_AVAILABLE','RECIPIENT_REFUSED','RESCHEDULE_REQUESTED','WRONG_ADDRESS'),
        status enum ('FAILED','SUCCESS') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_delivery_attempts_order_id on delivery_attempts (order_id);

create index idx_delivery_attempts_shipper_id on delivery_attempts (shipper_id);

create index idx_delivery_attempts_order_id_attempted_at on delivery_attempts (order_id, attempted_at);
