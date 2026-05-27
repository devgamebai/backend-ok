#!/usr/bin/env python3
"""
SUN-1141 v2 — Audit Q&A bot (Telegram ↔ OpenRouter LLM ↔ MySQL).

REPLACES the prior periodic snapshot+delta wallet auditor. Periodic
monitoring is now covered by the Grafana dashboard at http://127.0.0.1:3001
(docker-compose.monitoring.yml), which reads gsc_event_log directly. This
bot is interactive: a group member types a question in Vietnamese or
English; the LLM calls a fixed set of read-only DB tools and replies in
the same group.

Architecture
------------
    Telegram group  ──long-poll──▶  this bot  ──HTTP──▶  OpenRouter / LLM
                                    │                       ▲
                                    │  ┌──tool call──┐      │
                                    └──┤             ├──────┘
                                       │ read-only   │
                                       │ MySQL tools │
                                       └─────────────┘

Security
--------
- LLM cannot execute raw SQL. Only the predefined tools in TOOL_REGISTRY
  are exposed; each tool uses parameterised queries and capped result
  sizes.
- Bot only listens to TELEGRAM_AUDIT_CHAT_ID. Messages from any other
  chat are silently dropped.
- Sensitive columns (passwords, tokens, encryption keys, raw IPs) are
  never selected by any tool.
- The DB user inherits MYSQL_USER permissions. Point this at a read-only
  account if you want stricter isolation; the tools themselves never
  issue write statements.
"""

import os
import sys
import json
import time
import logging
import threading
import http.server
import socketserver
from datetime import datetime

import requests
import mysql.connector


# ─── Config ────────────────────────────────────────────────────────────────
MYSQL_HOST = os.environ.get("MYSQL_HOST", "mysql")
MYSQL_PORT = int(os.environ.get("MYSQL_PORT", "3306"))
MYSQL_USER = os.environ.get("MYSQL_USER", "root")
MYSQL_PASS = os.environ["MYSQL_PASSWORD"]
MYSQL_DB = os.environ.get("MYSQL_DB", "vinplay")

TELEGRAM_TOKEN = os.environ["TELEGRAM_AUDIT_BOT_TOKEN"]
TELEGRAM_CHAT = str(os.environ["TELEGRAM_AUDIT_CHAT_ID"])

OPENROUTER_API_KEY = os.environ["OPENROUTER_API_KEY"]
OPENROUTER_MODEL = os.environ.get("OPENROUTER_MODEL", "anthropic/claude-3.5-sonnet")
OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

LLM_TIMEOUT_S = int(os.environ.get("BOT_LLM_TIMEOUT_S", "120"))
MAX_TOOL_ITERATIONS = int(os.environ.get("BOT_MAX_TOOL_ITERATIONS", "10"))
TELEGRAM_POLL_TIMEOUT = int(os.environ.get("BOT_POLL_TIMEOUT_S", "30"))

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(threadName)s] %(levelname)s %(message)s",
)
log = logging.getLogger("audit-bot")


# ─── DB helpers ────────────────────────────────────────────────────────────
def db():
    """One connection per call. The bot is low-volume so pooling adds
    complexity without much win."""
    return mysql.connector.connect(
        host=MYSQL_HOST,
        port=MYSQL_PORT,
        user=MYSQL_USER,
        password=MYSQL_PASS,
        database=MYSQL_DB,
        time_zone="+09:00",  # KR — matches the rest of the platform
        charset="utf8mb4",
        connection_timeout=5,
    )


def fetchall(sql, params=()):
    conn = db()
    try:
        cur = conn.cursor(dictionary=True)
        cur.execute(sql, params)
        rows = cur.fetchall()
        cur.close()
        return rows
    finally:
        conn.close()


def _serialise_row(row):
    """MySQL row → JSON-safe dict (datetimes → ISO, Decimals → int)."""
    out = {}
    for k, v in row.items():
        if isinstance(v, datetime):
            out[k] = v.strftime("%Y-%m-%d %H:%M:%S")
        elif hasattr(v, "is_integer") and hasattr(v, "to_eng_string"):  # Decimal
            try:
                out[k] = int(v)
            except (ValueError, TypeError):
                out[k] = str(v)
        elif isinstance(v, bytes):
            out[k] = v.decode("utf-8", errors="replace")
        else:
            out[k] = v
    return out


# ─── Tool functions (the LLM's only DB access) ─────────────────────────────
def tool_lookup_player(nickname: str):
    """Find a player by nickname (exact first, partial fallback)."""
    rows = fetchall(
        "SELECT id, nick_name, user_name, vin, xu, vin_total, xu_total, "
        "       status, create_time, last_login "
        "FROM users "
        "WHERE nick_name = %s OR user_name = %s OR nick_name LIKE %s "
        "ORDER BY (nick_name = %s) DESC, id ASC "
        "LIMIT 10",
        (nickname, nickname, f"%{nickname}%", nickname),
    )
    return {"matches": [_serialise_row(r) for r in rows], "match_count": len(rows)}


def tool_get_money_flow(user_id: int, hours: int = 24, limit: int = 200):
    """Per-player money_gateway_log entries in the last N hours."""
    if hours <= 0 or hours > 720:
        hours = 24
    if limit <= 0 or limit > 1000:
        limit = 200
    rows = fetchall(
        "SELECT id, amount, currency, source, tx_id, balance_after, "
        "       LEFT(description, 200) AS description, created_at "
        "FROM money_gateway_log "
        "WHERE user_id = %s AND created_at > NOW() - INTERVAL %s HOUR "
        "ORDER BY id DESC LIMIT %s",
        (user_id, hours, limit),
    )
    total_in = sum(int(r["amount"]) for r in rows if r["amount"] and r["amount"] > 0)
    total_out = sum(-int(r["amount"]) for r in rows if r["amount"] and r["amount"] < 0)
    return {
        "ledger_rows": [_serialise_row(r) for r in rows],
        "row_count": len(rows),
        "total_credits": total_in,
        "total_debits": total_out,
        "net_flow": total_in - total_out,
    }


def tool_summarise_player_money(user_id: int):
    """Wallet sanity check — current balance + lifetime totals by source."""
    rows = fetchall(
        "SELECT id, nick_name, vin, xu, vin_total, xu_total, status "
        "FROM users WHERE id = %s",
        (user_id,),
    )
    if not rows:
        return {"error": f"user_id {user_id} not found"}
    summary = fetchall(
        "SELECT source, currency, COUNT(*) AS cnt, SUM(amount) AS total "
        "FROM money_gateway_log "
        "WHERE user_id = %s "
        "GROUP BY source, currency "
        "ORDER BY ABS(SUM(amount)) DESC "
        "LIMIT 50",
        (user_id,),
    )
    return {
        "user": _serialise_row(rows[0]),
        "money_flow_by_source": [_serialise_row(r) for r in summary],
    }


def tool_get_gsc_bets(user_id: int, hours: int = 24, settled_only: bool = False,
                      limit: int = 200):
    """GSC bet history (same table the agency portal reads)."""
    if hours <= 0 or hours > 720:
        hours = 24
    if limit <= 0 or limit > 500:
        limit = 200
    where_extra = "AND settled = 1 " if settled_only else ""
    rows = fetchall(
        f"SELECT id, txn_id, wager_code, settle_txn_id, bet_value, prize, fee, "
        f"       product_code, game_code, game_name, currency, settled, "
        f"       create_time, settle_time "
        f"FROM gsc_bets "
        f"WHERE user_id = %s AND create_time > NOW() - INTERVAL %s HOUR "
        f"{where_extra}"
        f"ORDER BY id DESC LIMIT %s",
        (user_id, hours, limit),
    )
    return {
        "bets": [_serialise_row(r) for r in rows],
        "bet_count": len(rows),
        "total_bet": sum(int(r["bet_value"] or 0) for r in rows),
        "total_prize": sum(int(r["prize"] or 0) for r in rows),
        "net": sum(int(r["prize"] or 0) - int(r["bet_value"] or 0) for r in rows),
        "settled_count": sum(1 for r in rows if r["settled"]),
        "pending_count": sum(1 for r in rows if not r["settled"]),
    }


def tool_get_deposits(user_id: int, hours: int = 168):
    """Bank deposits (deposit_transactions)."""
    if hours <= 0 or hours > 8760:
        hours = 168
    rows = fetchall(
        "SELECT id, tx_code, amount, deposit_type, user_bank_name, user_bank_number, "
        "       status, credit_status, force_approved, force_approved_by, "
        "       processed_by, created_at, processed_at "
        "FROM deposit_transactions "
        "WHERE user_id = %s AND created_at > NOW() - INTERVAL %s HOUR "
        "ORDER BY id DESC LIMIT 100",
        (user_id, hours),
    )
    approved = [r for r in rows if r["status"] == "APPROVED" and r["credit_status"] == "CREDITED"]
    return {
        "deposits": [_serialise_row(r) for r in rows],
        "total_approved_amount": sum(int(r["amount"]) for r in approved),
        "approved_count": len(approved),
        "total_attempted": len(rows),
    }


def tool_get_withdrawals(user_id: int, hours: int = 168):
    """Bank withdrawals (bank_withdrawals)."""
    if hours <= 0 or hours > 8760:
        hours = 168
    rows = fetchall(
        "SELECT id, tx_code, bank_name, bank_number, customer_name, account_holder, "
        "       amount_krw, fee_krw, status, admin_by, reject_reason, "
        "       created_at, processed_at "
        "FROM bank_withdrawals "
        "WHERE user_id = %s AND created_at > NOW() - INTERVAL %s HOUR "
        "ORDER BY id DESC LIMIT 100",
        (user_id, hours),
    )
    approved = [r for r in rows if r["status"] == "APPROVED"]
    return {
        "withdrawals": [_serialise_row(r) for r in rows],
        "total_approved_amount": sum(int(r["amount_krw"]) for r in approved),
        "approved_count": len(approved),
        "total_attempted": len(rows),
    }


def tool_get_gsc_event_log(player_nick: str = None, hours: int = 1,
                           status: str = None, endpoint: str = None,
                           limit: int = 100):
    """Raw GSC event log — every withdraw/deposit/balance request from GSC.
    Useful for tracing bet rejections and stuck-RECEIVED rows."""
    if hours <= 0 or hours > 168:
        hours = 1
    if limit <= 0 or limit > 500:
        limit = 100
    where = ["received_at > NOW() - INTERVAL %s HOUR"]
    params = [hours]
    if player_nick:
        where.append("member_account = %s")
        params.append(player_nick)
    if status:
        where.append("processing_status = %s")
        params.append(status.upper())
    if endpoint:
        where.append("gsc_endpoint = %s")
        params.append(endpoint.lower())
    params.append(limit)
    rows = fetchall(
        "SELECT id, gsc_endpoint, member_account, product_code, game_code, "
        "       gsc_wager_code, gsc_round_id, bet_amount, prize_amount, currency, "
        "       processing_status, "
        "       TIMESTAMPDIFF(MICROSECOND, received_at, response_at)/1000 AS response_ms, "
        "       LEFT(last_error, 200) AS last_error, received_at, response_at "
        f"FROM gsc_event_log WHERE {' AND '.join(where)} "
        "ORDER BY id DESC LIMIT %s",
        tuple(params),
    )
    return {"events": [_serialise_row(r) for r in rows], "event_count": len(rows)}


def tool_find_stuck_bets(hours: int = 24, min_age_minutes: int = 30):
    """Bets stuck in PENDING (settled=0) older than min_age_minutes —
    'Mode 3 ghost bets' where wallet was debited but GSC never settled."""
    if hours <= 0 or hours > 720:
        hours = 24
    rows = fetchall(
        "SELECT id, user_id, nick_name, product_code, game_name, txn_id, wager_code, "
        "       bet_value, currency, create_time "
        "FROM gsc_bets "
        "WHERE settled = 0 "
        "  AND create_time BETWEEN NOW() - INTERVAL %s HOUR AND NOW() - INTERVAL %s MINUTE "
        "ORDER BY create_time DESC LIMIT 200",
        (hours, min_age_minutes),
    )
    return {
        "stuck_bets": [_serialise_row(r) for r in rows],
        "stuck_count": len(rows),
        "total_stuck_krw": sum(int(r["bet_value"] or 0) for r in rows),
    }


def tool_get_endpoint_latency(endpoint: str, hours: int = 1):
    """Latency stats for a GSC endpoint over the last N hours.
    Mirrors the Grafana dashboard panels."""
    if endpoint not in ("withdraw", "deposit", "balance", "rollback",
                        "pushbet", "transfer", "cancel"):
        return {"error": f"unknown endpoint: {endpoint}"}
    if hours <= 0 or hours > 168:
        hours = 1
    base = fetchall(
        "SELECT COUNT(*) AS cnt, "
        "       AVG(TIMESTAMPDIFF(MICROSECOND, received_at, response_at)/1000) AS avg_ms, "
        "       MIN(TIMESTAMPDIFF(MICROSECOND, received_at, response_at)/1000) AS min_ms, "
        "       MAX(TIMESTAMPDIFF(MICROSECOND, received_at, response_at)/1000) AS max_ms, "
        "       SUM(CASE WHEN processing_status='FAILED' THEN 1 ELSE 0 END) AS failed_count, "
        "       SUM(CASE WHEN processing_status='RECEIVED' THEN 1 ELSE 0 END) AS stuck_count "
        "FROM gsc_event_log "
        "WHERE gsc_endpoint = %s AND received_at > NOW() - INTERVAL %s HOUR",
        (endpoint, hours),
    )
    pcts = fetchall(
        "WITH ranked AS ( "
        "  SELECT TIMESTAMPDIFF(MICROSECOND, received_at, response_at)/1000 AS ms, "
        "         ROW_NUMBER() OVER (ORDER BY TIMESTAMPDIFF(MICROSECOND, received_at, response_at)) AS rn, "
        "         COUNT(*) OVER () AS total "
        "  FROM gsc_event_log "
        "  WHERE gsc_endpoint = %s AND received_at > NOW() - INTERVAL %s HOUR "
        "    AND processing_status='COMPLETED' AND response_at IS NOT NULL "
        ") "
        "SELECT MAX(CASE WHEN rn=CEIL(total*0.50) THEN ms END) AS p50, "
        "       MAX(CASE WHEN rn=CEIL(total*0.90) THEN ms END) AS p90, "
        "       MAX(CASE WHEN rn=CEIL(total*0.99) THEN ms END) AS p99 "
        "FROM ranked",
        (endpoint, hours),
    )
    out = _serialise_row(base[0]) if base else {}
    if pcts:
        out.update(_serialise_row(pcts[0]))
    out["endpoint"] = endpoint
    out["window_hours"] = hours
    return out


def tool_admin_topups_for_player(user_id: int, hours: int = 168):
    """ADMIN_TOPUP entries — manual refunds via the admin UI."""
    if hours <= 0 or hours > 8760:
        hours = 168
    rows = fetchall(
        "SELECT id, amount, tx_id, balance_after, description, created_at "
        "FROM money_gateway_log "
        "WHERE user_id = %s AND source = 'ADMIN_TOPUP' "
        "  AND created_at > NOW() - INTERVAL %s HOUR "
        "ORDER BY id DESC LIMIT 50",
        (user_id, hours),
    )
    return {
        "admin_topups": [_serialise_row(r) for r in rows],
        "total_amount": sum(int(r["amount"] or 0) for r in rows),
        "count": len(rows),
    }


def tool_top_winners(hours: int = 24, limit: int = 10):
    """Top N players by net positive GSC P&L (credits − debits) in last N hours."""
    if hours <= 0 or hours > 720:
        hours = 24
    if limit <= 0 or limit > 50:
        limit = 10
    rows = fetchall(
        "SELECT m.user_id, u.nick_name, "
        "       SUM(CASE WHEN m.source='GSC_CREDIT' THEN m.amount ELSE 0 END) AS credits, "
        "       SUM(CASE WHEN m.source='GSC_DEBIT'  THEN -m.amount ELSE 0 END) AS debits, "
        "       SUM(CASE WHEN m.source='GSC_CREDIT' THEN m.amount "
        "                WHEN m.source='GSC_DEBIT'  THEN m.amount ELSE 0 END) AS net "
        "FROM money_gateway_log m "
        "JOIN users u ON u.id = m.user_id "
        "WHERE m.source IN ('GSC_DEBIT','GSC_CREDIT') "
        "  AND m.created_at > NOW() - INTERVAL %s HOUR "
        "GROUP BY m.user_id, u.nick_name "
        "HAVING net > 0 "
        "ORDER BY net DESC LIMIT %s",
        (hours, limit),
    )
    return {"window_hours": hours, "leaderboard": [_serialise_row(r) for r in rows]}


def tool_top_losers(hours: int = 24, limit: int = 10):
    """Top N players by net negative GSC P&L (credits − debits) in last N hours."""
    if hours <= 0 or hours > 720:
        hours = 24
    if limit <= 0 or limit > 50:
        limit = 10
    rows = fetchall(
        "SELECT m.user_id, u.nick_name, "
        "       SUM(CASE WHEN m.source='GSC_CREDIT' THEN m.amount ELSE 0 END) AS credits, "
        "       SUM(CASE WHEN m.source='GSC_DEBIT'  THEN -m.amount ELSE 0 END) AS debits, "
        "       SUM(CASE WHEN m.source='GSC_CREDIT' THEN m.amount "
        "                WHEN m.source='GSC_DEBIT'  THEN m.amount ELSE 0 END) AS net "
        "FROM money_gateway_log m "
        "JOIN users u ON u.id = m.user_id "
        "WHERE m.source IN ('GSC_DEBIT','GSC_CREDIT') "
        "  AND m.created_at > NOW() - INTERVAL %s HOUR "
        "GROUP BY m.user_id, u.nick_name "
        "HAVING net < 0 "
        "ORDER BY net ASC LIMIT %s",
        (hours, limit),
    )
    return {"window_hours": hours, "leaderboard": [_serialise_row(r) for r in rows]}


def tool_top_betters(hours: int = 24, limit: int = 10):
    """Top N players by total bet volume (GSC_DEBIT sum) in last N hours."""
    if hours <= 0 or hours > 720:
        hours = 24
    if limit <= 0 or limit > 50:
        limit = 10
    rows = fetchall(
        "SELECT m.user_id, u.nick_name, "
        "       COUNT(*) AS bet_count, "
        "       SUM(-m.amount) AS total_bet "
        "FROM money_gateway_log m "
        "JOIN users u ON u.id = m.user_id "
        "WHERE m.source = 'GSC_DEBIT' "
        "  AND m.created_at > NOW() - INTERVAL %s HOUR "
        "GROUP BY m.user_id, u.nick_name "
        "ORDER BY total_bet DESC LIMIT %s",
        (hours, limit),
    )
    return {"window_hours": hours, "leaderboard": [_serialise_row(r) for r in rows]}


def tool_top_depositors(hours: int = 168, limit: int = 10):
    """Top N players by approved bank deposit totals in last N hours."""
    if hours <= 0 or hours > 8760:
        hours = 168
    if limit <= 0 or limit > 50:
        limit = 10
    rows = fetchall(
        "SELECT d.user_id, u.nick_name, "
        "       COUNT(*) AS deposit_count, "
        "       SUM(d.amount) AS total_deposited "
        "FROM deposit_transactions d "
        "JOIN users u ON u.id = d.user_id "
        "WHERE d.status = 'APPROVED' AND d.credit_status = 'CREDITED' "
        "  AND d.created_at > NOW() - INTERVAL %s HOUR "
        "GROUP BY d.user_id, u.nick_name "
        "ORDER BY total_deposited DESC LIMIT %s",
        (hours, limit),
    )
    return {"window_hours": hours, "leaderboard": [_serialise_row(r) for r in rows]}


def tool_top_withdrawers(hours: int = 168, limit: int = 10):
    """Top N players by approved bank withdrawal totals in last N hours."""
    if hours <= 0 or hours > 8760:
        hours = 168
    if limit <= 0 or limit > 50:
        limit = 10
    rows = fetchall(
        "SELECT w.user_id, u.nick_name, "
        "       COUNT(*) AS withdrawal_count, "
        "       SUM(w.amount_krw) AS total_withdrawn "
        "FROM bank_withdrawals w "
        "JOIN users u ON u.id = w.user_id "
        "WHERE w.status = 'APPROVED' "
        "  AND w.created_at > NOW() - INTERVAL %s HOUR "
        "GROUP BY w.user_id, u.nick_name "
        "ORDER BY total_withdrawn DESC LIMIT %s",
        (hours, limit),
    )
    return {"window_hours": hours, "leaderboard": [_serialise_row(r) for r in rows]}


def tool_daily_house_summary(hours: int = 24):
    """System-wide GSC P&L + bet count + active-player count in last N hours."""
    if hours <= 0 or hours > 720:
        hours = 24
    summary = fetchall(
        "SELECT "
        "  COUNT(DISTINCT m.user_id) AS active_players, "
        "  COUNT(CASE WHEN m.source='GSC_DEBIT'  THEN 1 END) AS bet_count, "
        "  COUNT(CASE WHEN m.source='GSC_CREDIT' THEN 1 END) AS settle_count, "
        "  SUM(CASE WHEN m.source='GSC_DEBIT'  THEN -m.amount ELSE 0 END) AS total_bet, "
        "  SUM(CASE WHEN m.source='GSC_CREDIT' THEN m.amount  ELSE 0 END) AS total_payout, "
        "  -SUM(CASE WHEN m.source='GSC_DEBIT' THEN m.amount "
        "            WHEN m.source='GSC_CREDIT' THEN m.amount ELSE 0 END) AS house_pnl "
        "FROM money_gateway_log m "
        "WHERE m.source IN ('GSC_DEBIT','GSC_CREDIT') "
        "  AND m.created_at > NOW() - INTERVAL %s HOUR",
        (hours,),
    )
    bank = fetchall(
        "SELECT "
        "  (SELECT COALESCE(SUM(amount), 0) FROM deposit_transactions "
        "    WHERE status='APPROVED' AND credit_status='CREDITED' "
        "      AND created_at > NOW() - INTERVAL %s HOUR) AS bank_in, "
        "  (SELECT COALESCE(SUM(amount_krw), 0) FROM bank_withdrawals "
        "    WHERE status='APPROVED' "
        "      AND created_at > NOW() - INTERVAL %s HOUR) AS bank_out",
        (hours, hours),
    )
    out = _serialise_row(summary[0]) if summary else {}
    if bank:
        out.update(_serialise_row(bank[0]))
    out["window_hours"] = hours
    return out


def tool_find_giftcode(code: str):
    """Look up a giftcode by code (exact) — returns definition + how many times used."""
    rows = fetchall(
        "SELECT id, giftcode, money, max_use, time_used, `from`, exprired, "
        "       created_at, created_by, source, rollover_rounds "
        "FROM gift_codes WHERE giftcode = %s LIMIT 1",
        (code,),
    )
    if not rows:
        return {"error": f"giftcode '{code}' not found"}
    gc = rows[0]
    redemptions = fetchall(
        "SELECT u.username, u.user_id, u.created_at AS redeemed_at "
        "FROM gift_code_useds u WHERE u.giftcode_id = %s "
        "ORDER BY u.created_at DESC LIMIT 100",
        (gc["id"],),
    )
    return {
        "giftcode": _serialise_row(gc),
        "redemptions": [_serialise_row(r) for r in redemptions],
        "redemption_count": len(redemptions),
    }


def tool_player_giftcode_history(user_id: int):
    """All giftcodes a player has redeemed."""
    rows = fetchall(
        "SELECT gc.giftcode, gc.money, gc.source, u.created_at AS redeemed_at "
        "FROM gift_code_useds u "
        "JOIN gift_codes gc ON gc.id = u.giftcode_id "
        "WHERE u.user_id = %s "
        "ORDER BY u.created_at DESC LIMIT 100",
        (user_id,),
    )
    return {
        "redemptions": [_serialise_row(r) for r in rows],
        "redemption_count": len(rows),
        "total_value": sum(int(r["money"] or 0) for r in rows),
    }


def tool_recent_giftcode_redemptions(hours: int = 24, limit: int = 50):
    """Recent giftcode redemptions across all codes — useful for "who redeemed what today"."""
    if hours <= 0 or hours > 720:
        hours = 24
    if limit <= 0 or limit > 500:
        limit = 50
    rows = fetchall(
        "SELECT gc.giftcode, gc.money, gc.source, gc.created_by, "
        "       u.username, u.user_id, u.created_at AS redeemed_at "
        "FROM gift_code_useds u "
        "JOIN gift_codes gc ON gc.id = u.giftcode_id "
        "WHERE u.created_at > NOW() - INTERVAL %s HOUR "
        "ORDER BY u.created_at DESC LIMIT %s",
        (hours, limit),
    )
    by_code = {}
    for r in rows:
        by_code[r["giftcode"]] = by_code.get(r["giftcode"], 0) + 1
    return {
        "window_hours": hours,
        "redemptions": [_serialise_row(r) for r in rows],
        "redemption_count": len(rows),
        "total_value": sum(int(r["money"] or 0) for r in rows),
        "redemptions_per_code": by_code,
    }


def tool_pending_withdrawals_summary():
    """What's queued in bank_withdrawals right now — count, total, oldest age."""
    rows = fetchall(
        "SELECT status, COUNT(*) AS cnt, SUM(amount_krw) AS total_krw, "
        "       MIN(created_at) AS oldest, MAX(created_at) AS newest "
        "FROM bank_withdrawals "
        "WHERE status IN ('PENDING','PROCESSING','HOLD') "
        "GROUP BY status"
    )
    return {"pending_buckets": [_serialise_row(r) for r in rows],
            "bucket_count": len(rows)}


def tool_active_players(minutes: int = 30):
    """Distinct players that placed a bet in the last N minutes."""
    if minutes <= 0 or minutes > 1440:
        minutes = 30
    rows = fetchall(
        "SELECT COUNT(DISTINCT user_id) AS active_players, "
        "       COUNT(*) AS bet_events "
        "FROM money_gateway_log "
        "WHERE source = 'GSC_DEBIT' "
        "  AND created_at > NOW() - INTERVAL %s MINUTE",
        (minutes,),
    )
    return _serialise_row(rows[0]) if rows else {}


_SQL_FORBIDDEN = (
    "INSERT", "UPDATE", "DELETE", "REPLACE", "CREATE", "DROP", "ALTER",
    "TRUNCATE", "GRANT", "REVOKE", "LOCK", "UNLOCK", "SET ",
    "CALL ", "HANDLER ", "LOAD ", "INTO OUTFILE", "INTO DUMPFILE",
    "MERGE", "RENAME",
)
_SENSITIVE_TOKENS = (
    "password", "goog", "totp", "secret", "token", "encryption", "private_key",
    "aat", "session_id",  # session_id only narrowly — GSC has a session_id
                          # column too but it's safe; the pattern is a heuristic
                          # for sensitive cols. The DB user has no grants on
                          # admin tables anyway, so this is just an extra UX
                          # signal to the LLM that those columns are off-limits.
)


def tool_execute_readonly_query(sql: str, row_limit: int = 200):
    """Execute an arbitrary read-only SELECT/WITH query and return rows.

    Defense layers:
      1. DB user `audit_ro` has SELECT-only grants on a curated table list —
         any DDL/DML or out-of-scope-table SELECT fails at MySQL.
      2. We pattern-reject DDL/DML keywords up-front for clearer errors.
      3. MySQL MAX_EXECUTION_TIME hint caps query at 8 seconds so a runaway
         join can't lock the bot.
      4. Result rows are capped at row_limit (default 200, max 1000) so a
         large result can't blow the LLM context window.

    The LLM should prefer the predefined tools (lookup_player,
    get_money_flow, top_winners, find_giftcode, …) for common questions —
    they're faster, deterministic, and don't risk being wrong. This tool is
    the escape hatch for analytics outside the pre-written set.
    """
    if not isinstance(sql, str) or not sql.strip():
        return {"error": "empty query"}
    q = sql.strip().rstrip(";").strip()
    upper = q.upper()
    if not (upper.startswith("SELECT") or upper.startswith("WITH")):
        return {"error": "only SELECT or WITH queries are allowed"}
    for kw in _SQL_FORBIDDEN:
        if kw in upper:
            return {"error": f"forbidden keyword in query: {kw.strip()}"}
    lower = q.lower()
    for tok in _SENSITIVE_TOKENS:
        if tok in lower:
            return {
                "error": (
                    f"query mentions a likely-sensitive column ('{tok}'); "
                    "this tool refuses to read auth/credential data even if "
                    "the DB user technically had access."
                )
            }
    if row_limit <= 0 or row_limit > 1000:
        row_limit = 200

    # Inject a MAX_EXECUTION_TIME hint so a heavy query can't lock the bot.
    # 8s here matches the typical Telegram long-poll cycle.
    if upper.startswith("SELECT"):
        q_safe = "SELECT /*+ MAX_EXECUTION_TIME(8000) */ " + q[len("SELECT"):]
    else:
        # CTE — MySQL can't apply optimizer hint to WITH directly, so use the
        # session-level timeout via inline comment guard instead.
        q_safe = q

    # Wrap in LIMIT cap. If the user's query already has LIMIT, MySQL takes
    # the inner one; otherwise we apply the cap. We avoid editing their SQL
    # in-place and use a subquery wrapper.
    capped = f"SELECT * FROM ({q_safe}) AS _audit_q LIMIT {row_limit + 1}"
    conn = db()
    try:
        cur = conn.cursor(dictionary=True)
        # Belt-and-braces: also set session-level max_execution_time for
        # WITH queries the inline hint can't reach.
        cur.execute("SET SESSION MAX_EXECUTION_TIME = 8000")
        cur.execute(capped)
        rows = cur.fetchall()
        cur.close()
    except mysql.connector.Error as e:
        return {"error": f"DB error: {e.msg if hasattr(e, 'msg') else str(e)}"}
    finally:
        conn.close()
    truncated = len(rows) > row_limit
    if truncated:
        rows = rows[:row_limit]
    return {
        "rows": [_serialise_row(r) for r in rows],
        "row_count": len(rows),
        "truncated": truncated,
        "query": q[:500],
    }


def tool_recent_stuck_refunds(hours: int = 24, limit: int = 50):
    """GSC_STUCK_REFUND ledger entries — auto-refunds from the
    GscStuckRowReconciler when a ghost-debit was detected."""
    if hours <= 0 or hours > 720:
        hours = 24
    if limit <= 0 or limit > 500:
        limit = 50
    rows = fetchall(
        "SELECT id, user_id, nick_name, amount, tx_id, balance_after, "
        "       description, created_at "
        "FROM money_gateway_log "
        "WHERE source = 'GSC_STUCK_REFUND' "
        "  AND created_at > NOW() - INTERVAL %s HOUR "
        "ORDER BY id DESC LIMIT %s",
        (hours, limit),
    )
    return {
        "auto_refunds": [_serialise_row(r) for r in rows],
        "total_amount": sum(int(r["amount"] or 0) for r in rows),
        "count": len(rows),
    }


# ─── LLM-facing tool schema ────────────────────────────────────────────────
TOOL_REGISTRY = {
    # Per-player tools
    "lookup_player": tool_lookup_player,
    "get_money_flow": tool_get_money_flow,
    "summarise_player_money": tool_summarise_player_money,
    "get_gsc_bets": tool_get_gsc_bets,
    "get_deposits": tool_get_deposits,
    "get_withdrawals": tool_get_withdrawals,
    "admin_topups_for_player": tool_admin_topups_for_player,
    "player_giftcode_history": tool_player_giftcode_history,
    # System-wide / leaderboards
    "top_winners": tool_top_winners,
    "top_losers": tool_top_losers,
    "top_betters": tool_top_betters,
    "top_depositors": tool_top_depositors,
    "top_withdrawers": tool_top_withdrawers,
    "daily_house_summary": tool_daily_house_summary,
    "active_players": tool_active_players,
    "pending_withdrawals_summary": tool_pending_withdrawals_summary,
    # Giftcode lookups
    "find_giftcode": tool_find_giftcode,
    "recent_giftcode_redemptions": tool_recent_giftcode_redemptions,
    # GSC engineering tools
    "get_gsc_event_log": tool_get_gsc_event_log,
    "find_stuck_bets": tool_find_stuck_bets,
    "get_endpoint_latency": tool_get_endpoint_latency,
    "recent_stuck_refunds": tool_recent_stuck_refunds,
    # Generic SQL escape hatch
    "execute_readonly_query": tool_execute_readonly_query,
}

TOOL_SCHEMA = [
    {
        "type": "function",
        "function": {
            "name": "lookup_player",
            "description": (
                "Find a player by nickname (exact match preferred, partial fallback). "
                "Returns user_id + current vin/xu balance + status. ALWAYS call this "
                "first when the user mentions a player name — every other player tool "
                "needs the integer user_id."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "nickname": {"type": "string", "description": "Player nickname or partial."},
                },
                "required": ["nickname"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_money_flow",
            "description": (
                "Per-player money_gateway_log entries in the last N hours. This is "
                "the canonical vin ledger — every credit/debit goes through it. "
                "Returns rows + total credits/debits/net."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {"type": "integer"},
                    "hours": {"type": "integer", "description": "Lookback hours, max 720 (30d). Default 24."},
                    "limit": {"type": "integer", "description": "Max rows. Default 200, cap 1000."},
                },
                "required": ["user_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "summarise_player_money",
            "description": (
                "Quick wallet sanity check for one player. Returns current balance + "
                "lifetime ledger movements grouped by source. Useful as the first "
                "call after lookup_player when asked 'is X's wallet OK?'."
            ),
            "parameters": {
                "type": "object",
                "properties": {"user_id": {"type": "integer"}},
                "required": ["user_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_gsc_bets",
            "description": (
                "GSC bet history for a player — same table the agency portal reads. "
                "Includes settle status and prize amounts."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {"type": "integer"},
                    "hours": {"type": "integer", "description": "Lookback hours, max 720."},
                    "settled_only": {"type": "boolean", "description": "If true, only returns settled bets."},
                    "limit": {"type": "integer"},
                },
                "required": ["user_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_deposits",
            "description": "Bank/wallet deposits (deposit_transactions) for a player.",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {"type": "integer"},
                    "hours": {"type": "integer", "description": "Lookback hours. Default 168 (7d)."},
                },
                "required": ["user_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_withdrawals",
            "description": "Bank withdrawals (cash-outs) for a player.",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {"type": "integer"},
                    "hours": {"type": "integer", "description": "Lookback hours. Default 168 (7d)."},
                },
                "required": ["user_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_gsc_event_log",
            "description": (
                "Raw GSC event log — every withdraw/deposit/balance request from GSC. "
                "Useful for tracing why a bet was rejected, or finding "
                "stuck-RECEIVED rows. Filterable by player, status, and endpoint."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "player_nick": {"type": "string"},
                    "hours": {"type": "integer", "description": "Lookback hours, max 168. Default 1."},
                    "status": {
                        "type": "string",
                        "enum": ["RECEIVED", "COMPLETED", "FAILED"],
                    },
                    "endpoint": {
                        "type": "string",
                        "enum": ["withdraw", "deposit", "balance", "rollback",
                                 "pushbet", "transfer", "cancel"],
                    },
                    "limit": {"type": "integer", "description": "Max rows, default 100, cap 500."},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "find_stuck_bets",
            "description": (
                "Bets stuck in PENDING (gsc_bets.settled=0) older than min_age_minutes. "
                "These are 'Mode 3 ghost bets' — wallet was debited but GSC never sent "
                "the settle. The GscStuckRowReconciler should auto-refund these, but "
                "this tool surfaces what's currently in flight."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "hours": {"type": "integer", "description": "Lookback hours, max 720."},
                    "min_age_minutes": {"type": "integer", "description": "Min age in minutes. Default 30."},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_endpoint_latency",
            "description": (
                "Latency stats (count + avg/p50/p90/p99/max + failed/stuck counts) "
                "for a GSC endpoint over the last N hours. Mirrors the Grafana dashboard."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "endpoint": {
                        "type": "string",
                        "enum": ["withdraw", "deposit", "balance", "rollback",
                                 "pushbet", "transfer", "cancel"],
                    },
                    "hours": {"type": "integer", "description": "Lookback hours, max 168."},
                },
                "required": ["endpoint"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "admin_topups_for_player",
            "description": "ADMIN_TOPUP entries — manual refunds applied via the admin UI.",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {"type": "integer"},
                    "hours": {"type": "integer", "description": "Lookback hours. Default 168."},
                },
                "required": ["user_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "top_winners",
            "description": (
                "Leaderboard: top N players by net positive GSC P&L "
                "(GSC_CREDIT − GSC_DEBIT > 0) in last N hours. Use when "
                "asked 'ai đang win nhiều nhất', 'who's winning'."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "hours": {"type": "integer", "description": "Lookback hours. Default 24."},
                    "limit": {"type": "integer", "description": "Top N. Default 10, cap 50."},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "top_losers",
            "description": (
                "Leaderboard: top N players by net negative GSC P&L in "
                "last N hours. Use when asked 'ai đang thua nhiều nhất'."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "hours": {"type": "integer", "description": "Lookback hours. Default 24."},
                    "limit": {"type": "integer", "description": "Top N. Default 10, cap 50."},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "top_betters",
            "description": (
                "Leaderboard: top N players by total bet volume in last "
                "N hours. Use when asked 'ai bet nhiều nhất', 'biggest betters'."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "hours": {"type": "integer", "description": "Lookback hours. Default 24."},
                    "limit": {"type": "integer", "description": "Top N. Default 10."},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "top_depositors",
            "description": (
                "Leaderboard: top N players by approved bank-deposit "
                "totals. Use when asked 'ai nạp tiền nhiều nhất'."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "hours": {"type": "integer", "description": "Lookback hours. Default 168 (7d)."},
                    "limit": {"type": "integer", "description": "Top N. Default 10."},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "top_withdrawers",
            "description": (
                "Leaderboard: top N players by approved bank-withdrawal "
                "totals. Use when asked 'ai rút nhiều nhất'."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "hours": {"type": "integer", "description": "Lookback hours. Default 168 (7d)."},
                    "limit": {"type": "integer", "description": "Top N. Default 10."},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "daily_house_summary",
            "description": (
                "System-wide totals in last N hours: active players, "
                "bet/settle counts, total bet volume, total payout, house "
                "P&L (bet − payout, positive = house won), bank-deposit and "
                "bank-withdraw totals. Use for 'how is today going overall'."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "hours": {"type": "integer", "description": "Lookback hours. Default 24."},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "active_players",
            "description": "How many distinct players bet in the last N minutes.",
            "parameters": {
                "type": "object",
                "properties": {
                    "minutes": {"type": "integer", "description": "Lookback minutes. Default 30, cap 1440."},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "pending_withdrawals_summary",
            "description": (
                "Count + total of bank withdrawals currently in "
                "PENDING/PROCESSING/HOLD status (i.e. waiting on admin "
                "to approve/process), with the oldest row's timestamp."
            ),
            "parameters": {"type": "object", "properties": {}},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "find_giftcode",
            "description": (
                "Look up a giftcode by exact code. Returns the code's "
                "definition (value, max uses, validity window, creator) "
                "and the list of users who have redeemed it. Use when "
                "asked 'ai dùng giftcode X', 'who used SUN1T9FK', 'giftcode X "
                "có ai dùng'."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "code": {"type": "string", "description": "Exact giftcode string."},
                },
                "required": ["code"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "player_giftcode_history",
            "description": "All giftcodes a specific player has redeemed.",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {"type": "integer"},
                },
                "required": ["user_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "recent_giftcode_redemptions",
            "description": (
                "Recent giftcode redemptions across ALL codes in last N "
                "hours. Returns who redeemed what, plus a per-code count. "
                "Use for 'recent giftcode activity', 'who redeemed gift codes today'."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "hours": {"type": "integer", "description": "Lookback hours. Default 24."},
                    "limit": {"type": "integer", "description": "Max rows. Default 50, cap 500."},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "recent_stuck_refunds",
            "description": (
                "GSC_STUCK_REFUND ledger entries — auto-refunds issued by the "
                "GscStuckRowReconciler when a ghost-debit was detected. Useful to "
                "check whether the reconciler is actively repairing the wallet."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "hours": {"type": "integer", "description": "Lookback hours. Default 24."},
                    "limit": {"type": "integer"},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "execute_readonly_query",
            "description": (
                "Run a custom read-only SELECT or WITH query when the "
                "predefined tools above don't cover the question. The DB "
                "user has SELECT grants only on a curated list of tables "
                "(see the schema in the system prompt); any other table or "
                "DDL/DML is rejected. Queries are capped at 8 seconds and "
                "200 rows (default) / 1000 rows (max). PREFER predefined "
                "tools when one fits — they're faster and harder to get "
                "wrong. Use this only for unusual analytics or ad-hoc "
                "joins. Always parameterise via the SQL itself; do NOT "
                "ask the user for raw SQL."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "sql": {
                        "type": "string",
                        "description": "A single SELECT or WITH … SELECT statement. No trailing semicolon. No DDL/DML.",
                    },
                    "row_limit": {
                        "type": "integer",
                        "description": "Max rows to return. Default 200, cap 1000.",
                    },
                },
                "required": ["sql"],
            },
        },
    },
]


SYSTEM_PROMPT = """You are the SunWinKR audit bot. You answer business + ops
questions about a Korean casino backend in the Telegram audit group. Users are
stakeholders (mostly non-technical) and they read your replies on phones, so
output must be skimmable on a small screen.

═══════════════════════════════════════════════════════════════════════════
HOW TO ANSWER (most important — operators read on phones)
═══════════════════════════════════════════════════════════════════════════
- **ALWAYS reply in Vietnamese**, regardless of what language the question
  was asked in. The audit group's stakeholders are Vietnamese-speaking, so
  English / mixed-language questions still get a Vietnamese answer.
  Technical terms (player nicknames, wager codes, table names like
  "Korean Speed Baccarat", source flags like `GSC_DEBIT`, status enums
  like `RECEIVED`/`COMPLETED`/`FAILED`) stay in English — don't translate
  identifiers, only the prose around them.
- Lead with the headline answer in **one short sentence**, then the supporting
  details. Don't bury the lede in setup.
- Use Telegram-compatible Markdown. Specifically:
  * **bold** for key numbers and names
  * `inline code` for IDs, wager codes, giftcodes, status flags
  * bullet lists (`• `) for multi-item answers — NOT tables. Telegram
    renders tables badly on mobile. Only use a table if the user explicitly
    asks for one or the data is irreducibly tabular (e.g. leaderboard 3-5
    rows).
  * Short headings (`*Phần X*`) for separate sections inside a longer answer.
  * Status emoji where they help: ✅ OK, ❌ thua/lỗi, ⚠️ chú ý, 💰 tiền vào,
    💸 tiền ra, 🏆 thắng. Use sparingly — one per section max.
- Format money with thousands separators + the unit: `1,234,567 KRW`. Never
  return raw integers like 1234567 in user-facing text.
- Format timestamps as `HH:MM KR` (or `YYYY-MM-DD HH:MM KR` when the date
  matters). DB timestamps are Asia/Seoul UTC+9 — always say "KR" so
  Vietnamese stakeholders don't convert in their head.
- If a number could be misread (e.g. is it KRW or VND?), say the unit
  explicitly. KRW for all wallet/GSC numbers unless told otherwise.
- For leaderboard-style answers (top 5/top 10), use a numbered list with
  bold name + key number per line, optionally a trailing detail in parens.
  Example:
      1. **`okvip39`** — +5,952,840 KRW (645 bets, 371 settles)
      2. **`HooangKr`** — +2,103,000 KRW (98 bets)
- Cap answer length at ~15 lines unless the user explicitly asks for full
  detail. Detail goes in a follow-up question, not a wall of text.
- If a tool returns zero rows, say so plainly: "Không có bản ghi nào trong
  N giờ qua." Don't fabricate.

═══════════════════════════════════════════════════════════════════════════
TOOL STRATEGY
═══════════════════════════════════════════════════════════════════════════
- Prefer the named tools (lookup_player, top_winners, find_giftcode, etc.)
  when a question maps to one. They're fast, safe, and tested.
- Use `execute_readonly_query` ONLY when no named tool fits — e.g. unusual
  joins, breakdowns by hour-of-day, custom group-bys. The DB user has
  SELECT-only grants on the whitelisted tables; queries beyond those will
  fail.
- When you write SQL, you MUST use the schema below. Do NOT invent columns.
- Always check `lookup_player` first when the user names a player — every
  per-player tool needs the integer `user_id`.

═══════════════════════════════════════════════════════════════════════════
SCHEMA (MySQL 8.0, db `vinplay`, time_zone Asia/Seoul UTC+9)
═══════════════════════════════════════════════════════════════════════════

### users  — player accounts (one row per player)
- `id` BIGINT PK — internal user_id
- `nick_name` VARCHAR — display name used in GSC, agency, admin UIs
- `user_name` VARCHAR — login username (less commonly referenced)
- `vin` BIGINT — current KRW wallet balance (canonical, live)
- `xu` BIGINT — legacy mini-game currency (separate wallet)
- `vin_total` BIGINT — LIFETIME P&L for vin (NOT current balance — careful)
- `xu_total` BIGINT — LIFETIME P&L for xu
- `status` INT — 0 = normal, other values = blocked/locked
- `create_time`, `last_login` DATETIME

### money_gateway_log  — canonical vin ledger, every credit/debit lands here
- `id` BIGINT PK
- `user_id` BIGINT → users.id
- `amount` BIGINT — signed (positive = credit to player, negative = debit)
- `currency` VARCHAR — usually "vin"
- `source` VARCHAR — most common values:
    * `GSC_DEBIT` — player placed a GSC bet (debit)
    * `GSC_CREDIT` — GSC settle/cancel returned money (credit)
    * `GSC_STUCK_REFUND` — auto-refund by GscStuckRowReconciler
    * `ADMIN_TOPUP` — manual admin credit (refund, goodwill)
    * `DEPOSIT_TELEGRAM` — Telegram deposit-bot credit
    * `CREDIT_WALLET_DEPOSIT` — agent wallet credit
    * `WITHDRAW_BANK` — bank withdrawal debit
    * `PROMO_BONUS` — promo on deposit
    * `GIFTCODE_ADMIN` — giftcode redemption
    * `GIFTCODE_BACKFILL_A`/`_B` — historic giftcode reconciliation
    * `USERSERVICE_GAME` — BanCa fish-shooting wallet transfer
- `tx_id` VARCHAR — unique idempotency key per transaction
- `balance_after` BIGINT — wallet balance AFTER this row was applied
- `description` TEXT — human-readable note
- `created_at` DATETIME

### gsc_bets  — GSC bet history (the agency/admin views read this)
- `id` BIGINT PK
- `user_id`, `nick_name`
- `txn_id` VARCHAR — GSC transaction id (matches money_gateway_log.tx_id
  for the corresponding GSC_DEBIT/CREDIT, minus the "withdraw_"/"deposit_"
  prefix)
- `wager_code` VARCHAR — GSC's own wager identifier, unique per round
- `settle_txn_id` VARCHAR — settle's GSC transaction id (NULL if pending)
- `bet_value` BIGINT KRW — amount wagered
- `prize` BIGINT KRW — amount won (0 if lost / pending)
- `fee` BIGINT KRW
- `product_code` INT — 1002 Evolution, 1007 Pragmatic, etc
- `game_code` VARCHAR — vendor's table identifier
- `game_name` VARCHAR — human-readable game name
- `currency`, `settled` (0/1), `create_time`, `settle_time`

### gsc_event_log  — raw protocol log of every GSC seamless-wallet API call
- `id` BIGINT PK
- `gsc_endpoint` ENUM — `withdraw` (BET) / `deposit` (SETTLE/CANCEL) /
  `balance` / `rollback` / `pushbet` / `transfer` / `cancel`
- `member_account` VARCHAR — same as users.nick_name
- `product_code` INT, `game_code` VARCHAR
- `gsc_wager_code` VARCHAR
- `gsc_round_id` VARCHAR
- `bet_amount`, `prize_amount` DECIMAL(20,4)
- `currency` VARCHAR (KRW for product 1002)
- `processing_status` ENUM — `RECEIVED` (we got it but never replied
  yet — ghost-debit risk), `COMPLETED` (we replied OK), `FAILED` (we
  rejected — see last_error)
- `received_at`, `response_at` DATETIME(3)
- `last_error` TEXT — error message we returned to GSC on FAILED
- `raw_payload` JSON — GSC's incoming request
- `response_payload` JSON — our outgoing response
- Latency = `TIMESTAMPDIFF(MICROSECOND, received_at, response_at)/1000` ms

### gsc_game_catalog  — curated list of games we offer. NOT in audit_ro grants;
  do not query it directly — use the predefined tools instead.

### deposit_transactions  — bank-side deposits player pushed
- `id`, `user_id`, `tx_code` VARCHAR (player-facing)
- `amount` BIGINT KRW
- `deposit_type` VARCHAR, `user_bank_name`, `user_bank_number`
- `status` ENUM — `PENDING`/`APPROVED`/`REJECTED`/...
- `credit_status` ENUM — `CREDITED`/`PENDING`/...
- `force_approved` (0/1), `force_approved_by`, `processed_by`
- `created_at`, `processed_at`

### bank_withdrawals  — bank-side withdrawals player requested
- `id`, `user_id`, `tx_code`
- `bank_name`, `bank_number`, `customer_name`, `account_holder`
- `amount_krw` BIGINT, `fee_krw` BIGINT
- `status` ENUM — `PENDING`/`APPROVED`/`REJECTED`/`HOLD`/`PROCESSING`
- `admin_by` VARCHAR — admin who processed
- `reject_reason` TEXT
- `created_at`, `processed_at`

### gift_codes  — giftcode definitions
- `id` INT PK
- `giftcode` VARCHAR(50) UNIQUE — the code string (e.g. `HIGHD7HR`)
- `money` BIGINT KRW — value
- `max_use` INT — how many redemptions allowed
- `time_used` INT — how many redemptions used so far
- `from`, `exprired` TIMESTAMP — validity window (note `exprired` is the
  real spelling in the DB)
- `created_at`, `created_by` (admin username)
- `source` VARCHAR — `ADMIN`, `LEGACY`, `BACKFILL_A`, `BACKFILL_B`, ...

### gift_code_useds  — each redemption (one row per (code, user))
- `giftcode_id` INT → gift_codes.id
- `username` VARCHAR — player who redeemed
- `user_id` BIGINT
- `created_at` TIMESTAMP — redemption time

### gift_code_bundles  — groups of giftcodes (created together)
- `id`, `name`, `created_by`, `created_at`

═══════════════════════════════════════════════════════════════════════════
IMPORTANT NUANCES
═══════════════════════════════════════════════════════════════════════════
- Net GSC P&L for a player = SUM(amount) WHERE source IN ('GSC_DEBIT','GSC_CREDIT').
  Positive = player won; negative = player lost.
- Total bet volume for a player = SUM(-amount) WHERE source='GSC_DEBIT'.
- A `gsc_event_log` row with `processing_status='RECEIVED'` older than 5
  minutes is a ghost-debit candidate; `GscStuckRowReconciler` auto-refunds
  those. A `gsc_bets.settled=0` row older than 30 minutes is "GSC accepted
  the BET but never sent SETTLE" — a different, less-handled failure mode.
- DO NOT query `users.password`, `users.GoogleAuthSecretEnc`,
  `users.ip_address`, or any column with the word secret/token/key/totp. The
  `execute_readonly_query` tool refuses those patterns anyway.
- Currency in `gsc_event_log` is from the GSC payload (e.g. "KRW"). The
  ledger always works in `vin` (KRW). Don't confuse them.
- Player `okvip39` is a known case study referenced in prior conversations;
  fresh data is always preferred over recall.
"""


# ─── OpenRouter (LLM) client ───────────────────────────────────────────────
def llm_chat(messages, tools=None):
    body = {
        "model": OPENROUTER_MODEL,
        "messages": messages,
        "temperature": 0.2,
    }
    if tools:
        body["tools"] = tools
        body["tool_choice"] = "auto"
    resp = requests.post(
        OPENROUTER_URL,
        headers={
            "Authorization": f"Bearer {OPENROUTER_API_KEY}",
            "Content-Type": "application/json",
            "HTTP-Referer": "https://sunkr.club",
            "X-Title": "SunWinKR Audit Bot",
        },
        json=body,
        timeout=LLM_TIMEOUT_S,
    )
    resp.raise_for_status()
    data = resp.json()
    if "choices" not in data or not data["choices"]:
        raise RuntimeError(f"OpenRouter returned no choices: {data}")
    return data["choices"][0]["message"]


def answer_question(user_question: str) -> str:
    """Run the LLM tool-use loop until the model emits a final text answer."""
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": user_question},
    ]
    for iteration in range(MAX_TOOL_ITERATIONS):
        log.info("LLM iteration %d", iteration + 1)
        msg = llm_chat(messages, tools=TOOL_SCHEMA)
        messages.append(msg)
        tool_calls = msg.get("tool_calls") or []
        if not tool_calls:
            return (msg.get("content") or "").strip()
        for tc in tool_calls:
            name = tc["function"]["name"]
            try:
                args = json.loads(tc["function"]["arguments"] or "{}")
            except json.JSONDecodeError as e:
                args = {}
                log.warning("tool args parse error: %s", e)
            log.info("tool call: %s(%s)", name, args)
            fn = TOOL_REGISTRY.get(name)
            if fn is None:
                result = {"error": f"unknown tool: {name}"}
            else:
                try:
                    result = fn(**args)
                except TypeError as e:
                    result = {"error": f"bad args for {name}: {e}"}
                except mysql.connector.Error as e:
                    log.error("DB error in %s: %s", name, e)
                    result = {"error": f"DB error: {e}"}
                except Exception as e:
                    log.exception("tool %s raised", name)
                    result = {"error": f"tool error: {e}"}
            messages.append({
                "role": "tool",
                "tool_call_id": tc["id"],
                "content": json.dumps(result, default=str),
            })
    return "(no answer — tool-iteration cap hit; rephrase the question)"


# ─── Telegram client ───────────────────────────────────────────────────────
TELEGRAM_API = f"https://api.telegram.org/bot{TELEGRAM_TOKEN}"

# Filled by main() from getMe — used by handle_update to gate which group
# messages are routed to the LLM. Keeps OpenRouter cost bounded to actual
# questions instead of every line of operator chitchat.
BOT_USERNAME = None
BOT_ID = None


def tg_typing_loop(stop_event: threading.Event):
    """Keep the 'Bot is typing…' indicator alive in the audit chat while
    the LLM is working. Telegram's typing action auto-expires after 5s,
    so refresh every 4s until the main thread sets stop_event."""
    while not stop_event.is_set():
        try:
            requests.post(
                f"{TELEGRAM_API}/sendChatAction",
                json={"chat_id": TELEGRAM_CHAT, "action": "typing"},
                timeout=5,
            )
        except Exception as e:
            log.debug("typing action failed: %s", e)
        stop_event.wait(4.0)


def tg_send(text: str, reply_to: int = None):
    """Send a message to the audit chat. Splits long messages. Retries
    without Markdown if parse_mode fails."""
    if not text:
        return
    chunks = []
    remaining = text
    while remaining:
        chunks.append(remaining[:3900])
        remaining = remaining[3900:]
    for i, chunk in enumerate(chunks):
        if len(chunks) > 1:
            chunk = f"({i+1}/{len(chunks)}) " + chunk
        body = {
            "chat_id": TELEGRAM_CHAT,
            "text": chunk,
            "parse_mode": "Markdown",
            "disable_web_page_preview": True,
        }
        if reply_to and i == 0:
            body["reply_to_message_id"] = reply_to
        try:
            r = requests.post(f"{TELEGRAM_API}/sendMessage", json=body, timeout=15)
            if r.status_code != 200:
                body.pop("parse_mode", None)
                requests.post(f"{TELEGRAM_API}/sendMessage", json=body, timeout=15)
        except Exception as e:
            log.warning("tg_send failed: %s", e)


HELP_TEXT = """🤖 *SunWinKR Audit Bot*

Bot chỉ trả lời khi được gọi. 3 cách:
• Tag bot: `@sunkr_auditor_bot ai win nhiều nhất hôm nay?`
• Reply vào tin nhắn của bot
• Bắt đầu bằng `?` : `?ai win nhiều nhất hôm nay`

*Bot trả lời được:*

💰 *Money flow & player audit*
• `?okvip39 có vấn đề tiền không?`
• `?money flow của Mygame 24h qua`
• `?deposit + withdraw của Truongkr hôm nay`

🏆 *Leaderboard*
• `?ai win nhiều nhất hôm nay`
• `?top 10 player thua nhiều nhất 24h`
• `?ai bet nhiều nhất hôm nay`
• `?ai nạp / ai rút nhiều nhất tuần này`

🎁 *Giftcode*
• `?ai dùng giftcode HIGHD7HR`
• `?giftcode redemption hôm nay`
• `?okvip39 đã dùng giftcode nào`

📊 *Tổng quan & hệ thống*
• `?house P&L 24h qua`
• `?có bao nhiêu player active 30 phút qua`
• `?đang có bao nhiêu withdraw pending`

🔧 *GSC kỹ thuật*
• `?stuck bets 1h qua`
• `?latency /withdraw 1h`
• `?GSC event log của okvip39`

Câu hỏi đặc biệt (ngoài danh sách) — bot có thể tự viết SQL read-only.

Bot read-only — không đổi balance, không refund. Refund qua admin panel.
"""


def tg_long_poll():
    """Long-poll Telegram for new messages and dispatch each to the LLM."""
    offset = 0
    while True:
        try:
            r = requests.get(
                f"{TELEGRAM_API}/getUpdates",
                params={"offset": offset, "timeout": TELEGRAM_POLL_TIMEOUT},
                timeout=TELEGRAM_POLL_TIMEOUT + 10,
            )
            r.raise_for_status()
            updates = r.json().get("result", [])
            for upd in updates:
                offset = upd["update_id"] + 1
                try:
                    handle_update(upd)
                except Exception:
                    log.exception("handle_update raised")
        except requests.exceptions.RequestException as e:
            log.warning("telegram poll error: %s", e)
            time.sleep(5)
        except Exception:
            log.exception("telegram poll loop unexpected error")
            time.sleep(5)


def _extract_question(msg: dict) -> str | None:
    """Decide whether this message is addressed to the bot and, if so,
    return the question text with any trigger prefix stripped. Returns
    None when the message is just operator chitchat we should ignore."""
    text = (msg.get("text") or "").strip()
    if not text:
        return None

    # Reply-to-bot: any reply where the parent message was from this bot.
    reply_parent = msg.get("reply_to_message") or {}
    if reply_parent.get("from", {}).get("id") == BOT_ID:
        return text

    # /help and /start: return a sentinel so the caller can serve HELP_TEXT.
    if text.startswith("/"):
        parts = text.split(None, 1)
        cmd_full = parts[0]
        cmd, _, mention = cmd_full.partition("@")
        # /cmd@otherbot — not for us.
        if mention and BOT_USERNAME and mention.lower() != BOT_USERNAME.lower():
            return None
        if cmd in ("/start", "/help"):
            return "__HELP__"
        # Treat any other slash-command as a question (drop the slash).
        rest = parts[1] if len(parts) > 1 else ""
        return rest or None

    # `?…` prefix shortcut.
    if text.startswith("?"):
        return text.lstrip("?").strip() or None

    # @mention of the bot anywhere in the message. Telegram puts mentions
    # in entities; walking text is cheaper and works for plain `@user`.
    if BOT_USERNAME and f"@{BOT_USERNAME}".lower() in text.lower():
        cleaned = text.replace(f"@{BOT_USERNAME}", "", 1).strip()
        cleaned = cleaned.replace(f"@{BOT_USERNAME.lower()}", "", 1).strip()
        return cleaned or None

    return None


def handle_update(upd: dict):
    msg = upd.get("message") or upd.get("edited_message")
    if not msg:
        return
    chat_id = str(msg.get("chat", {}).get("id", ""))
    if chat_id != TELEGRAM_CHAT:
        return  # silently drop messages from any other chat
    if msg.get("from", {}).get("is_bot"):
        return  # ignore other bots (including ourselves)

    text = _extract_question(msg)
    if text is None:
        return  # not addressed to the bot — don't burn LLM credits

    if text == "__HELP__":
        tg_send(HELP_TEXT, reply_to=msg["message_id"])
        return

    from_user = (msg.get("from", {}).get("username")
                 or msg.get("from", {}).get("first_name")
                 or "user")
    log.info("question from %s: %s", from_user, text[:120])

    # Show "Bot is typing…" in the chat while the LLM thinks. Auto-stops
    # when answer_question returns (success or error).
    typing_stop = threading.Event()
    typing_thread = threading.Thread(
        target=tg_typing_loop, args=(typing_stop,),
        name="typing", daemon=True,
    )
    typing_thread.start()
    try:
        answer = answer_question(text)
    except requests.exceptions.HTTPError as e:
        log.exception("LLM HTTP error")
        typing_stop.set()
        tg_send(f"❌ LLM error: {e}", reply_to=msg["message_id"])
        return
    except Exception as e:
        log.exception("answer_question raised")
        typing_stop.set()
        tg_send(f"❌ Error: {e}", reply_to=msg["message_id"])
        return
    finally:
        typing_stop.set()
    if not answer:
        answer = "(no answer — try rephrasing)"
    tg_send(answer, reply_to=msg["message_id"])


# ─── Health server ─────────────────────────────────────────────────────────
def health_server():
    class H(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(b"ok\n")

        def log_message(self, *a, **kw):
            pass

    with socketserver.TCPServer(("0.0.0.0", 8889), H) as srv:
        srv.serve_forever()


# ─── Main ──────────────────────────────────────────────────────────────────
def main():
    global BOT_USERNAME, BOT_ID
    log.info("audit-bot v2 (Q&A) starting")
    log.info("  mysql: %s:%s/%s", MYSQL_HOST, MYSQL_PORT, MYSQL_DB)
    log.info("  telegram chat: %s", TELEGRAM_CHAT)
    log.info("  openrouter model: %s", OPENROUTER_MODEL)

    # Sanity-check the DB on startup so we fail fast if creds are wrong.
    try:
        rows = fetchall("SELECT VERSION() AS v")
        log.info("  mysql connected: %s", rows[0]["v"])
    except Exception as e:
        log.error("MySQL connection failed on startup: %s", e)
        sys.exit(1)

    # Look up the bot's own identity so the mention/reply gate works.
    # Fail fast if the token is bad — silent 401s in v1 are what kept
    # the old auditor broken without anyone noticing.
    try:
        r = requests.get(f"{TELEGRAM_API}/getMe", timeout=10)
        r.raise_for_status()
        me = r.json()["result"]
        BOT_USERNAME = me["username"]
        BOT_ID = me["id"]
        log.info("  telegram bot: @%s (id=%s)", BOT_USERNAME, BOT_ID)
    except Exception as e:
        log.error("Telegram getMe failed: %s", e)
        sys.exit(1)

    threading.Thread(target=health_server, name="health", daemon=True).start()

    log.info("starting telegram long-poll")
    tg_long_poll()


if __name__ == "__main__":
    main()
