create table settlement_batches (
        balance_amount decimal(38,2),
        id integer not null auto_increment,
        locked_sent bit not null,
        paid_amount decimal(38,2),
        shop_id integer not null,
        warning_sent bit not null,
        created_at datetime(6) not null,
        updated_at datetime(6),
        code varchar(50),
        status enum ('COMPLETED','FAILED','PENDING') not null,
        primary key (id)
    ) engine=InnoDB;

alter table settlement_batches 
       add constraint UK9r6ickdbr3h6mhu1b19mvg5xk unique (code);

create index idx_settlement_batches_shop_id on settlement_batches (shop_id);
