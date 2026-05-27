# Tài Liệu Tích Hợp FE — Tài Xỉu + Xí Ngầu Sicbo (Standalone REST, Phase E/F)

Tài liệu dành cho team Front-End. Mô tả contract REST + STOMP của **Tài Xỉu** và **Xí Ngầu Sicbo** sau khi chuyển sang kiến trúc standalone (Phase E/F của SUN-1341). Lottery XSMB được tách riêng tại `FE_INTEGRATION_LOTTERY_LODE_VN.md`.

- **Staging base URL:** `https://staging-play.sunkr.bet`
- **Container:** `sunwinkr-minigame-api` (Spring Boot, port 9290)
- **Verified staging:** 2026-05-15
- **Đồng hồ server:** `Asia/Seoul` (Korea) — không ảnh hưởng đến FE, server là source of truth

---

## 1. Tổng quan

### V2 cũ so với kiến trúc hiện tại (Phase E/F)

Trước Phase E/F, Tài Xỉu và Sicbo chạy qua server `game-minigame` (BitZero/Java, TCP/WebSocket binary protocol). FE kết nối qua `ws-bridge` để dịch binary ↔ JSON. Luồng này phức tạp, khó debug, và yêu cầu Cocos Creator client.

Từ Phase E/F:

- Cả hai game chạy như **Spring Boot REST service** trong container `minigame-api` riêng biệt.
- Round được điều khiển bởi **scheduler nội bộ** (`TaiXiuRoundScheduler`) — không phụ thuộc BitZero.
- FE dùng **REST API** để đặt cược và lấy state, dùng **STOMP WebSocket** để nhận push real-time.
- Không còn binary protocol, không cần ws-bridge, không cần Cocos client.
- Authentication dùng **cùng `accessToken`** từ login portal `c=3` — không có flow login mới.

Tài liệu này là nguồn duy nhất về contract cho Tài Xỉu và Sicbo. Bất kỳ mô tả nào trong tài liệu cũ (bridge-shadow mode, BitZero packet layout) đều không còn áp dụng.

---

## 2. Kiến trúc

```
FE (browser)
  │
  ├── REST: GET/POST https://staging-play.sunkr.bet/api/v2/{taixiu|sicbo}/...
  │         ?at=<token>  hoặc  Authorization: Bearer <token>
  │
  └── STOMP WebSocket: wss://staging-play.sunkr.bet/ws/minigame
        subscribe /topic/taixiu/announce   ← sự kiện vòng quay
        subscribe /topic/sicbo/announce    ← sự kiện vòng quay
```

**Authentication:** Token 32 ký tự lấy từ `c=3` login. Gửi theo một trong hai cách:

```
?at=<accessToken>                         ← query param, nhất quán với portal cũ
Authorization: Bearer <accessToken>       ← header (cả hai đều được chấp nhận)
```

Thiếu hoặc sai token → HTTP 403:
```json
{"timestamp":"2026-05-15T...","status":403,"error":"Forbidden","path":"/api/v2/taixiu/bet"}
```

**Không có SockJS fallback** — STOMP endpoint dùng raw WebSocket. Cocos Creator client dùng raw WS; browser FE cần thư viện STOMP hỗ trợ raw WebSocket (ví dụ `@stomp/stompjs`).

---

## 3. Vòng quay (round cycle)

Cả Tài Xỉu và Sicbo dùng chu kỳ **60 giây** do `TaiXiuRoundScheduler` điều khiển:

| Thời điểm | Sự kiện | Trạng thái |
|-----------|---------|-----------|
| t = 0 s | Round mới mở | Cửa sổ cược mở. `bettingState=true`. `safeBetExpiresAt` được đặt = `now + 30s`. |
| t = 30 s | Khóa cược | Server từ chối mọi bet mới. `bettingState=false`. STOMP push `BETTING_LOCKED`. |
| t = 40 s | Reveal + settle | Tung xúc xắc, tính thắng/thua, cộng tiền, push STOMP `ROUND_RESULT` với dice + kết quả. |
| t = 60 s | Round kế tiếp | Vòng lặp bắt đầu lại từ t = 0. |

**Ghi chú về clock skew:** Server dùng `clock.millis() >= safeBetExpiresAt` để reject bet. FE dùng `safeBetExpiresAt - Date.now()` cho countdown. Nếu đồng hồ client lệch server vài giây, bet gửi ngay khi countdown về 0 vẫn có thể bị reject. Xử lý `BET_WINDOW_CLOSED` gracefully (xem §7.1).

---

## 4. Endpoint chính

### 4.1 Tài Xỉu — `GET /api/v2/taixiu/state`

Lấy snapshot hiện tại của round đang chạy.

**Request:**
```
GET /api/v2/taixiu/state?moneyType=1&at=<token>
```

| Param | Kiểu | Bắt buộc | Mô tả |
|-------|------|:---:|-------|
| `moneyType` | short | Không | `0`=XU, `1`=VIN (mặc định 1) |

**Response:** xem §5.1 để có sample đầy đủ.

---

### 4.2 Tài Xỉu — `POST /api/v2/taixiu/bet`

Đặt cược trong cửa sổ mở.

**Request body:**

| Field | Kiểu | Bắt buộc | Mô tả |
|-------|------|:---:|-------|
| `moneyType` | short | Có | `0`=XU, `1`=VIN |
| `betValue` | long | Có | Tối thiểu 100 |
| `betSide` | short | Có | `0`=Xỉu (tổng ≤ 10), `1`=Tài (tổng ≥ 11) |
| `clientNonce` | string | Khuyến nghị | UUID v4, dùng cho idempotency retry-safe |

**Response:** xem §6.1.

**Lưu ý đặc biệt:** Trong một round, player **không thể** cược cả Tài lẫn Xỉu. Bet lần hai vào phía ngược lại → `errorCode "0005"`.

---

### 4.3 Tài Xỉu — `GET /api/v2/taixiu/history`

Lịch sử các round đã settle của caller.

**Request:**
```
GET /api/v2/taixiu/history?moneyType=1&n=20&at=<token>
```

| Param | Kiểu | Mặc định | Mô tả |
|-------|------|---------|-------|
| `moneyType` | short | 1 | Ví query |
| `n` | int | 100 | Số round tối đa, max 120 |

**Response:**
```json
{"entries": [], "count": 0}
```

---

### 4.4 Xí Ngầu Sicbo — `GET /api/v2/sicbo/state`

Cùng cấu trúc với TaiXiu, thêm field `phase` (chuỗi: `OPEN` / `LOCKED` / `REVEALED` / `SETTLED`). Xem §5.2.

**Request:**
```
GET /api/v2/sicbo/state?moneyType=1&at=<token>
```

---

### 4.5 Xí Ngầu Sicbo — `POST /api/v2/sicbo/bet`

Đặt cược. Khác Tài Xỉu ở chỗ `betSide` là **chuỗi** (không phải số).

**Request body:**

| Field | Kiểu | Bắt buộc | Mô tả |
|-------|------|:---:|-------|
| `moneyType` | short | Có | `0`=XU, `1`=VIN |
| `betValue` | long | Có | Tối thiểu 100 |
| `betSide` | string | Có | Tên loại cược (xem bảng §4.7) |
| `clientNonce` | string | Khuyến nghị | UUID v4 |

Sicbo **cho phép** cược nhiều `betSide` khác nhau trong cùng một round (không bị chặn chéo phía).

**Response:** xem §6.2.

---

### 4.6 Xí Ngầu Sicbo — `GET /api/v2/sicbo/history`

```
GET /api/v2/sicbo/history?moneyType=1&n=20&at=<token>
```

Cùng shape với TaiXiu history.

---

### 4.7 Bảng `betSide` Sicbo — 52 loại cược

`betSide` là tên chuỗi, decode server-side qua `SicboBetType.byName(...)`. Tên không hợp lệ → `errorCode "0006"`.

**Tổng — điểm (14 loại):** Tổng ba xúc xắc bằng đúng giá trị.

| `betSide` | Payout | `betSide` | Payout |
|-----------|-------:|-----------|-------:|
| `POINT_4` | ×61 | `POINT_11` | ×7 |
| `POINT_5` | ×31 | `POINT_12` | ×7 |
| `POINT_6` | ×18 | `POINT_13` | ×9 |
| `POINT_7` | ×13 | `POINT_14` | ×13 |
| `POINT_8` | ×9  | `POINT_15` | ×18 |
| `POINT_9` | ×7  | `POINT_16` | ×31 |
| `POINT_10` | ×7 | `POINT_17` | ×61 |

**Một mặt xúc xắc (6 loại):** Một trong ba xúc xắc hiện mặt N.

| `betSide` | Ghi chú |
|-----------|---------|
| `ONE_DICE_1` .. `ONE_DICE_6` | Payout theo số lần xuất hiện: 1 lần→×2, 2 lần→×3, 3 lần→×4 |

**Hai xúc xắc khớp — double (21 loại):** Đúng hai trong ba xúc xắc hiện cặp x, y.

`DOUBLE_DICES_1_1`, `DOUBLE_DICES_1_2`, `DOUBLE_DICES_1_3`, `DOUBLE_DICES_1_4`, `DOUBLE_DICES_1_5`, `DOUBLE_DICES_1_6`, `DOUBLE_DICES_2_2`, `DOUBLE_DICES_2_3`, `DOUBLE_DICES_2_4`, `DOUBLE_DICES_2_5`, `DOUBLE_DICES_2_6`, `DOUBLE_DICES_3_3`, `DOUBLE_DICES_3_4`, `DOUBLE_DICES_3_5`, `DOUBLE_DICES_3_6`, `DOUBLE_DICES_4_4`, `DOUBLE_DICES_4_5`, `DOUBLE_DICES_4_6`, `DOUBLE_DICES_5_5`, `DOUBLE_DICES_5_6`, `DOUBLE_DICES_6_6` — tất cả payout ×6.

**Ba xúc xắc giống nhau — triple cụ thể (6 loại):**

`TRIPLE_DICES_1` .. `TRIPLE_DICES_6` — payout ×31. Khi bất kỳ triple xuất hiện, tất cả bet khác (TAI, XIU, CHAN, LE, ONE_DICE_*, DOUBLE_DICES_*, POINT_*) đều thua — chỉ `TRIPLE_DICES_n` đúng + `ANY_TRIPLE_DICES` thắng.

**Tài / Xỉu / Chẵn / Lẻ (4 loại):**

| `betSide` | Điều kiện | Payout |
|-----------|-----------|-------:|
| `TAI` | Tổng ≥ 11 | ×2 |
| `XIU` | Tổng ≤ 10 | ×2 |
| `CHAN` | Tổng chẵn | ×2 |
| `LE` | Tổng lẻ | ×2 |

**Bất kỳ triple (1 loại):**

`ANY_TRIPLE_DICES` — cả ba xúc xắc giống nhau, bất kể giá trị. Payout ×31.

---

## 5. State response — mẫu thực tế

### 5.1 Tài Xỉu state (captured staging)

```json
{
  "referenceId": 1,
  "remainTime": 36,
  "bettingState": true,
  "safeBetExpiresAt": 1778839816717,
  "roundId": 1,
  "potTai": 0,
  "potXiu": 0,
  "myBetTai": 0,
  "myBetXiu": 0,
  "jpTai": 0,
  "jpXiu": 0,
  "dice1": 0,
  "dice2": 0,
  "dice3": 0,
  "result": -1,
  "numBetTai": 0,
  "numBetXiu": 0,
  "realNumBetTai": 0,
  "realNumBetXiu": 0
}
```

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `referenceId` | long | Round id (= `roundId`) — legacy alias |
| `roundId` | long | Round id chính (provider-contract field) |
| `safeBetExpiresAt` | long (epoch ms) | Deadline cược tuyệt đối. `0` = engine idle (chưa có round). FE countdown: `safeBetExpiresAt - Date.now()` |
| `bettingState` | bool | `true` = đang mở cược. Dùng kết hợp với `safeBetExpiresAt > 0` |
| `remainTime` | short | Giây còn lại (legacy) |
| `potTai`, `potXiu` | long | Tổng tiền mỗi phía trong round hiện tại |
| `myBetTai`, `myBetXiu` | long | Số tiền caller đã cược mỗi phía round này |
| `jpTai`, `jpXiu` | long | Jackpot accumulator |
| `dice1`, `dice2`, `dice3` | short | **Luôn = 0 trước reveal** (anti-cheat) |
| `result` | short | **Luôn = -1 trước reveal**; 0=Xỉu, 1=Tài sau reveal |
| `numBetTai`, `numBetXiu` | short | Tổng số người cược (gồm bot) |
| `realNumBetTai`, `realNumBetXiu` | short | Chỉ người chơi thật |

### 5.2 Sicbo state (captured staging)

```json
{
  "referenceId": 1,
  "safeBetExpiresAt": 1778839826689,
  "roundId": 1,
  "remainTime": 0,
  "bettingState": true,
  "potTai": 0,
  "potXiu": 0,
  "myBetTai": 0,
  "myBetXiu": 0,
  "jpTai": 0,
  "jpXiu": 0,
  "dice1": null,
  "dice2": null,
  "dice3": null,
  "phase": "OPEN"
}
```

Khác Tài Xỉu:
- `dice1/2/3` là `null` (nullable `Short`) khi pre-reveal, không phải `0`. FE guard: kiểm tra `dice1 != null` thay vì `!= 0`.
- `phase` (chuỗi): `OPEN` → `LOCKED` → `REVEALED` → `SETTLED`.
- Không có `result` field — kết quả tính từ tổng ba xúc xắc khi `dice1 != null`.

---

## 6. Bet response — thành công và lỗi

### 6.1 Tài Xỉu

**Thành công:**
```json
{
  "success": true,
  "errorCode": "0000",
  "currentMoney": 35044254,
  "perBetTxId": 1390085,
  "message": "OK"
}
```

**BET_WINDOW_CLOSED (captured staging):**
```json
{
  "success": false,
  "errorCode": "0007",
  "currentMoney": 35046254,
  "perBetTxId": 0,
  "message": "BET_WINDOW_CLOSED"
}
```

### 6.2 Sicbo

**Thành công:** (thêm `transactionCode` và `betSideId`)
```json
{
  "success": true,
  "errorCode": "0000",
  "currentMoney": 35044254,
  "perBetTxId": 1390085,
  "transactionCode": "1-1",
  "betSideId": 48,
  "message": "OK"
}
```

`transactionCode` = `"{refId}-{betIndex}"` — dùng cho FE-side reconciliation. `betSideId` = numeric id của `SicboBetType` đã bet (xem cột id trong §4.7).

**BET_WINDOW_CLOSED (captured staging):**
```json
{
  "success": false,
  "errorCode": "0007",
  "currentMoney": 35046254,
  "perBetTxId": 0,
  "transactionCode": null,
  "betSideId": null,
  "message": "BET_WINDOW_CLOSED"
}
```

---

## 7. Mã lỗi

### 7.1 Tài Xỉu

| `errorCode` | Tên lỗi | Thông báo FE | Retry? |
|-------------|---------|-------------|:------:|
| `0000` | OK | — | n/a |
| **`0007`** | **BET_WINDOW_CLOSED** | **"Phiên đã đóng, chờ phiên mới"** | Không |
| `0001` | Wallet failure / race | "Có lỗi xử lý, thử lại" | Có (cùng nonce) |
| `0002` | Betting closed (legacy path) | "Phiên đã đóng" | Không |
| `0003` | Không đủ tiền | "Số dư không đủ" | Không |
| `0004` | Dưới mức tối thiểu | "Cược tối thiểu 100 VND" | Không |
| `0005` | Cross-side (đã cược phía ngược) | "Bạn đã cược Tài/Xỉu phiên này" | Không |
| HTTP 403 | Token sai/hết hạn | Login lại | Có (sau login) |

### 7.2 Xí Ngầu Sicbo

| `errorCode` | Tên lỗi | Thông báo FE | Retry? |
|-------------|---------|-------------|:------:|
| `0000` | OK | — | n/a |
| **`0007`** | **BET_WINDOW_CLOSED** | **"Phiên đã đóng, chờ phiên mới"** | Không |
| `0001` | Wallet failure / race | "Có lỗi xử lý, thử lại" | Có (cùng nonce) |
| `0003` | Không đủ tiền | "Số dư không đủ" | Không |
| `0004` | Dưới mức tối thiểu | "Cược tối thiểu 100 VND" | Không |
| `0006` | `betSide` không hợp lệ | "Loại cược không tồn tại" | Không |
| HTTP 403 | Token sai/hết hạn | Login lại | Có (sau login) |

---

## 8. STOMP — kết nối và topics

**Endpoint:** `wss://staging-play.sunkr.bet/ws/minigame`

Raw WebSocket — không có SockJS fallback. Kết nối bằng thư viện STOMP hỗ trợ raw WS (ví dụ `@stomp/stompjs` v6+).

**Header khi connect:**
```
Authorization: Bearer <accessToken>
```

### 8.1 Topics Tài Xỉu

| Topic | Tần suất | Payload mẫu |
|-------|---------|------------|
| `/topic/taixiu/{moneyType}/tick` | 1Hz (mỗi giây khi round active) | StateDto (dice censored pre-reveal) |
| `/topic/taixiu/{moneyType}/reveal` | 1 lần tại t=40s | `{"dice1":3,"dice2":5,"dice3":2,"sum":10,"result":0}` |
| `/topic/taixiu/{moneyType}/pot` | Mỗi khi có bet mới | `{"potTai":1000,"potXiu":5000}` |
| `/topic/taixiu/{moneyType}/round-start` | Mỗi đầu round | `{"roundId":42,"safeBetExpiresAt":...}` |
| `/topic/taixiu/announce` | Round events từ scheduler | Xem §8.3 |

### 8.2 Topics Xí Ngầu Sicbo

| Topic | Tần suất | Payload mẫu |
|-------|---------|------------|
| `/topic/sicbo/{moneyType}/tick` | 1Hz | SicboStateDto (dice null pre-reveal) |
| `/topic/sicbo/{moneyType}/reveal` | 1 lần tại reveal | `{"dice1":4,"dice2":1,"dice3":6,"sum":11}` |
| `/topic/sicbo/{moneyType}/pot` | Mỗi bet mới | `{"potTai":2000,"potXiu":0}` |
| `/topic/sicbo/{moneyType}/round-start` | Đầu round | `{"roundId":7,"safeBetExpiresAt":...}` |
| `/topic/sicbo/announce` | Round events từ scheduler | Cùng format với §8.3 |

### 8.3 Announce payload từ scheduler (`/topic/{game}/announce`)

Scheduler push ba loại event trên cùng topic `/topic/taixiu/announce`:

**ROUND_START** (t=0):
```json
{"event":"ROUND_START","roundId":42,"safeBetExpiresAt":1778840000000}
```

**BETTING_LOCKED** (t=30):
```json
{"event":"BETTING_LOCKED","roundId":42}
```

**ROUND_RESULT** (t=40):
```json
{
  "event": "ROUND_RESULT",
  "roundId": 42,
  "dice1": 3,
  "dice2": 5,
  "dice3": 2,
  "sum": 10,
  "result": 0,
  "settled": 12
}
```

`result`: `1`=Tài, `0`=Xỉu. `settled`: số bet đã xử lý thành công trong round.

---

## 9. Settle và lịch sử

### 9.1 Hai bản ghi mỗi bet

Với mỗi cược được settle, server ghi **hai bản ghi**:

1. **Wallet ledger** (`log_money_user_vin`): `action_name="TaiXiu"` hoặc `"Sicbo"`.
   - Debit khi đặt cược (trừ tiền).
   - Credit khi thắng (cộng tiền).
   - `money_exchange=0` khi thua (không có hành động ví thêm — ví đã trừ lúc bet).

2. **Legacy mirror**: Mongo doc trong `log_taixiu` / `log_sicbo` + MySQL row trong `vinplay_minigame.transaction_tai_xiu_sicbo` — để `c=303` player history có thể hiển thị.

### 9.2 Lịch sử qua command `c=303`

Player gọi legacy portal endpoint để lấy lịch sử:

```
GET /api?c=303&nn=<nickname>&game=taixiu&n=20&at=<token>
GET /api?c=303&nn=<nickname>&game=sicbo&n=20&at=<token>
```

Mỗi bet đã settle trả về các field: `gameID`, `result`, `totalbet`, `detail`. Bet đặt qua REST path sẽ xuất hiện trong `c=303` nhờ `LegacyTaixiuHistoryPort` / `LegacySicboHistoryPort` ghi mirror trong cùng flow bet.

---

## 10. Admin chargeback (`unsettle`)

Admin-only. Yêu cầu role `ROLE_MINIGAME_ADMIN`. Player-facing FE không cần tích hợp phần này.

**Tài Xỉu:**
```
POST /api/v2/admin/taixiu/unsettle
Authorization: Bearer <admin_token>
Content-Type: application/json
```

**Xí Ngầu Sicbo:**
```
POST /api/v2/admin/sicbo/unsettle
Authorization: Bearer <admin_token>
Content-Type: application/json
```

**Request body:**
```json
{
  "ticketId": 1390085,
  "reason": "Lỗi kỹ thuật — void theo yêu cầu ops"
}
```

**Response thành công (TaiXiu):**
```json
{
  "success": true,
  "ticketId": 1390085,
  "nickname": "zuestang",
  "newStatus": "VOIDED",
  "reversalAmount": 1000,
  "moneyType": "vin",
  "actor": "superadmin"
}
```

**Response thành công (Sicbo):**
```json
{
  "success": true,
  "ticketId": 1390085,
  "nickname": "zuestang",
  "settleStatus": "VOIDED",
  "walletBalance": 35045254
}
```

**Lỗi — vé không ở trạng thái SETTLED:**
```json
{"success":false,"errorCode":"NOT_SETTLED","message":"Bet is not in SETTLED state: VOIDED"}
```

**Hành vi server khi unsettle:**
- Tra cứu row bet trong `taixiu_bet` / `sicbo_bet` theo `ticketId`.
- Xác nhận `settle_status = 'SETTLED'` — nếu không → 400 `NOT_SETTLED`.
- Đảo ngược ví: nếu player đã thắng (prize > 0) → debit prize lại; nếu thua (prize = 0) → credit lại tiền cược.
- Flip `settle_status = 'VOIDED'` trong MySQL.
- Ghi audit row vào `log_money_user_vin` với actor, reason, amount.

Endpoint idempotent ở DB layer: `markVoided` dùng `WHERE settle_status='SETTLED'`, cuộc gọi thứ hai trả 400 `NOT_SETTLED`.

---

## 11. Migration FE — từ BitZero sang REST

### Trước (bridge/BitZero path)

```javascript
// Cũ: kết nối TCP/WS binary qua ws-bridge
const ws = new WebSocket("wss://staging-play.sunkr.bet/wss/minigame");
ws.onopen = () => {
  // Gửi binary packet login + join room
  ws.send(buildJoinPacket(userId, roomId));
};
ws.onmessage = (evt) => {
  // Parse binary response, extract betState từ byte offset
  const state = parseBinaryState(evt.data);
  startCountdown(state.remainTime * 1000); // client-side countdown từ remainTime
};
```

### Sau (REST + STOMP)

```javascript
// Mới: REST để lấy state, STOMP để nhận push
const token = await loginAndGetToken(); // c=3, không thay đổi

// Lấy state ban đầu
const state = await fetch(`/api/v2/taixiu/state?moneyType=1&at=${token}`)
  .then(r => r.json());

// Countdown dựa trên safeBetExpiresAt (epoch ms tuyệt đối)
const canBet = state.safeBetExpiresAt > 0 && Date.now() < state.safeBetExpiresAt;
if (canBet) {
  startCountdown(state.safeBetExpiresAt - Date.now());
}

// Subscribe STOMP để nhận push real-time
const client = new Client({
  brokerURL: "wss://staging-play.sunkr.bet/ws/minigame",
  connectHeaders: { Authorization: `Bearer ${token}` }
});
client.onConnect = () => {
  // Tick mỗi giây — cập nhật countdown + pot
  client.subscribe("/topic/taixiu/1/tick", (msg) => {
    const snapshot = JSON.parse(msg.body);
    updateUI(snapshot);
  });
  // Nhận kết quả round
  client.subscribe("/topic/taixiu/announce", (msg) => {
    const ev = JSON.parse(msg.body);
    if (ev.event === "ROUND_RESULT") {
      showDiceAndResult(ev.dice1, ev.dice2, ev.dice3, ev.result);
    }
  });
};
client.activate();

// Đặt cược
async function placeBet(betSide, betValue) {
  const nonce = crypto.randomUUID();
  const res = await fetch(`/api/v2/taixiu/bet?at=${token}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ moneyType: 1, betValue, betSide, clientNonce: nonce })
  }).then(r => r.json());

  if (res.success) {
    showSuccess(`Đặt cược thành công — mã ${res.perBetTxId}`);
  } else if (res.errorCode === "0007") {
    showWarning("Phiên đã đóng, chờ phiên mới");
    refreshState();  // fetch lại state
  } else {
    showError(res.message);
  }
}
```

**Điểm thay đổi chính:**
- Không còn binary protocol, không cần `ws-bridge`, không cần parse byte offset.
- `safeBetExpiresAt` là nguồn duy nhất cho countdown — không cần tính từ `remainTime`.
- `clientNonce` thay thế retry manual — gửi cùng nonce nếu timeout, server trả cached response.

---

## 12. Câu hỏi thường gặp

**Q1: Dùng `bettingState` hay `safeBetExpiresAt` để kiểm soát countdown?**

Dùng `safeBetExpiresAt`. Đây là epoch ms tuyệt đối mà server dùng để reject bet (kiểm tra `clock.millis() >= safeBetExpiresAt`). `bettingState` là boolean phụ thuộc state update chu kỳ, có thể có độ trễ vài ms so với thực tế. `safeBetExpiresAt - Date.now()` cho countdown chính xác hơn và nhất quán với cách xử lý GSC/EVO games trên platform.

Guard bắt buộc: `canBet = safeBetExpiresAt > 0 && Date.now() < safeBetExpiresAt`.

---

**Q2: Clock skew xử lý thế nào?**

Server dùng đồng hồ server để reject. Client dùng `Date.now()` cho countdown visual. Nếu lệch 1-2 giây, bet gửi khi countdown về 0 vẫn có thể bị reject vì server đã đóng. Đây là behavior bình thường — xử lý `errorCode "0007"` gracefully: hiển thị "Phiên đã đóng, chờ phiên mới", gọi `GET /state` để refresh `safeBetExpiresAt` của round mới. Không retry bet bị reject vì `0007` — đây là lỗi thời gian, không phải lỗi network.

---

**Q3: `roundId` là gì? Dùng để làm gì?**

`roundId` là số nguyên monotonically tăng dần, reset mỗi khi server khởi động. Round đầu tiên sau boot = 1. FE dùng để:
- Phát hiện round mới bắt đầu (so sánh `roundId` cũ với mới sau refresh state).
- Hiển thị label "Phiên #42" cho người dùng.
- Tracing support ticket: "round #42 của Tài Xỉu".

`roundId` = `referenceId` — cả hai field đều xuất hiện trong response để backward compatibility.

---

**Q4: Khi nhận `BET_WINDOW_CLOSED` (`errorCode "0007"`) FE làm gì?**

1. Hiển thị thông báo: "Phiên đã đóng, vui lòng chờ phiên mới".
2. Gọi `GET /state` để lấy `safeBetExpiresAt` của round sắp tới.
3. Không retry bet với cùng `clientNonce` — `0007` là lỗi thời gian, retry sẽ trả cùng rejection.
4. Subscribe `/topic/{game}/announce` để nhận event `ROUND_START` khi round mới mở.

---

**Q5: Thua thì ví thay đổi thế nào? Có row nào ghi không?**

Khi thua, **không có thêm hành động nào với ví** sau settle. Ví đã bị trừ lúc bet (`money_exchange` là số âm). Lúc settle thua, `money_exchange = 0` trong settle row. Kết quả thuần: ví trừ đúng `betValue`, không có gì khác. Trong lịch sử (`c=303`), bet hiện `settle_status=SETTLED` với prize = 0.

---

**Q6: Player có thể xem lại các round đã settle không?**

Có, qua hai cách:
- `GET /api/v2/{taixiu|sicbo}/history?n=N` — trả `{entries, count}` cho N round gần nhất (tối đa 120). Hiện tại entries có thể rỗng (legacy bridge mirror chưa hoàn chỉnh — follow-up Plan §2.8 H1/H2).
- Legacy command `c=303?nn=<nickname>&game={taixiu|sicbo}&n=20` — trả lịch sử từ `transaction_tai_xiu_sicbo` MySQL. Đây là nguồn đáng tin cậy hiện tại cho player history UI, vì `LegacyTaixiuHistoryPort` / `LegacySicboHistoryPort` ghi mirror vào bảng này trong cùng flow bet/settle.

---

**Source authority:**
- `backend-master/game/minigame-api/src/main/java/com/sunwinkr/minigame/api/controller/TaiXiuController.java`
- `backend-master/game/minigame-api/src/main/java/com/sunwinkr/minigame/api/controller/sicbo/SicboController.java`
- `backend-master/game/minigame-api/src/main/java/com/sunwinkr/minigame/api/controller/AdminTaiXiuUnsettleController.java`
- `backend-master/game/minigame-api/src/main/java/com/sunwinkr/minigame/api/controller/sicbo/AdminSicboController.java`
- `backend-master/game/minigame-api/src/main/java/com/sunwinkr/minigame/api/scheduler/TaiXiuRoundScheduler.java`
- `backend-master/game/minigame-api/src/main/java/com/sunwinkr/minigame/api/config/StompConfig.java`
- `backend-master/game/minigame-engine/src/main/java/com/sunwinkr/minigame/engine/sicbo/bet/SicboBetType.java`
- `backend-master/game/minigame-api/src/main/java/com/sunwinkr/minigame/api/dto/StateDto.java`
- `backend-master/game/minigame-api/src/main/java/com/sunwinkr/minigame/api/dto/sicbo/SicboStateDto.java`
- `backend-master/game/minigame-api/src/main/java/com/sunwinkr/minigame/api/dto/BetResponseDto.java`
- `backend-master/game/minigame-api/src/main/java/com/sunwinkr/minigame/api/dto/sicbo/SicboBetResponseDto.java`
