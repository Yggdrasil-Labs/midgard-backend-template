ALTER TABLE `customer`
    RENAME COLUMN `created_at` TO `create_time`,
    RENAME COLUMN `updated_at` TO `update_time`,
    ADD COLUMN `create_by` VARCHAR(64) NOT NULL DEFAULT 'system',
    ADD COLUMN `update_by` VARCHAR(64) NOT NULL DEFAULT 'system',
    ADD COLUMN `version` INT NOT NULL DEFAULT 0;
