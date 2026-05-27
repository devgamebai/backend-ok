# Money Flow on a Bet — Two Channels Side by Side

**Audience:** ops + FE devs debugging "balance not real-time" complaints.
**Updated:** 2026-05-08
**Scope:** what happens between "user clicks BET" and "HUD shows new balance" for offline games (TaiXiu, SicBo, Slot, etc.). GSC and AWC seamless follow a different (provider-driven) path; out of scope here.

This doc answers three questions the operator asked today:

1. *Does the bot lose money in the same round when the player loses?*
2. *Why does my HUD only refresh after the round settles, not when I bet?*
3. *What is the canonical balance source — MySQL, Redis Streams, or the WebSocket push? Where can the read be wrong?*

---

## 1. Bird's-eye flow on a single bet

```
                         ┌─────────────────────────┐
                         │ Player clicks BET in    │
                         │ Cocos client            │
                         └────────────┬────────────┘
                                      │ cmd 28110 (BetSicboCmd)
                                      ▼
                         ┌─────────────────────────┐
                         │ SicboModule.handle      │
                         │   → MGRoomSicbo.bet…    │
                         └────────────┬────────────┘
                                      │ userService.updateMoney(-bet)
                                      ▼
                         ┌─────────────────────────┐
                         │ UserServiceImpl         │
                         │ updateMoney(...)        │
                         └────────────┬────────────┘
                                      │ MoneyGateway.creditUserWithCumulative(-bet)
                                      ▼
              ┌──────────────────────────────────────────┐
              │ MoneyGateway (atomic, single-DB-tx)      │
              │  1. isDuplicate(txId, source, userId)?   │
              │  2. UPDATE users SET vin = vin + delta   │
              │       WHERE id = ? AND vin + delta >= 0  │
              │  3. SELECT vin, vin_total FROM users     │
              │  4. INSERT IGNORE money_gateway_log      │
              │  5. COMMIT                               │
              └────────────────────┬─────────────────────┘
                                   │ (returns CreditResultWithCumulative.newBalance)
                                   ▼
                ┌────────────────────────────────────┐
                │ UserServiceImpl (inline, post-tx)  │
                │  • userMap.lock(nickname)          │
                │  • user.setMoney(col, newBalance)  │
                │  • user.setTotalPnl(...)           │
                │  • userMap.put(nick, user)         │
                │  • publish(queue_payment, msg)     │  → vbee → log_money_user_*
                │  • publish(queue_log_money, msg)   │  → mongo log feed
                └─────────┬─────────────┬────────────┘
                          │             │
                  channel A│             │channel B
                          │             │
                          ▼             ▼
   ┌──────────────────────────┐   ┌──────────────────────────┐
   │ MGRoomSicbo replies on   │   │ MoneyGateway.updateCache │
   │ the GAME WS:             │   │ AndPushSync (post-tx):   │
   │  cmd 28110 BetSicboMsg   │   │  • lock users map again  │
   │  payload includes        │   │    (redundant safety)    │
   │  currentMoney field      │   │  • PUBLISH RMQ           │
   │  (== newBalance)         │   │      queue_action_minigame│
   └────────┬─────────────────┘   │      queue_action_portal │
            │                     └─────────────┬────────────┘
            │                                   │
            ▼                                   ▼
   ┌──────────────────────┐    ┌────────────────────────────────────┐
   │ Cocos: case BET → … │    │ portal-api: PortalBalanceConsumer  │
   │ updateBalance(money) │    │ reads queue_action_portal,         │
   │ Configs.Login.Coin = │    │ resolves nickname → /ws/balance    │
   │ broadcastReceiver.   │    │ session, sends JSON                │
   │ send(USER_UPDATE_COIN)│    │ {"type":"balance","vin":N,"xu":M}  │
   └──────────────────────┘    └─────────────┬──────────────────────┘
                                             │
                                             ▼
                                ┌────────────────────────┐
                                │ FE /ws/balance handler │
                                │ updates the HUD label  │
                                └────────────────────────┘
```

Two **independent** channels carry the new balance to the client. Both
reach the FE within ~100 ms of each other on a healthy stack. If the HUD
"only updates after the round settles", one of those channels is broken
on the client side — the wire data is there.

---

## 2. Source-of-truth contract

| Layer | Role | Latency | Volatile? |
|---|---|---|---|
| **MySQL `vinplay.users.vin` / `vin_total`** | Canonical balance. Atomic UPDATE inside MoneyGateway. | sync, ~5–10 ms | No — survives JVM restart. |
| **MySQL `money_gateway_log`** | Audit row for every credit/debit. UNIQUE `(tx_id, source, user_id, currency)` is the idempotency anchor. | written in same tx as the UPDATE | No. |
| **Hazelcast `users` IMap** | Hot cache for game-server reads (no SQL hit on every bet). Stamped inline by `UserServiceImpl` after MoneyGateway commit. | ~1 ms after MoneyGateway tx | Yes — TTL 1h on entries. |
| **RabbitMQ `queue_payment`** | Delivers `MoneyMessageInMinigame` to vbee consumer → writes `mongo log_money_user_vin / _xu`. Used by reporting/agency, NOT by HUD. | async ~10–50 ms | Yes — drained on consume. |
| **RabbitMQ `queue_action_minigame`** | Delivers `NotiGameMessage` to game-server `BalanceUpdateProcessor` → in-game player widget refresh. | async ~10–50 ms | Yes. |
| **RabbitMQ `queue_action_portal`** | Delivers same `NotiGameMessage` to portal-api `PortalBalanceConsumer` → `/ws/balance` JSON push. | async ~10–50 ms | Yes. |

Order of writes is fixed:

```
SQL commit  →  Hazelcast cache update  →  RMQ publish (3 queues)
```

The SQL commit is the system-of-record. Everything else derives from it.
Cache and queues are best-effort: if they crash mid-flight the next
read pulls from MySQL.

**Redis Streams is NOT in this path.** RedisStreams is used for
cross-game-server messaging (game-room state, taixiu round signals),
NOT for player wallet updates. The "RMQ vs Redis Streams" toggle in
`.env` (`MESSAGE_BUS_*`) only affects the queue transport; the wallet
update logic itself is unchanged.

---

## 3. Two ways the FE learns the new balance

### 3.1 Channel A — game-WebSocket bet response (synchronous, low-latency)

`MGRoomSicbo.betTaiXiu` immediately writes a `BetSicboMsg` (cmd `28110`)
back to the player's BitZero session. Payload includes `currentMoney`
read from `MoneyResponse.getCurrentMoney()` — that is `newBalance` from
the MoneyGateway tx.

The Cocos client handler at
`sunwinkr-client/assets/SicboTaiPhu/scripts/TaiXiuFull.ts:704-710`:

```ts
case cmd.Code.BET:
    let betRs = new cmd.ReceiveBet(data);
    this.nodePlayers.children[0].getComponent("TaiXiuFullPlayer")
        .updateBalance(betRs.currentMoney);
    Configs.Login.Coin = betRs.currentMoney;
    BroadcastReceiver.send(BroadcastReceiver.USER_UPDATE_COIN);
```

The avatar widget's label refreshes. `Configs.Login.Coin` global
updates. The `USER_UPDATE_COIN` broadcast fires.

For the HUD label (the top-bar wallet display) to refresh, **somebody
in the SicBo scene must subscribe to that broadcast**. As of
2026-05-08 the SicBo scene is the only minigame that did NOT register
a `BroadcastReceiver.register(USER_UPDATE_COIN, ...)` call in `start()`.
Patch in source at `TaiXiuFull.ts:start()` adds it; needs a Cocos
Creator 2.4.4 build + redeploy. XocDia / MauBinh / Lieng / Slot1-11 /
Poker / ShootFish / Loto already have the observer.

### 3.2 Channel B — `/ws/balance` JSON push (async, lobby & cross-game)

Independent WebSocket on `/ws/balance?token={accessToken}` served by
`com.vinplay.api.ws.BalanceWebSocketServlet` in portal-api.

- On connect, server reads `users` IMap (Hazelcast) and sends
  `{"type":"auth_ok","nickname":...,"vin":N,"xu":M}`.
- After auth, server pushes `{"type":"balance","vin":N,"xu":M}` JSON
  whenever `PortalBalanceConsumer` receives a `queue_action_portal`
  message for that nickname.
- The consumer reads the FRESH `users` IMap entry (NOT the message
  payload) — so the value pushed is whatever Hazelcast holds at consume
  time. Cache lag = staleness of the push.

This channel is what the **lobby HUD** typically uses — it survives
across game scene transitions because the WebSocket lives on
`staging-play.sunkr.club/ws/balance`, not on the game-WS subdomain.

### 3.3 Why the operator sees "only on lose"

Working hypothesis after today's debug session:

- Channel A (cmd `28110` bet response) **is** firing at bet time. CDP
  network capture confirms.
- Channel A's update reaches the avatar widget but **not the HUD label**
  because the SicBo scene is missing the `USER_UPDATE_COIN` observer.
- Channel B's push fires too — but the lobby HUD that listens to
  `/ws/balance` may be hidden underneath the SicBo scene; the SicBo
  scene paints over it.
- Round settle (LOSE) emits a different cmd that the SicBo scene DOES
  observe (e.g., result message), and that handler reads
  `Configs.Login.Coin` to repaint the HUD — by then Channel A has
  already mutated the global, so the displayed value is fresh.

→ Fix is the missing observer in `TaiXiuFull.ts:start()`. Source patched
2026-05-08; needs a Cocos Creator client rebuild + Web Mobile redeploy.

---

## 4. Bot money on a SicBo round

Q: *Does the bot lose money when I lose?*

A: **No.** Bots are virtual. Their "balances" displayed in
`UpdateSicboPerSecondMsg` (cmd `28112`) come from `BotMinigame.banVin`
which adjusts an in-memory cache via `BotServiceImpl.addMoney`. There
is no MySQL write for a bot bet/loss/win. Bot rows in the `users` table
exist (with `is_bot=1`) so a real player can see them in the room, but
the bet/settle path treats them as a no-op.

Concretely in `MGRoomSicbo.betTaiXiu(String nickname, …, boolean isBot)`:

```java
if (!isBot || isLivestream) {
    res = userService.updateMoney(...);   // real player → SQL debit
} else {
    res.setSuccess(true);                 // bot path: no DB write
}
```

The bot's balance number you see in the per-second tick is the
in-memory accounting only. Real money never moves on a bot bet.

---

## 5. Why the bet was failing earlier today

Found and fixed during this session — logged here for completeness so
nobody re-introduces it:

> `MoneyGateway.isDuplicate(txId, source)` filtered only on
> `(tx_id, source)`. The actual UNIQUE constraint
> `uk_tx_source(tx_id, source, user_id, currency)` includes `user_id`,
> so the SQL guard accepts cross-user duplicates correctly — but the
> in-app pre-check did not. `UserServiceImpl.updateMoney` builds
> `txId = "userservice:" + roundId`; every player in a SicBo round
> shared the same `txId`, so the second bettor onward got
> `1031 Duplicate transaction` and the bet never debited.
>
> Fix at `MoneyGateway.isDuplicate(txId, source, userId)` — added the
> third parameter, threaded `userId` through every caller
> (creditUser, creditUserWithCumulative, debitUser,
> debitUserAllowNegative, transferBetweenUsers).
> Server-side debit verified live: laviai 49920710 → 49919710 (-1000)
> on a 1k bet, `money_gateway_log` row landed in same transaction.

---

## 6. To make balance update INSTANTLY on bet

Two complementary fixes, pick either or both:

1. **Cocos client: register the missing observer** (recommended, 1-line
   change already applied in source — needs a build).

   ```ts
   protected start(): void {
     BroadcastReceiver.register(BroadcastReceiver.USER_UPDATE_COIN,
       () => {
         const me = this.nodePlayers && this.nodePlayers.children[0];
         me?.getComponent("TaiXiuFullPlayer")?.updateBalance(Configs.Login.Coin);
       }, this);
     // … existing socket listeners …
   }
   ```

   When the Channel A bet response arrives (within ~10 ms of the SQL
   commit), `Configs.Login.Coin` is set, the broadcast fires, the
   observer reads the new value and updates the HUD label. No
   server-side change required.

2. **Server: also send a `cmd 28210`-style explicit balance push from
   `MGRoomSicbo.betTaiXiu` — belt-and-braces.** Optional. Channel A
   already carries the value; this only helps if the cocos client's
   bet handler is itself broken.

The `/ws/balance` channel is also delivering the new value via the
`PortalBalanceConsumer` path (verifiable by listening on
`wss://staging-play.sunkr.club/ws/balance?token=…` and watching for
`{"type":"balance","vin":...}` after a bet).

---

## 7. Verification queries

```sql
-- A. Most recent debit for player + balance trail
SELECT id, nick_name, amount, source, tx_id, balance_after, created_at
  FROM vinplay.money_gateway_log
 WHERE user_id = (SELECT id FROM vinplay.users WHERE nick_name = 'laviai')
 ORDER BY id DESC LIMIT 10;

-- B. Live balance vs cache
SELECT id, nick_name, vin, vin_total FROM vinplay.users WHERE nick_name = 'laviai';
-- Compare with /ws/balance auth_ok response or with `users.get('laviai').vin`
-- read from a Hazelcast client. Drift > 1 round = cache replication issue.

-- C. Idempotency UNIQUE intact
SHOW INDEX FROM vinplay.money_gateway_log WHERE Non_unique = 0;
-- Expected: uk_tx_source on (tx_id, source, user_id, currency).
```

---

## 8. Out of scope

- GSC seamless-wallet (third-party live casino). Different debit path
  via `WithdrawProcess` + `MoneyGateway` with `GSC_DEBIT` source.
- AWC seamless-wallet — same, `AWC_DEBIT` source.
- Recovery/refund flows triggered by `WithdrawCallbackProcessor` etc.
- Agency wallet credits (DOWNLINE rebate) — separate from player wallet.
