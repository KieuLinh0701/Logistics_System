create table job_postings (
        created_by integer not null,
        office_id integer not null,
        quantity_needed integer,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        description TEXT not null,
        title varchar(255) not null,
        role_type enum ('DRIVER','SHIPPER') not null,
        shift enum ('AFTERNOON','EVENING','FULL_DAY','MORNING'),
        status enum ('CLOSED','OPEN') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_job_postings_created_by on job_postings (created_by);

create index idx_job_postings_office_id on job_postings (office_id);
