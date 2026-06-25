create table bank_accounts (
        id integer not null auto_increment,
        is_default bit not null,
        user_id integer not null,
        created_at datetime(6),
        updated_at datetime(6),
        account_number varchar(50) not null,
        account_name NVARCHAR(100) not null,
        bank_name NVARCHAR(100) not null,
        notes NVARCHAR(500) not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_bank_accounts_user_id on bank_accounts (user_id);
