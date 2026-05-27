# Tài liệu API Lịch sử Cược (Betting History)

Tài liệu này mô tả chi tiết 2 API lấy Lịch sử cược dành cho **Admin (Toàn sàn)** và **Đại lý (Theo tuyến dưới)**.

Cả 2 API đều đã được tích hợp tính năng **Lọc Bot (Bot Filter)** mới nhất.

---

## 1. Lịch sử Cược Toàn Sàn (Admin)

API này lấy danh sách lịch sử cược của tất cả người chơi trên toàn hệ thống. Dành riêng cho quyền Admin.

- **Endpoint**: `/api_backend?c=9930`
- **Method**: `GET` / `POST`
- **Yêu cầu Auth**: Có (Cần `aat` hoặc `at` Admin token)

### Parameters (Request)

| Field | Type | Bắt buộc | Mặc định | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| `c` | `int` | **Có** | `9930` | ID của command. |
| `p` | `int` | Không | `1` | Trang hiện tại (Page). |
| `l` | `int` | Không | `10` | Số bản ghi trên 1 trang (Limit). |
| `nn` | `String` | Không | Rỗng | Lọc theo Nickname người chơi (tìm kiếm gần đúng/LIKE). |
| `gn` | `String` | Không | Rỗng | Lọc theo Tên Game. |
| `ft` | `String` | Không | Rỗng | Thời gian bắt đầu (Format: `YYYY-MM-DD HH:mm:ss`). |
| `et` | `String` | Không | Rỗng | Thời gian kết thúc (Format: `YYYY-MM-DD HH:mm:ss`). |
| `sort` | `String` | Không | `time` | Trường cần sắp xếp (`time`, `bet`, `prize`, `net`, `money_before`, `money_after`). |
| `dir` | `String` | Không | `desc` | Chiều sắp xếp (`asc` hoặc `desc`). |
| `hide_bot`| `int`/`bool`| Không | `1` / `true` | **Mới:** Ẩn dữ liệu của Bot. Truyền `0` hoặc `false` nếu muốn xem cả Bot. |

---

## 2. Lịch sử Cược Đại Lý (Agency)

API này lấy danh sách lịch sử cược của những người chơi **thuộc tuyến dưới của Đại lý**.

- **Endpoint**: `/api_agent?c=9843`
- **Method**: `GET` / `POST`
- **Yêu cầu Auth**: Có (Qua session đại lý `sid` & `aid` hoặc Admin bypass)

### Parameters (Request)

| Field | Type | Bắt buộc | Mặc định | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| `c` | `int` | **Có** | `9843` | ID của command. |
| `rc` | `String` | **Có** | | Nickname hoặc Referral Code của Đại lý. |
| `p` | `int` | Không | `1` | Trang hiện tại (Page). |
| `l` | `int` | Không | `10` | Số bản ghi trên 1 trang (Limit). |
| `nn` | `String` | Không | Rỗng | Lọc theo Nickname người chơi thuộc tuyến dưới. |
| `gn` | `String` | Không | Rỗng | Lọc theo Tên Game. |
| `ft` | `String` | Không | Rỗng | Thời gian bắt đầu (`YYYY-MM-DD HH:mm:ss`). |
| `et` | `String` | Không | Rỗng | Thời gian kết thúc (`YYYY-MM-DD HH:mm:ss`). |
| `sort` | `String` | Không | `time` | Trường cần sắp xếp. |
| `dir` | `String` | Không | `desc` | Chiều sắp xếp (`asc` hoặc `desc`). |
| `hide_bot`| `int`/`bool`| Không | `1` / `true` | **Mới:** Ẩn dữ liệu của Bot thuộc tuyến dưới đại lý. Truyền `0` để xem cả Bot. |

---

## Cấu trúc Response (Chung cho cả 2 API)

Cấu trúc JSON trả về của cả 2 API là tương đồng.

```json
{
  "success": true,
  "errorCode": "0",
  "total": 1250,
  "page": 1,
  "totalPages": 125,
  "data_approximate": false,
  "scope_players": 450,
  "scope_limited": false,
  "summary": {
    "total_bet": 50000000,
    "total_prize": 48000000,
    "total_net": -2000000,
    "approximate": false
  },
  "data": [
    {
      "time": "2026-05-02 10:15:30",
      "player": "nickname_user",
      "game": "Tài Xỉu",
      "bet": 10000,
      "prize": 19800,
      "net": 9800,
      "fee": 200,
      "money_before": 100000,
      "money_after": 109800
    }
  ],
  "note": ""
}
```

### Giải thích các field quan trọng trong Response:

- `data`: Mảng danh sách các bản ghi cược. Mỗi object chứa thông tin một vé cược:
  - `time`: Thời gian cược.
  - `player`: Nickname người chơi.
  - `game`: Tên trò chơi.
  - `bet`: Tổng tiền đặt cược.
  - `prize`: Tổng tiền nhận lại.
  - `net`: Lợi nhuận (`prize - bet`).
  - `fee`: Phí game (nếu có).
  - `money_before`: Số dư ví trước khi cược (Nếu có tính toán trong luồng).
  - `money_after`: Số dư ví sau khi cược.
- `total`: Tổng số bản ghi (Tổng số lượng vé cược lọc được).
- `summary`: Object chứa thống kê tổng quát của toàn bộ kết quả tìm kiếm (không chỉ tính riêng trang hiện tại). 
  - Giao diện có thể dùng `summary.total_bet`, `summary.total_prize` và `summary.total_net` để hiển thị hàng tổng kết.
- `data_approximate` / `summary.approximate`: Nếu bằng `true`, dữ liệu bị giới hạn bởi một khung tìm kiếm nhất định để tránh overload server. FE nên hiển thị dấu `~` trước số tổng (VD: `~ 50,000,000`).
- `scope_players` (Chỉ API Đại lý): Tổng số lượng người chơi thuộc tuyến dưới đại lý tìm được.
- `scope_limited` (Chỉ API Đại lý): Bằng `true` nếu danh sách người chơi tuyến dưới lớn hơn mức giới hạn (VD: > 5000 user).

### Ghi chú đặc biệt về `hide_bot`:
- Mặc định API sẽ tự động làm sạch toàn bộ dữ liệu cược của Bot khỏi danh sách.
- Khi người dùng muốn xem dữ liệu gốc (Bao gồm Bot), FE chỉ cần thêm `hide_bot=0` vào API call, các response fields như `total`, `summary` sẽ tự động nhảy vọt do lấy được cả các vé cược của bot sinh ra.
