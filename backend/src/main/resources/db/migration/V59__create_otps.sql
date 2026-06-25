create table otps (
        id integer not null auto_increment,
        is_used bit not null,
        expires_at datetime(6) not null,
        email varchar(255) not null,
        otp varchar(255) not null,
        type enum ('REGISTER','RESET','UPDATE_EMAIL') not null,
        primary key (id)
    ) engine=InnoDB;
