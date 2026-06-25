create table shipping_requests (
        contact_city_code integer,
        contact_ward_code integer,
        handler_id integer,
        id integer not null auto_increment,
        office_id integer,
        order_id integer,
        user_id integer,
        paid_at datetime(6),
        response_at datetime(6),
        updated_at datetime(6),
        contact_phone_number varchar(20),
        handler_phone_number varchar(20),
        code varchar(50),
        contact_email varchar(100),
        handler_email varchar(100),
        contact_city_name NVARCHAR(255),
        contact_detail NVARCHAR(255),
        contact_full_address NVARCHAR(255),
        contact_name NVARCHAR(100),
        contact_ward_name NVARCHAR(255),
        handler_name NVARCHAR(100),
        request_content TEXT,
        response TEXT,
        request_type enum ('CHANGE_ORDER_INFO','COMPLAINT','DELIVERY_REMINDER','INQUIRY','PICKUP_REMINDER') not null,
        status enum ('CANCELLED','PENDING','PROCESSING','REJECTED','RESOLVED') not null,
        primary key (id)
    ) engine=InnoDB;

alter table shipping_requests 
       add constraint UKq3ps2wm47cooe9pbhgwjs2uet unique (code);

create index idx_shipping_requests_handler_id on shipping_requests (handler_id);

create index idx_shipping_requests_office_id on shipping_requests (office_id);

create index idx_shipping_requests_order_id on shipping_requests (order_id);

create index idx_shipping_requests_user_id on shipping_requests (user_id);

create index idx_shipping_requests_user_id_status_updated_at on shipping_requests (user_id, status, updated_at);
