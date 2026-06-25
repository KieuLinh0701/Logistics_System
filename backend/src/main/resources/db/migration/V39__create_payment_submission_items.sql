create table payment_submission_items (
        id integer not null auto_increment,
        order_product_id integer not null,
        payment_submission_id integer not null,
        quantity integer not null,
        total_amount decimal(38,2) not null,
        unit_amount decimal(38,2) not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_payment_submission_items_order_product_id on payment_submission_items (order_product_id);

create index idx_payment_submission_items_payment_submission_id on payment_submission_items (payment_submission_id);
