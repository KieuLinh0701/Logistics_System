create table permission_group_apis (
        api_id integer not null,
        group_id integer not null,
        created_at datetime(6),
        primary key (api_id, group_id)
    ) engine=InnoDB;

create index idx_permission_group_apis_api_id on permission_group_apis (api_id);

create index idx_permission_group_apis_group_id on permission_group_apis (group_id);
