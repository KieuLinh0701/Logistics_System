create table permission_modules (
        id integer not null auto_increment,
        is_active bit not null,
        is_system_only bit not null,
        sort_order integer not null,
        created_at datetime(6),
        updated_at datetime(6),
        name varchar(100) not null,
        description varchar(255),
        primary key (id)
    ) engine=InnoDB;
