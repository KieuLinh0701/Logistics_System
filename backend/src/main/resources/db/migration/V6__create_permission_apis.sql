create table permission_apis (
        id integer not null auto_increment,
        is_active bit not null,
        is_ui_selectable bit not null,
        created_at datetime(6),
        updated_at datetime(6),
        method varchar(10) not null,
        name varchar(100) not null,
        url varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

alter table permission_apis 
       add constraint uq_url_method unique (url, method);
