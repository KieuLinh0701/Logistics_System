
ALTER TABLE orders
ADD COLUMN pickup_proof_image_url VARCHAR(500) NULL;

ALTER TABLE delivery_attempts
ADD COLUMN proof_image_url VARCHAR(500) NULL;