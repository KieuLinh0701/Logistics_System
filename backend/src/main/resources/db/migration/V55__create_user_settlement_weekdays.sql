CREATE TABLE user_settlement_weekdays
(
    schedule_id INTEGER NOT NULL,
    weekday     ENUM ('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') NOT NULL,

    PRIMARY KEY (schedule_id, weekday),

    CONSTRAINT fk_settlement_weekdays_schedule FOREIGN KEY (schedule_id) REFERENCES user_settlement_schedules (id)
) ENGINE=InnoDB;

CREATE INDEX idx_user_settlement_weekdays_schedule_id ON user_settlement_weekdays (schedule_id);
