# Game Catalog Admin API — handover for FE team

Quản lý bật/tắt game cho **AWC** và **GSC** từ admin portal.

- Base URL: `https://staging-admin.sunkr.bet/api_backend`
- Auth: tham số `aat` lấy từ `c=701` (login admin), TTL 8h.
- Method: GET hoặc POST đều OK (đọc cả query string và form body).
- Response: tất cả endpoint trả `{success, errorCode, message?, data}`.

## Source of truth: `vinplay.games`

Tất cả endpoint dưới đây đọc/ghi vào bảng unified catalog `vinplay.games` (SUN-GAME-FK Phase 4). Cùng 1 row được dùng bởi:

- LS Cược (admin + agency, MongoDB-backed) — render game_name từ catalog
- LS Rolling (rebate_logs) — render game_name từ catalog
- Game launch flow (AwcGameNameResolver, GscGameNameResolver)

Đổi tên hoặc bật/tắt qua endpoint dưới = đổi tên/khả dụng ở MỌI view ngay (cache TTL ~5min cho lobby in-process).

Khoá unique: `(provider, vendor_platform, game_code, table_tag)`. Cột `table_tag` cho phép tách per-table cho các provider share 1 game_code (vd Sexy Live SEXYBCRT MX-LIVE-001 dùng chung cho tất cả bàn baccarat — phân biệt qua `table_tag` = M01, M31, C05, …).

---

## c=9980 — List providers under platform

Liệt kê provider (CQ9, Evo, JILI, Pragmatic, …) thuộc 1 platform, kèm số lượng game tổng và đang active.

**Request**
```
GET /api_backend?c=9980&aat=<token>&platform=awc
GET /api_backend?c=9980&aat=<token>&platform=gsc
```

| Param | Type | Required | Note |
|---|---|---|---|
| `aat` | string | yes | admin token |
| `platform` | string | yes | `awc` hoặc `gsc` |

**Response**
```json
{
  "success": true,
  "errorCode": "0",
  "data": {
    "platform": "gsc",
    "providers": [
      { "provider": "1002", "provider_name": "Evolution",      "total": 18,  "active": 18 },
      { "provider": "1006", "provider_name": "Pragmatic Play", "total": 711, "active": 711 },
      { "provider": "1091", "provider_name": "JILI",           "total": 176, "active": 170 }
    ]
  }
}
```

- `provider` cho **AWC** = platform code (ví dụ `JILI`, `PG`, `PP`, `SEXYBCRT`).
- `provider` cho **GSC** = `product_code` dạng số (vd `1006`).
- `provider_name` cho **GSC** lấy từ bảng `gsc_product_map`.

**Lỗi**
| errorCode | Khi nào |
|---|---|
| `1001` | thiếu `aat` hoặc token sai |
| `4001` | thiếu `platform` |
| `4002` | `platform` không phải `awc`/`gsc` |
| `9999` | lỗi nội bộ |

---

## c=9981 — List games per provider (paging + search + filter)

**Request**
```
GET /api_backend?c=9981&aat=<token>&platform=awc&provider=JILI&page=1&size=50
GET /api_backend?c=9981&aat=<token>&platform=gsc&provider=1091&q=ace&active=0&page=1&size=20
```

| Param | Type | Required | Default | Note |
|---|---|---|---|---|
| `aat` | string | yes | — | admin token |
| `platform` | string | yes | — | `awc` / `gsc` |
| `provider` | string | yes | — | provider code (xem 9980) |
| `q` | string | no | — | substring match `game_code` hoặc `game_name` |
| `active` | int | no | — | `0` = chỉ inactive, `1` = chỉ active, bỏ trống = tất cả |
| `page` | int | no | 1 | trang ≥ 1 |
| `size` | int | no | 50 | tối đa 200 |

**Response**
```json
{
  "success": true,
  "errorCode": "0",
  "data": {
    "platform": "awc",
    "provider": "SEXYBCRT",
    "page": 1,
    "size": 20,
    "total": 17,
    "items": [
      { "id": 12, "game_code": "*",           "table_tag": "",     "game_name": "SEXYBCRT (default)",  "category_id": 1, "active": 1 },
      { "id": 13, "game_code": "MX-LIVE-001", "table_tag": "",     "game_name": "Sexy Baccarat",       "category_id": 1, "active": 1 },
      { "id": 30, "game_code": "MX-LIVE-001", "table_tag": "M01",  "game_name": "Sexy Baccarat M01",   "category_id": 1, "active": 1 },
      { "id": 31, "game_code": "MX-LIVE-001", "table_tag": "M31",  "game_name": "Sexy Baccarat M31",   "category_id": 1, "active": 1 }
    ]
  }
}
```

Mỗi row có:
- `id` — primary key trong `vinplay.games` (dùng cho c=9982 toggle nếu muốn target chính xác 1 row)
- `game_code` — từ vendor (vd `MX-LIVE-001`, `*` là placeholder ở platform level)
- `table_tag` — phân biệt sub-table khi nhiều bàn share 1 `game_code` (rỗng nếu không tách)
- `game_name` — tên chính tắc, là tên dùng cho LS Cược + LS Rolling
- `category_id` — phân loại nội bộ (live casino, slot, sport, …)
- `active` — 0/1 hoạt động hay không

Bảng GSC có cùng shape; `vendor_platform` (param `provider`) là `product_code` dạng số (vd `1091` = JILI).

**Lỗi**
| errorCode | Khi nào |
|---|---|
| `1001` | thiếu/sai `aat` |
| `4001` | thiếu `platform` hoặc `provider` |
| `4002` | `platform` không hợp lệ, hoặc `provider` không phải số khi `platform=gsc`, hoặc `active` không phải 0/1 |
| `9999` | lỗi nội bộ |

---

## c=9982 — Toggle active (single hoặc bulk)

Bật/tắt 1 hoặc nhiều game cùng lúc cho cùng 1 provider. **2 cách target row:**

- `ids` (preferred) — CSV id từ c=9981 → toggle chính xác từng row (kể cả per-table).
- `game_codes` (legacy bulk) — CSV game_code → toggle TẤT CẢ row có code đó (kể cả mọi `table_tag`). Dùng khi muốn bật/tắt cả game.

**Request**
```
# Per-row (preferred)
GET /api_backend?c=9982&aat=<token>&platform=awc&provider=SEXYBCRT&ids=30,31&active=0

# Bulk by game_code (toggle all table_tag rows under a code)
GET /api_backend?c=9982&aat=<token>&platform=gsc&provider=1091&game_codes=103,110,35&active=1
GET /api_backend?c=9982&aat=<token>&platform=awc&provider=JILI&game_code=20&active=0
```

| Param | Type | Required | Note |
|---|---|---|---|
| `aat` | string | yes | admin token |
| `platform` | string | yes | `awc` / `gsc` |
| `provider` | string | yes | provider code (vd `SEXYBCRT`, `1091`) |
| `ids` | string | yes (hoặc `game_codes`) | CSV `games.id` (lấy từ c=9981) |
| `game_codes` | string | yes (hoặc `ids`) | CSV game_code, hoặc 1 game ngắn gọn dùng `game_code` |
| `active` | int / bool | yes | `0` / `1` / `true` / `false` |

**Response**
```json
{
  "success": true,
  "errorCode": "0",
  "data": {
    "platform": "gsc",
    "provider": "1091",
    "requested": 3,
    "rows_affected": 3,
    "not_found": [],
    "new_active": 1,
    "cache_note": "Lobby cache TTL ~5min; restart portal-api for instant flip."
  }
}
```

- `requested`: số game truyền vào.
- `rows_affected`: số dòng thật sự bị UPDATE (game đã ở trạng thái mong muốn → 0).
- `not_found`: array game_code không tìm thấy trong catalog.
- `cache_note`: portal lobby cache 5 phút. Cần thấy ngay → `docker restart sunwinkr-portal-api`.

**Audit**: mỗi lần gọi ghi vào `vinplay_admin.log_admin` với `action='gamecatalog.toggle'`, kèm admin nick và list game_codes.

**Lỗi**
| errorCode | Khi nào |
|---|---|
| `1001` | thiếu/sai `aat` |
| `4001` | thiếu `platform`/`provider`/`game_codes`/`active` |
| `4002` | `platform` không hợp lệ, `provider` không phải số khi `platform=gsc`, `active` sai format |
| `9999` | lỗi nội bộ |

---

## c=9983 — Sync AWC platforms từ provider

Sync danh sách platform AWC (JILI, PG, PP, …) vào `vinplay.games` để fresh DB không cần seed thủ công.

**Hạn chế**: AWC không expose endpoint per-game list, chỉ có platform list. Mỗi platform được seed 1 dòng placeholder `game_code='*'`. Game cụ thể vẫn cần admin thêm thủ công (qua SQL migration hoặc UI add-game riêng).

**Chỉ hỗ trợ AWC.** GSC catalog đã sẵn 1398 dòng do ops seed.

**Request**
```
GET /api_backend?c=9983&aat=<token>&platform=awc
```

| Param | Type | Required | Note |
|---|---|---|---|
| `aat` | string | yes | admin token |
| `platform` | string | yes | bắt buộc `awc` |

**Response (success)**
```json
{
  "success": true,
  "errorCode": "0",
  "data": {
    "platform": "awc",
    "synced": 12,
    "platforms": ["JILI","PG","PP","SEXYBCRT","HOTROAD","BG","..."],
    "note": "AWC does not expose a per-game list endpoint. Each platform is seeded with a single game_code='*' placeholder. Add specific game_codes via SQL migration or the admin UI's add-game flow."
  }
}
```

**Lỗi**
| errorCode | Khi nào |
|---|---|
| `1001` | thiếu/sai `aat` |
| `4001` | thiếu `platform` |
| `4002` | `platform` ≠ `awc` |
| `5001` | AWC API trả lỗi (HTTP 4xx/5xx hoặc status ≠ "0000"). Message kèm `desc` từ AWC. |
| `5002` | `AWC_ENABLED=0` → AWC bị tắt, không gọi |
| `9999` | lỗi nội bộ |

---

## Login (c=701) — lấy `aat`

```
GET /api_backend?c=701&un=<username>&pw=<md5(password)>&cp=<captcha>&cid=<captcha_id>&otp=<2fa_if_enabled>
```

Response (rút gọn):
```json
{
  "success": true,
  "errorCode": "0",
  "accessToken": "f54c05a3bcb34076abf30f8d69bda5c9",
  "data": "{\"adminId\":1,\"fullName\":\"...\",\"adminToken\":\"f54c...\",\"tokenExpiry\":...}"
}
```

→ Lưu `accessToken` (= `adminToken`) làm `aat` cho mọi call tiếp theo.

---

## c=9984 — List blocks of a user

Liệt kê các rule block đang active cho 1 người chơi cụ thể.

**Request**
```
GET /api_backend?c=9984&aat=<token>&user_id=42
GET /api_backend?c=9984&aat=<token>&nick_name=zuestang
```

| Param | Type | Required | Note |
|---|---|---|---|
| `aat` | string | yes | admin token |
| `user_id` | long | yes (hoặc `nick_name`) | id trong `users` |
| `nick_name` | string | yes (hoặc `user_id`) | nickname |

**Response** (mỗi item là 1 row trong `user_game_block`):
```json
{
  "success": true, "errorCode": "0",
  "data": {
    "user_id": 42, "nick_name": "zuestang", "count": 2,
    "items": [
      { "id": 1, "user_id": 42, "nick_name": "zuestang",
        "provider": "GSC", "vendor_platform": "1002",
        "game_code": null, "table_tag": null, "category_id": null,
        "reason": "self-exclusion", "blocked_by": "superadmin",
        "blocked_at": "2026-05-09 03:00:00", "expires_at": null, "active": true },
      { "id": 2, "user_id": 42, "nick_name": "zuestang",
        "provider": null, "vendor_platform": null,
        "game_code": null, "table_tag": null, "category_id": 1,
        "reason": "abuse pattern on baccarat", "blocked_by": "superadmin",
        "active": true }
    ]
  }
}
```

---

## c=9985 — Add a block

Thêm 1 rule block. Mỗi tham số NULL/missing/empty = "any".

**Examples**
```
# Block toàn bộ Evo (GSC product 1002) cho user 42
GET /api_backend?c=9985&aat=<token>&user_id=42&provider=GSC&vendor_platform=1002&reason=self-exclusion

# Block tất cả baccarat (cả AWC & GSC) cho zuestang
GET /api_backend?c=9985&aat=<token>&nick_name=zuestang&category_id=1&reason=baccarat-abuse

# Block chỉ Sexy Baccarat M01 cho user 42
GET /api_backend?c=9985&aat=<token>&user_id=42&provider=AWC&vendor_platform=SEXYBCRT&game_code=MX-LIVE-001&table_tag=M01

# Tạm khoá GSC trong 7 ngày
GET /api_backend?c=9985&aat=<token>&user_id=42&provider=GSC&expires_at=2026-05-16+00:00:00&reason=cool-off
```

| Param | Type | Required | Note |
|---|---|---|---|
| `aat` | string | yes | admin token |
| `user_id` | long | yes (hoặc `nick_name`) | nếu chỉ truyền 1 trong 2 thì BE tra cứu cái còn lại |
| `nick_name` | string | yes (hoặc `user_id`) | |
| `provider` | string | no | `AWC` / `GSC`; bỏ trống = chặn cả 2 |
| `vendor_platform` | string | no | AWC code (vd `SEXYBCRT`) hoặc GSC product_code (`1002`); bỏ trống = mọi platform |
| `game_code` | string | no | exact game_code; bỏ trống = mọi game |
| `table_tag` | string | no | exact table tag; bỏ trống = mọi bàn |
| `category_id` | int | no | id category (1=Baccarat, 2=DragonTiger, 3=Roulette, 4=Sicbo, 5=Blackjack, 6=GameShows, 7=Poker, 8=Slot, 9=Fish, 10=Sport, 11=Other) |
| `reason` | string | no | text audit |
| `expires_at` | datetime | no | `YYYY-MM-DD HH:MM:SS` hoặc ISO-8601; rỗng = vĩnh viễn |

**Response**
```json
{
  "success": true, "errorCode": "0",
  "data": {
    "id": 7, "user_id": 42, "nick_name": "zuestang",
    "provider": "GSC", "vendor_platform": "1002",
    "game_code": null, "table_tag": null, "category_id": null,
    "expires_at": null,
    "note": "Block takes effect within 60s (UserGameBlock cache TTL)."
  }
}
```

**Lỗi**
| errorCode | Khi nào |
|---|---|
| `1001` | thiếu/sai `aat` |
| `4001` | thiếu cả `user_id` lẫn `nick_name` |
| `4002` | `user_id` không phải số, `expires_at` sai format |
| `9999` | DB error |

---

## c=9986 — Remove a block

Xoá (soft-delete) 1 rule block bằng id.

**Request**
```
GET /api_backend?c=9986&aat=<token>&id=7
```

| Param | Type | Required |
|---|---|---|
| `aat` | string | yes |
| `id` | long | yes — lấy từ c=9984 list hoặc c=9985 add |

**Response**
```json
{ "success": true, "errorCode": "0", "data": { "id": 7, "active": 0,
  "note": "Block deactivation propagates within 60s (UserGameBlock cache TTL)." } }
```

**Lỗi**
| errorCode | Khi nào |
|---|---|
| `1001` | unauth |
| `4001` | thiếu id |
| `4002` | id không phải số |
| `1002` | id không tồn tại |

---

## Hiệu lực block

- Block effect sau ≤ 60 giây (cache TTL).
- Tích hợp tại bet-time của AWC + GSC: nếu match, BE refuse với code `2000` (PRODUCT_UNDER_MAINTENANCE) cho GSC, hoặc lỗi 1xxx cho AWC seamless.
- SETTLE / CANCEL / BONUS không bị chặn — chỉ BET. (Để vẫn close được vòng đã mở.)

## Quy ước chung

- TTL token 8h.
- Lobby cache 5 phút sau khi đổi active. Cần thấy ngay → restart `sunwinkr-portal-api`.
- Audit ghi vào `vinplay_admin.log_admin`. Action: `gamecatalog.toggle` (9982), `gamecatalog.sync` (9983), `usergameblock.add` (9985), `usergameblock.remove` (9986).
- AWC platforms hiện có: `JILI`, `PG`, `PP`, `SEXYBCRT` (mới 4 — ops mở thêm khi sign HĐ provider).
- GSC providers: `gsc_product_map` (Evo=1002, Pragmatic=1006, PG Soft=1007, JILI=1091, Saba=1046, …).
