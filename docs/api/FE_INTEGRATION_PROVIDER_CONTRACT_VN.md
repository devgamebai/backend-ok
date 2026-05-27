# Hướng Dẫn Migration FE — Provider-Style Bet Contract (SUN-1339)

Tài liệu dành cho team Front-End. Mô tả contract mới thống nhất theo chuẩn provider (GSC/EVO) áp dụng cho **Lô Đề XSMB**, **Tài Xỉu**, và **Xí Ngầu Sicbo** sau Phase A+B của SUN-1339.

- **Staging base:** `https://staging-play.sunkr.bet`
- **Verified live:** 2026-05-15
- **Áp dụng từ:** staging branch, commit e9f7ce4e trở đi

---

## 1. Tổng quan

### Tại sao có thay đổi này?

Trước Phase A+B, mỗi game có cách riêng để kiểm soát cửa sổ cược:

- **Lottery** dùng field boolean `bettingOpen` kết hợp với `phase` enum.
- **TaiXiu/Sicbo** dùng `bettingState` boolean.

Vấn đề thực tế:

1. **Clock skew 1 giây** giữa server và client có thể khiến FE hiển thị "đang mở" trong khi server đã đóng (hoặc ngược lại) — gây confusion khi player nhấn đặt cược ngay biên thời gian.
2. Không có trường epoch ms tuyệt đối → FE phải tự tính toán từ `HH:mm` wall-clock string + TZ + today/tomorrow logic.
3. Third-party casino integration (GSC, EVO) dùng field `betEndAt` (epoch ms) làm chuẩn. Để đồng nhất UX và logic FE cho tất cả các nguồn game trên platform, internal games cần cùng convention.

### Phase A+B làm gì?

**Không xóa bất kỳ field cũ nào.** Tất cả field hiện tại (`bettingOpen`, `bettingState`, `phase`, `lockTime` HH:mm, `remainTime`, v.v.) vẫn được trả về đầy đủ cho backward compatibility.

Phase A+B **bổ sung** 3 trường mới vào state response của cả 3 game:

| Trường mới | Kiểu | Ý nghĩa |
|---|---|---|
| `safeBetExpiresAt` | `long` (epoch ms UTC) | Thời điểm server-authoritative mà sau đó mọi bet đều bị reject. Semantic tương đương `betEndAt` của GSC/EVO. |
| `settleAt` | `long` (epoch ms UTC) | Thời điểm dự kiến settle. Lottery: từ `scrapeTime`. TaiXiu/Sicbo: ngầm định (round kết thúc ngay sau `safeBetExpiresAt + delta nhỏ`). |
| `roundId` | `long` | Định danh round hiện tại. Lottery: `yyyymmdd` (vd `20260515`). TaiXiu/Sicbo: số thứ tự monotonic = `referenceId`. |

Ngoài ra, **server-side validation** được tighten: server reject với `BET_WINDOW_CLOSED` nếu `clock.millis() >= safeBetExpiresAt` — không còn dựa vào phase enum đơn thuần.

### FE cần làm gì?

**Mức tối thiểu:** Không cần thay đổi gì nếu flow hiện tại đang hoạt động ổn. Các field cũ vẫn có.

**Khuyến nghị:** Migrate countdown và bet-gate sang `safeBetExpiresAt` để:
- Tránh off-by-one-second bug tại biên cửa sổ cược.
- Code đơn giản hơn (1 trường epoch ms thay vì parse HH:mm + Tính TZ + xét today/tomorrow).
- Nhất quán với cách FE xử lý GSC/EVO game.

---

## 2. Các trường mới trong state response

### 2.1 Lottery XSMB (`GET /api/v2/lottery/xsmb/state`)

| Trường | Kiểu | Backward compat? | Ý nghĩa |
|--------|------|:---:|---------|
| `phase` | string | Cũ | `DRAW_PENDING` \| `DRAW_LOCKED` \| `SCRAPING` \| `SETTLING` \| `SETTLED` |
| `bettingOpen` | bool | Cũ | `true` khi `phase ∈ {DRAW_PENDING, SETTLED}` |
| `lockTime` | long (epoch ms) | Cũ* | Thời điểm chốt cược sắp tới (18:10 VN). **Đã là epoch ms từ SUN-1339 Phase A.** |
| `scrapeTime` | long (epoch ms) | Cũ* | Thời điểm scrape sắp tới (18:35 VN). |
| `vnDate` | string | Cũ | Ngày hiện tại theo Vietnam wall clock, `"yyyy-MM-dd"`. |
| **`safeBetExpiresAt`** | **long (epoch ms)** | **Mới** | **= `lockTime`. Server dùng trường này để reject bet. Dùng cho countdown FE.** |
| **`settleAt`** | **long (epoch ms)** | **Mới** | **= `scrapeTime`. Thời điểm dự kiến kết quả được settle.** |
| **`roundId`** | **long** | **Mới** | **= ngày XSMB dạng `yyyymmdd` (vd `20260515`). Dùng làm key idempotency và display.** |

> *`lockTime` và `scrapeTime` đã được chuyển sang epoch ms từ Phase A (trước đó là HH:mm string). Nếu code FE đang parse string, cần update.

### 2.2 Tài Xỉu (`GET /api/v2/taixiu/state`)

| Trường | Kiểu | Backward compat? | Ý nghĩa |
|--------|------|:---:|---------|
| `referenceId` | long | Cũ | Round id (monotonic) |
| `remainTime` | short | Cũ | Giây còn lại trong phase hiện tại |
| `bettingState` | bool | Cũ | `true` = đang mở cược |
| `potTai`, `potXiu` | long | Cũ | Pot hiện tại mỗi phía |
| `result` | short | Cũ | `-1` pre-reveal; `0`=Xỉu, `1`=Tài post-reveal |
| **`safeBetExpiresAt`** | **long (epoch ms)** | **Mới** | **Epoch ms khi cửa sổ cược đóng. `0` = engine idle, chưa có round active.** |
| **`settleAt`** | **long (epoch ms)** | **Mới** | **Ngầm định: `safeBetExpiresAt + delta nhỏ` (~vài giây). Không phải điểm quan trọng cho FE.** |
| **`roundId`** | **long** | **Mới** | **= `referenceId`. Đưa vào body bet để tracing nếu cần.** |

> **Lưu ý `safeBetExpiresAt=0`:** Engine TaiXiu có thể ở trạng thái idle (chưa có round nào active). Khi đó `safeBetExpiresAt=0`. FE phải guard: chỉ dùng `safeBetExpiresAt` cho countdown khi `safeBetExpiresAt > 0` và `bettingState == true`.

### 2.3 Xí Ngầu Sicbo (`GET /api/v2/sicbo/state`)

Cùng shape với TaiXiu. Các trường mới giống hệt.

| Trường | Kiểu | Backward compat? | Ý nghĩa |
|--------|------|:---:|---------|
| `referenceId` | long | Cũ | Round id |
| `remainTime` | short | Cũ | Giây còn lại |
| `bettingState` | bool | Cũ | `true` = mở cược |
| `phase` | string | Cũ | `OPEN` \| `LOCKED` \| `REVEAL` \| v.v. |
| **`safeBetExpiresAt`** | **long (epoch ms)** | **Mới** | **Epoch ms đóng cửa sổ cược. `0` nếu idle.** |
| **`settleAt`** | **long (epoch ms)** | **Mới** | **Dự kiến settle.** |
| **`roundId`** | **long** | **Mới** | **= `referenceId`.** |

---

## 3. Quy tắc xác thực bet (server-side)

### 3.1 Timestamp guard

Từ Phase A+B, server thực hiện kiểm tra:

```
if (clock.millis() >= safeBetExpiresAt) → reject với BET_WINDOW_CLOSED
```

Kiểm tra này chạy **trước** mọi kiểm tra khác (phase enum, `bettingState`, v.v.). Nếu request đến đúng biên (1-2ms sau `safeBetExpiresAt`), server luôn reject — không có grace period.

**Trước Phase A+B:** Server kiểm tra `phase != DRAW_PENDING` (Lottery) hoặc `bettingState == false` (TaiXiu/Sicbo). Clock skew 1 giây giữa FE và server có thể khiến FE show "mở" nhưng server reject.

**Sau Phase A+B:** Server kiểm tra `clock.millis() >= safeBetExpiresAt`. FE dùng `safeBetExpiresAt - Date.now()` cho countdown → khi countdown về 0, server cũng đóng đúng lúc.

### 3.2 FE nên làm gì khi nhận `BET_WINDOW_CLOSED`

1. Hiển thị thông báo tiếng Việt phù hợp (xem §4).
2. Gọi `GET /state` để refresh trạng thái — có thể round mới đã mở.
3. **Không retry cùng bet.** Đây là lỗi do thời gian, không phải network error. Retry với cùng `clientNonce` sẽ trả về cùng rejection.

---

## 4. Mã lỗi & cách xử lý

### 4.1 Lottery XSMB

| `errorCode` | Tên lỗi | Thông báo FE (tiếng Việt) | Retry? |
|---|---|---|:---:|
| `0000` | OK | — | n/a |
| **`0002`** | **BET_WINDOW_CLOSED** | **"Đã hết giờ cược, vui lòng chờ phiên mới"** | ❌ |
| `0001` | Wallet failure | "Có lỗi xử lý ví, thử lại" | ✅ (cùng nonce) |
| `0003` | Insufficient balance | "Số dư không đủ" | ❌ |
| `0004` | Below min bet (< 1000 VND) | "Cược tối thiểu 1.000 VND" | ❌ |
| `0006` | Invalid mode / num format | "Định dạng số không hợp lệ" | ❌ |
| HTTP 403 | Token invalid/expired | Login lại | ✅ (sau login) |

### 4.2 Tài Xỉu

| `errorCode` | Tên lỗi | Thông báo FE (tiếng Việt) | Retry? |
|---|---|---|:---:|
| `0000` | OK | — | n/a |
| **`7`** | **BET_WINDOW_CLOSED** | **"Phiên đã đóng, chờ phiên mới"** | ❌ |
| `0001` | Wallet failure | "Có lỗi xử lý ví, thử lại" | ✅ (cùng nonce) |
| `0003` | Insufficient balance | "Số dư không đủ" | ❌ |
| `0004` | Below min bet (< 100) | "Cược tối thiểu 100 VND" | ❌ |
| `0005` | Cross-side bet | "Bạn đã cược Tài/Xỉu phiên này, không thể đặt ngược" | ❌ |
| HTTP 403 | Token invalid/expired | Login lại | ✅ (sau login) |

### 4.3 Xí Ngầu Sicbo

| `errorCode` | Tên lỗi | Thông báo FE (tiếng Việt) | Retry? |
|---|---|---|:---:|
| `0000` | OK | — | n/a |
| **`7`** | **BET_WINDOW_CLOSED** | **"Phiên đã đóng, chờ phiên mới"** | ❌ |
| `0001` | Wallet failure | "Có lỗi xử lý ví, thử lại" | ✅ (cùng nonce) |
| `0003` | Insufficient balance | "Số dư không đủ" | ❌ |
| `0006` | Invalid betSide string | "Loại cược không hợp lệ" | ❌ |
| HTTP 403 | Token invalid/expired | Login lại | ✅ (sau login) |

---

## 5. Endpoint admin unsettle (CMS only)

**Chỉ dành cho ops team / admin CMS.** Player-facing FE không cần tích hợp phần này.

Ba endpoint mới (Phase B), role-gated — yêu cầu admin token (`aat`):

```
POST /api/v2/lottery/xsmb/admin/unsettle
POST /api/v2/admin/taixiu/unsettle
POST /api/v2/admin/sicbo/unsettle
```

### 5.1 Request body

```json
{
  "ticketId": 12345,
  "reason": "Lỗi kỹ thuật — scrape nhầm kết quả 2026-05-15"
}
```

| Field | Kiểu | Bắt buộc | Mô tả |
|-------|------|:---:|-------|
| `ticketId` | long | ✅ | ID vé/giao dịch cần void |
| `reason` | string | ✅ | Lý do audit (ghi vào `log_money_user_vin`) |

### 5.2 Response thành công

```json
{
  "success": true,
  "errorCode": "0",
  "ticketId": 12345,
  "settleStatus": "VOIDED",
  "message": null
}
```

### 5.3 Response lỗi — vé đã VOIDED trước đó

```json
{
  "success": false,
  "errorCode": "4004",
  "message": "Ticket already voided"
}
```

### 5.4 Ví dụ curl (Lottery)

```bash
$ curl -X POST "https://staging-admin.sunkr.bet/api/v2/lottery/xsmb/admin/unsettle" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{"ticketId": 12345, "reason": "Lỗi kỹ thuật — void theo yêu cầu ops"}'
```

### 5.5 Hành vi phía server

Khi unsettle thành công:

1. Hoàn tiền vào ví player (tạo ledger entry ngược).
2. Flip `settle_status` của row thành `VOIDED`.
3. Ghi audit row vào `log_money_user_vin` với actor, `old_value=SETTLED`, `new_value=VOIDED`, `reason`.

---

## 6. Settle status enum

Các row bet (bảng `lode` cho Lottery, bảng tương ứng cho TaiXiu/Sicbo) có field `settle_status` với 3 giá trị:

| Giá trị | Ý nghĩa phía server | Ý nghĩa với player |
|---------|---------------------|--------------------|
| `PENDING` | Bet đã accepted, round đang diễn ra, chưa settle. | "Cược đang chờ kết quả" — ví đã trừ, chưa biết thắng/thua. |
| `SETTLED` | Round kết thúc. Nếu thắng: ví đã cộng prize. Nếu thua: không có hành động thêm. | "Đã có kết quả" — cộng/giữ như kết quả. |
| `VOIDED` | Admin đã chargeback. Ví đã được hoàn lại đúng `betValue × rate` (hoặc tương đương). | "Vé bị hủy — tiền đã hoàn" — không thắng, không thua, tiền về ví. |

### Cách FE hiển thị

| `settle_status` | Màu / badge đề xuất | Text |
|----------------|---------------------|------|
| `PENDING` | Vàng / "Đang chờ" | "Chờ kết quả" |
| `SETTLED` | Xanh lá (thắng) / Đỏ (thua) | "Thắng X VND" / "Thua" |
| `VOIDED` | Xám / "Đã hủy" | "Đã hoàn tiền" |

Field này có trong response của `GET /history` (cả 3 game). FE lấy từ đó để render lịch sử cược.

---

## 7. Hướng dẫn migration FE

### 7.1 Countdown — trước và sau

**Trước (dùng `bettingOpen` boolean):**

```javascript
// Lottery — cách cũ
const state = await fetchState();
if (state.bettingOpen) {
  // Countdown tự tính từ "18:10" string + Asia/Ho_Chi_Minh TZ
  const todayVN = new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Ho_Chi_Minh" });
  const lockTime = new Date(`${todayVN}T18:10:00+07:00`);
  const remainMs = lockTime.getTime() - Date.now();
  startCountdown(remainMs);
}

// TaiXiu/Sicbo — cách cũ
if (state.bettingState && state.remainTime > 0) {
  startCountdown(state.remainTime * 1000); // client-side đếm ngược từ remainTime
}
```

**Sau (dùng `safeBetExpiresAt` epoch ms):**

```javascript
// Tất cả 3 game — cách mới (thống nhất)
const state = await fetchState();

// Guard: safeBetExpiresAt=0 nghĩa là engine idle (TaiXiu/Sicbo only)
const canBet = state.safeBetExpiresAt > 0 && Date.now() < state.safeBetExpiresAt;

if (canBet) {
  const remainMs = state.safeBetExpiresAt - Date.now();
  startCountdown(remainMs);  // đếm ngược đến hết giờ cược
}
```

> Không còn parse TZ, không còn tính today/tomorrow. `safeBetExpiresAt` là epoch ms tuyệt đối — `Date.now()` trừ ra là xong.

### 7.2 Bet-gate — trước và sau

**Trước:**

```javascript
// Lottery
if (!state.bettingOpen || state.phase === "DRAW_LOCKED") {
  showError("Phiên đã đóng");
  return;
}

// TaiXiu/Sicbo
if (!state.bettingState) {
  showError("Phiên đã đóng");
  return;
}
```

**Sau:**

```javascript
// Tất cả 3 game — cách mới
if (state.safeBetExpiresAt === 0 || Date.now() >= state.safeBetExpiresAt) {
  showError("Đã hết giờ cược");
  return;
}
// Vẫn có thể gọi POST /bet
```

> FE-side guard này không thay thế server validation — chỉ để cải thiện UX. Nếu player nhấn đúng biên và server vẫn reject `BET_WINDOW_CLOSED`, FE xử lý như §3.2.

### 7.3 Xử lý `BET_WINDOW_CLOSED` từ server

```javascript
async function placeBet(params) {
  const nonce = crypto.randomUUID();
  const res = await postBet({ ...params, clientNonce: nonce });

  if (res.success) {
    showSuccess(`Đặt cược thành công — mã ${res.ticketId ?? res.perBetTxId}`);
    return;
  }

  // Lottery errorCode "0002" | TaiXiu/Sicbo errorCode "7"
  const BET_WINDOW_CLOSED = { "0002": true, "7": true };

  if (BET_WINDOW_CLOSED[res.errorCode]) {
    showWarning("Đã hết giờ cược, vui lòng chờ phiên mới");
    await refreshState();  // fetch lại state để cập nhật safeBetExpiresAt
    return;
  }

  // Xử lý các lỗi khác...
}
```

### 7.4 Dùng `roundId` cho display và tracing

```javascript
// Trước — dùng referenceId (TaiXiu/Sicbo) hoặc vnDate (Lottery)
const roundLabel = game === "lottery" ? state.vnDate : `#${state.referenceId}`;

// Sau — thống nhất 1 field cho cả 3 game
const roundLabel = `#${state.roundId}`;
// Lottery: roundId = 20260515 → hiển thị "Phiên 20260515" hoặc parse ra "2026-05-15"
// TaiXiu/Sicbo: roundId = 1042 → hiển thị "Phiên #1042"
```

---

## 8. Câu hỏi thường gặp (FAQ)

**Q1: `bettingOpen` (Lottery) và `safeBetExpiresAt` khác nhau ở điểm nào?**

`bettingOpen` là boolean — nó phụ thuộc vào `phase` enum được cập nhật theo chu kỳ server-side. Có thể có độ trễ vài millisecond giữa lúc `safeBetExpiresAt` qua và lúc `bettingOpen` flip sang `false`. `safeBetExpiresAt` là epoch ms tuyệt đối — server dùng chính trường này để reject bet. FE nên dùng `safeBetExpiresAt` làm nguồn duy nhất cho countdown và bet-gate để tránh mismatch ở biên thời gian.

---

**Q2: Nếu server time drift so với thực tế thì sao? FE có bị ảnh hưởng không?**

Server time là authoritative — mọi reject/accept đều tính theo `clock.millis()` của server, so với `safeBetExpiresAt` mà chính server đã set. FE dùng `Date.now()` (client clock) cho countdown visual, nhưng server không dùng client clock để quyết định. Nếu client clock lệch server clock 1-2 giây:

- Countdown FE có thể về 0 sớm hoặc muộn hơn vài giây so với server thực sự đóng cược.
- Bet gửi ngay khi countdown FE về 0 vẫn có thể được server accept (nếu server clock chưa đến `safeBetExpiresAt`) hoặc reject (nếu server clock đã qua). Đây là behavior bình thường — FE chỉ cần xử lý `BET_WINDOW_CLOSED` gracefully như §3.2.
- Khuyến nghị: đừng hiển thị đồng hồ đếm ngược đến millisecond — độ chính xác 1 giây là đủ và đúng với thực tế.

---

**Q3: `safeBetExpiresAt=0` nghĩa là gì? Có phải lỗi không?**

Với **TaiXiu và Sicbo**, `safeBetExpiresAt=0` nghĩa là engine đang idle — chưa có round nào đang active (ví dụ server vừa khởi động, hoặc round trước vừa kết thúc và round mới chưa bắt đầu). Đây là trạng thái hợp lệ, không phải lỗi. FE guard: chỉ dùng `safeBetExpiresAt` khi giá trị `> 0`.

Với **Lottery**, `safeBetExpiresAt` luôn là epoch ms của lần chốt cược sắp tới (18:10 VN hôm nay hoặc ngày mai) — không bao giờ bằng 0.

---

**Q4: `VOIDED` khác `SETTLED` thua như thế nào từ góc nhìn player?**

- `SETTLED` thua: player thực sự thua — ví đã trừ khi bet, không có hoàn trả.
- `VOIDED`: admin đã chargeback — ví được hoàn lại đúng `betValue × rate` (hoặc tương đương cho TaiXiu/Sicbo). Player không thắng nhưng cũng không thua cho vé này. Trong lịch sử cược, badge "Đã hoàn tiền" phân biệt rõ với "Thua".

Lý do VOIDED thường gặp: lỗi kỹ thuật (scrape nhầm kết quả, server ngoại tuyến giữa round), sự kiện force-unsettle của ops team.

---

**Q5: `settleAt` trên TaiXiu/Sicbo có dùng để làm gì không?**

Với TaiXiu/Sicbo, `settleAt` là ước tính ngầm định và không quan trọng cho FE — round settle ngay sau khi `safeBetExpiresAt` qua (trong vài giây). FE không cần dùng `settleAt` cho countdown hay display ở TaiXiu/Sicbo. Trường này hữu ích hơn ở **Lottery**, nơi `settleAt = scrapeTime` (18:35 VN) — cách `safeBetExpiresAt` (18:10 VN) đến 25 phút.

---

**Q6: Có cần gửi `roundId` trong body `POST /bet` không?**

Không bắt buộc. Server không yêu cầu `roundId` trong request body hiện tại. Trường này trên state response chủ yếu để:
- FE tự detect khi round mới bắt đầu (so sánh `roundId` cũ với mới sau khi refresh state).
- Tracing trong support tickets ("round 20260515" thay vì phải describe phase/date).

---

**Q7: Các trường cũ có bị remove không? Lộ trình là gì?**

Theo SUN-1339, không có kế hoạch remove các trường cũ (`bettingOpen`, `bettingState`, `phase`, `remainTime`, `lockTime` HH:mm string cũ). Chúng vẫn được trả về song song. Khi nào team FE đã migrate hoàn toàn sang contract mới và xác nhận ổn trên staging, sẽ thông báo riêng về deprecation timeline — không phải trong ticket này.

---

**Q8: Round ID cho Lottery là số nguyên `20260515` hay string `"2026-05-15"`?**

Trong state response, `roundId` là **`long` (số nguyên) = `20260515`** cho ngày 2026-05-15. Đây là số nguyên `yyyymmdd` theo Asia/Ho_Chi_Minh — không phải string ngày. Nếu FE cần hiển thị theo định dạng ngày, tự parse:

```javascript
const roundId = 20260515; // từ state.roundId
const year  = Math.floor(roundId / 10000);       // 2026
const month = Math.floor((roundId % 10000) / 100); // 05
const day   = roundId % 100;                       // 15
const label = `${year}-${String(month).padStart(2,"0")}-${String(day).padStart(2,"0")}`;
// → "2026-05-15"
```

---

## 9. Sample requests / responses

### 9.1 Lottery XSMB state (phase DRAW_PENDING)

```bash
$ curl "https://staging-play.sunkr.bet/api/v2/lottery/xsmb/state" \
    -H "Authorization: Bearer $TOKEN"
```

```json
{
  "phase": "DRAW_PENDING",
  "bettingOpen": true,
  "lockTime": 1778843400000,
  "safeBetExpiresAt": 1778843400000,
  "scrapeTime": 1778844900000,
  "settleAt": 1778844900000,
  "vnDate": "2026-05-15",
  "roundId": 20260515
}
```

Diễn giải:
- `lockTime = safeBetExpiresAt = 1778843400000` → 18:10 VN ngày 2026-05-15 (= `2026-05-15T11:10:00Z`).
- `scrapeTime = settleAt = 1778844900000` → 18:35 VN ngày 2026-05-15 (= `2026-05-15T11:35:00Z`).
- `roundId = 20260515` → phiên XSMB ngày 2026-05-15.
- FE countdown: `remainMs = 1778843400000 - Date.now()`.

### 9.2 Sicbo state (round active)

```bash
$ curl "https://staging-play.sunkr.bet/api/v2/sicbo/state?moneyType=1" \
    -H "Authorization: Bearer $TOKEN"
```

```json
{
  "referenceId": 1,
  "safeBetExpiresAt": 1778818961354,
  "roundId": 1,
  "remainTime": 0,
  "bettingState": true,
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
  "realNumBetXiu": 0,
  "phase": "OPEN"
}
```

Diễn giải:
- `safeBetExpiresAt = 1778818961354` → epoch ms cụ thể khi cửa sổ đóng.
- `roundId = 1` = `referenceId = 1` → round đầu tiên sau khi engine khởi động.
- `dice1/2/3 = 0, result = -1` → pre-reveal censoring (không hiển thị cho player).
- `bettingState = true` → đang mở cược. FE countdown: `1778818961354 - Date.now()`.

### 9.3 TaiXiu state (engine idle)

```bash
$ curl "https://staging-play.sunkr.bet/api/v2/taixiu/state?moneyType=1" \
    -H "Authorization: Bearer $TOKEN"
```

```json
{
  "referenceId": 1,
  "remainTime": 0,
  "bettingState": true,
  "safeBetExpiresAt": 0,
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

Diễn giải:
- `safeBetExpiresAt = 0` → engine idle, chưa có round active. FE phải guard: không dùng giá trị này cho countdown.
- `bettingState = true` có thể đồng thời với `safeBetExpiresAt = 0` trong trạng thái khởi động. FE nên kiểm tra cả hai: `bettingState && safeBetExpiresAt > 0`.
- Round mới sẽ có `safeBetExpiresAt > 0` sau khi engine ghi nhận người chơi đầu tiên join.

### 9.4 Bet bị reject do hết giờ (Lottery)

```bash
$ curl -X POST "https://staging-play.sunkr.bet/api/v2/lottery/xsmb/bet" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"modeId":1,"ticket":"12","betValue":1000,"clientNonce":"550e8400-e29b-41d4-a716-446655440000"}'
```

```json
{
  "success": false,
  "errorCode": "0002",
  "currentMoney": 1248105,
  "ticketId": 0,
  "message": "Betting closed"
}
```

### 9.5 Bet bị reject do hết giờ (TaiXiu/Sicbo)

```bash
$ curl -X POST "https://staging-play.sunkr.bet/api/v2/taixiu/bet" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"moneyType":1,"betValue":1000,"betSide":1,"clientNonce":"uuid-v4"}'
```

```json
{
  "success": false,
  "errorCode": "7",
  "currentMoney": 1248105,
  "perBetTxId": 0,
  "message": "Bet window closed"
}
```

### 9.6 Unsettle thành công (Lottery — admin only)

```bash
$ curl -X POST "https://staging-admin.sunkr.bet/api/v2/lottery/xsmb/admin/unsettle" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"ticketId":12345,"reason":"Lỗi kỹ thuật — void theo yêu cầu ops"}'
```

```json
{
  "success": true,
  "errorCode": "0",
  "ticketId": 12345,
  "settleStatus": "VOIDED",
  "message": null
}
```

---

**Verified on staging 2026-05-15.** Source authority:
- State response shapes: `backend-master/game/lottery-engine/src/main/java/com/sunwinkr/lottery/api/dto/LotteryStateDto.java`
- TaiXiu/Sicbo state: `backend-master/game/minigame/src/main/java/com/sunwinkr/minigame/dto/StateDto.java`
- BET_WINDOW_CLOSED guard: `backend-master/game/lottery-engine/src/main/java/com/sunwinkr/lottery/engine/service/LotteryBetService.java` (Lottery `errorCode=0002`), `backend-master/game/minigame/src/main/java/com/sunwinkr/minigame/service/BetService.java` (TaiXiu/Sicbo `errorCode=7`)
- Unsettle endpoints: `backend-master/game/lottery-engine/src/main/java/com/sunwinkr/lottery/api/controller/AdminUnsettleController.java`, tương tự cho TaiXiu/Sicbo
- Settle status enum: `backend-master/game/lottery-engine/src/main/java/com/sunwinkr/lottery/engine/model/SettleStatus.java`
- Related docs: `docs/api/FE_INTEGRATION_LOTTERY_LODE_VN.md`, `docs/api/FE_INTEGRATION_MINIGAME_V2.md`
