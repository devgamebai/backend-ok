-- Replace bank names with Korean (한국어) names per user requirement.
-- Backup saved at install/config/mysql/db/banks_backup_20260406.sql

DELETE FROM vinplay.banks;
ALTER TABLE vinplay.banks AUTO_INCREMENT = 1;

INSERT INTO vinplay.banks (bank_name, code, logo, status, add_by, create_date, update_date) VALUES
  ('농협',                    'NH',       '', 1, 'system', NOW(), NOW()),
  ('카카오뱅크',               'KAKAO',    '', 1, 'system', NOW(), NOW()),
  ('신협',                    'SHINHYUP', '', 1, 'system', NOW(), NOW()),
  ('기업은행',                 'IBK',      '', 1, 'system', NOW(), NOW()),
  ('경남은행',                 'KYONGNAM', '', 1, 'system', NOW(), NOW()),
  ('우리은행',                 'WOORI',    '', 1, 'system', NOW(), NOW()),
  ('토스뱅크',                 'TOSS',     '', 1, 'system', NOW(), NOW()),
  ('수협',                    'SUHYUP',   '', 1, 'system', NOW(), NOW()),
  ('하나은행',                 'HANA',     '', 1, 'system', NOW(), NOW()),
  ('케이뱅크',                 'KBANK',    '', 1, 'system', NOW(), NOW()),
  ('국민은행',                 'KB',       '', 1, 'system', NOW(), NOW()),
  ('제주은행',                 'JEJU',     '', 1, 'system', NOW(), NOW()),
  ('새마을금고',               'MG',       '', 1, 'system', NOW(), NOW()),
  ('신한은행',                 'SHINHAN',  '', 1, 'system', NOW(), NOW()),
  ('우체국',                   'POST',     '', 1, 'system', NOW(), NOW()),
  ('부산은행',                 'BUSAN',    '', 1, 'system', NOW(), NOW()),
  ('SC제일은행',               'SC',       '', 1, 'system', NOW(), NOW()),
  ('전북은행',                 'JEONBUK',  '', 1, 'system', NOW(), NOW()),
  ('광주은행',                 'GWANGJU',  '', 1, 'system', NOW(), NOW()),
  ('저축은행',                 'SAVINGS',  '', 1, 'system', NOW(), NOW()),
  ('대구은행 (IM뱅크)',         'DAEGU',    '', 1, 'system', NOW(), NOW()),
  ('외환은행',                 'KEB',      '', 1, 'system', NOW(), NOW()),
  ('산업은행',                 'KDB',      '', 1, 'system', NOW(), NOW()),
  ('씨티은행',                 'CITI',     '', 1, 'system', NOW(), NOW()),
  ('산림조합중앙회',            'NFCF',     '', 1, 'system', NOW(), NOW());
