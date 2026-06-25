create table notifications (
        creator_id integer,
        id integer not null auto_increment,
        is_read bit not null,
        user_id integer,
        created_at datetime(6),
        updated_at datetime(6),
        message VARCHAR(500),
        related_id varchar(255),
        related_type varchar(255),
        title NVARCHAR(255) not null,
        type varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_notifications_creator_id on notifications (creator_id);

create index idx_notifications_user_id on notifications (user_id);

create index idx_notifications_user_id_created_at on notifications (user_id, created_at);
