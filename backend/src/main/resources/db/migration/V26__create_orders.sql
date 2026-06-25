create table orders (
        actual_collected decimal(19,2),
        adjusted_height decimal(10,2),
        adjusted_length decimal(10,2),
        adjusted_original_weight decimal(10,2),
        adjusted_weight decimal(10,2),
        adjusted_width decimal(10,2),
        cod integer not null,
        current_office_id integer,
        discount_amount integer not null,
        employee_id integer,
        from_office_id integer,
        height decimal(10,2),
        id integer not null auto_increment,
        length decimal(10,2),
        order_value integer not null,
        original_weight decimal(10,2),
        pending_destination_confirm bit not null,
        promotion_id integer,
        recipient_address_id integer,
        recipient_city_code integer not null,
        recipient_latitude float(53) not null,
        recipient_longitude float(53) not null,
        recipient_ward_code integer not null,
        returned_amount decimal(19,2),
        sender_address_id integer,
        sender_city_code integer not null,
        sender_latitude float(53) not null,
        sender_longitude float(53) not null,
        sender_ward_code integer not null,
        service_type_id integer not null,
        settlement_batch_id integer,
        shipping_fee integer not null,
        to_office_id integer,
        total_fee integer not null,
        user_id integer,
        weight decimal(10,2) not null,
        width decimal(10,2),
        created_at datetime(6),
        delivered_at datetime(6),
        paid_at datetime(6),
        ready_for_pickup_at datetime(6),
        returned_at datetime(6),
        updated_at datetime(6),
        version BIGINT NOT NULL DEFAULT 0 not null,
        tracking_number varchar(50),
        notes NVARCHAR(1000),
        recipient_city_name NVARCHAR(255) not null,
        recipient_detail NVARCHAR(255) not null,
        recipient_full_address NVARCHAR(255) not null,
        recipient_name NVARCHAR(255) not null,
        recipient_phone varchar(255) not null,
        recipient_ward_name NVARCHAR(255) not null,
        sender_city_name NVARCHAR(255) not null,
        sender_detail NVARCHAR(255) not null,
        sender_full_address NVARCHAR(255) not null,
        sender_name NVARCHAR(255) not null,
        sender_phone varchar(255) not null,
        sender_ward_name NVARCHAR(255) not null,
        cod_status enum ('EXPECTED','NONE','PENDING','RECEIVED','SUBMITTED','TRANSFERRED') not null,
        created_by_type enum ('ADMIN','MANAGER','USER') not null,
        payer enum ('CUSTOMER','SHOP') not null,
        payment_status enum ('PAID','REFUNDED','UNPAID') not null,
        pickup_notification_stage enum ('NONE','STAGE_1','STAGE_2','URGENT') not null,
        pickup_type enum ('AT_OFFICE','PICKUP_BY_COURIER') not null,
        status enum ('AT_DEST_OFFICE','AT_ORIGIN_OFFICE','CANCELLED','CONFIRMED','DELIVERED','DELIVERING','DELIVERY_FAILED_FINAL','DELIVERY_RETRY','DRAFT','IN_TRANSIT','PARTIAL_DELIVERY','PARTIAL_RETURN','PENDING','PICKED_UP','PICKING_UP','PICKUP_FAILED_FINAL','PICKUP_RETRY','READY_FOR_PICKUP','RETURNED','RETURNING','RETURN_AT_ORIGIN_OFFICE','RETURN_FAILED_FINAL','RETURN_RETRY','TRANSIT_TO_OFFICE','URGENT_PICKUP') not null,
        primary key (id)
    ) engine=InnoDB;

alter table orders 
       add constraint UKnew938pg97mqegt6j0irfoimc unique (tracking_number);

create index idx_orders_current_office_id on orders (current_office_id);

create index idx_orders_employee_id on orders (employee_id);

create index idx_orders_from_office_id on orders (from_office_id);

create index idx_orders_promotion_id on orders (promotion_id);

create index idx_orders_recipient_address_id on orders (recipient_address_id);

create index idx_orders_sender_address_id on orders (sender_address_id);

create index idx_orders_service_type_id on orders (service_type_id);

create index idx_orders_settlement_batch_id on orders (settlement_batch_id);

create index idx_orders_to_office_id on orders (to_office_id);

create index idx_orders_user_id on orders (user_id);

create index idx_orders_user_id_status_created_at on orders (user_id, status, created_at);

create index idx_orders_current_office_id_status on orders (current_office_id, status);

create index idx_orders_employee_id_status on orders (employee_id, status);

create index idx_orders_settlement_batch_id_status on orders (settlement_batch_id, status);
