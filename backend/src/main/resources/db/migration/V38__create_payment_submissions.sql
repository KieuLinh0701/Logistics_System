create table payment_submissions (
        actual_amount decimal(38,2) not null,
        batch_id integer,
        checked_by integer,
        id integer not null auto_increment,
        order_id integer not null,
        shipper_id integer not null,
        system_amount decimal(38,2) not null,
        checked_at datetime(6),
        paid_at datetime(6),
        updated_at datetime(6),
        code varchar(50),
        notes varchar(255),
        status enum ('ADJUSTED','MATCHED','MISMATCHED','PENDING','PROCESSING') not null,
        primary key (id)
    ) engine=InnoDB;

alter table payment_submissions 
       add constraint UKf7eulb608rqawbvatap3mu912 unique (code);

create index idx_payment_submissions_batch_id on payment_submissions (batch_id);

create index idx_payment_submissions_checked_by on payment_submissions (checked_by);

create index idx_payment_submissions_order_id on payment_submissions (order_id);

create index idx_payment_submissions_shipper_id on payment_submissions (shipper_id);

create index idx_payment_submissions_batch_id_status on payment_submissions (batch_id, status);

create index idx_payment_submissions_shipper_id_status on payment_submissions (shipper_id, status);
