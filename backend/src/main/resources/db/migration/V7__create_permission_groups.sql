create table permission_groups (
        id integer not null auto_increment,
        is_active bit not null,
        is_system_only bit not null,
        module_id integer not null,
        parent_id integer,
        sort_order integer not null,
        created_at datetime(6),
        updated_at datetime(6),
        code varchar(50) not null,
        name varchar(100) not null,
        description varchar(255),
        primary key (id)
    ) engine=InnoDB;

alter table permission_groups 
       add constraint UK4qmruvdnu8a7d83p0ux7j7v81 unique (code);

create index idx_permission_groups_module_id on permission_groups (module_id);

create index idx_permission_groups_parent_id on permission_groups (parent_id);
