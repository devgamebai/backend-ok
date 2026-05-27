-- Hierarchy Redesign Migration (2026-04-08)
-- SUN-704/705: Fix agent hierarchy model
-- Adds path_ancestors for indexed subtree queries, fixes data, updates triggers

-- Step 1: Add path_ancestors column
ALTER TABLE vinplay_admin.useragent ADD COLUMN path_ancestors VARCHAR(255) DEFAULT '/' AFTER ancestors;
CREATE INDEX idx_path_ancestors ON vinplay_admin.useragent(path_ancestors(64));

-- Step 2: Fix incorrect parentid (agents that should be under LeeTDL_0)
UPDATE vinplay_admin.useragent SET parentid = 128 WHERE id IN (133, 138);

-- Step 3: Backfill path_ancestors from corrected hierarchy
WITH RECURSIVE agent_tree AS (
  SELECT id, parentid, CAST('/' AS CHAR(255)) AS computed_path
  FROM vinplay_admin.useragent WHERE parentid IS NULL OR parentid <= 0
  UNION ALL
  SELECT c.id, c.parentid, CONCAT(p.computed_path, p.id, '/')
  FROM vinplay_admin.useragent c JOIN agent_tree p ON c.parentid = p.id
)
UPDATE vinplay_admin.useragent u JOIN agent_tree t ON u.id = t.id SET u.path_ancestors = t.computed_path;

-- Step 4: Update triggers to maintain path_ancestors
DROP TRIGGER IF EXISTS vinplay_admin.tg_before_useragent_insert;
DELIMITER //
CREATE TRIGGER vinplay_admin.tg_before_useragent_insert BEFORE INSERT ON vinplay_admin.useragent FOR EACH ROW
BEGIN
  DECLARE p_ancestors_old VARCHAR(255);
  DECLARE p_path VARCHAR(255);
  IF NEW.parentid IS NOT NULL AND NEW.parentid > 0 THEN
    SELECT IFNULL(ancestors,''), IFNULL(path_ancestors,'/') INTO p_ancestors_old, p_path FROM useragent WHERE id = NEW.parentid;
    IF p_ancestors_old = '' THEN SET NEW.ancestors = CAST(NEW.parentid AS CHAR);
    ELSE SET NEW.ancestors = CONCAT(p_ancestors_old, ',', NEW.parentid); END IF;
    SET NEW.path_ancestors = CONCAT(p_path, NEW.parentid, '/');
    SET NEW.level = LENGTH(NEW.path_ancestors) - LENGTH(REPLACE(NEW.path_ancestors, '/', ''));
  ELSE
    SET NEW.ancestors = ''; SET NEW.path_ancestors = '/'; SET NEW.level = 1;
  END IF;
END//
DELIMITER ;

DROP TRIGGER IF EXISTS vinplay_admin.tg_before_useragent_update;
DELIMITER //
CREATE TRIGGER vinplay_admin.tg_before_useragent_update BEFORE UPDATE ON vinplay_admin.useragent FOR EACH ROW
BEGIN
  DECLARE p_ancestors_old VARCHAR(255);
  DECLARE p_path VARCHAR(255);
  IF NEW.parentid IS NOT NULL AND NEW.parentid > 0 THEN
    SELECT IFNULL(ancestors,''), IFNULL(path_ancestors,'/') INTO p_ancestors_old, p_path FROM useragent WHERE id = NEW.parentid;
    IF p_ancestors_old = '' THEN SET NEW.ancestors = CAST(NEW.parentid AS CHAR);
    ELSE SET NEW.ancestors = CONCAT(p_ancestors_old, ',', NEW.parentid); END IF;
    SET NEW.path_ancestors = CONCAT(p_path, NEW.parentid, '/');
    SET NEW.level = LENGTH(NEW.path_ancestors) - LENGTH(REPLACE(NEW.path_ancestors, '/', ''));
  ELSE
    SET NEW.ancestors = ''; SET NEW.path_ancestors = '/'; SET NEW.level = 1;
  END IF;
END//
DELIMITER ;
