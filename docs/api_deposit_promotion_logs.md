# 🎁 API Lịch Sử Khuyến Mãi Nạp Tiền — `c=9643`

> **Endpoint:** `/api_backend`  
> **Phương thức:** `GET` / `POST`  
> **Xác thực:** Yêu cầu `aat` (Admin Access Token)

---

## Mục lục
- [Mô tả chức năng](#mô-tả-chức-năng)
- [Tham số đầu vào](#tham-số-đầu-vào)
- [Response thành công](#response-thành-công)
- [Mã lỗi](#mã-lỗi)
- [Ví dụ gọi API](#ví-dụ-gọi-api)
- [⚠️ Breaking Changes — FE cần cập nhật](#️-breaking-changes--fe-cần-cập-nhật)

---

## Mô tả chức năng

Trả về danh sách log các lần user nhận khuyến mãi nạp tiền (deposit promotion).  
Dùng để Admin xem lịch sử phát thưởng theo chương trình khuyến mãi: ai nhận, nhận bao nhiêu, từ giao dịch nạp nào.

---

## Tham số đầu vào

| Param | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `c` | int | Có | Luôn bằng `9643` |
| `aat` | string | Có | Admin Access Token |
| `promo_id` | long | Không | Lọc theo ID khuyến mãi (phải là số nguyên). Lỗi `4002` nếu truyền sai |
| `user_id` | long \| string | Không | Lọc theo ID người dùng (nếu là số nguyên). **Nếu truyền không phải số, tự động treat như `nick_name`** — xem chi tiết bên dưới |
| `nick_name` | string | Không | Lọc theo nickname (tìm kiếm gần đúng, `LIKE %nick_name%`). Ưu tiên hơn fallback từ `user_id` |
| `date_from` | string | Không | Lọc từ ngày (định dạng `yyyy-MM-dd`, so sánh với `claim_date`) |
| `date_to` | string | Không | Lọc đến ngày (định dạng `yyyy-MM-dd`, so sánh với `claim_date`) |
| `page` | int | Không | Trang hiện tại. Mặc định: `1`. Giá trị < 1 được clamp thành 1 |
| `limit` | int | Không | Số bản ghi mỗi trang. Mặc định: `20`. Tối thiểu: `1`. Tối đa: `100` |

### Logic xử lý `user_id` + `nick_name`

Backend áp dụng thứ tự ưu tiên sau:

```
Nếu nick_name được truyền tường minh  →  Tìm LIKE %nick_name%
Nếu user_id là số nguyên              →  Tìm chính xác user_id = <số>
Nếu user_id là chuỗi không phải số   →  Fallback: tìm LIKE %user_id%
```

**Ví dụ:**
- `user_id=12345` → filter `user_id = 12345`
- `user_id=vinhdev` → filter `nick_name LIKE '%vinhdev%'`
- `nick_name=vinh&user_id=123` → filter `nick_name LIKE '%vinh%'` (nick_name ưu tiên)

> ⚠️ Trước phiên bản này, truyền `user_id=vinhdev` (không phải số) sẽ gây lỗi `errorCode: 1001`. Giờ đây backend tự fallback sang tìm theo nickname.

---

## Response thành công

```json
{
    "success": true,
    "errorCode": "0",
    "page": 1,
    "limit": 20,
    "data": [
        {
            "id": 3,
            "promo_id": 3,
            "promo_type": 1,
            "user_id": 14183,
            "nick_name": "zuestang9999123",
            "deposit_tx_id": 17,
            "deposit_amount": 200000,
            "bonus_amount": 40000,
            "is_completed": 0,
            "claim_date": "2026-04-04",
            "created_at": "2026-04-04T09:29:30.000+0000"
        },
        ...
    ]
}
```

### Giải thích các field trong `data[]`

| Field | Kiểu | Mô tả |
|---|---|---|
| `id` | long | ID bản ghi log (auto-increment) |
| `promo_id` | long | ID chương trình khuyến mãi (FK sang bảng `deposit_promotions`) |
| `promo_type` | int | Loại khuyến mãi: `1` = Nạp lần đầu, `2` = Nạp hàng ngày |
| `user_id` | long | ID người dùng |
| `nick_name` | string | Nickname tại thời điểm nhận thưởng |
| `deposit_tx_id` | long | ID giao dịch nạp kích hoạt khuyến mãi (FK sang `deposit_transactions`) |
| `deposit_amount` | long | Số tiền nạp (đơn vị: Vin) |
| `bonus_amount` | long | Số tiền thưởng đã cộng (đơn vị: Vin) |
| `is_completed` | int | `1` = Đã hoàn thành rollover, `0` = Đang trong thời gian rollover |
| `claim_date` | string | Ngày nhận thưởng (format `yyyy-MM-dd`) |
| `created_at` | string / datetime | Timestamp chính xác khi ghi log |

---

## Mã lỗi

| `errorCode` | Mô tả |
|---|---|
| `0` | Thành công |
| `4002` | Param sai kiểu dữ liệu (ví dụ: `promo_id=abc`). Response kèm `message` mô tả chi tiết |
| `9999` | Lỗi hệ thống nội bộ. Response kèm `message` (nếu có) |

> **Lưu ý:** Trước khi update, mọi lỗi đều trả `errorCode: 1001` không kèm message. Từ bản này:
> - Param sai → `4002` + `message`
> - Lỗi server → `9999` + `message`

---

## Ví dụ gọi API

### 1. Lấy tất cả log (không filter)
```
GET /api_backend?c=9643&aat=<token>
```

### 2. Lọc theo chương trình khuyến mãi + ngày
```
GET /api_backend?c=9643&aat=<token>&promo_id=3&date_from=2026-04-01&date_to=2026-04-30
```

### 3. Tìm theo user_id (số)
```
GET /api_backend?c=9643&aat=<token>&user_id=14183
```

### 4. Tìm theo nickname (tường minh)
```
GET /api_backend?c=9643&aat=<token>&nick_name=zuestang
```

### 5. Tìm theo nickname (dùng user_id — backward compat)
```
GET /api_backend?c=9643&aat=<token>&user_id=zuestang
```
> Kết quả giống case 4, backend tự nhận diện không phải số → tìm LIKE

### 6. Phân trang
```
GET /api_backend?c=9643&aat=<token>&page=2&limit=50
```

---

## ⚠️ Breaking Changes — FE cần cập nhật

### Những gì ĐÃ THAY ĐỔI trong phiên bản này:

#### 1. `errorCode` đã chuẩn hóa (⚠️ **FE cần handle thêm**)

| Trước | Sau |
|---|---|
| Mọi lỗi → `errorCode: 1001`, không có `message` | Param sai → `errorCode: 4002` + `message` |
| | Lỗi server → `errorCode: 9999` + `message` |
| | Thành công → `errorCode: "0"` (mới thêm field này) |

**FE nên cập nhật:**
- Xử lý thêm case `errorCode: 4002` (hiện thị message cho user hoặc log lỗi)
- Không hardcode check `errorCode === "1001"` là lỗi chung

#### 2. Hỗ trợ tìm kiếm nickname (✅ **Tính năng mới**)

Thêm param `nick_name` vào form filter:
```
// Cách dùng tường minh (khuyến nghị)
?nick_name=<chuỗi tìm kiếm>

// Backward compat (vẫn hoạt động)
?user_id=<nickname> // nếu không phải số sẽ tự tìm theo nickname
```

**FE nên cập nhật:**
- Thêm ô tìm kiếm nickname riêng biệt, gửi qua param `nick_name`
- Không cần gửi cả hai `user_id` + `nick_name` cùng lúc — `nick_name` sẽ luôn được ưu tiên
- Nếu UI hiện tại có input "user_id" dạng text (cho phép nhập cả số lẫn chữ), giờ đây sẽ hoạt động đúng tự động mà không cần sửa

#### 3. Không có `totalRecords` / `totalPages` trong response

API **chưa trả về** tổng số bản ghi. FE tự xác định hết trang khi `data.length < limit`.

> **TODO:** Backend sẽ bổ sung `totalRecords` và `totalPages` trong phiên bản tới nếu FE cần.

---

## Liên quan

- **Bảng DB:** `vinplay.deposit_promotion_logs`
- **API Danh sách khuyến mãi:** `c=9640` (ListDepositPromotionProcessor)
- **API Tạo khuyến mãi:** `c=9641` (CreateDepositPromotionProcessor)
- **Branch:** `fix/9643-nick-name-search`
- **Ngày update:** 2026-05-03
