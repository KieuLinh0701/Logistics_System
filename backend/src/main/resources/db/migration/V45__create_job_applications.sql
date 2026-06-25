create table job_applications (
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        job_posting_id bigint not null,
        phone varchar(20) not null,
        email varchar(150) not null,
        full_name varchar(150) not null,
        cv_url varchar(500) not null,
        address NVARCHAR(255) not null,
        status enum ('APPROVED','PENDING','REJECTED','REVIEWING') not null,
        primary key (id)
    ) engine=InnoDB;

create index idx_job_applications_job_posting_id on job_applications (job_posting_id);
