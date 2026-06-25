create table internal_chat_rooms (
        employee_account_id integer not null,
        id integer not null auto_increment,
        last_sender_account_id integer,
        manager_account_id integer not null,
        office_id integer not null,
        created_at datetime(6) not null,
        last_message_at datetime(6),
        updated_at datetime(6),
        employee_role varchar(20) not null,
        employee_name varchar(100) not null,
        manager_name varchar(100),
        office_name varchar(255) not null,
        last_message TEXT,
        primary key (id)
    ) engine=InnoDB;

create index idx_internal_chat_rooms_employee_account_id_manager_7749b48b on internal_chat_rooms (employee_account_id, manager_account_id);
