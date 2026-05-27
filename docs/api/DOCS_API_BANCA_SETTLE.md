# BanCa Unified-Wallet Settle Bridge — c=9998

SUN-1054 / Wallet Phase 5b. HTTP bridge dùng cho BanCa C# server post session-settle qua MoneyGateway của Java backend (ví thống nhất `PLAYER_VIN`).

- **Base URL:** `http://backend-api:19082/api_backend` (chỉ truy cập trong Docker network, không expose qua nginx public).
- **Method:** POST, `Content-Type: application/json`
- **Command ID:** `9998`
- **Auth:** header `X-Service-Token` phải khớp env `BANCA_SERVICE_TOKEN` (cùng secret như c=9854 LogBetCommission). Không dùng `aat`.
- **Caller:** `banca/Core/Libs/UnifiedWallet/MoneyGatewayClient.cs` (`SettleAsync`, `BatchSettleAsync`). Mọi field tự encode chính xác cùng format dưới.
- **Kill switch (caller side):** env `BANCA_USE_UNIFIED_WALLET=0` (default OFF). Khi OFF, BanCa rơi về legacy Redis `IncEpicCash` path — endpoint này không được gọi.

---

## Request

```
POST /api_backend?c=9998 HTTP/1.1
Host: backend-api:19082
X-Service-Token: <BANCA_SERVICE_TOKEN>
Content-Type: application/json

{
  "user_id":       12345,
  "amount_milli":  -870000,
  "session_id":    "bc-12345-7",
  "tx_type":       "WAGER_DEBIT_BANCA",
  "nick_name":     "laviai",
  "checkpoint_ms": 1713750000123
}
```

| Field | Type | Required | Note |
|---|---|---|---|
| `user_id` | long | yes | `users.id` |
| `amount_milli` | long (signed) | yes | BanCa làm việc trên đơn vị milli-VND. **Dấu khớp `tx_type`**: credit dùng dương, debit dùng âm. |
| `session_id` | string | yes | BanCa-side. Định dạng `bc-{userId}-{worldId}`, `bc-tick-{userId}-{worldId}`, `bc-openTx-{userId}-{worldId}`, `bc-revive-{userId}-{worldId}`. |
| `tx_type` | string | yes | Một trong: `WAGER_DEBIT_BANCA` (thua), `WAGER_CREDIT_BANCA` (thắng), `EMERGENCY_BANCA` (Revive crash recovery). |
| `nick_name` | string | no | Hint để tránh round-trip MySQL. Khi rỗng, processor SELECT `users.nick_name WHERE id=?`. |
| `checkpoint_ms` | long | yes | `TimeUtil.TimeStamp` lúc dispatch ở C#. Idempotency key. |

### Quy ước milli-VND → VND

Processor convert `abs(amount_milli) / 1000` (floor toward zero). Sub-VND residue (`amount_milli` từ -999 → 999, ≠ 0) trả `success=true, reason=sub_vnd_residue` mà không đụng ledger — caller giữ residue và flush kế tiếp.

### external_ref (idempotency)

Server build:
```
banca:settle:{user_id}:{session_id}:{checkpoint_ms}
```
Khớp byte-for-byte với `BuildExternalRef` trong `MoneyGatewayClient.cs`. Dedup qua UNIQUE `(tx_id, source)` trên `money_gateway_log`. C# retry (mặc định 3 lần) gọi lại cùng `external_ref` → server trả `reason=deduped` với `balance_after_vnd` của lần đầu.

---

## Response — Success

HTTP `200`:

```json
{
  "success": true,
  "ledger_tx_id": 0,
  "balance_after_vnd": 1500000,
  "reason": "posted"
}
```

| `reason` | Khi nào |
|---|---|
| `posted` | Lần đầu tiên — money_gateway_log + money_ledger (nếu `MONEY_LEDGER_DUAL_WRITE=true`) đã ghi. |
| `deduped` | `(tx_id, source)` đã tồn tại; trả về `balance_after_vnd` đã ghi. |
| `zero_amount` | `amount_milli=0` — no-op ack. |
| `sub_vnd_residue` | `|amount_milli| < 1000` — no-op ack, residue carry-over ở C#. |

Trường `ledger_tx_id` hiện trả `0` (sentinel); sẽ wire real id khi `CreditResult` được mở rộng theo pattern SUN-1248.

`balance_after_vnd` = số dư VND sau khi settle (cột `users.vin`).

---

## Response — Error

HTTP `200` với `success=false` cho mọi lỗi terminal (C# **không retry**); HTTP `5xx` cho transient (C# retry tối đa 3 lần với exponential backoff 100/200/400ms).

```json
{ "success": false, "errorCode": "4002", "message": "unsupported tx_type: WAGER_FOO" }
```

| errorCode | HTTP | Khi nào | C# retry? |
|---|---|---|---|
| `1001` | 200 | `X-Service-Token` thiếu hoặc sai, hoặc env `BANCA_SERVICE_TOKEN` chưa set. | NO (terminal) |
| `1002` | 200 | `user_id` không tồn tại trong `users`. | NO |
| `4001` | 200 | Thiếu field bắt buộc (`user_id`, `amount_milli`, `session_id`, `tx_type`, `checkpoint_ms`), JSON body rỗng/invalid. | NO |
| `4002` | 200 | `tx_type` không nằm trong whitelist 3 giá trị hợp lệ. | NO |
| `4003` | 200 | Dấu của `amount_milli` không khớp hướng `tx_type` (vd credit nhưng âm). Surface lỗi BanCa-side, không silent. | NO |
| `4004` | 200 | Insufficient balance — debit > `users.vin`. Terminal: replay không bao giờ thành công. | NO |
| `9999` | 200 | Lỗi nội bộ (DB outage, MoneyGateway throw). | YES |

C# `MoneyGatewayClient.PostWithRetry`:
- HTTP 2xx → success ngay.
- HTTP 4xx → log + push `banca:failed_settle`, không retry.
- HTTP 5xx / timeout / exception → retry 3 lần, sau đó push `banca:failed_settle`.

---

## Idempotency semantics — important

Cùng `external_ref` post lại phải trả ledger outcome y hệt lần đầu. Cụ thể:

1. Lần 1 (POSTED): `success=true`, `reason=posted`, `balance_after_vnd=X`.
2. Lần 2 (replay): `success=true`, `reason=deduped`, `balance_after_vnd=X` (đọc lại từ `money_gateway_log.balance_after` của lần 1).

Hệ thống tuyệt đối không double-credit / double-debit. Đảm bảo qua UNIQUE constraint `uk_tx_source` trên `money_gateway_log(tx_id, source, user_id, currency)`.

---

## End-to-end example

```bash
# Generate token first time
docker exec sunwinkr-game-banca env | grep BANCA_SERVICE_TOKEN
# -> BANCA_SERVICE_TOKEN=<32-char-hex>

# Simulate a BanCa quit settle (player lost 870 VND)
curl -s -X POST 'http://backend-api:19082/api_backend?c=9998' \
  -H 'Content-Type: application/json' \
  -H "X-Service-Token: $BANCA_SERVICE_TOKEN" \
  -d '{
    "user_id":       12345,
    "amount_milli":  -870000,
    "session_id":    "bc-12345-7",
    "tx_type":       "WAGER_DEBIT_BANCA",
    "nick_name":     "laviai",
    "checkpoint_ms": 1713750000123
  }'

# Expected (POSTED):
# {"success":true,"ledger_tx_id":0,"balance_after_vnd":1499130,"reason":"posted"}

# Replay same payload → deduped:
# {"success":true,"ledger_tx_id":0,"balance_after_vnd":1499130,"reason":"deduped"}
```

---

## Source mapping (Java side)

| `tx_type` (HTTP) | `SOURCE_*` constant | MoneyGateway path | Ledger type | System account |
|---|---|---|---|---|
| `WAGER_DEBIT_BANCA`  | `SOURCE_WAGER_DEBIT_BANCA`  | `debitUser`  | `WAGER_DEBIT`  | `HOUSE_GAME_POT` |
| `WAGER_CREDIT_BANCA` | `SOURCE_WAGER_CREDIT_BANCA` | `creditUser` | `WAGER_CREDIT` | `HOUSE_GAME_POT` |
| `EMERGENCY_BANCA`    | `SOURCE_EMERGENCY_BANCA`    | `debitUser`  | `WAGER_DEBIT`  | `HOUSE_GAME_POT` |

Mọi entry đều đi qua dual-write (`MONEY_LEDGER_DUAL_WRITE=true`) — nếu flag bật thì có thêm row trong `money_transaction` (ledger PLAYER_VIN ↔ HOUSE_GAME_POT). Khi flag OFF chỉ có row `money_gateway_log`.

---

## Operational notes

- **Latency target:** ≤100ms p99 nội mạng Docker. Đo bằng script ở `docs/WALLET_PHASE5B_BANCA_GAME_LOOP_IMPL.md` (Phase 5a benchmark, chưa chạy production).
- **Audit:** mỗi call (kể cả deduped) sinh log line ở `backend.log` qua `MoneyGateway.{credit,debit}User: ...`.
- **Bot exclusion:** `users.is_bot=1` được skip dual-write (xem `MoneyGateway.isBotUser`) nhưng `money_gateway_log` vẫn ghi đầy đủ. BanCa caller không cần phân biệt.
- **`MONEY_LEDGER_DUAL_WRITE`:** env var ở Java backend bật/tắt ghi `money_ledger`. Mặc định OFF; bật trong staging trước khi flip BanCa kill switch.
- **`BANCA_USE_UNIFIED_WALLET`:** env var ở BanCa container bật/tắt việc gọi endpoint này. Mặc định OFF.

## Configuration env vars (BanCa container)

| Env | Default | Purpose |
|---|---|---|
| `BANCA_USE_UNIFIED_WALLET` | `0` | Master kill switch. `1` → gọi c=9998. `0` → legacy Redis IncEpicCash. |
| `BANCA_MONEYGATEWAY_URL` | derived from `xxeng-backend` | URL endpoint, ví dụ `http://backend-api:19082/api_backend?c=9998`. |
| `BANCA_SERVICE_TOKEN` | _required_ | Khớp với env Java backend cùng tên. |
| `BANCA_SETTLE_INTERVAL_MS` | `5000` | Periodic flush cadence. |
| `BANCA_SETTLE_THRESHOLD` | `10000` | Min `|profit|` flush mỗi tick. |
| `BANCA_BIG_BET_THRESHOLD` | `50000` | Immediate-settle threshold cho 1 shot/kill. |
| `BANCA_SETTLE_TIMEOUT_MS` | `5000` | HTTP timeout. |
| `BANCA_SETTLE_MAX_RETRIES` | `3` | Số lần retry trước khi LPUSH `banca:failed_settle`. |

## Follow-up

- Phase 5a benchmark: chạy script trong `WALLET_PHASE5B_BANCA_GAME_LOOP_IMPL.md` để xác nhận p99 ≤100ms trước khi flip `BANCA_USE_UNIFIED_WALLET=1`.
- Phase 5d shadow + reconcile: hourly job so sánh Redis `User_Cash:{id}` vs ledger PLAYER_VIN.
- Phase 5c sub-games (Loto, OneTwoThree, BanCaService) sẽ thêm tx_type mới (`WAGER_DEBIT_LOTO`, `WAGER_CREDIT_123`, …) — mỗi tx_type cần entry mới ở cả 4 map của `MoneyGateway`.
- Failed-settle replay worker chưa có — entries trong `banca:failed_settle` hiện chỉ pile up, cần daemon LPOP + re-post (xem `WALLET_PHASE5B_BANCA_GAME_LOOP_IMPL.md` "Open follow-ups").
