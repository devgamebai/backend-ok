-- Fix bundle_id overflow: INT max (2.1B) < current unix millis (~1.7T)
ALTER TABLE gift_codes MODIFY COLUMN bundle_id BIGINT DEFAULT NULL;
