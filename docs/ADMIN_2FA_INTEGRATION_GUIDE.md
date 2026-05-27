# Admin 2FA (Two-Factor Authentication) API Integration Guide

Tài liệu này mô tả chi tiết các thay đổi và API mới dành cho Frontend (FE) để tích hợp tính năng bảo mật 2FA (Google Authenticator / Microsoft Authenticator) cho trang quản trị Admin (CMS).

---

## 1. Cập nhật Luồng Đăng Nhập (Login Flow)

API Đăng nhập Admin (`c=701`) đã được cập nhật để hỗ trợ 2FA. 

**Endpoint:** `GET /api_backend`

### Tham số request:
| Tham số | Bắt buộc | Mô tả |
| :--- | :--- | :--- |
| `c` | Có | `701` |
| `un` | Có | Username đăng nhập |
| `pw` | Có | Password (MD5) |
| `otp` | **Không** | Mã OTP 6 số (từ App) **hoặc** Mã dự phòng (Recovery Code 14 ký tự format `XXXX-XXXX-XXXX`). Truyền khi có yêu cầu từ Server. |

### Các kịch bản xử lý (FE cần handle):

**Kịch bản 1: User chưa bật 2FA**
Trường hợp này API hoạt động như cũ, trả về `success: true` và `accessToken` ngay lập tức. FE cho phép vào thẳng Dashboard.

**Kịch bản 2: User đã bật 2FA nhưng CHƯA truyền `otp`**
Server sẽ chặn và trả về lỗi `1008`.
* **Phản hồi:** `{"success": false, "errorCode": "1008"}`
* **Hành động FE:** Hiển thị Popup / Chuyển hướng sang màn hình **"Nhập mã xác thực 2FA"** hoặc "Sử dụng mã dự phòng". Sau khi người dùng nhập mã, gọi lại chính lệnh `c=701` này kèm thêm tham số `&otp=Mã_Người_Dùng_Nhập`.

**Kịch bản 3: Truyền sai mã OTP hoặc Mã dự phòng**
* **Phản hồi:** `{"success": false, "errorCode": "1009"}`
* **Hành động FE:** Báo lỗi "Mã xác thực không chính xác hoặc đã hết hạn".

**Kịch bản 4: Nhập sai OTP quá 5 lần**
Hệ thống chống Brute-force sẽ khoá tài khoản trong 15 phút.
* **Phản hồi:** `{"success": false, "errorCode": "1010"}`
* **Hành động FE:** Báo lỗi "Tài khoản tạm khoá 15 phút do nhập sai quá nhiều lần".

---

## 2. Luồng Bật 2FA (Enrollment Flow)

Để bật 2FA, người dùng cần trải qua 2 bước: Lấy mã QR (Secret) và Xác nhận OTP.

### Bước 2.1: Lấy mã Secret để hiển thị QR Code
**Endpoint:** `GET /api_backend?c=9902&aat={Token_Admin}`

**Phản hồi thành công:**
```json
{
  "success": true,
  "errorCode": "0",
  "secret": "JBSWY3DPEHPK3PXP"
}
```
* **Hành động FE:** 
  1. Sử dụng thư viện tạo QR Code (vd: `qrcode.react`).
  2. Tạo chuỗi nội dung QR theo chuẩn: `otpauth://totp/SunwinAdmin:{Username}?secret={secret}&issuer=SunwinAdmin` (Thay `{Username}` bằng tên tài khoản hiện tại).
  3. Hiển thị mã QR và chuỗi `secret` (Text) để người dùng có thể nhập tay vào App nếu camera hỏng.
* **Mã lỗi đặc biệt:**
  * `1011`: Trả về nếu tài khoản **đã bật 2FA**. FE cần hiển thị thông báo: "Bạn đã bật 2FA. Vui lòng tắt 2FA hiện tại trước khi tạo mã mới."

### Bước 2.2: Xác nhận OTP và Kích hoạt 2FA
Sau khi user dùng App quét QR, họ phải nhập mã 6 số hiện trên App vào UI để xác nhận bật.
**Endpoint:** `GET /api_backend?c=9903&aat={Token_Admin}&otp={Mã_6_Số}`

**Phản hồi thành công:**
```json
{
  "success": true,
  "errorCode": "0",
  "recoveryCodes": [
    "9THU-QHK3-2B2E",
    "G3XQ-D6K4-T693",
    "9TRM-H94W-M9TE",
    "8GYY-U2XY-NPRQ",
    "DDPN-CH6W-EUB3",
    "T2XP-VUN5-E8TR",
    "UXH4-NFRF-B475",
    "4EVP-L5H6-RHRV"
  ]
}
```
* **Hành động FE:** 
  1. Báo bật thành công. 
  2. **VÔ CÙNG QUAN TRỌNG:** Hiển thị mảng `recoveryCodes` lên màn hình và bắt buộc người dùng COPY / LƯU LẠI 8 mã này. Giải thích cho user rằng đây là cách duy nhất để đăng nhập nếu họ mất điện thoại.
* **Mã lỗi:** Trả về `1009` nếu mã OTP nhập vào bị sai.

---

## 3. Luồng Tắt / Reset 2FA (Disable Flow)

Admin có thể tự tắt 2FA của chính mình, HOẶC các tài khoản có **Role là Admin (Quản trị viên cấp cao)** có thể ép tắt 2FA của tài khoản nhân viên cấp dưới (Ví dụ: Khi nhân viên báo mất điện thoại).

**Endpoint:** `GET /api_backend?c=9905`

### Tham số request:
| Tham số | Bắt buộc | Mô tả |
| :--- | :--- | :--- |
| `aat` | Có | Token đăng nhập của người đang thao tác |
| `targetUsername` | Không | Username của tài khoản muốn tắt 2FA. (Để trống = Tự tắt của chính mình) |
| `otp` | Phụ thuộc | **BẮT BUỘC** nếu tự tắt cho chính mình. Bỏ qua nếu người thao tác có Role Admin và đang tắt cho người khác. |

**Kịch bản 1: User tự tắt 2FA của bản thân**
FE cần hiển thị Popup yêu cầu nhập mã OTP (để chống việc người lạ cầm chuột tắt lén).
* URL gọi: `...c=9905&aat={Token}&otp={Mã_OTP_Từ_App}`

**Kịch bản 2: Tài khoản có Role Admin reset 2FA cho nhân viên**
FE (ở màn hình Quản lý tài khoản Admin) gọi API kèm tên của nhân viên đó.
* URL gọi: `...c=9905&aat={Token}&targetUsername={Tên_Nhân_Viên}`

**Phản hồi thành công:**
```json
{
  "success": true,
  "errorCode": "0"
}
```
* **Mã lỗi đặc biệt:** 
  * `1001`: Không có quyền (Nếu user cố gắng tắt của người khác mà không phải Super Admin).
  * `1009`: Sai mã OTP (Nếu tự tắt).

---

## 4. Hiển thị trạng thái 2FA trên giao diện

Các API lấy danh sách Admin (`c=9820`) và lấy chi tiết Admin (`c=9713`) đã được cập nhật thêm một field boolean:
* Trả về thêm field: `"is2FAEnabled": true / false`

**Hành động FE:** 
* Tại màn hình danh sách Admin, FE có thể thêm cột "Trạng Thái Bảo Mật" (Hiển thị tick xanh nếu `is2FAEnabled == true`).
* Tại màn hình chi tiết hoặc Profile, dựa vào `is2FAEnabled` để hiện nút "Bật 2FA" hoặc "Tắt 2FA".
