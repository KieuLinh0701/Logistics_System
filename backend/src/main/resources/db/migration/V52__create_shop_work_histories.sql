create table shop_work_histories (
        id integer not null auto_increment,
        is_current bit not null,
        role_id integer,
        shop_id integer not null,
        user_id integer not null,
        created_at datetime(6) not null,
        joined_at datetime(6) not null,
        left_at datetime(6),
        updated_at datetime(6),
        note NVARCHAR(255),
        primary key (id)
    ) engine=InnoDB;

create index idx_shop_work_histories_role_id on shop_work_histories (role_id);

create index idx_shop_work_histories_shop_id on shop_work_histories (shop_id);

create index idx_shop_work_histories_user_id on shop_work_histories (user_id);
