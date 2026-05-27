# Tài Liệu Tích Hợp FE — Lô Đề XSMB (`/api/v2/lottery/xsmb`)

Tài liệu chi tiết dành cho team Front-End. Tất cả endpoint là **player-facing** (đã loại trừ admin). Kiểm tra trực tiếp với source code + container đang chạy trên staging.

- **Base URL staging:** `https://staging-play.sunkr.bet`
- **Container:** `sunwinkr-lottery-spring-api` (Spring Boot 2.7.18, port 9292)
- **Ngày verify:** 2026-05-14
- **Timezone server:** Asia/Ho_Chi_Minh (Vietnam) — pin cứng ở code, độc lập với container env

---

## 1. Tổng quan

Lô Đề XSMB (Xổ Số Miền Bắc) là game **chu kỳ ngày** — không có round real-time như TaiXiu/Sicbo.

| Khía cạnh | Mô tả |
|-----------|-------|
| Chu kỳ | 1 ngày = 1 phiên |
| Giờ mở cược | 00:00 → 18:10 (giờ Hà Nội) |
| Giờ chốt cược (lock) | 18:10 (giờ Hà Nội) |
| Giờ quay số chính thức | ~18:15 trên TV |
| Giờ scrape kết quả | 18:35 (server tự động lấy từ az24.vn) |
| Giờ trả kết quả cho FE | ~18:36 (sau khi settle xong) |
| Loại tiền | `vin` (VIN wallet) — không hỗ trợ XU |
| Tối thiểu | 1000 VND |
| Tối đa | Không giới hạn (giới hạn bởi ví) |

**Không có cancel bet.** Khi đã `POST /bet` thành công, cược không thể hủy.

---

## 2. Authentication

Dùng login portal sẵn có (`c=3`) — KHÔNG có endpoint login riêng cho lottery.

```bash
$ curl "https://staging-play.sunkr.bet/api?c=3&un=zuestang&pw=<MD5(password)>"
{"success":true,"data":{...,"accessToken":"32-char-hex...","sessionKey":"..."}}
```

**2 cách gửi token (chọn 1):**

```
?at=<accessToken>                        ← khuyến nghị — nhất quán với portal cũ
Authorization: Bearer <accessToken>      ← alternative (cùng accepted)
```

**Ví dụ với `?at=`** (style nhất quán với legacy `c=*` endpoints):
```
GET https://staging-play.sunkr.bet/api/v2/lottery/xsmb/state?at=<token>
POST https://staging-play.sunkr.bet/api/v2/lottery/xsmb/bet?at=<token>
```

Sai/thiếu token → **HTTP 403 Forbidden**:
```json
{"timestamp":"2026-05-14T13:53:37.952+00:00","status":403,"error":"Forbidden","path":"/api/v2/lottery/xsmb/state"}
```

---

## 3. ENUM: `LotteryMode` — 10 chế độ cược

Source: `LotteryMode.java`. **SUN-1295 snapshot semantics**: `rate` + `prizeMultiplier` được snap vào `lode.rate_at_purchase` + `lode.prize_multiplier` lúc đặt cược → không bị ảnh hưởng nếu admin đổi rate sau đó.

| `id` (gửi lên server) | Java name | Tên tiếng Việt | Mô tả | `rate` (hệ số cược) | `prizeMultiplier` (hệ số thưởng) | Định dạng `num` |
|:---:|---|---|---|:---:|:---:|---|
| **1** | `LO_2_SO` | LÔ 2 SỐ | Chọn 2 số | 22 | 80 | 2 chữ số, vd `"12"` |
| **2** | `LO_3_SO` | LÔ 3 SỐ | Chọn 3 số | 23 | 600 | 3 chữ số, vd `"123"` |
| **3** | `LO_XIEN_2` | LÔ XIÊN 2 | Chọn 2 số | 1 | 12 | CSV 2 số, vd `"12,34"` |
| **4** | `LO_XIEN_3` | LÔ XIÊN 3 | Chọn 3 số | 1 | 48 | CSV 3 số, vd `"12,34,56"` |
| **5** | `LO_XIEN_4` | LÔ XIÊN 4 | Chọn 4 số | 1 | 160 | CSV 4 số, vd `"12,34,56,78"` |
| **6** | `DAU` | ĐẦU | Chọn 1 số | 1 | 8 | 1 chữ số, vd `"5"` |
| **7** | `DUOI` | ĐUÔI | Chọn 1 số | 1 | 8 | 1 chữ số |
| **8** | `DE_DAU` | ĐỀ ĐẦU | Chọn 2 số | 1 | 80 | 2 chữ số |
| **9** | `DE` | ĐỀ ĐẶC BIỆT | Chọn 2 số | 1 | 85 | 2 chữ số |
| **11** | `BA_CANG` | BA CÀNG | Chọn 3 số | 1 | 450 | 3 chữ số |

> **Lưu ý các id bỏ trống:** id `0` và `10` KHÔNG tồn tại. Đừng gửi.

### 3.1 Cách server tính tiền trúng

```
prize = số_lần_trùng × betValue × prizeMultiplier
```

Trong đó `số_lần_trùng` tùy theo mode (xem §3.2).

**Tiền cược trừ vào ví:**

```
finalBetValue = betValue × rate
```

→ Ví dụ với mode `LO_2_SO` (`rate=22`), `betValue=1000` → ví trừ **22000 VND**.

### 3.2 Quy tắc trùng kết quả từng mode

XSMB có **27 dãy số giải** (gồm Đặc Biệt + Giải 1 → Giải 7). Mỗi mode có quy tắc trùng riêng:

| Mode | Quy tắc trùng | Tiền trả |
|------|---------------|----------|
| `LO_2_SO` (1) | Đếm số lần 2 chữ số cuối của 1 dãy trong 27 dãy = `num` | `số_lần × bet × 80` |
| `LO_3_SO` (2) | Đếm trong 24 dãy (loại G7 vì G7 chỉ 2 chữ số) | `số_lần × bet × 600` |
| `LO_XIEN_2` (3) | Cả 2 số `num1` và `num2` đều có trong 27 dãy | flat: `bet × 12` |
| `LO_XIEN_3` (4) | Cả 3 số đều trong 27 dãy | flat: `bet × 48` |
| `LO_XIEN_4` (5) | **Cả 4 số đều trong 27 dãy** ⚠️ | flat: `bet × 160` |
| `DAU` (6) | Chữ số đầu của `de` (2 chữ cuối ĐB) == `num` | `bet × 8` |
| `DUOI` (7) | Chữ số cuối của `de` == `num` | `bet × 8` |
| `DE_DAU` (8) | Có bất kỳ dãy ĐB nào kết thúc bằng `num` | `bet × 80` |
| `DE` (9) | `de` == `num` (2 chữ số cuối ĐB) | flat: `bet × 85` |
| `BA_CANG` (11) | Có bất kỳ dãy ĐB nào kết thúc bằng `num` (3 chữ số) | `bet × 450` |

> ⚠️ **Mode 5 (LÔ XIÊN 4) — canonical = 4/4.** Legacy Java code cũ chỉ yêu cầu 3/4 (bug). Hiện tại default = **4/4 match** (theo C# stack + spec). Có feature flag `LOTTERY_MODE5_LEGACY_3OF4` để rollback 1 release.

---

## 4. ENUM: `LotteryPhase` — chu kỳ ngày

Source: `LotteryPhase.java`.

| Phase | Khung giờ Hà Nội | Bets accept? | Hiện kết quả? | Ý nghĩa |
|-------|------------------|:------------:|:-------------:|---------|
| `DRAW_PENDING` | 00:00 → 18:10 | ✅ | Hôm qua | Phiên mở cược, kết quả hôm qua đã có |
| `DRAW_LOCKED` | 18:10 → 18:35 | ❌ | Hôm qua | Đã chốt cược, chờ scrape |
| `SCRAPING` | 18:35 → ~18:36 | ❌ | Hôm qua | Server đang scrape từ az24.vn, hôm nay vẫn ẩn |
| `SETTLING` | ~18:36 (vài giây) | ❌ | Hôm qua | `result_lottery.settled_at IS NULL` — settle loop đang chạy |
| `SETTLED` | ~18:36 → 24:00 | ✅ | Hôm nay + hôm qua | `settled_at IS NOT NULL` — phiên hôm nay đã đóng hoàn toàn |

**Quan trọng:**
- `acceptsBets()` returns `true` **chỉ khi** phase = `DRAW_PENDING` hoặc `SETTLED`
- `resultVisible()` returns `true` **chỉ khi** phase = `SETTLED` cho ngày đó
- Khi phase = `DRAW_LOCKED`/`SCRAPING`/`SETTLING` cho HÔM NAY, `GET /result/today` trả **HTTP 404** (anti-cheat — chống lộ kết quả trước khi settle xong)

---

## 5. Endpoints chi tiết

### 5.1 `POST /api/v2/lottery/xsmb/bet` — Đặt cược

**Body** (tên field PHẢI khớp exact):

```json
{
  "modeId": 1,            // int — id của LotteryMode (1..9, 11). Tên field là "modeId" KHÔNG phải "mode"
  "ticket": "12",         // string — định dạng theo modeId (xem §3). Tên field là "ticket" KHÔNG phải "num"
  "betValue": 1000,       // long — số tiền cược cơ bản (chưa nhân rate)
  "clientNonce": "uuid"   // string, optional nhưng KHUYẾN NGHỊ — UUID v4 cho idempotency
}
```

> ⚠️ Sai tên field → **HTTP 400 Bad Request** với message `must not be null`. Dùng `modeId` + `ticket`, KHÔNG dùng `mode` + `num`.

**Response thành công:**

```json
{
  "success": true,
  "errorCode": "0000",
  "currentMoney": 1247105,       // số dư ví sau khi trừ
  "ticketId": 12345,             // id vé trong bảng `lode`
  "message": null
}
```

**Response lỗi (lấy từ smoke thật):**

```json
{
  "success": false,
  "errorCode": "0002",
  "currentMoney": 1248105,
  "ticketId": 0,
  "message": "Betting closed"
}
```

**Validation server-side:**

- `mode` phải ∈ {1,2,3,4,5,6,7,8,9,11}. Sai → `errorCode: "0006"`
- `num` phải đúng định dạng mode (xem §3 cột "Định dạng num")
- `betValue` ≥ 1000. Thấp hơn → `errorCode: "0004"`
- Wallet đủ tiền (sau `betValue × rate`). Không đủ → `errorCode: "0003"`
- Phase phải accept bets (DRAW_PENDING hoặc SETTLED). Không → `errorCode: "0002"`
- `clientNonce` trùng trong 10 phút → trả response cache (không tạo bet trùng)

**Hành vi đặc biệt:**
- Server snapshot `rate_at_purchase` + `prize_multiplier` vào row `lode` lúc đặt cược. Nếu admin đổi rate sau đó, vé này KHÔNG bị ảnh hưởng (SUN-1295 fix).

### 5.2 `GET /api/v2/lottery/xsmb/state` — Phase hiện tại

```bash
$ curl "https://staging-play.sunkr.bet/api/v2/lottery/xsmb/state" \
    -H "Authorization: Bearer $TOKEN"
```

**Response:**

```json
{
  "phase": "DRAW_PENDING",         // string — LotteryPhase enum
  "bettingOpen": true,             // bool — true iff phase ∈ {DRAW_PENDING, SETTLED}
  "lockTime": 1747318200000,       // long — epoch ms UTC của LẦN CHỐT SẮP TỚI (18:10 VN today/tomorrow)
  "scrapeTime": 1747319700000,     // long — epoch ms UTC của LẦN SCRAPE SẮP TỚI (18:35 VN today/tomorrow)
  "vnDate": "2026-05-14"           // ISO date — ngày hiện tại theo Vietnam wall clock
}
```

**`lockTime` + `scrapeTime` semantics:**
- Server trả **timestamp** (epoch ms UTC), KHÔNG phải chuỗi `"HH:mm"`
- Là thời điểm SẮP TỚI: nếu `now < 18:10 VN today` thì là hôm nay, ngược lại là ngày mai
- FE đếm ngược: `remainMs = response.lockTime - Date.now()`
- Lợi: FE không phải tự tính TZ + parse `HH:mm` + xét today/tomorrow

FE dùng response này để:
- Hiện countdown đến `lockTime` (đếm ngược từ client-side)
- Bật/tắt button "Đặt cược" dựa vào `phase`
- Quyết định có gọi `/result/{today}` được không (chỉ khi `todaySettleComplete = true`)

### 5.3 `GET /api/v2/lottery/xsmb/result/{date}` — Kết quả 1 ngày

```bash
$ curl "https://staging-play.sunkr.bet/api/v2/lottery/xsmb/result/2026-05-14" \
    -H "Authorization: Bearer $TOKEN"
```

**Trước khi settle xong (anti-cheat):**

```json
{
  "timestamp": "2026-05-14T13:53:37.952+00:00",
  "status": 404,
  "error": "Not Found",
  "path": "/api/v2/lottery/xsmb/result/2026-05-14"
}
```

> ⚠️ 404 trước settle là **BY DESIGN** — không phải bug. FE KHÔNG ĐƯỢC suy đoán kết quả từ bất kỳ nguồn nào khác. Chờ STOMP `/announce` hoặc poll `/state`.

**Sau khi settle:**

```json
{
  "date": "2026-05-14",
  "DB": "12345",                            // Đặc Biệt (5 chữ số)
  "G1": ["67890"],                          // Giải 1 (1 dãy)
  "G2": ["12345", "67890"],                 // Giải 2 (2 dãy)
  "G3": ["...", "...", "...", "...", "...", "..."],  // Giải 3 (6 dãy)
  "G4": ["...", "...", "...", "..."],       // Giải 4 (4 dãy)
  "G5": ["...", "...", "...", "...", "...", "..."],  // Giải 5 (6 dãy)
  "G6": ["...", "...", "..."],              // Giải 6 (3 dãy)
  "G7": ["...", "...", "...", "..."],       // Giải 7 (4 dãy, 2 chữ số)
  "settledAt": "2026-05-14T18:36:00Z"        // ISO timestamp khi settle xong
}
```

**Tổng số dãy:** 1 + 1 + 2 + 6 + 4 + 6 + 3 + 4 = **27 dãy** (Đặc Biệt + G1-G7). Đây là pool dùng cho mode LO_2_SO/XIEN_*.

> **Lưu ý:** `G7` chỉ có 2 chữ số (không phải 5 như các giải khác). Vì vậy mode `LO_3_SO` chỉ check trong 24 dãy (loại G7).

### 5.4 `GET /api/v2/lottery/xsmb/results?from=YYYY-MM-DD&to=YYYY-MM-DD` — Kết quả nhiều ngày

```bash
$ curl "https://staging-play.sunkr.bet/api/v2/lottery/xsmb/results?from=2026-05-08&to=2026-05-14" \
    -H "Authorization: Bearer $TOKEN"
```

**Response:** Array các object cùng shape với §5.3. Chỉ trả các ngày đã `settled_at NOT NULL`. Ngày chưa settle → bỏ qua khỏi array.

### 5.5 `GET /api/v2/lottery/xsmb/history?n=20` — Lịch sử vé của caller

```bash
$ curl "https://staging-play.sunkr.bet/api/v2/lottery/xsmb/history?n=20" \
    -H "Authorization: Bearer $TOKEN"
```

**Response:**

```json
{
  "entries": [
    {
      "ticketId": 12345,
      "createdAt": "2026-05-14T12:00:00Z",
      "modeId": 1,
      "modeName": "LÔ 2 SỐ",
      "ticket": "12",
      "betValue": 1000,
      "rateAtPurchase": 22,          // snapshot SUN-1295
      "prizeMultiplier": 80,
      "prize": null,                  // null nếu chưa settle
      "settledAt": null               // null nếu chưa settle
    },
    ...
  ],
  "count": 20
}
```

**Query params:**
- `n`: số vé tối đa, mặc định 20, max 120
- Caller chỉ thấy vé của chính mình. Không có cách query của user khác (player-facing).

### 5.6 `GET /api/v2/lottery/products` — Danh sách sản phẩm

```bash
$ curl "https://staging-play.sunkr.bet/api/v2/lottery/products"
```

**Response (public, không cần auth):**

```json
[{"code":"xsmb","lockTime":1747318200000,"timezone":"Asia/Ho_Chi_Minh","name":"Xổ Số Miền Bắc","scrapeTime":1747319700000}]
```

Các field:
| field | type | ý nghĩa |
|-------|------|---------|
| `code` | string | mã sản phẩm dùng cho endpoint path (vd `xsmb` → `/api/v2/lottery/xsmb/*`) |
| `name` | string | tên hiển thị tiếng Việt |
| `lockTime` | **long (epoch ms UTC)** | **timestamp** thời điểm CHỐT cược SẮP TỚI. Nếu `now < 18:10 VN hôm nay` → trả về 18:10 VN hôm nay; nếu đã quá → trả 18:10 VN ngày mai. FE đếm ngược qua `lockTime - Date.now()` |
| `scrapeTime` | **long (epoch ms UTC)** | timestamp lần scrape tiếp theo (18:35 VN hôm nay/mai), cùng cơ chế |
| `timezone` | string | TZ áp dụng (luôn `Asia/Ho_Chi_Minh` cho lottery) |

> Hiện tại chỉ có 1 product = XSMB. Sản phẩm XSMT/XSMN/Mega/Keno được liệt kê là roadmap, **chưa có**.

---

## 6. WebSocket / STOMP

**Endpoint:** `wss://staging-play.sunkr.bet/ws/lottery` (SockJS-compatible)

Connect với header STOMP:
```
Authorization: Bearer <accessToken>
```

### 6.1 Topics

| Topic | Loại event | Payload |
|-------|-----------|---------|
| `/topic/lottery/xsmb/lock` | One-shot khi phase → DRAW_LOCKED (lúc 18:10 VN) | `{"type":"locked","lockTime":"18:10"}` |
| `/topic/lottery/xsmb/announce` | One-shot khi phase → SETTLED (~18:36 VN) | `{"type":"settled","date":"2026-05-14"}` |
| `/topic/lottery/xsmb/chat` | Tin nhắn chat phòng (relay 2 chiều) | `{"user":"abc","msg":"...","ts":1234567890}` |

### 6.2 Quan trọng — WS KHÔNG bao giờ chở kết quả

`/topic/lottery/xsmb/announce` payload **CHỈ chứa**:
```json
{ "type": "settled", "date": "2026-05-14" }
```

KHÔNG có ĐB, G1-G7, prize, hay bất kỳ field nào khác liên quan đến kết quả. FE phải:

1. Nhận `announce` → trigger `GET /api/v2/lottery/xsmb/result/{date}`
2. REST endpoint mới là nguồn duy nhất của kết quả

→ Đây là anti-cheat invariant. Backend test (`StompAnnouncePayloadHasNoResultTest`) assert payload không chứa các key: `DB`, `ĐB`, `G1`, `G2`, ..., `G7`, `result`, `results`, `prize`, `draw`.

### 6.3 Gửi chat

Client SEND đến destination:
```
/app/lottery/xsmb/chat
Body: { "msg": "GLHF tất cả!" }
```

Server publish đến `/topic/lottery/xsmb/chat` (broadcast cho tất cả subscriber).

---

## 7. Cấu trúc kết quả XSMB

XSMB có 27 dãy số trong 8 hạng giải:

| Giải | Số dãy | Số chữ số | Pool cho mode |
|------|:------:|:---------:|---------------|
| **Đặc Biệt (ĐB)** | 1 | 5 | Tất cả mode dùng `de` (DAU, DUOI, DE, DE_DAU, BA_CANG) |
| **G1** | 1 | 5 | LO_2_SO, LO_3_SO |
| **G2** | 2 | 5 | LO_2_SO, LO_3_SO |
| **G3** | 6 | 5 | LO_2_SO, LO_3_SO |
| **G4** | 4 | 4 | LO_2_SO, LO_3_SO |
| **G5** | 6 | 4 | LO_2_SO, LO_3_SO |
| **G6** | 3 | 3 | LO_2_SO, LO_3_SO |
| **G7** | 4 | 2 | LO_2_SO (KHÔNG vào LO_3_SO vì chỉ 2 chữ số) |

**Tổng:** 27 dãy cho LO_2_SO; 24 dãy cho LO_3_SO.

**Định nghĩa `de`:** 2 chữ số cuối của ĐB. Vd ĐB = `"12345"` → `de = "45"`.

---

## 8. Error codes (lottery-specific)

| code | Ý nghĩa | Hành động FE | Retry? |
|------|---------|---------------|:------:|
| `0000` | OK | (không) | n/a |
| `0001` | Wallet failure / race condition | Hiển thị "Có lỗi, thử lại" | ✅ (cùng nonce) |
| `0002` | Phase đang lock (DRAW_LOCKED/SCRAPING/SETTLING) | "Phiên đã đóng, chờ phiên mới" | ❌ |
| `0003` | Không đủ tiền | "Số dư không đủ" | ❌ |
| `0004` | `betValue < 1000` | Validate FE trước khi gọi API | ❌ |
| `0006` | `mode` không hợp lệ HOẶC `num` sai format | Hiển thị lỗi định dạng | ❌ |
| HTTP 403 | Token sai/hết hạn | Login lại | ✅ (sau login) |
| HTTP 404 | `/result/{date}` chưa settle | Chờ STOMP `/announce` rồi gọi lại | ✅ |
| HTTP 429 | Rate limit | Backoff (delay tăng dần) | ✅ |

---

## 9. Cancel/refund — KHÔNG HỖ TRỢ

**Hiện tại không có endpoint hủy cược.** Khi `POST /bet` trả `success: true`:

1. Ví đã bị trừ `betValue × rate`
2. Row đã được insert vào `lode` table với snapshot rate/prize
3. Settle sẽ chạy lúc ~18:36 VN dựa trên kết quả XSMB hôm đó
4. Không thể rollback — vi phạm SUN-1295 anti-TOCTOU

### 9.1 FE pattern bắt buộc

```
1. User chọn mode + num + betValue
2. FE hiện dialog "Xác nhận đặt cược X VND cho mode Y, số Z?"
3. User confirm → mới gọi POST /bet
4. Nếu network timeout → retry với CÙNG clientNonce (server trả cache, không tạo bet trùng)
5. Nếu success → bet đã commit, không undo được
```

### 9.2 Trường hợp scrape XSMB lỗi

Nếu az24.vn down lúc 18:35 → server không pull được kết quả → `phase` ở lại `SCRAPING`/`SETTLING` đến khi ops thủ công can thiệp. Các vé hôm đó: `prize = null`, `settled_at = null` — chờ cho đến khi scrape thành công ở lần sau. **Không có auto-refund.**

(Đây là behavior preserved từ legacy. Ticket follow-up: thêm endpoint admin manual-refund nếu cần — chưa làm.)

---

## 10. Flow chuẩn FE

### 10.1 Khi load màn hình Lô Đề

```
1. GET /api/v2/lottery/xsmb/state
   → biết phase, lockTime, todaySettleComplete, lastSettledDate

2. Nếu phase ∈ {DRAW_PENDING, SETTLED}: bật form đặt cược
   Nếu phase ∈ {DRAW_LOCKED, SCRAPING, SETTLING}: ẩn form, hiện "Phiên đã đóng, đang chờ kết quả"

3. Hiển thị countdown:
   - Nếu phase = DRAW_PENDING: đếm ngược đến lockTime hôm nay (18:10 VN)
   - Nếu phase = SETTLED: đếm ngược đến 18:10 NGÀY MAI

4. WS connect: wss://staging-play.sunkr.bet/ws/lottery
   Subscribe: /topic/lottery/xsmb/lock + /announce

5. GET /api/v2/lottery/xsmb/result/{lastSettledDate} → hiện kết quả mới nhất
   (Hoặc GET /results?from=&to= để hiện 7 ngày gần nhất)
```

### 10.2 Khi user đặt cược

```
1. Validate FE:
   - mode ∈ {1..9, 11}
   - num đúng format mode (xem §3 cột "Định dạng num")
   - betValue ≥ 1000

2. Hiển thị xác nhận: "Cược X cho mode Y, số Z?"

3. const nonce = crypto.randomUUID();
   POST /bet { modeId, ticket, betValue, clientNonce: nonce }

4. Xử lý response:
   - success:true → hiện "Đặt cược thành công, mã vé #ticketId"
   - errorCode:"0002" → "Phiên đã đóng"
   - errorCode:"0003" → "Không đủ tiền"
   - errorCode:"0004" → "Cược tối thiểu 1000 VND"
   - errorCode:"0006" → "Định dạng số sai"
   - HTTP 403 → token hết hạn, login lại
   - Network timeout → retry CÙNG nonce
```

### 10.3 Khi đến giờ chốt + có kết quả

```
1. WS push /lock đến: hiển thị "Phiên đã đóng — chờ kết quả ~25 phút"

2. WS push /announce đến (sau ~25 phút): { type:"settled", date:"2026-05-14" }

3. GET /api/v2/lottery/xsmb/result/2026-05-14 → render bảng kết quả

4. GET /api/v2/lottery/xsmb/history?n=20 → cập nhật vé của user
   (Vé hôm nay giờ đã có `prize` và `settledAt` populated)

5. Phase tự động chuyển sang SETTLED → form đặt cược cho ngày mai mở lại
```

### 10.4 Timer countdown trên FE

**Vì server pin Vietnam TZ và FE có thể ở bất kỳ TZ nào, nguyên tắc:**

```javascript
// Lấy thời điểm lock CHÍNH XÁC theo UTC từ server state
// state.lockTime = "18:10" (Vietnam wall clock)
// → Convert sang UTC: 18:10 VN = 11:10 UTC

// Cách an toàn: dùng Intl API + ZoneId
const todayInVN = new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Ho_Chi_Minh" });
const lockTimeVN = new Date(`${todayInVN}T18:10:00+07:00`);  // ISO với offset VN
const remainMs = lockTimeVN.getTime() - Date.now();

// remainMs là số ms còn lại trên đồng hồ user
// Hiển thị giảm 1 lần / giây
```

**KHÔNG dùng `new Date()` trực tiếp** vì kết quả phụ thuộc TZ của user browser. Luôn ép `Asia/Ho_Chi_Minh` cho mọi tính toán liên quan đến lottery.

---

## 11. Timezone — Vietnam (pin cứng ở server)

Server lottery code pin `ZoneId.of("Asia/Ho_Chi_Minh")` cho:
- Lock check (`isBettingOpen` ở `LotteryClock.java`)
- Scheduler scrape job (`@Scheduled(cron="0 35 18 * * *", zone="Asia/Ho_Chi_Minh")`)
- `LotteryClock.LOCK_TIME = 18:10 VN`
- `LotteryClock.SCRAPE_TIME = 18:35 VN`
- `LotteryClock.POST_LOCK_HOLD = 45 phút`

**Note ops:** Global container ENV là `TZ=Asia/Seoul` (Korea, intentional cho TaiXiu/Sicbo realtime). Lottery code phải pin VN ở code level → đã làm. FE không cần lo về TZ của container.

---

## 12. Anti-cheat — pre-settle censorship

**Server invariant (đã test):**

| Tình huống | Kết quả từ REST |
|------------|------------------|
| `GET /result/{today}` khi phase ≠ SETTLED cho today | **HTTP 404** |
| `GET /results?from=&to=` chứa today chưa settle | Today **bị bỏ qua** khỏi array trả về |
| WS `/announce` payload | **KHÔNG** chứa `DB`, `G1-G7`, `prize`, `draw`, `result`, `results` — chỉ `{type, date}` |

**FE không được:**
1. Cache kết quả từ phiên cũ và hiển thị cho ngày mới
2. Suy đoán kết quả từ heuristic (vd số người trúng)
3. Hiện kết quả của ngày X khi `GET /result/{X}` trả 404

Nếu FE phát hiện server trả kết quả TRƯỚC khi `state.todaySettleComplete = true` → đó là bug backend, báo ngay.

---

## 13. Tóm tắt nhanh cho FE team

| Câu hỏi | Trả lời |
|---------|---------|
| Login như nào? | `c=3` cũ, lấy `accessToken`, gửi qua `Authorization: Bearer ...` hoặc `?at=...` |
| Có bao nhiêu mode? | **10 mode** (id 1..9 + 11). KHÔNG có id 0 và 10. |
| Cược tối thiểu? | 1000 VND |
| Tiền cược trừ ví? | `betValue × rate` (vd LO_2_SO: rate=22 → cược 1000 trừ ví 22000) |
| Tiền thắng? | `số_lần_trùng × betValue × prizeMultiplier` (xem §3.2) |
| Có cancel cược? | **KHÔNG**. Confirm dialog ở FE TRƯỚC khi gọi `/bet` |
| Idempotency? | Bắt buộc gửi `clientNonce` (UUID v4) trong body `/bet` |
| Countdown? | FE tự đếm tới `lockTime` (18:10 VN) — KHÔNG có 1Hz tick từ server |
| Khi nào có kết quả? | ~18:36 VN. STOMP `/announce` event → gọi `GET /result/{date}` |
| WS có chở kết quả không? | **KHÔNG**. Luôn fetch REST sau khi nhận `/announce` |
| TZ? | Server pin Asia/Ho_Chi_Minh. FE phải convert wall clock VN sang TZ user khi hiển thị |
| Test account staging? | `zuestang` / MD5(`123456a@`) — đã verify hoạt động |

---

**Đã verify trên staging 2026-05-14.** Source authority:
- `backend-master/game/lottery-engine/src/main/java/com/sunwinkr/lottery/engine/model/LotteryMode.java` (10 modes)
- `backend-master/game/lottery-engine/src/main/java/com/sunwinkr/lottery/engine/clock/LotteryPhase.java` (5 phases)
- `backend-master/game/lottery-engine/src/main/java/com/sunwinkr/lottery/engine/clock/LotteryClock.java` (constants VN, LOCK_TIME=18:10, SCRAPE_TIME=18:35)
- `backend-master/game/lottery-api/src/main/java/com/sunwinkr/lottery/api/controller/XsmbController.java` (6 endpoints)
- `docs/specs/lottery-rules-spec.md` (16 invariants, full payout table)
- `docs/specs/lottery-anticheat-audit.md` (anti-cheat hardening)
