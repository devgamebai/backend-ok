# Runbook — Rotate `TELEGRAM_DEPOSIT_BOT_TOKEN`

**When to use:** the deposit Telegram bot is showing persistent HTTP 409 `Conflict: terminated by other getUpdates request` in `sunwinkr-vbee` logs, meaning a non-production client is also polling the bot. Symptom for the operator: clicking Approve/Reject on Telegram does nothing OR works inconsistently; `sunwinkr-vbee` logs show no `Telegram callback: action=...` lines.

**Total time:** 5 minutes hands-on. Bot is offline for ~10 seconds during the vbee restart.

**Reversible?** Yes — rotate again to a third token if needed. The rotation itself is one-way (the revoked token never works again).

---

## Pre-flight

1. **Open the bot's chat history in Telegram on your phone** — you'll need @BotFather access.
2. **Identify the operator(s) who currently use the bot.** They lose access for ~10 seconds during step 3; warn them in the alerts chat first if that matters.
3. **Confirm the symptom is the 409 conflict, not something else:**

   ```bash
   docker logs --since 5m sunwinkr-vbee 2>&1 | grep -c "HTTP 409"
   # Expect: a number > 10 (one per ~11 seconds). If 0, the bot isn't blocked by a rival client — this runbook won't help.
   ```

4. **Make sure you have shell + git access to the production host** (the `.env` and `docker compose` commands run from `/root/sunwinkr/sunwinkr-backend`).

---

## Step 1 — Revoke + regenerate token via @BotFather

In Telegram:

```
Open chat with @BotFather
→ /mybots
→ pick the deposit bot (the one whose token starts with the prefix in your .env)
→ tap "API Token"
→ tap "Revoke current token"
→ confirm
```

BotFather will then display the **new token**. Copy it.

Effect: the old token is invalid globally from this moment. All clients — ours AND the rival — get HTTP 401 on their next call. The rival client crashes or backs off; whoever was running it will notice their integration broke (which is how we surface the unknown deployment).

The bot's username, chat memberships, and admin permissions are unchanged.

---

## Step 2 — Update `.env` and restart vbee

On the production host:

```bash
cd /root/sunwinkr/sunwinkr-backend
# 1. Edit .env (use the editor of your choice — vim, nano, sed)
#    Replace the value of TELEGRAM_DEPOSIT_BOT_TOKEN with the new one from BotFather.
vim .env

# 2. Verify the new value is correct (don't echo the full token in shared logs; check the prefix)
grep '^TELEGRAM_DEPOSIT_BOT_TOKEN=' .env | head -c 40
# Expect: TELEGRAM_DEPOSIT_BOT_TOKEN=<new-prefix>...

# 3. Force-recreate vbee with the new env (8s downtime, only vbee)
docker compose -f docker-compose.backend.yml up -d --force-recreate --no-deps sunwinkr-vbee
```

Within ~10 seconds vbee is back up with the new token.

---

## Step 3 — Verify the fix

```bash
# Should be near-zero (one or two transient 409s during the restart window are OK)
docker logs --since 1m sunwinkr-vbee 2>&1 | grep -c "HTTP 409"

# Should show the poller started cleanly
docker logs --since 1m sunwinkr-vbee 2>&1 | grep -E "DepositTelegramPoller|DepositBotLauncher"
# Expect lines like:
#   DepositBotLauncher: Telegram poller thread started
#   DepositTelegramPoller started
```

Then have an operator **click Approve on any pending deposit** in Telegram, and confirm:

```bash
docker logs --since 2m sunwinkr-vbee 2>&1 | grep "Telegram callback: action="
# Expect a line like:
#   Telegram callback: action=approve txId=NNN operator=seoul_6789 messageId=...

docker logs --since 2m sunwinkr-vbee 2>&1 | grep "approved by.*via Telegram"
# Expect:
#   Deposit NNN approved by seoul_6789 via Telegram, credited=true
```

If both lines show up, the bot is back and processing approvals through our backend.

---

## Step 4 — Roll the new token to the other 7 containers (optional, can be deferred)

The other containers (`backend-api`, `game-minigame`, `portal-api`, `game-thirdparty`, `banca`, `game-slot`, `history-drainer`) use the bot only for **sending** messages (errors, deposit notifications, audit alerts). While they still have the old token, their `sendMessage` calls fail with 401 — but those are best-effort alerts, not blocking. The main approval flow works the moment vbee has the new token.

You can roll them at your convenience. To do it now:

```bash
docker compose -f docker-compose.backend.yml up -d --force-recreate --no-deps \
  sunwinkr-backend-api sunwinkr-portal-api sunwinkr-game-minigame \
  sunwinkr-game-thirdparty sunwinkr-banca sunwinkr-game-slot \
  sunwinkr-history-drainer
```

Each is ~8s downtime. Stagger them if you want zero-overlap, or do all at once (they don't share critical state for this purpose).

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `HTTP 409` errors continue after the restart | The rival client also has the new token. Did you paste it somewhere it could be exfiltrated? | Repeat the rotation; this time keep the new token strictly in `.env` + BotFather. |
| `HTTP 401 Unauthorized` in logs | Token in `.env` doesn't match BotFather's. Typo, trailing whitespace, or `.env` not picked up. | `grep TELEGRAM_DEPOSIT_BOT_TOKEN .env` and visually compare to BotFather. Re-run the compose recreate. |
| No callbacks coming through, no 409, no 401 | No operator has clicked anything yet, or the bot isn't in the deposit alerts chat. | Send a test message to the bot via curl: `curl "https://api.telegram.org/bot$TOKEN/sendMessage?chat_id=$TELEGRAM_DEPOSIT_CHAT_ID&text=test"`. If you see it in Telegram, the bot is wired correctly — the issue is just no inbound clicks. |
| `DepositBotLauncher: disabled via DEPOSIT_BOT_ENABLED=false` | This vbee instance is configured as a replica that shouldn't run the poller (SUN-796 horizontal-scale guard). | Check `.env` — for a single-vbee deployment this should be `true` or unset. |
| `DepositBotLauncher: TELEGRAM_DEPOSIT_CHAT_ID not set` | `.env` is missing the chat id (separate from the token) | Restore from the previous `.env` backup. |

---

## Post-rotation hygiene

1. **Audit who knew the old token.** Anyone with the old token is gone now, but if it leaked once, the new token can leak too. Check `.env` permissions:

   ```bash
   ls -la /root/sunwinkr/sunwinkr-backend/.env
   # Expect: -rw------- (mode 600), owned by root
   ```

2. **If the rival reappears with the new token within a few hours**, the leak is active and you have a bigger problem than this runbook can fix — investigate the `.env` distribution chain (CI/CD, backup copies on dev machines, staging configs).

3. **Add a permanent alert.** A cron that greps `docker logs sunwinkr-vbee` for `HTTP 409` and pages if the count exceeds 20/hour will catch a repeat instantly.

   ```bash
   # /root/sunwinkr/sunwinkr-backend/scripts/telegram-bot-409-alarm.sh (new)
   # Runs every 15 min via cron, pages if 409s > 20 in last 15 min
   ```

4. **Consider migrating to webhook mode (Option B).** Once we have a webhook URL registered with Telegram, `getUpdates` returns 409 to ALL other long-pollers structurally — token theft alone can't bring the rival back online. See [WALLET_LEDGER_MIGRATION_PLAN.md](../architecture/WALLET_LEDGER_MIGRATION_PLAN.md) for the broader hardening roadmap; webhook migration is a candidate for that Phase 6 work.

---

## What this rotation does NOT fix

- **Stuck deposit rows in `PENDING_CREDIT`** from before the rotation (e.g., Dat2lit's tx 483) are not auto-recovered. Manual SQL patch per [docs/incidents/2026-05-11_*.md](../incidents/) or the deposit-reconciliation drainer (planned, not built).
- **Operators who clicked Approve in the window where the rival was winning callbacks.** Their click was processed by the rival, which we don't control. The rival might have committed partial state to our DB (Dat2lit's row showing `lock_platform=TELEGRAM` despite our vbee never seeing the callback is consistent with this). Audit deposit rows for `status=APPROVED AND credit_status=PENDING_CREDIT` and run a reconciliation:

  ```sql
  SELECT dt.id, dt.tx_code, dt.amount, dt.processed_at,
         mgl.id AS mgl_credit_id, mgl.created_at AS credited_at
  FROM vinplay.deposit_transactions dt
  LEFT JOIN vinplay.money_gateway_log mgl
    ON mgl.tx_id = CAST(dt.id AS CHAR) AND mgl.source IN ('DEPOSIT_TELEGRAM', 'DEPOSIT_ADMIN')
  WHERE dt.status = 'APPROVED' AND dt.credit_status = 'PENDING_CREDIT'
    AND dt.processed_at < NOW() - INTERVAL 5 MINUTE;
  ```

  For each row: if `mgl_credit_id` is non-null, money was credited and the row state is stuck — fix per the Dat2lit pattern (UPDATE `credit_status='CREDITED'` + INSERT audit row).

- **The architectural reason the 409 was so bad.** Long-polling with a shared secret is fragile by design. Webhook + secret-token header per request is the structural fix; this runbook just rotates the secret.

---

## Quick reference card

```
1. Telegram: @BotFather → /mybots → pick bot → API Token → Revoke
2. Copy new token
3. ssh prod-host
   cd /root/sunwinkr/sunwinkr-backend
   vim .env  # update TELEGRAM_DEPOSIT_BOT_TOKEN
   docker compose -f docker-compose.backend.yml up -d --force-recreate --no-deps sunwinkr-vbee
4. docker logs --since 1m sunwinkr-vbee | grep "HTTP 409"   # expect 0
5. Operator clicks Approve on a real deposit. Watch:
   docker logs --since 1m sunwinkr-vbee | grep "Telegram callback"
6. Optional: roll the new token to the other 7 containers later
```
