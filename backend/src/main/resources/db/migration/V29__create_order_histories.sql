create table order_histories (
        from_office_id integer,
        id integer not null auto_increment,
        order_id integer not null,
        shipment_id integer,
        to_office_id integer,
        action_time datetime(6),
        action enum ('AT_DEST_OFFICE','CANCELLED','CONFIRMED','DELIVERED','DELIVERING','DELIVERY_FAILED_FINAL','DELIVERY_RETRY','EXPORTED','IMPORTED','PARTIAL_DELIVERY','PARTIAL_RETURN','PENDING','PICKED_UP','PICKING_UP','PICKUP_FAILED_FINAL','READY_FOR_PICKUP','RETURNED','RETURNING','RETURN_AT_ORIGIN_OFFICE','RETURN_FAILED_FINAL','RETURN_RETRY','TRANSIT_TO_OFFICE','URGENT_PICKUP') not null,
        note longtext,
        primary key (id)
    ) engine=InnoDB;

create index idx_order_histories_from_office_id on order_histories (from_office_id);

create index idx_order_histories_order_id on order_histories (order_id);

create index idx_order_histories_shipment_id on order_histories (shipment_id);

create index idx_order_histories_to_office_id on order_histories (to_office_id);
