# Lỗi cho phép player chơi GSC khi balance = 0 — báo cáo stakeholder

Ngày: 2026-04-26
Mức độ: *NGHIÊM TRỌNG — ảnh hưởng tài chính trực tiếp*

## Tóm tắt 1 câu

Hệ thống đang cho phép player đặt cược tại GSC (Live Casino: Baccarat, Sic Bo, Roulette, …) ngay cả khi ví thực tế đã hết tiền — vì ví của ta báo sai số dư cho GSC, GSC tin theo và xử lý cược như bình thường.

## Luồng diễn ra (đơn giản hoá)

```
1. Player bấm đặt cược trong game GSC.
                            ↓
2. GSC gọi API "rút tiền" (/withdraw) tới hệ thống Sun.
                            ↓
3. Hệ thống Sun đọc số dư từ cache (Hazelcast).
                            ↓
4. Cache đang sai — báo: "có tiền".
                            ↓
5. Hệ thống Sun trả GSC: "OK, player còn tiền, cho phép cược".
                            ↓
6. GSC TIN tuyệt đối phản hồi của ta — ghi nhận cược thành công.
                            ↓
7. Player chơi với "tiền ma" (tiền không có thật trong ví).
```

*GSC không có quyền xác minh số dư của Sun — họ buộc phải tin response của ta. Lỗi nằm hoàn toàn ở phía Sun.*

## Tác động tài chính

Khi player cược "tiền ma":

| Tình huống | Hệ quả thực tế |
|---|---|
| Player *thua* cược ma | GSC giữ số tiền cược → *Sun nợ GSC khoản đó* khi đối soát |
| Player *thắng* cược ma | GSC gửi tiền thắng → ta cộng vào ví → player có thể rút → *Sun mất tiền thật* |

*Đằng nào cũng mất tiền thật*. Không phải lỗi hiển thị suông.

## Sự việc thực tế đã xảy ra (case CauNamday)

* **Player**: CauNamday (id 8545)
* **Ngày**: 2026-04-25
* **Tiền admin nạp cho player**: *2.000.000 KRW* (2 lần × 1tr, status=1, by superadmin)
* **Tổng cược trong báo cáo**: *40.352.100 KRW* (rolling/turnover)
* **Tổng thắng**: *35.852.900 KRW*
* **Báo cáo agency hiển thị mất**: *4.499.200 KRW*
* **Player chỉ có 2tr nhưng "mất" 4.5tr** → chênh *2.500.000 KRW* là cược ma

Bằng chứng từ log nội bộ:
* Cược đầu tiên (Prosperity Tree Baccarat lúc 12:16:57 UTC) — *thực tế ví có 1tr* (sau lần admin cộng đầu).
* Sau khoảng 20 phút chơi liên tục, *ví thực âm xuống ~ -1.000.000 KRW* mà hệ thống vẫn tiếp tục cho cược.
* Sau đó player còn được cho rút *500.000 KRW về ngân hàng* khi ví đang âm.
* Cuối cùng player kết thúc tại 0 — *Sun phải gánh 2.5tr cho GSC* khi đối soát các cược ma đã thua.

## Tại sao xảy ra

* Cache số dư (Hazelcast) bị lệch với DB thật, hoặc không reflect được admin credit kịp thời, hoặc nhiều cược chạy song song "đọc cùng 1 giá trị tốt" rồi cùng trừ → ví xuống âm tập thể.
* Code không có một điểm kiểm tra balance ATOMIC duy nhất — kiểm tra ở Hazelcast, trừ ở Hazelcast, không lock row, không reconcile.
* Phía GSC làm đúng theo spec — ta sai phản hồi → GSC chấp nhận cược.

## Đề xuất

1. *Lập ticket SUN-1122 mức CRITICAL* — fix balance check để dùng nguồn duy nhất (atomic counter trong Hazelcast hoặc `UPDATE … WHERE vin >= bet` trong MySQL với row-count check).
2. *Quét toàn bộ player* trong 30 ngày qua: đếm các trường hợp tương tự (`current_money` từng âm). Ước lượng tổng exposure của Sun với GSC.
3. *Đối chiếu với GSC settlement* tháng 4 — nếu ta đang nợ GSC do cược ma, nên chủ động xác nhận trước khi GSC phát hiện.
4. *Tạm thời*: yêu cầu admin chỉ nạp tiền qua giao diện chính thức (đã verify update đủ ở mọi layer); tránh cập nhật trực tiếp vào DB hoặc cache.

*Audit kỹ thuật đầy đủ với timestamp + log có trong* `install/audits/2026-04-26-CauNamday-negative-balance.md`.
