create table account_roles (
        account_id integer not null,
        id integer not null auto_increment,
        is_active bit not null,
        role_id integer not null,
        primary key (id)
    ) engine=InnoDB;

alter table account_roles 
       add constraint UK8phc5fdatfmofp1k5lj4rfqc6 unique (account_id, role_id);

create index idx_account_roles_account_id on account_roles (account_id);

create index idx_account_roles_role_id on account_roles (role_id);
