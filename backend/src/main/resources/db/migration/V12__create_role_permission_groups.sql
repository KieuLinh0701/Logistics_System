create table role_permission_groups (
        permission_group_id integer not null,
        role_id integer not null,
        primary key (permission_group_id, role_id)
    ) engine=InnoDB;

create index idx_role_permission_groups_permission_group_id on role_permission_groups (permission_group_id);

create index idx_role_permission_groups_role_id on role_permission_groups (role_id);
