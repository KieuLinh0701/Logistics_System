create table users (
        account_id integer,
        created_by integer,
        current_shop_id integer,
        id integer not null auto_increment,
        locked bit not null,
        created_at datetime(6) not null,
        updated_at datetime(6),
        phone_number varchar(15) not null,
        code varchar(50),
        first_name NVARCHAR(50) not null,
        images varchar(255),
        last_name NVARCHAR(50) not null,
        primary key (id)
    ) engine=InnoDB;

alter table users 
       add constraint UK1yov8c5ew74vlt8qra6cewq99 unique (account_id);

alter table users 
       add constraint UK9q63snka3mdh91as4io72espi unique (phone_number);

alter table users 
       add constraint UK71vrxovabe8x9tom8xwefi3e7 unique (code);

create index idx_users_created_by on users (created_by);

create index idx_users_current_shop_id on users (current_shop_id);

create index idx_users_created_at on users (created_at);
