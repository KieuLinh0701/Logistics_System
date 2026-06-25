create table accounts (
        id integer not null auto_increment,
        is_active bit not null,
        is_verified bit not null,
        created_at datetime(6) not null,
        last_login_at datetime(6),
        updated_at datetime(6),
        email varchar(100) not null,
        password varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

alter table accounts 
       add constraint UKn7ihswpy07ci568w34q0oi8he unique (email);

create index idx_accounts_created_at on accounts (created_at);
