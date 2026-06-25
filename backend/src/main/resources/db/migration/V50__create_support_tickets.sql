create table support_tickets (
        assigned_to_account_id integer,
        closed_by_account_id integer,
        created_by_account_id integer not null,
        id integer not null auto_increment,
        office_id integer,
        related_id integer,
        closed_at datetime(6),
        created_at datetime(6) not null,
        updated_at datetime(6),
        code varchar(20) not null,
        priority varchar(20),
        closed_by_name varchar(255),
        related_type varchar(255),
        subject varchar(255),
        status enum ('ASSIGNED','CLOSED','OPEN','PENDING','RESOLVED'),
        primary key (id)
    ) engine=InnoDB;

alter table support_tickets 
       add constraint UKgje7e44b8p2kc7w8nqeldvmmj unique (code);

create index idx_support_tickets_created_by_account_id_status on support_tickets (created_by_account_id, status);

create index idx_support_tickets_assigned_to_account_id_status on support_tickets (assigned_to_account_id, status);
