ALTER TABLE payment_submission_batches
MODIFY COLUMN status ENUM('OPEN', 'COMPLETED', 'PROCESSING') NOT NULL;

UPDATE payment_submission_batches
SET status = 'COMPLETED'
WHERE status IS NULL
  AND checked_by IS NOT NULL;

UPDATE payment_submission_batches
SET status = 'OPEN'
WHERE status IS NULL;