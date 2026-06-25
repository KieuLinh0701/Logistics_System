create table employees (
        account_role_id integer not null,
        id integer not null auto_increment,
        office_id integer not null,
        user_id integer,
        created_at datetime(6) not null,
        hire_date datetime(6) not null,
        updated_at datetime(6),
        code varchar(50),
        shift enum ('AFTERNOON','EVENING','FULL_DAY','MORNING') not null,
        status enum ('ACTIVE','INACTIVE','LEAVE') not null,
        primary key (id)
    ) engine=InnoDB;

alter table employees 
       add constraint UK3um79qgwg340lpaw7phtwudtc unique (code);

create index idx_employees_account_role_id on employees (account_role_id);

create index idx_employees_office_id on employees (office_id);

create index idx_employees_user_id on employees (user_id);
