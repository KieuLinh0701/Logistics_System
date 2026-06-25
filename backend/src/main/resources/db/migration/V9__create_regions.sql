create table regions (
        code_city integer not null,
        id integer not null auto_increment,
        created_at datetime(6),
        updated_at datetime(6),
        region_name NVARCHAR(10) not null,
        primary key (id)
    ) engine=InnoDB;

alter table regions 
       add constraint UKqdbpdoqxcxofuuy7syc4d4gk8 unique (code_city);
