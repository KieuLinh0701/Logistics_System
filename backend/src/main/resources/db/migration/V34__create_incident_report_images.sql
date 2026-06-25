CREATE TABLE incident_report_images
(
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    incident_report_id INTEGER NOT NULL,
    image_url          LONGTEXT,

    PRIMARY KEY (id),
    CONSTRAINT fk_inc_rep_img_report FOREIGN KEY (incident_report_id) REFERENCES incident_reports (id)
) ENGINE=InnoDB;

CREATE INDEX idx_incident_report_images_incident_report_id ON incident_report_images (incident_report_id);
