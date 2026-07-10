
ALTER TABLE pickup_attempts ADD COLUMN proof_image_url VARCHAR(500) NULL;

ALTER TABLE delivery_attempts ADD COLUMN attempt_type VARCHAR(30) NULL;

UPDATE delivery_attempts SET attempt_type = 'DELIVERY' WHERE attempt_type IS NULL;

ALTER TABLE delivery_attempts MODIFY COLUMN attempt_type VARCHAR(30) NOT NULL;
