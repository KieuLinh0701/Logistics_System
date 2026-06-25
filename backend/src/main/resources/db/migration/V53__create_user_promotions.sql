create table user_promotions (
        assigned_by integer,
        id integer not null auto_increment,
        promotion_id integer not null,
        used_count integer not null,
        user_id integer not null,
        created_at datetime(6),
        updated_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

create index idx_user_promotions_promotion_id on user_promotions (promotion_id);

create index idx_user_promotions_user_id on user_promotions (user_id);
