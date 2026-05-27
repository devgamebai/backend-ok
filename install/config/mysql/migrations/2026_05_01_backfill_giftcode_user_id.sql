-- Migration: Backfill user_id trong gift_code_useds cho dữ liệu cũ
-- Run once trên cả LOCAL và STAGING
-- 2026-05-01 — Liên quan: CryptoDepositHistoryProcessor (c=3022) giftcode history

UPDATE gift_code_useds gcu
JOIN users u ON u.nick_name = gcu.username
SET gcu.user_id = u.id
WHERE gcu.user_id IS NULL;
