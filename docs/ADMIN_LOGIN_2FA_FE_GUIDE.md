# Hướng Dẫn Tích Hợp Flow Đăng Nhập 2FA Admin CMS (Frontend)

Tài liệu này hướng dẫn cách Frontend (FE) xử lý luồng đăng nhập Admin khi có bật xác thực 2 bước (2FA) nhằm tối ưu UX, giúp người dùng không phải nhập Captcha 2 lần.

## 1. Thay Đổi Từ Backend

Trước đây, mỗi khi người dùng gửi request Login thành công ở bước 1 (đúng mật khẩu, đúng Captcha), Backend sẽ lập tức xóa Captcha đó. Điều này khiến cho bước 2 (nhập OTP) luôn bị báo lỗi sai Captcha và bắt người dùng lấy lại Captcha mới.

**Hiện tại:** Backend đã được update để **trì hoãn (delay) việc xóa Captcha**. 
- Nếu API trả về mã `1008` (Yêu cầu OTP) hoặc `1009` (Sai mã OTP), Captcha **vẫn được giữ lại** trên hệ thống.
- FE hoàn toàn có thể tái sử dụng lại `cid` và `cp` cũ để gửi lên kèm với `otp`.

## 2. Luồng Xử Lý Mới Dành Cho Frontend

Dưới đây là các bước FE cần thực hiện để luồng 2FA diễn ra mượt mà:

### Bước 1: Request Đăng Nhập Lần Đầu
FE gửi form đăng nhập như bình thường với các params:
```json
{
  "un": "admin_username",
  "pw": "admin_password",
  "cid": "captcha_id",
  "cp": "captcha_value"
}
```

### Bước 2: Xử Lý Response `1008` (Yêu cầu OTP)
Nếu API trả về:
```json
{
  "success": false,
  "errorCode": "1008"
}
```
**Hành động của FE:**
- **KHÔNG** làm mới (refresh) Captcha.
- **GIỮ NGUYÊN** giá trị `cid` và `cp` trong form/state hiện tại.
- Ẩn (hoặc vô hiệu hóa) các ô Username, Password, Captcha.
- Hiển thị thêm ô nhập **Mã OTP** cho người dùng.

### Bước 3: Gửi Request Mã OTP
Khi người dùng nhập OTP và nhấn xác nhận, FE **gửi lại toàn bộ form cũ kèm theo mã OTP**:
```json
{
  "un": "admin_username",
  "pw": "admin_password",
  "cid": "captcha_id",     // Sử dụng lại mã cũ
  "cp": "captcha_value",   // Sử dụng lại text cũ
  "otp": "123456"          // Tham số mới
}
```
Backend sẽ tự động kiểm tra lại và trả về `success: true` + Token đăng nhập.

### Bước 4: Xử Lý Mã Lỗi `1009` (Sai mã OTP)
Nếu người dùng nhập sai OTP, API sẽ trả về `errorCode: "1009"`.
**Hành động của FE:**
- Thông báo lỗi: "Mã OTP không chính xác".
- **KHÔNG** làm mới Captcha.
- Chỉ xóa trắng ô nhập OTP và yêu cầu người dùng nhập lại số khác.
- Cho phép người dùng submit lại request với `cid` và `cp` cũ.

> **⚠️ LƯU Ý QUAN TRỌNG:**
> Hệ thống có cơ chế block: Nếu nhập sai OTP 5 lần sẽ khóa tài khoản trong 15 phút (trả về mã lỗi `1010`).

### Bước 5: Các Lỗi Khác (1005, 1007, 115, v.v...)
Với **TẤT CẢ** các mã lỗi còn lại (ví dụ: `1005` - Sai tài khoản, `1007` - Sai mật khẩu, `115` - Sai Captcha, `1010` - Khóa tài khoản), Backend **sẽ tiêu thụ và xóa Captcha** để bảo mật.
**Hành động của FE:**
- Báo lỗi tương ứng cho người dùng.
- **BẮT BUỘC** gọi API lấy ảnh Captcha mới (`cid` mới).
- Xóa trắng ô text Captcha yêu cầu người dùng gõ lại.

---

## 3. Sơ Đồ Tóm Tắt Logic (Mã Lỗi / Hành Động Captcha)

| Mã Lỗi (`errorCode`) | Ý nghĩa | Hành động Captcha (Frontend) |
| :--- | :--- | :--- |
| `0` | Đăng nhập thành công | Chuyển hướng vào Dashboard |
| `1008` | Yêu cầu nhập OTP | **GIỮ NGUYÊN**, chỉ hiện ô OTP |
| `1009` | Nhập sai OTP | **GIỮ NGUYÊN**, báo lỗi, cho nhập lại OTP |
| `1010` | Bị khóa (Do sai OTP 5 lần) | **REFRESH** Captcha mới |
| `115` | Sai mã Captcha | **REFRESH** Captcha mới |
| `1005` | Sai Username | **REFRESH** Captcha mới |
| `1007` | Sai Password | **REFRESH** Captcha mới |
| Khác | Các lỗi hệ thống khác | **REFRESH** Captcha mới |

Việc code FE tuân thủ đúng các action này sẽ đảm bảo người dùng có trải nghiệm tốt nhất, không bao giờ phải chịu sự khó chịu của việc phải "gõ lại Captcha 2 lần" khi đăng nhập 2FA.
