create table settlement_transactions (
        amount decimal(18,2) not null,
        id integer not null auto_increment,
        settlement_batch_id integer not null,
        created_at datetime(6) not null,
        paid_at datetime(6),
        account_number varchar(50),
        code varchar(50),
        account_name NVARCHAR(100),
        bank_name NVARCHAR(100),
        reference_code varchar(255),
        status enum ('FAILED','PENDING','SUCCESS') not null,
        type enum ('SHOP_TO_SYSTEM','SYSTEM_TO_SHOP') not null,
        primary key (id)
    ) engine=InnoDB;

alter table settlement_transactions 
       add constraint UKrff3mtowidvtutabo88mvcjfm unique (code);

create index idx_settlement_transactions_settlement_batch_id on settlement_transactions (settlement_batch_id);
