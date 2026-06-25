
alter table account_roles 
       add constraint FK61h48dsir3h82pxbq3cwgp0ce 
       foreign key (account_id) 
       references accounts (id);

alter table account_roles 
       add constraint FK6r8nxkn3hctohyllteivfr5hy 
       foreign key (role_id) 
       references roles (id);

alter table addresses 
       add constraint FK1fa36y2oqhao3wgg2rw1pi459 
       foreign key (user_id) 
       references users (id);

alter table ai_route_plan_routes 
       add constraint FKknvdjad19hgcx9a3e8myll6gl 
       foreign key (plan_id) 
       references ai_route_plans (id);

alter table ai_route_plan_stops 
       add constraint FK1s3wpde1wjnxe1qa40rktuya 
       foreign key (order_id) 
       references orders (id);

alter table ai_route_plan_stops 
       add constraint FK6dt5umi6b2jpcckmq6nn95yok 
       foreign key (route_id) 
       references ai_route_plan_routes (id);

alter table ai_route_plans 
       add constraint FKryxxxfnckc02wbcoaa852bokt 
       foreign key (manager_employee_id) 
       references employees (id);

alter table ai_route_plans 
       add constraint FK6agc433wg1ucy2x7g221ly7wf 
       foreign key (office_id) 
       references offices (id);

alter table audit_logs 
       add constraint FK6mkafjngolhubhl40x6yix21 
       foreign key (office_id) 
       references offices (id);

alter table audit_logs 
       add constraint FK5j4ofa5pj7o7vd6yj4xjw4n0w 
       foreign key (shop_id) 
       references users (id);

alter table audit_logs 
       add constraint FKjs4iimve3y0xssbtve5ysyef0 
       foreign key (user_id) 
       references users (id);

alter table bank_accounts 
       add constraint fk_bankaccount_user 
       foreign key (user_id) 
       references users (id);

alter table delivery_attempts 
       add constraint FKdyxjln25f0qo6jktosxukhh30 
       foreign key (order_id) 
       references orders (id);

alter table delivery_attempts 
       add constraint FKsu60u5eynv8vw6d6n7tr18pab 
       foreign key (shipper_id) 
       references users (id);

alter table employee_leave_requests 
       add constraint FK9u2t7uwrays76ophx64tnfv3s 
       foreign key (approved_by) 
       references employees (id);

alter table employee_leave_requests 
       add constraint FKpqycjy765hq2cke0xn2vvm4xk 
       foreign key (employee_id) 
       references employees (id);

alter table employee_leave_requests 
       add constraint FKi87sm5co7dvb0cm1qxwvnrdce 
       foreign key (office_id) 
       references offices (id);

alter table employees 
       add constraint FKneh06va45lg1f84kpag1k3x4v 
       foreign key (account_role_id) 
       references account_roles (id);

alter table employees 
       add constraint FKcelobek54amw1bedldhp6f98r 
       foreign key (office_id) 
       references offices (id);

alter table employees 
       add constraint FK69x3vjuy1t5p18a5llb8h2fjx 
       foreign key (user_id) 
       references users (id);

alter table fee_configurations 
       add constraint FKii1xqipnxd395tvuwhu23fvwm 
       foreign key (service_type_id) 
       references service_types (id);

alter table incident_report_images 
       add constraint FKcilkkoutbyt3h0tpaql2tywcw 
       foreign key (incident_report_id) 
       references incident_reports (id);

alter table incident_reports 
       add constraint fk_incident_handler 
       foreign key (handled_by) 
       references users (id);

alter table incident_reports 
       add constraint FK9mr5ajf3er016qbsp4mmohrbw 
       foreign key (office_id) 
       references offices (id);

alter table incident_reports 
       add constraint fk_incident_order 
       foreign key (order_id) 
       references orders (id);

alter table incident_reports 
       add constraint fk_incident_shipper 
       foreign key (shipper_id) 
       references users (id);

alter table job_applications 
       add constraint FKfjwyy10f2yywhxflbf9trb6nb 
       foreign key (job_posting_id) 
       references job_postings (id);

alter table job_postings 
       add constraint FKpg62d6w4hexmp80ym3wmy9jyb 
       foreign key (created_by) 
       references accounts (id);

alter table job_postings 
       add constraint FKeo2yp1fq10s5oxqdyeqtu21uf 
       foreign key (office_id) 
       references offices (id);

alter table notifications 
       add constraint FKso1okvqukj2qcdtthnym6bv3v 
       foreign key (creator_id) 
       references users (id);

alter table notifications 
       add constraint FK9y21adhxn0ayjhfocscqox7bh 
       foreign key (user_id) 
       references users (id);

alter table offices 
       add constraint FKw1639yegpyf1wpqwhptew7ft 
       foreign key (manager_id) 
       references employees (id);

alter table order_histories 
       add constraint FK37wlffmoa9u22axy8juyahpqr 
       foreign key (from_office_id) 
       references offices (id);

alter table order_histories 
       add constraint FK4x7xskavxw4wtfuwmbq5fujus 
       foreign key (order_id) 
       references orders (id);

alter table order_histories 
       add constraint FKirt5cqpog260r1qjqvolrgl8t 
       foreign key (shipment_id) 
       references shipments (id);

alter table order_histories 
       add constraint FKeimdry049oxacaowppym9r7bp 
       foreign key (to_office_id) 
       references offices (id);

alter table order_products 
       add constraint FKawxpt1ns1sr7al76nvjkv21of 
       foreign key (order_id) 
       references orders (id);

alter table order_products 
       add constraint FKdxjduvg7991r4qja26fsckxv8 
       foreign key (product_id) 
       references products (id);

alter table orders 
       add constraint FK6uv8mmbch9vea454684lt30a9 
       foreign key (current_office_id) 
       references offices (id);

alter table orders 
       add constraint FKfhl8bv0xn3sj33q2f3scf1bq6 
       foreign key (employee_id) 
       references employees (id);

alter table orders 
       add constraint FK253vdaoy4l1r1kegii6bsnabp 
       foreign key (from_office_id) 
       references offices (id);

alter table orders 
       add constraint FK42bki7v5u9s62olp5is82sd74 
       foreign key (promotion_id) 
       references promotions (id);

alter table orders 
       add constraint FK8dl6ydkskdvl8fe1ufvrqg3ho 
       foreign key (recipient_address_id) 
       references addresses (id) 
       on delete set null;

alter table orders 
       add constraint FK8b9ybyyln5p1gf9hmlnkvojaf 
       foreign key (sender_address_id) 
       references addresses (id) 
       on delete set null;

alter table orders 
       add constraint FK49ej5d7m3a2qkhnygniu2rccb 
       foreign key (service_type_id) 
       references service_types (id);

alter table orders 
       add constraint FKta2aowue13qk78j0bi7g3eh9v 
       foreign key (settlement_batch_id) 
       references settlement_batches (id);

alter table orders 
       add constraint FKivm0klq0ywy0nbyxuwxy498j3 
       foreign key (to_office_id) 
       references offices (id);

alter table orders 
       add constraint FK32ql8ubntj5uh44ph9659tiih 
       foreign key (user_id) 
       references users (id);

alter table payment_submission_batches 
       add constraint FK6hf6taox2wix0wbvlsdt04i9n 
       foreign key (checked_by) 
       references users (id);

alter table payment_submission_batches 
       add constraint FKdsicwbxkqe8uiigqvajtl0d80 
       foreign key (office_id) 
       references offices (id);

alter table payment_submission_batches 
       add constraint FKsv8qr8k62riw80qeecl7arxh5 
       foreign key (shipper_id) 
       references users (id);

alter table payment_submission_items 
       add constraint FKh2vi89qvptaej6qtv0tyuwyr9 
       foreign key (order_product_id) 
       references order_products (id);

alter table payment_submission_items 
       add constraint FKfuc0md6c393tc9euya8wpjpyf 
       foreign key (payment_submission_id) 
       references payment_submissions (id);

alter table payment_submissions 
       add constraint FKbw4vyew1vt6w5oicbx5tj12rp 
       foreign key (batch_id) 
       references payment_submission_batches (id);

alter table payment_submissions 
       add constraint FKl23jck003e0j3rcmltadt1ho3 
       foreign key (checked_by) 
       references users (id);

alter table payment_submissions 
       add constraint FKqf251mlr1cb9hs65myjbjiej5 
       foreign key (order_id) 
       references orders (id);

alter table payment_submissions 
       add constraint FK3pq3f5jxgcb2f1tsejstbtwpd 
       foreign key (shipper_id) 
       references users (id);

alter table permission_group_apis 
       add constraint FKlt04ot94gw5o215y8cnchgevg 
       foreign key (api_id) 
       references permission_apis (id);

alter table permission_group_apis 
       add constraint FKpdak0o17ror1gfo6btgs1ps03 
       foreign key (group_id) 
       references permission_groups (id);

alter table permission_groups 
       add constraint FKmlwkbqbv2dw8uyqberomx6sqm 
       foreign key (module_id) 
       references permission_modules (id);

alter table permission_groups 
       add constraint FKenbh7aw9ufmvv5aql4248o6wx 
       foreign key (parent_id) 
       references permission_groups (id);

alter table pickup_attempts 
       add constraint FKo71l3t6sq0r9cs9xpoin9gc2s 
       foreign key (order_id) 
       references orders (id);

alter table pickup_attempts 
       add constraint FKf3aaa3q122ob8tro3xjcc53cx 
       foreign key (shipper_id) 
       references users (id);

alter table products 
       add constraint FKdb050tk37qryv15hd932626th 
       foreign key (user_id) 
       references users (id);

alter table promotion_service_types 
       add constraint FK7vt6fed9ik5ccld9836xxtcni 
       foreign key (service_type_id) 
       references service_types (id);

alter table promotion_service_types 
       add constraint FK3u430p88cbukchnsoiq69288b 
       foreign key (promotion_id) 
       references promotions (id);

alter table role_permission_groups 
       add constraint FKcoyqx8exjbhqwattdo42dl9it 
       foreign key (permission_group_id) 
       references permission_groups (id);

alter table role_permission_groups 
       add constraint FK9je3g8ceh3lws5u6kpvv6la2k 
       foreign key (role_id) 
       references roles (id);

alter table roles 
       add constraint FK4p096wodakgqp5i3xfvwbpbof 
       foreign key (user_owner_id) 
       references users (id);

alter table settlement_batches 
       add constraint FKt7g127g764q2tqdi8wrk3k1t 
       foreign key (shop_id) 
       references users (id);

alter table settlement_transactions 
       add constraint FKnsw0ewl5k9oaiqs34vlfni4 
       foreign key (settlement_batch_id) 
       references settlement_batches (id);

alter table shipment_orders 
       add constraint FK8v65xp1nlph59vyrdmd9f7fwi 
       foreign key (order_id) 
       references orders (id);

alter table shipment_orders 
       add constraint FKnewhq3gujbb7nvpej4d6nuhqc 
       foreign key (shipment_id) 
       references shipments (id);

alter table shipments 
       add constraint FKnfhxinwgrxp6ek6fpfd7kex67 
       foreign key (created_by) 
       references employees (id);

alter table shipments 
       add constraint FKnf7ffn66gcluluj5148ohd0n 
       foreign key (employee_id) 
       references employees (id);

alter table shipments 
       add constraint FKe6fb0qm0q9t0y3bb87j112mnr 
       foreign key (from_office_id) 
       references offices (id);

alter table shipments 
       add constraint FK8iuy7dwhsljeu63si51f1yv40 
       foreign key (to_office_id) 
       references offices (id);

alter table shipments 
       add constraint FKnajg0k0dstjtsp64u3125burs 
       foreign key (vehicle_id) 
       references vehicles (id);

alter table shipper_assignments 
       add constraint FK78rv078v3uhxl07frrbwvomhx 
       foreign key (shipper_id) 
       references users (id);

alter table shipper_vehicles 
       add constraint FKaxh3wlk4pxawfmmco4hcc44io 
       foreign key (shipper_id) 
       references employees (id);

alter table shipping_rates 
       add constraint FKig2ucfjeem3h3iuu5s1350trx 
       foreign key (service_type_id) 
       references service_types (id);

alter table shipping_request_attachments 
       add constraint FKdndqlx52jhggb9n3nrboji7mb 
       foreign key (shipping_request_id) 
       references shipping_requests (id);

alter table shipping_requests 
       add constraint FK4447fc2mfqg4b2a6tj1pyyrw0 
       foreign key (handler_id) 
       references users (id);

alter table shipping_requests 
       add constraint FK5ton92kkacquv2b0ahta5kcvr 
       foreign key (office_id) 
       references offices (id);

alter table shipping_requests 
       add constraint FK1qkclw0d2crjpwxsdbf31pkwa 
       foreign key (order_id) 
       references orders (id);

alter table shipping_requests 
       add constraint FKfqlq61na0t3kwjraojky7lvf2 
       foreign key (user_id) 
       references users (id);

alter table shop_work_histories 
       add constraint FK20nifqvjo9u0g0gnhusddkwj0 
       foreign key (role_id) 
       references roles (id);

alter table shop_work_histories 
       add constraint FK6vhbvfrgqqnjindmbt2e2ibj2 
       foreign key (shop_id) 
       references users (id);

alter table shop_work_histories 
       add constraint FKi4q8m9yvdrjid6qgjb6q9a8fw 
       foreign key (user_id) 
       references users (id);

alter table user_promotions 
       add constraint FKt48sqypmf97fs19jgx5t9lglr 
       foreign key (promotion_id) 
       references promotions (id);

alter table user_promotions 
       add constraint FKmn41clkojlmywpsgbgmltkcwu 
       foreign key (user_id) 
       references users (id);

alter table user_settlement_schedules 
       add constraint FKhshr60n0j382mno8g91aq7ie8 
       foreign key (user_id) 
       references users (id);

alter table user_settlement_weekdays 
       add constraint FKshh0h4dn467nk4x021pnlwjd 
       foreign key (schedule_id) 
       references user_settlement_schedules (id);

alter table users 
       add constraint FKfm8rm8ks0kgj4fhlmmljkj17x 
       foreign key (account_id) 
       references accounts (id);

alter table users 
       add constraint FKibk1e3kaxy5sfyeekp8hbhnim 
       foreign key (created_by) 
       references users (id);

alter table users 
       add constraint FK30n5gmaeecpanqwfm1ua4n0rn 
       foreign key (current_shop_id) 
       references users (id);

alter table vehicle_trackings 
       add constraint FKok3b3ptjc18y334h3o3gh7wu9 
       foreign key (shipment_id) 
       references shipments (id);

alter table vehicle_trackings 
       add constraint FKcabykswvn4h0lmbmx1k1heuny 
       foreign key (vehicle_id) 
       references vehicles (id);

alter table vehicles 
       add constraint FK2uofoxrng4ep85432v1mdq17w 
       foreign key (office_id) 
       references offices (id);
