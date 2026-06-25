create table support_messages (
        id integer not null auto_increment,
        is_internal_note bit not null,
        is_read bit,
        sender_account_id integer not null,
        ticket_id integer not null,
        created_at datetime(6) not null,
        image_url varchar(500),
        message TEXT not null,
        message_type enum ('IMAGE','SYSTEM','TEXT') not null,
        sender_type enum ('ADMIN','BOT','MANAGER','SYSTEM','USER') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_support_messages_ticket_id_created_at on support_messages (ticket_id, created_at);
