# Lô Đề — Cách chơi & Tỷ lệ thưởng

Tài liệu giải thích đơn giản cách hoạt động và tỷ lệ thắng của game Lô Đề (XSMB) trong Sunkr.

## Cách chơi

1. **Người chơi đặt cược** vào 1 số (hoặc nhiều số) theo 1 trong 11 hình thức (mode).
2. Hệ thống **trừ tiền cược** trong ví game của người chơi.
3. **Cuối ngày**, sau khi có kết quả XSMB, hệ thống tự động đối chiếu.
4. Nếu **trúng**, hệ thống **cộng tiền thưởng** vào ví game của người chơi.

Người chơi không cần làm gì sau khi đã đặt cược — kết quả tự động.

## Các hình thức cược và tỷ lệ thưởng

Mỗi hình thức (mode) có cách thắng và tỷ lệ thưởng khác nhau. Bảng dưới đây là tỷ lệ thưởng dựa trên **1 đồng tiền cược** (vd: cược 10.000đ thì nhân tỷ lệ ra số tiền thắng).

### Đánh theo lô (xuất hiện trong nhiều giải)

| Mode | Tên | Mô tả | Trúng khi nào | Tỷ lệ thắng |
|---|---|---|---|---|
| 1 | **Lô 2 số** | Chọn 1 số 2 chữ số (vd: 27) | Số đó là 2 chữ số cuối của bất kỳ giải nào trong 27 giải (ĐB, G1, G2, G3, G4, G5, G6, G7) | **× 99 mỗi lần trúng** (nếu trùng nhiều giải, ăn nhiều lần) |
| 2 | **Lô 3 số** | Chọn 1 số 3 chữ số (vd: 234) | Số đó là 3 chữ số cuối của bất kỳ giải nào trong 24 giải (loại trừ G7 vì G7 chỉ có 2 chữ số) | **× 900 mỗi lần trúng** |

### Đánh theo xiên (nhiều số phải cùng trúng)

| Mode | Tên | Mô tả | Trúng khi nào | Tỷ lệ thắng |
|---|---|---|---|---|
| 3 | **Xiên 2** | Chọn 2 số 2 chữ số | Cả 2 số đều xuất hiện trong 27 giải | **× 17** |
| 4 | **Xiên 3** | Chọn 3 số 2 chữ số | Cả 3 số đều xuất hiện trong 27 giải | **× 65** |
| 5 | **Xiên 4** | Chọn 4 số 2 chữ số | Cả 4 số đều xuất hiện | **× 250** |

> Cảnh báo: hiện tại Xiên 4 (mode 5) đang xét trúng khi **3 trong 4 số** xuất hiện (lẽ ra phải là 4/4). Cần ops xác nhận đúng/sai.

### Đánh đầu / đuôi / đề (chỉ xét giải Đặc Biệt)

Giải Đặc Biệt (ĐB) là 1 số 5 chữ số. Lấy 2 chữ số cuối ĐB ra (vd: ĐB = 12345 → "đề" = 45).

| Mode | Tên | Mô tả | Trúng khi nào | Tỷ lệ thắng |
|---|---|---|---|---|
| 6 | **Đầu** | Chọn 1 chữ số (0-9) | Chữ số đầu của "đề" khớp (vd: đề=45 → đầu=4) | **× 8** |
| 7 | **Đuôi** | Chọn 1 chữ số (0-9) | Chữ số cuối của "đề" khớp (vd: đề=45 → đuôi=5) | **× 8** |
| 9 | **Đề** | Chọn 1 số 2 chữ số | 2 chữ số cuối ĐB đúng số đã chọn (vd: chọn 45 + đề = 45) | **× 80** |
| 8 | **Đuôi đặc biệt** | Tương tự đuôi nhưng tỷ lệ khác (theo rate) | Số đặt khớp đuôi của bất kỳ giải ĐB nào | **× 80 chia rate** |

### Đánh 3 càng (3 chữ số cuối ĐB)

| Mode | Tên | Mô tả | Trúng khi nào | Tỷ lệ thắng |
|---|---|---|---|---|
| 11 | **3 Càng** | Chọn 1 số 3 chữ số | 3 chữ số cuối của ĐB khớp số đã chọn | **× 800** |

## Ví dụ tính tiền thắng

### Lô 2 số

- Cược: 10.000đ vào số **27** (mode "Lô 2 số")
- Kết quả XSMB: số 27 xuất hiện 2 lần (trong 1 giải ĐB và 1 giải G6)
- Tiền thắng = `2 × 10.000 × 99` = **1.980.000đ**

### Đề

- Cược: 50.000đ vào số **45** (mode "Đề")
- ĐB của ngày = 12345 (đề = 45)
- Tiền thắng = `50.000 × 80` = **4.000.000đ**

### Xiên 3

- Cược: 20.000đ vào 3 số **27, 38, 91** (mode "Xiên 3")
- Cả 3 số đều xuất hiện trong 27 giải
- Tiền thắng = `20.000 × 65` = **1.300.000đ**
- Nếu chỉ 2/3 số trúng → không được thưởng

### 3 Càng

- Cược: 5.000đ vào số **123** (mode "3 Càng")
- ĐB = 56123 → 3 chữ cuối = 123 → trúng
- Tiền thắng = `5.000 × 800` = **4.000.000đ**

## Quy trình hệ thống (đơn giản)

1. **Người chơi đặt cược** → backend trừ ngay tiền cược.
2. **Trong ngày**: hệ thống lưu cược chờ kết quả.
3. **Cuối ngày sau khi có XSMB**: scheduler chạy → đối chiếu từng cược với kết quả thực tế.
4. **Trả thưởng** tự động vào ví game nếu trúng.
5. **Lưu lịch sử** (cược + tiền thưởng) vào database để tra cứu.

## Rủi ro / cần kiểm tra

1. **Mode 5 (Xiên 4)** — đang chấp nhận thắng khi 3/4 số trúng (lẽ ra phải 4/4). Cần ops xác nhận đây là bug hay intentional.
2. **Phụ thuộc nguồn kết quả XSMB external** — nếu trang nguồn đổi cấu trúc HTML, hệ thống không lấy được kết quả → không trả thưởng được. Cần monitoring.
3. **Trả thưởng có thể bị trùng** trong trường hợp hi hữu nếu scheduler chạy lại — hiện chưa có dedup chặt chẽ.

## Tham chiếu

- Mã nguồn chính: `backend-master/game/Minigame/src/main/java/game/modules/minigame/LotteryModule.java`
- Service kết quả XSMB: `api-xsmb-today-main/` (port 49111)
- Bảng database: `vinplay.transaction_lode`
