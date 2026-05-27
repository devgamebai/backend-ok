-- Phase 3c prep — seed baseline RTP config for the 15 remaining slot rooms
-- that don't have a row in game_rtp_config yet. All set to 92% (matches
-- BALANCED_WIN_RATE hardcoded baseline) — no behavior change at seed time.
--
-- After this migration, admin can tune each slot individually via
-- c=9771 UpdateGameRtpConfig. Engine wiring will be done per-room
-- in SUN-589 (Phase 3c).

INSERT INTO vinplay.game_rtp_config (game_code, win_rate_pct, status, updated_by, note)
VALUES
  ('slot_audition',   92.00, 1, 'system', 'baseline — AuditionRoom'),
  ('slot_avenger',    92.00, 1, 'system', 'baseline — AvengerRoom'),
  ('slot_benley',     92.00, 1, 'system', 'baseline — BenleyRoom'),
  ('slot_bikini',     92.00, 1, 'system', 'baseline — BikiniRoom'),
  ('slot_candy',      92.00, 1, 'system', 'baseline — CandyRoom'),
  ('slot_chiemtinh',  92.00, 1, 'system', 'baseline — ChiemTinhRoom (Pirate King)'),
  ('slot_maybach',    92.00, 1, 'system', 'baseline — MayBachRoom'),
  ('slot_rangerover', 92.00, 1, 'system', 'baseline — RangeRoverRoom'),
  ('slot_rollroy',    92.00, 1, 'system', 'baseline — RollRoyRoom'),
  ('slot_generic',    92.00, 1, 'system', 'baseline — SlotRoom (generic)'),
  ('slot_spartan',    92.00, 1, 'system', 'baseline — SpartanRoom'),
  ('slot_tamhung',    92.00, 1, 'system', 'baseline — TamHungRoom'),
  ('slot_thanbai',    92.00, 1, 'system', 'baseline — ThanBaiRoom'),
  ('slot_thanden',    92.00, 1, 'system', 'baseline — ThanDenRoom'),
  ('slot_thantai',    92.00, 1, 'system', 'baseline — ThanTaiRoom (uppercase variant)')
ON DUPLICATE KEY UPDATE
  note = VALUES(note);
