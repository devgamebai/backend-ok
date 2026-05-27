# Login History — Duplicate IP Detection API — handover for FE team

Phát hiện multi-accounting / chia sẻ tài khoản bằng cách nhóm `user_login_info` theo **IP thực** (client IP, bỏ qua các hop sau như Cloudflare edge) và trả về các IP có ≥ N user_id khác nhau cùng login từ đó.

- Base URL (staging): `https://staging-admin.sunkr.bet/api_backend`
- Base URL (local with Swagger proxy): `http://localhost:18080/api_backend`
- Auth: tham số `aat` lấy từ `c=701` (login admin), TTL 8h.
- Method: GET hoặc POST đều OK.
- Response envelope: tất cả endpoint trả `{success, errorCode, message?, data, total?, totalRecord?, groups?}`.
- Spec: [docs/superpowers/specs/2026-05-11-login-history-duplicate-ip-design.md](../superpowers/specs/2026-05-11-login-history-duplicate-ip-design.md) (in sunkr-admin-next repo).

## Source of truth: MongoDB collection `user_login_info`

Cùng nguồn với:
- `c=109` — flat list (paginated) phục vụ trang `/usergame/login-history` hiện tại
- `c=9733` — `ListUserLoginLogsProcessor` (used by other admin views)

Trường `ip` lưu **raw XFF chain** dạng `"client_ip, cloudflare_edge_ip"` (có thể nhiều hop). Endpoint này extract phần trước dấu phẩy đầu tiên (`$indexOfBytes`/`$substrBytes` trong aggregation) để group theo IP thực. Records có `ip` null hoặc rỗng bị loại trừ pre-group.

---

## c=110 — List duplicate-IP login groups

Nhóm các bản ghi đăng nhập theo IP thực, trả về **chỉ những IP** có `≥ mu` user_id khác nhau cùng dùng. Mỗi group kèm danh sách các user (kèm số lần login, lần đầu, lần cuối) trên IP đó.

### Mục đích nghiệp vụ

- Phát hiện bonus farming / multi-accounting (1 người dùng nhiều tài khoản từ 1 IP).
- Phát hiện agent / khách dùng chung máy (có thể là legit, có thể là collusion).
- Drill-down từ 1 nickname/IP cụ thể để xem ai khác liên đới.

### Request

**Bulk scan (default 7 ngày gần nhất):**
```
GET /api_backend?c=110&aat=<token>
```

**Bulk scan trong khoảng tự chọn, tăng threshold:**
```
GET /api_backend?c=110&aat=<token>&ts=2026-05-01&te=2026-05-11&mu=3
```

**Drill-down theo nickname (Pattern B — nn áp dụng pre-group):**
```
GET /api_backend?c=110&aat=<token>&nn=zuestang21
```
→ Tìm các IP mà user `zuestang21` đã login trong khoảng, sau đó liệt kê **tất cả** user khác trên những IP đó.

**Drill-down theo IP cụ thể:**
```
GET /api_backend?c=110&aat=<token>&ip=140.99.130.21
```
→ Liệt kê các user trên đúng IP này (vẫn yêu cầu ≥ `mu` user khác nhau để hiện).

### Params

| Param | Type | Required | Default | Constraint / Note |
|---|---|---|---|---|
| `aat` | string(32 hex) | yes | — | admin token |
| `ts` | date `YYYY-MM-DD` | no | `today - 7 days` | server-fills khi rỗng |
| `te` | date `YYYY-MM-DD` | no | `today` | server-fills khi rỗng |
| `type` | int | no | — | `1` (web) / `2` (mobile) / bỏ trống = tất cả |
| `nn` | string | no | — | nickname filter, **case-insensitive substring** (server escape regex meta-char) |
| `ip` | string | no | — | exact match against IP thực (post-split), pre-group |
| `mu` | int | no | `2` | min user khác nhau per IP; clamp `[2, 50]` |
| `p` | int | no | `1` | 1-based; **50 groups/page** |

### Validation (theo thứ tự)

1. `ts/te` rỗng → fill default (7 ngày, timezone `Asia/Ho_Chi_Minh`).
2. `ts > te` → swap silently.
3. `te - ts > 90 days` → reject với `ERR_DATE_RANGE_TOO_LARGE`.
4. `mu < 2` → coerce `2`; `mu > 50` → coerce `50`; non-numeric → `2`.
5. `p < 1` → coerce `1`.

### Response (success)

```json
{
  "success": true,
  "errorCode": "0",
  "message": null,
  "data": null,
  "total": 12,
  "totalRecord": 1234,
  "groups": [
    {
      "ip": "140.99.130.21",
      "user_count": 3,
      "first_seen": "2026-05-04 12:30:00",
      "last_seen":  "2026-05-11 23:52:49",
      "users": [
        {
          "user_id": 50017,
          "user_name": "zuestang2",
          "nick_name": "zuestang21",
          "login_count": 12,
          "first_login": "2026-05-04 12:30:00",
          "last_login":  "2026-05-11 23:52:49"
        },
        {
          "user_id": 50099,
          "user_name": "playerB",
          "nick_name": "playerB",
          "login_count": 5,
          "first_login": "2026-05-06 09:11:42",
          "last_login":  "2026-05-10 21:04:17"
        }
      ]
    }
  ]
}
```

**Field semantics:**

| Field | Loại | Ý nghĩa |
|---|---|---|
| `total` | int | Số IP groups **trong trang này** (≤ 50) |
| `totalRecord` | int | Tổng số IP groups khớp filter **toàn dataset** — dùng cho pagination UI |
| `groups[].ip` | string | IP thực (first hop, không phải edge IP) |
| `groups[].user_count` | int | Số user_id distinct đã login từ IP này |
| `groups[].first_seen` / `last_seen` | `YYYY-MM-DD HH:mm:ss` | Khoảng thời gian IP xuất hiện trong window |
| `groups[].users[].login_count` | int | Số lần user này login từ IP này |
| `groups[].users[].first_login` / `last_login` | `YYYY-MM-DD HH:mm:ss` | Khoảng thời gian user này dùng IP |

Sort mặc định: `user_count DESC, last_seen DESC`. Trong mỗi group, users sort `last_login DESC`.

### Response (error — 90 ngày cap)

```json
{
  "success": false,
  "errorCode": "ERR_DATE_RANGE_TOO_LARGE",
  "message": "Khoảng thời gian không được quá 90 ngày",
  "data": null,
  "total": 0,
  "totalRecord": 0,
  "groups": []
}
```

### Response (error — invalid date format)

```json
{
  "success": false,
  "errorCode": "ERR_INVALID_DATE",
  "message": "Định dạng ngày không hợp lệ (yyyy-MM-dd)",
  "data": null,
  "total": 0,
  "totalRecord": 0,
  "groups": []
}
```

### Empty result

Khi không có IP nào đạt threshold trong khoảng đã chọn:
```json
{
  "success": true,
  "errorCode": "0",
  "message": null,
  "data": null,
  "total": 0,
  "totalRecord": 0,
  "groups": []
}
```

FE nên hiện empty state "Không tìm thấy IP trùng trong khoảng đã chọn".

### Lỗi

| errorCode | Khi nào |
|---|---|
| `0` | thành công |
| `ERR_DATE_RANGE_TOO_LARGE` | `te - ts > 90 ngày` |
| `ERR_INVALID_DATE` | `ts` hoặc `te` không parse được theo `yyyy-MM-dd` |
| `9001` | thiếu `aat` hoặc token hết hạn / không hợp lệ |
| `1001` | lỗi nội bộ (xem backend log) |

### Behavior adaptive theo filter (Pattern C)

| Filter input | Behavior |
|---|---|
| Không `nn`, không `ip` | **Bulk scan** — tất cả IP duplicate trong window |
| Có `nn` only | **Drill-down from user** — chỉ scan các IP user X từng dùng, sau đó hiện tất cả user trên các IP đó |
| Có `ip` only | **Drill-down to IP** — group cho đúng IP này (vẫn yêu cầu ≥ `mu` user) |
| Có cả `nn` và `ip` | AND-combined (hiếm dùng) |

**Quan trọng**: `nn` là pre-group filter, **không phải** "show only this user". Operator cần hiểu rằng kết quả sẽ bao gồm các user KHÁC trên những IP mà user X dùng.

---

## Examples

### Example 1 — Local Swagger Try-it-out

```
http://localhost:18080/swagger/
```

1. Lấy `aat`: `.\scripts\windows\get-aat.ps1`
2. Authorize → paste → Close.
3. Cuộn tới tag **`login`** → mở entry `c/110 — List duplicate-IP login groups`.
4. Default 7-day window → Execute.

### Example 2 — curl thuần

```bash
curl -sS "http://localhost:18080/api_backend?c=110&aat=$AAT&mu=2" | jq '.totalRecord, (.groups[0] | {ip, user_count, users: (.users | length)})'
```

### Example 3 — drill-down từ nickname

```bash
curl -sS "http://localhost:18080/api_backend?c=110&aat=$AAT&nn=zuestang21" | jq '.groups[] | {ip, user_count, usernames: [.users[].nick_name]}'
```

---

## Performance note

Aggregation chính hit `$match` trên `time_log` range trước khi `$group` theo IP. **Cần index `user_login_info.time_log`** để tránh full-scan khi dataset lớn.

Apply index (idempotent — chạy nhiều lần OK):
```bash
mongo <host>/<db> install/config/mongo/changes/2026-05-11-user-login-info-time-log-index.js
```

Hoặc chạy trực tiếp trong mongo shell:
```js
db.user_login_info.createIndex(
  { time_log: 1 },
  { name: "idx_user_login_info_time_log", background: true }
);
```

Với scale hiện tại (~6K records) không bắt buộc — full-scan dưới 100ms. Tăng thành 100K+ thì index trở nên load-bearing.

---

## Frontend integration

Wrapper Next.js API route (sunkr-admin-next):

```
GET /api/usergame/login-history/duplicate?nn=&ip=&ts=&te=&type=&mu=2&p=1
```

File: `sunkr-admin-next/src/app/api/usergame/login-history/duplicate/route.ts`.

Auth bằng NextAuth session — token được FE inject server-side, không cần truyền `aat` từ client.

Component grouped view: `sunkr-admin-next/src/components/login-history/duplicate-ip-table.tsx` (expandable outer table + per-IP user sub-table).

TypeScript types (re-export từ `@/types/login-history`):
- `DuplicateIpGroup`
- `DuplicateIpUser`
- `DuplicateLoginResponse`

---

## Limitations / Phase 2 ideas

- Hiện không track `user_agent` per group. Có thể bổ sung nếu ops cần phân biệt cùng IP nhưng khác device.
- Không có export CSV. Operator phải copy từ UI hoặc dùng curl + jq.
- Không có notification / scheduled report. Ops phải chủ động vào trang xem.
- `nn` substring match dùng `$regex` với `Pattern.quote` — không hỗ trợ wildcards có chủ ý từ user input. Đây là cố ý (an toàn) — nếu cần wildcard, có thể thêm flag `nn_mode=regex` trong version sau.
- Index trên `SUBSTRING_INDEX(ip, ',', 1)` không indexable trực tiếp trong Mongo. Nếu cần optimize, có thể thêm field denormalized `first_ip` ở write path (cần migration).
