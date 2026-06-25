create table shipping_request_attachments (
        id integer not null auto_increment,
        shipping_request_id integer not null,
        created_at datetime(6) not null,
        url varchar(500) not null,
        file_name varchar(255) not null,
        type enum ('REQUEST','RESPONSE') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_shipping_request_attachments_shipping_request_id on shipping_request_attachments (shipping_request_id);
