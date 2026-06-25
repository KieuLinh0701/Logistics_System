create table internal_chat_messages (
        id integer not null auto_increment,
        is_read bit not null,
        room_id integer not null,
        sender_account_id integer not null,
        created_at datetime(6) not null,
        message_type varchar(20) not null,
        sender_role varchar(20) not null,
        sender_name varchar(100) not null,
        image_url varchar(500),
        message TEXT not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_internal_chat_messages_room_id 
       on internal_chat_messages (room_id);

create index idx_internal_chat_messages_created_at 
       on internal_chat_messages (created_at);
