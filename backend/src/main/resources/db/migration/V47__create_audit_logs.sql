create table audit_logs (
        id integer not null auto_increment,
        office_id integer,
        shop_id integer,
        user_id integer not null,
        created_at datetime(6) not null,
        description NVARCHAR(500),
        entity_id varchar(255),
        error_message LONGTEXT,
        action enum ('APPROVE','CANCEL','CONFIRM','CREATE','DELETE','EXPORT','IMPORT','LOGIN','PASSWORD_RESET','PAY','PRINT','PROCESS','REGISTER','REJECT','UPDATE','UPDATE_STATUS') not null,
        entity enum ('ACCOUNT','ACCOUNT_ROLE','ADDRESS','API_ROUTE_PLAN','API_ROUTE_PLAN_ROUT','API_ROUTE_PLAN_STOP','AUDIT_LOG','BANK_ACCOUNT','DELIVERY_ATTEMPT','EMPLOYEE','EMPLOYEE_LEAVE_REQUEST','FEEDBACK','FEE_CONFIGURATION','INCIDENT_REPORT','INTERNAL_CHAT_MESSAGE','INTERNAL_CHAT_ROOM','JOB_APPLICATION','JOB_POSTING','NOTIFICATION','OFFICE','ORDER','ORDER_HISTORY','ORDER_PRODUCT','OTP','PAYMENT_SUBMISSION','PAYMENT_SUBMISSION_BATCH','PAYMENT_SUBMISSION_ITEM','PERMISSION_API','PERMISSION_GROUP','PERMISSION_GROUP_API','PERMISSION_MODULE','PICKUP_ATTEMPT','PRODUCT','PROMOTION','REGION','ROLE','SERVICE_TYPE','SETTLEMENT_BATCH','SETTLEMENT_TRANSACTION','SHIPMENT','SHIPMENT_ORDER','SHIPPER_ASSIGNMENT','SHIPPING_RATE','SHIPPING_REQUEST','SHIPPING_REQUEST_ATTACHMENT','SHOP_WORK_HISTORY','SUPPORT_MESSAGE','SUPPORT_TICKET','SYSTEM_CONFIG','USER','USER_PROMOTION','USER_SETTLEMENT_SCHEDULE','VEHICLE','VEHICLE_TRACKING') not null,
        payload_request_body LONGTEXT,
        payload_result LONGTEXT,
        status enum ('FAILED','FORBIDDEN','SUCCESS') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_audit_user_id 
       on audit_logs (user_id);

create index idx_audit_office_id 
       on audit_logs (office_id);

create index idx_audit_shop_id 
       on audit_logs (shop_id);

create index idx_audit_entity
    on audit_logs (entity, entity_id);

create index idx_audit_created_at 
       on audit_logs (created_at);

create index idx_audit_action 
       on audit_logs (action);
