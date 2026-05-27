# HƯỚNG DẪN CHẠY BACKEND TRÊN WINDOWS (PowerShell)

*Phiên bản Windows của [`BACKEND_SETUP_GUIDE.md`](BACKEND_SETUP_GUIDE.md). Toàn bộ workflow viết bằng PowerShell native — không cần WSL, không cần học Git Bash.*

Tài liệu này hướng dẫn dev Backend chạy hệ thống Sunwinkr trên Windows 10/11 theo đúng thứ tự, từ lần đầu cài máy tới các flow nâng cao (sync data staging, tunnel xuyên thấu vào hạ tầng staging).

Toàn bộ script Windows nằm trong [`scripts/windows/`](../scripts/windows/).

---

## 📋 BẢNG TÓM TẮT THỨ TỰ CHẠY

| Khi nào | Lệnh | Thư mục |
|---|---|---|
| **Lần đầu setup máy** | Cài Docker Desktop, Git for Windows, PuTTY, set ExecutionPolicy | — |
| **Lần đầu deploy project** | `.\deploy.ps1` | `scripts\windows\` |
| **Mở máy hằng ngày** | `.\deploy.ps1 status` rồi `.\deploy.ps1` (nếu cần) | `scripts\windows\` |
| **Sửa code Java/etc.** | `.\deploy.ps1 -Rebuild` hoặc `.\deploy.ps1 -NoStart` rồi `..\..\start.sh backend` | `scripts\windows\` |
| **Test với data staging** (Phần 2) | `.\sync-staging.ps1` | `scripts\windows\` |
| **Debug live staging** (Phần 3) | `staging-tunnel.ps1 Open` → `set-config-target.ps1 Tunnel` → rebuild → ... → revert | `scripts\windows\` |
| **Tắt cuối ngày** | `.\deploy.ps1 stop` | `scripts\windows\` |

---

## 🛠️ PHẦN 0: CÀI ĐẶT MỘT LẦN

Làm 1 lần duy nhất khi nhận máy mới.

### 0.1 Phần mềm bắt buộc

| Phần mềm | Cách cài | Bắt buộc cho |
|---|---|---|
| **Docker Desktop** | https://www.docker.com/products/docker-desktop/ | Tất cả |
| **Git for Windows** | https://git-scm.com/download/win | `deploy.ps1` (cần `bash.exe` để delegate) |
| **PuTTY** (`plink.exe` + `pscp.exe`) | `scoop install putty` hoặc `choco install putty` | Phần 2, Phần 3 (SSH lên staging) |

> 💡 Nếu chưa có scoop: chạy 1 dòng PowerShell tại https://scoop.sh để cài. Hoặc dùng `choco` (Chocolatey).

### 0.2 Cấu hình PowerShell

PowerShell mặc định chặn script local. Bật quyền chạy 1 lần:

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

### 0.3 Clone source code

```powershell
git clone <repo-url> sunwinkr
cd sunwinkr
git checkout staging
git pull origin staging
```

### 0.4 Tạo file `.env`

Copy mẫu trong phụ lục `BACKEND_SETUP_GUIDE.md` (cuối tài liệu đó) ra file `.env` ở thư mục gốc dự án (cùng cấp với thư mục `backend-master`). KHÔNG commit `.env` lên git.

### 0.5 (Tuỳ chọn) Set biến môi trường password staging

Để các script Phần 2 / Phần 3 không phải dùng password mặc định trong code:

```powershell
# Tạm thời (chỉ phiên PowerShell hiện tại):
$env:STAGING_SSH_PASSWORD = 'paste-your-staging-password'

# Vĩnh viễn (cho user hiện tại):
[Environment]::SetEnvironmentVariable('STAGING_SSH_PASSWORD', 'paste-your-staging-password', 'User')
```

---

## 🚀 PHẦN 1: DEPLOY VÀ KHỞI CHẠY HỆ THỐNG

> **Mục đích:** Build Java backend + .NET BanCa, dựng đầy đủ 32 service (DB + API + 16 game server + web tier). Đây là flow chính cho 95% trường hợp.

### 1.1 Lần đầu deploy

Mở **PowerShell** tại thư mục `scripts\windows\` của dự án:

```powershell
cd scripts\windows
.\deploy.ps1
```

Script sẽ tự động:
1. Strip CRLF cho `gradlew` + tất cả `*.sh` (Git for Windows hay checkout file shell với line ending sai)
2. Build Java backend trong container `eclipse-temurin:8-jdk` (lần đầu mất 2-5 phút)
3. Build BanCa fish game trong container `mcr.microsoft.com/dotnet/sdk:5.0`
4. Bàn giao cho `..\..\deploy.sh` qua Git Bash để generate `.env`, dựng network/volume, init MySQL, load `full_backup.sql`, patch `game_config`, rồi `docker compose up` 32 service

> ⏱️ Lần đầu cần ~10-15 phút (chủ yếu là pull image + build + init MySQL). Lần sau chỉ vài chục giây.

### 1.2 Verify (kiểm tra hệ thống đã sống chưa)

```powershell
.\deploy.ps1 status
```

Hoặc kiểm tra trực tiếp container:

```powershell
docker ps --format 'table {{.Names}}\t{{.Status}}' | Select-String 'sunwinkr-'
```

Tất cả `sunwinkr-*` phải `Up X minutes (healthy)`. Test API:

```powershell
$pwd_md5 = -join ([System.Security.Cryptography.MD5]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes('admin123')) | ForEach-Object { $_.ToString('x2') })
curl.exe -s "http://localhost:8088/api?c=3&un=superadmin&pw=$pwd_md5"
```

### 1.3 Các lệnh thường dùng

```powershell
.\deploy.ps1                       # Full deploy (idempotent — chạy lại an toàn)
.\deploy.ps1 -Rebuild              # Bắt buộc rebuild lại Java + .NET (sau khi sửa code)
.\deploy.ps1 -NoStart              # Build và setup, KHÔNG khởi động service
.\deploy.ps1 stop                  # Dừng tất cả container
.\deploy.ps1 status                # Xem trạng thái 32 service
.\deploy.ps1 logs portal-api       # Theo dõi log realtime (Ctrl+C để thoát)
.\deploy.ps1 logs                  # Log của tất cả service
```

### 1.4 Vòng lặp dev hằng ngày

```powershell
# Buổi sáng
cd scripts\windows
.\deploy.ps1 status            # Xem có service nào bị down không
.\deploy.ps1                   # Khởi động lại nếu cần (idempotent)

# Sửa code Java
# ... edit files trong backend-master/ ...
.\deploy.ps1 -Rebuild          # Rebuild + restart

# Theo dõi log khi test
.\deploy.ps1 logs backend-api

# Cuối ngày
.\deploy.ps1 stop
```

---

## ♻️ PHẦN 2: SYNC DATA TỪ STAGING VỀ MÁY (KHUYẾN NGHỊ)

> **Mục đích:** Khi làm tính năng mới, test giao dịch nạp/rút, test luồng nhả Bot game bài. Việc kéo data live về máy giúp data **cô lập tuyệt đối** — không phá CSDL staging và không cướp message MQ thật.

### 2.1 Kéo data Mongo + MySQL trong 1 lệnh

```powershell
cd scripts\windows
.\sync-staging.ps1
```

Script sẽ:
1. SSH lên staging (qua `plink -pw`)
2. `mongodump` collection `win123club.log_mini_poker` + `win123club.bau_cua_transaction`
3. `mysqldump` toàn bộ schema `vinplay_gamebai`
4. `pscp` các dump file về `%TEMP%\`
5. `docker cp` vào container local rồi `mongorestore --drop` + `mysql import`

### 2.2 Chỉ kéo Mongo hoặc chỉ MySQL

```powershell
.\sync-staging.ps1 -Mode Mongo     # Chỉ Mongo (~10MB, nhanh)
.\sync-staging.ps1 -Mode MySQL     # Chỉ MySQL (toàn bộ vinplay_gamebai)
```

### 2.3 Yêu cầu

- DB containers local đã được tạo (qua `.\deploy.ps1` hoặc `..\..\deploy.sh database` từ Git Bash)
- `plink.exe` + `pscp.exe` có trong PATH (cài qua scoop/choco — xem Phần 0.1)
- Password staging: lấy từ `$env:STAGING_SSH_PASSWORD` hoặc hardcoded mặc định trong `_common.ps1`

> **Lưu ý:** Sync xong, code local của bạn đọc ghi vào DB local (đã có data y hệt staging). Hoàn toàn an toàn để test phá phách. Khi muốn quay về data trắng, chạy lại `.\deploy.ps1` với DB volume xoá đi (hoặc tạo lại từ `full_backup.sql`).

---

## ⚡ PHẦN 3: KẾT NỐI TRỰC TIẾP LÊN HẠ TẦNG STAGING (FULL-INFRA TUNNEL)

> **Mục đích:** Debug nóng — bắt bug live bằng tài khoản có sẵn, thao tác trực tiếp với Redis/Hazelcast staging nơi đang phát sinh lỗi mà không cần data giả.

> ⚠️ **CẢNH BÁO:** Ở chế độ này, mỗi node Java trên máy bạn đóng vai trò là **node "thật"** trong cluster live. Khi hệ thống nạp thẻ nhả message, code máy bạn có thể consume message và xử lý nó (cướp quyền của server thật). Hết sức cẩn trọng — chỉ dùng khi thật sự cần thiết.

### 3.1 Mở tunnel SSH

```powershell
cd scripts\windows
.\staging-tunnel.ps1 -Action Open -StopLocalContainers
```

Cờ `-StopLocalContainers` sẽ tự động tắt các container DB local (`sunwinkr-mysql/-mongodb/-redis/-rabbitmq/-hazelcast`) để giải phóng port cho tunnel.

Khi mở thành công, máy bạn map 1-1 với hạ tầng staging:

| Local | Staging |
|---|---|
| `0.0.0.0:3307` | `172.21.0.6:3306` (MySQL) |
| `0.0.0.0:27018` | `172.21.0.4:27017` (MongoDB) |
| `0.0.0.0:6379` | `172.21.0.5:6379` (Redis) |
| `0.0.0.0:5672` | `172.21.0.3:5672` (RabbitMQ) |
| `0.0.0.0:5701` | `172.21.0.2:5701` (Hazelcast) |

### 3.2 Trỏ config Java vào tunnel

Backend Java đang đọc config từ `*.properties` với hostname kiểu `mysql`, `mongodb`, `rabbitmq` (Docker service names). Cần sửa hàng loạt sang `172.17.0.1` (Docker gateway) để chui vào tunnel:

```powershell
.\set-config-target.ps1 -Target Tunnel
```

Script sẽ rewrite **96 file** `*.properties` (24 hazelcast + 24 rmq + 24 mongo + 24 db_pool) trong `backend-master/`. Mỗi file sửa được tạo thêm `.bak` backup.

> 💡 Xem trước thay đổi mà chưa ghi: `.\set-config-target.ps1 -Target Tunnel -DryRun`

### 3.3 Rebuild backend với config mới

Vì config được nhúng vào JAR khi build, phải rebuild để config mới có hiệu lực:

```powershell
.\deploy.ps1 -Rebuild
```

Hoặc chỉ build lại backend tier mà không restart DB:

```powershell
# Trong Git Bash (start.sh chưa có bản PS):
cd <repo-root>
./start.sh backend
```

### 3.4 Kiểm tra tunnel

```powershell
.\staging-tunnel.ps1 -Action Status
```

Output mẫu:
```
==> Tunnel status
    Running. PID: 12345
    [OK] 0.0.0.0:3307  -> 172.21.0.6:3306    (MySQL Staging)
    [OK] 0.0.0.0:27018 -> 172.21.0.4:27017   (MongoDB Staging)
    ...
```

### 3.5 Đóng tunnel + revert config (LÀM SAU KHI XONG VIỆC)

> ⚠️ Đừng quên bước này! Để tunnel mở qua đêm = LAN của bạn có thể vào DB staging.

```powershell
.\staging-tunnel.ps1 -Action Close
.\set-config-target.ps1 -Target Local
.\deploy.ps1 -Rebuild              # Rebuild lại backend với config local
```

### 3.6 Quy trình debug staging đầy đủ

```powershell
cd scripts\windows

# 1. Mở tunnel + tắt DB local
.\staging-tunnel.ps1 -Action Open -StopLocalContainers

# 2. Trỏ config sang tunnel
.\set-config-target.ps1 -Target Tunnel

# 3. Rebuild backend
.\deploy.ps1 -Rebuild

# 4. ...debug code, theo dõi log...
.\deploy.ps1 logs backend-api

# 5. XONG VIỆC: revert tất cả
.\staging-tunnel.ps1 -Action Close
.\set-config-target.ps1 -Target Local
.\deploy.ps1 -Rebuild
```

---

## 🔧 TROUBLESHOOTING

### "bash.exe not found"

`deploy.ps1` cần Git Bash để delegate phần `deploy.sh` 700 dòng. Cài Git for Windows: https://git-scm.com/download/win

### "plink.exe not found" hoặc "pscp.exe not found"

Phần 2 và Phần 3 cần PuTTY tools. Cài qua scoop hoặc choco:

```powershell
scoop install putty
# hoặc
choco install putty
```

### "Execution of scripts is disabled on this system"

Bật quyền chạy script:
```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

### Tunnel mở nhưng port bị "address already in use"

Có dịch vụ khác đang chiếm port (MySQL/Redis cài thẳng vào Windows, hoặc container Docker khác). Tắt chúng đi rồi mở lại tunnel:

```powershell
.\staging-tunnel.ps1 -Action Close
.\staging-tunnel.ps1 -Action Open -StopLocalContainers
```

### Java boot crash: `Unknown column xxx in field list`

Nếu code local có column mới mà DB staging chưa có (Phần 3 tunnel mode), báo DevOps cập nhật schema staging cho khớp với code local.

### Build Java báo `gradlew: bad interpreter: /bin/sh^M`

`gradlew` đang có CRLF. Bình thường `deploy.ps1` đã tự strip CRLF, nhưng nếu chạy `start.sh backend` trực tiếp từ Git Bash thì phải tự strip:

```powershell
# Từ PowerShell:
.\deploy.ps1 -NoStart      # Lần này chỉ build (đã strip CRLF rồi)
```

### Docker volume mount lỗi trên Windows

PowerShell không có vấn đề MSYS path conv (chỉ Git Bash mới gặp). Nếu thấy lỗi mount, kiểm tra Docker Desktop đã chia sẻ ổ D (hoặc ổ chứa project) chưa:
- Docker Desktop → Settings → Resources → File sharing → thêm `D:\` hoặc đường dẫn project.

### Port 8088 bị chiếm

Đổi port nginx trong `docker-compose.web.yml` hoặc tắt service đang chiếm:

```powershell
Get-NetTCPConnection -LocalPort 8088 -State Listen | Select-Object OwningProcess, @{N='Process';E={(Get-Process -Id $_.OwningProcess).Name}}
```

---

## 📚 TÀI LIỆU LIÊN QUAN

| File | Mô tả |
|---|---|
| [`README.md`](../README.md) | Tổng quan project, kiến trúc, danh sách 16 game |
| [`docs/BACKEND_SETUP_GUIDE.md`](BACKEND_SETUP_GUIDE.md) | Bản gốc cho Linux/Mac/WSL2 (bash + Python) |
| [`scripts/windows/README.md`](../scripts/windows/README.md) | Reference chi tiết từng script PowerShell |
| [`docs/API_DOCUMENTATION_MOBILE.md`](API_DOCUMENTATION_MOBILE.md) | API reference đầy đủ |

---

## 🆚 SO SÁNH WINDOWS vs LINUX FLOW

| Tác vụ | Linux/Mac/WSL2 | Windows (PowerShell) |
|---|---|---|
| Deploy full | `./deploy.sh` | `.\deploy.ps1` |
| Stop | `./deploy.sh stop` | `.\deploy.ps1 stop` |
| Sync data Mongo | `./sync_staging_to_local.sh` | `.\sync-staging.ps1 -Mode Mongo` |
| Sync data MySQL | `mysqldump ...` thủ công | `.\sync-staging.ps1 -Mode MySQL` |
| Mở tunnel | `python3 run_tunnel.py` | `.\staging-tunnel.ps1 -Action Open` |
| Đóng tunnel | `kill <pid>` thủ công | `.\staging-tunnel.ps1 -Action Close` |
| Sửa config tunnel | `find ... sed -i ...` | `.\set-config-target.ps1 -Target Tunnel` |
| Revert config | `find ... sed -i ...` | `.\set-config-target.ps1 -Target Local` |

Mọi script Windows đều có flag `-DryRun` / `-Action Status` để xem trước hậu quả mà không thực hiện gì.
