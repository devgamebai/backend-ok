-- Agent referral code request/approval workflow (vanity codes)
-- See docs/AGENT_REFERRAL_CODE_SPEC.md

USE vinplay_admin;

-- Queue of agent code requests
CREATE TABLE IF NOT EXISTS agent_code_request (
  id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  agent_id        INT          NOT NULL,
  agent_nickname  VARCHAR(50)  NOT NULL,
  desired_code    VARCHAR(20)  NOT NULL,
  normalized_code VARCHAR(20)  NOT NULL,
  status          ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  reviewed_by     VARCHAR(64)  NULL,
  reviewed_at     DATETIME     NULL,
  reject_reason   VARCHAR(255) NULL,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_agent (agent_id, created_at),
  KEY idx_status (status, created_at),
  KEY idx_normalized (normalized_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reserved/blacklist codes
CREATE TABLE IF NOT EXISTS agent_code_reserved (
  code_pattern VARCHAR(50)  NOT NULL PRIMARY KEY,
  reason       VARCHAR(255) NOT NULL,
  created_by   VARCHAR(64)  NOT NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO agent_code_reserved (code_pattern, reason, created_by) VALUES
  ('ADMIN',    'system reserved', 'system'),
  ('ROOT',     'system reserved', 'system'),
  ('SYSTEM',   'system reserved', 'system'),
  ('SUPPORT',  'system reserved', 'system'),
  ('STAFF',    'system reserved', 'system'),
  ('OFFICIAL', 'system reserved', 'system'),
  ('TEST',     'system reserved', 'system'),
  ('DEMO',     'system reserved', 'system'),
  ('SUNKR',    'brand reserved',  'system'),
  ('SUNWIN',   'brand reserved',  'system'),
  ('RIK888',   'brand reserved',  'system'),
  ('B52',      'brand reserved',  'system'),
  ('GO88',     'brand reserved',  'system');

-- Enforce uniqueness on useragent.code (safe: 0 duplicates verified)
-- Drop old non-unique idx_code first if present, then add unique constraint.
ALTER TABLE useragent DROP INDEX idx_code;
ALTER TABLE useragent ADD UNIQUE KEY uk_useragent_code (code);
