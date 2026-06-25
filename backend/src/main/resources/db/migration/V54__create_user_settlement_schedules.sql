create table user_settlement_schedules (
        id integer not null auto_increment,
        user_id integer not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_user_settlement_schedules_user_id on user_settlement_schedules (user_id);
