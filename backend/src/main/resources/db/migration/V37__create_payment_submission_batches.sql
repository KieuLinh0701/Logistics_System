create table payment_submission_batches (
        checked_by integer,
        id integer not null auto_increment,
        office_id integer not null,
        shipper_id integer not null,
        total_actual_amount decimal(38,2),
        total_system_amount decimal(38,2) not null,
        checked_at datetime(6),
        created_at datetime(6),
        updated_at datetime(6),
        code varchar(50),
        notes varchar(255),
        status enum ('COMPLETED','PROCESSING') not null,
        primary key (id)
    ) engine=InnoDB;

alter table payment_submission_batches 
       add constraint UKfyycjuf802eggiig8952sqfas unique (code);

create index idx_payment_submission_batches_checked_by on payment_submission_batches (checked_by);

create index idx_payment_submission_batches_office_id on payment_submission_batches (office_id);

create index idx_payment_submission_batches_shipper_id on payment_submission_batches (shipper_id);

create index idx_payment_submission_batches_shipper_id_status_created_at on payment_submission_batches (shipper_id, status, created_at);
