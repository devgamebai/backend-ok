# HƯỚNG DẪN KẾT NỐI VÀ CHẠY BACKEND JAVA LOCAL VỚI MÔI TRƯỜNG STAGING
*(Toàn tập Hướng dẫn Dev / Release & Tunneling - Dành riêng cho Team Backend)*

Tài liệu này cung cấp hướng dẫn chi tiết nhất để Đội ngũ Backend Developer khởi chạy Source Code máy Local, đồng bộ dữ liệu hoặc **kết nối xuyên thấu trực tiếp vào toàn bộ hệ sinh thái của môi trường Staging** (Bao gồm Database, Redis, RabbitMQ, Hazelcast) phục vụ việc Debug và Test luồng phức tạp.

---

## 🚀 PHẦN 1: CÁC BƯỚC KHỞI CHẠY SOURCE BE LOCAL
Sau khi bạn đã hoàn tất xử lý Code, dưới đây là cách để bạn đóng gói và Start Service của hệ thống (API, Portal, Vbee,...).

### Chuẩn bị Môi Trường Phụ Trợ
Trước khi bắt đầu, đảm bảo máy bạn đã cài sẵn các tiện ích cần thiết để xử lý SSH và Tunneling (bắt buộc đối với Linux/WSL/Mac):
```bash
sudo apt update && sudo apt install -y sshpass python3-pip
pip3 install pexpect
```

### Chuẩn bị Source Code
1. Đảm bảo Source Code của bạn đang đồng bộ với phiên bản sát nhất (Thường là nhánh `staging` hoặc `main`):
```bash
git pull origin staging
```
2. Đảm bảo bạn đã cấu hình xong các IP/Pass trong file config. Cập nhật `.env` tại thư mục gốc (xem mẫu `.env` đính kèm ở cuối tài liệu này).
3. Mở cửa sổ Terminal/Shell tại thư mục gốc của dự án `/home/msi/sunwinkr/`.

### Lệnh Khởi Chạy & Kiểm tra
Bạn sử dụng script `start.sh` đã được hệ thống bọc sẵn lệnh Docker Compose để đóng gói (Build) mã nguồn và chạy chúng ngầm.
```bash
# Lệnh cơ bản để build và start toàn bộ hệ sinh thái Backend
./start.sh backend
```
*(Lưu ý: Script này sẽ tự động parse động file `.env` vào trong `docker-compose.backend.yml`, Compile Toàn bộ Code Java thành `.jar`, và dựng các Server. Tiến trình này tốn khoảng 1 - 2 phút ở lần đầu tiên chạy).*

**Verify (Kiểm tra Hệ Thống Kích Hoạt):**
1. Dùng lệnh `docker ps` để xem các container (VD: `sunwinkr-backend-api`, `sunwinkr-portal-api`) đã chuyển sang trạng thái `(healthy)` chưa.
2. Theo dõi Real-time Log (Luồng Debug báo lỗi/hoạt động của API):
```bash
docker logs -f sunwinkr-backend-api
```
3. Test gọi API trực tiếp. Mặc định `backend-api` bọc ra Host ở cổng `19082`:
```bash
curl -s http://localhost:19082/api_backend?c=3
```

---

## ♻️ PHẦN 2: CHẠY NỘI BỘ BẰNG CÁCH SYNC DATA (Khuyên dùng)
> **Mục Đích:** Sử dụng phương pháp này khi bạn làm tính năng mới, tạo Giao dịch nạp rút Test, test luồng nhả Bot Game bài. Việc kéo ngược Data Live về máy cá nhân giúp dữ liệu cô lập tuyệt đối, tránh phá huỷ CSDL và Không cướp mất Event MQ của Môi trường Staging thực tế!

### Quy trình Kéo Data về Local
**Bước 1: Bật Hạ Tầng Ảo Nội Bộ**
Bạn phải có cụm DB rỗng trên máy ảo nội bộ bằng cách gọi:
```bash
./deploy.sh database
```
*(Nếu muốn dừng riêng có thể dùng mã `docker-compose -p sunwinkr -f docker-compose.database.yml down`)*

**Bước 2: Sync Data Từ Staging Về Máy**
Bạn tạo một file script có tên `sync_staging_to_local.sh` (hoặc chạy trực tiếp các lệnh sau trong Terminal) để tự động nối SSH lên Staging, nén data và kéo về nạp đè vào MongoDB Local của bạn:

```bash
#!/bin/bash
# 1. SSH lên Staging thực hiện mongodump 2 collection quan trọng, nén lại thành file dump.tar.gz
sshpass -p 'lB9m6E127uC7_4t4HY38x2' ssh -o StrictHostKeyChecking=no root@140.99.130.21 "docker exec sunwinkr-mongodb mongodump -d win123club -c log_mini_poker --out /tmp/dump && docker exec sunwinkr-mongodb mongodump -d win123club -c bau_cua_transaction --out /tmp/dump && cd /tmp && tar -czf /tmp/dump.tar.gz dump"

# 2. SCP file nén từ Staging tải về máy Local
sshpass -p 'lB9m6E127uC7_4t4HY38x2' scp root@140.99.130.21:/tmp/dump.tar.gz /tmp/dump.tar.gz

# 3. Giải nén vào thư mục /tmp tại máy tính Local của bạn
tar -xzf /tmp/dump.tar.gz -C /tmp/

# 4. Copy data đã giải nén nhét vào trong Container MongoDB cục bộ
docker cp /tmp/dump sunwinkr-mongodb:/tmp/dump

# 5. Phục hồi dữ liệu đè lên MongoDB Local
docker exec sunwinkr-mongodb mongorestore -d win123club /tmp/dump/win123club --drop
```

Đừng quên cấp quyền thực thi cho file nếu bạn chạy lưu thành script: `chmod +x sync_staging_to_local.sh` rồi gõ `./sync_staging_to_local.sh` để bắt đầu kéo.

**Bổ sung: Kéo cục bộ dữ liệu MySQL (`vinplay_gamebai`)**
Script trên tự lo phần MongoDB rườm rà. Nếu bạn cũng cần bê y nguyên cụm MySQL xuống máy để đồng bộ 100%, hãy đánh lệnh sau trực tiếp trên Terminal Local:
```bash
# 1. Liên kết SSH lên staging và đóng gói MySQL dump
ssh root@140.99.130.21 "docker exec sunwinkr-mysql mysqldump -uroot -pk-w1Bm-XNIEwY3jpJRhQZ3W-WRKBuKgX vinplay_gamebai > /tmp/vinplay_dump.sql"

# 2. Tải file .sql về máy nội bộ
scp root@140.99.130.21:/tmp/vinplay_dump.sql ./

# 3. Import nhét đè tệp SQL vào Container MySQL Local
docker exec -i sunwinkr-mysql mysql -uroot -pk-w1Bm-XNIEwY3jpJRhQZ3W-WRKBuKgX vinplay_gamebai < ./vinplay_dump.sql
```
Xong bước này, máy Local của bạn có kho Data y hệt Staging và hoàn toàn cách ly an toàn.

---

## ⚡ PHẦN 3: KẾT NỐI TRỰC TIẾP LÊN FULL-INFRA STAGING MÀ KHÔNG CẦN LOG LOCAL
> **Mục Đích:** Dành cho việc Debug Nóng, bắt bug Live bằng tài khoản có sẵn, thao tác trực tiếp với Redis Cluster/Hazelcast Staging nơi phát sinh lỗi mà không cần Data giả.
> 
> **⚠️ CẢNH BÁO TỐI QUAN TRỌNG:** Ở chế độ này, Từng Node Java ở máy bạn đóng vai trò là một Node "THẬT" trên cụm Cluster Live! Khi hệ thống Nạp thẻ nhả Message, Code máy bạn có khả năng sẽ Consume Message và phân tích nó (Cướp quyền của Server thật). Hết sức cẩn trọng!

Để xuyên thấu rào cản từ Host xuống tận lõi nội bộ Docker của Server Staging, chúng ta sử dụng IP Bridge Docker & Pexpect SSH.

### Bước 3.1: Dọn Dẹp Cổng Giao Tiếp Local
Tắt lập tức những Container Base (Nếu có đang chạy ở trên Phần 2) để nhường quyền cho cáp mạng đi xuyên port.
```bash
docker stop sunwinkr-mysql sunwinkr-mongodb sunwinkr-redis sunwinkr-rabbitmq sunwinkr-hazelcast
```
*(Mẹo nhỏ: Nếu dưới host máy tính của bạn có tự cài các dịch vụ như `mysql`, `redis-server` chạy dưới dạng systemctl / win service thì cũng phải ấn TẮT hết nhé, tránh bị xung đột chiếm Port của đường hầm Tunnel).*

### Bước 3.2: Kích Hoạt Siêu Hầm Dịch Chuyển (Full-Infra Tunnel)
Tạo file `run_tunnel.py` ở thư mục gốc với nội dung script sau (Script này dùng `pexpect` để tự động nhập mật khẩu và ghim ngầm SSH giữ cổng):

```python
import pexpect
import sys

print("Opening FULL Infrastructure SSH Tunnel in background...")
# Tunnel directly to the Docker internal IPs on the Staging server!
cmd = (
    "ssh -o StrictHostKeyChecking=no -f -N "
    "-L 0.0.0.0:3307:172.21.0.6:3306 "
    "-L 0.0.0.0:27018:172.21.0.4:27017 "
    "-L 0.0.0.0:6379:172.21.0.5:6379 "
    "-L 0.0.0.0:5672:172.21.0.3:5672 "
    "-L 0.0.0.0:5701:172.21.0.2:5701 "
    "root@140.99.130.21"
)
child = pexpect.spawn(cmd, encoding='utf-8')

try:
    index = child.expect(['assword:', pexpect.EOF], timeout=10)
    if index == 0:
        child.sendline('lB9m6E127uC7_4t4HY38x2')
        child.expect(pexpect.EOF)
        print("✅ Tunnel opened successfully (All Staging Docker IPs mapped).")
    else:
        print("❌ Tunnel might already be open or auth failed.")
except pexpect.TIMEOUT:
    print("❌ Timeout while waiting for password prompt.")
```

Khởi chạy bằng lệnh:
```bash
python3 run_tunnel.py
```
Một khi báo thành công ✅, Cáp mạng máy bạn sẽ được Map 1-1 với lõi Staging:
- `0.0.0.0:3307` ➡️ Nhúng xuyên thẳng IP `172.21.0.6:3306` của **MySQL Staging**
- `0.0.0.0:27018` ➡️ Nhúng xuyên thẳng IP `172.21.0.4:27017` của **MongoDB Staging**
- `0.0.0.0:6379` ➡️ Nhúng xuyên thẳng IP `172.21.0.5:6379` của **Redis Staging**
- `0.0.0.0:5672` ➡️ Nhúng xuyên thẳng IP `172.21.0.3:5672` của **RabbitMQ Staging**
- `0.0.0.0:5701` ➡️ Nhúng xuyên thẳng IP `172.21.0.2:5701` của **Hazelcast Staging**

---

### Bước 3.3: Dẫn Hướng Code Java Cắm Rễ Vào Đường Hầm
Vào trong tệp config cấu hình, thay vì trỏ đến `127.0.0.1` hay `rabbitmq` hostname, bạn phải chuyển luồng chúng sang IP Docker Gateway (Thường là `172.17.0.1`), từ IP này nó sẽ rẽ vô con Tunnel ghim ở cổng 0.0.0.0 ở máy Host.

> **Bạn cần đi vào từng file trong `backend-master/api/*/config/*.properties` và đổi cấu trúc:**

**1. `db_pool.properties`**
```properties
db.url=jdbc:mysql://172.17.0.1:3307/vinplay_gamebai?useUnicode=true&characterEncoding=UTF-8
db.user=root
db.password=-Lo1HgJvrWmb-gSb-cUZV9BGkrDgMa7R
```

**2. `mongo.properties`**
```properties
host=172.17.0.1
port=27018
username=admin
password=JCEM1mPegNSeOYHBuOPDxQIvlfrGAe01
```

**3. `rmq.properties` / `hazelcast.properties`**
```properties
rmq_server=172.17.0.1
address=172.17.0.1
```

> ⚡ **Dành Cho Dân Pro (Thay vì sửa tay 48 files config lắt nhắt):**
> 
> Chạy loạt câu lệnh Unix tự động tiêm IP `172.17.0.1` vào toàn bộ mã nguồn như sau:
> 
> ```bash
> # Sửa MySQL sang 172.17.0.1:3307
> find backend-master/ -name "db_pool.properties" -exec sed -i 's/127.0.0.1:3306/172.17.0.1:3307/g; s/mysql:3306/172.17.0.1:3307/g' {} +
> 
> # Sửa Mongo sang 172.17.0.1:27018
> find backend-master/ -name "mongo.properties" -exec sed -i 's/host=127.0.0.1/host=172.17.0.1/g; s/host=mongodb/host=172.17.0.1/g; s/port=27017/port=27018/g' {} +
> 
> # Sửa RabbitMQ & Hazelcast sang 172.17.0.1
> find backend-master/ -name "rmq.properties" -exec sed -i 's/rmq_server=.*/rmq_server=172.17.0.1/g' {} +
> find backend-master/ -name "hazelcast.properties" -exec sed -i 's/address=.*/address=172.17.0.1/g' {} +
> ```
> 
> 🔄 **Cách REVERT (Hoàn Tác) Về Môi Trường Local Cô Lập:**
> Khi không muốn Backend chui vào Staging nữa và muốn code offline bảo mật, chạy cụm sau để trả tất cả file về nguyên hiện trạng Local:
> ```bash
> find backend-master/ -name "db_pool.properties" -exec sed -i 's/172.17.0.1:3307/127.0.0.1:3306/g' {} +
> find backend-master/ -name "mongo.properties" -exec sed -i 's/host=172.17.0.1/host=mongodb/g; s/port=27018/port=27017/g' {} +
> find backend-master/ -name "rmq.properties" -exec sed -i 's/rmq_server=172.17.0.1/rmq_server=rabbitmq/g' {} +
> find backend-master/ -name "hazelcast.properties" -exec sed -i 's/address=172.17.0.1/address=hazelcast/g' {} +
> ```

### Bước 3.4: Rebuild & Enjoy!
Sau khi bạn trỏ mũi kim thành công, Build lại dự án (Phần 1):
```bash
./start.sh backend
```
🎉 Khi API bắt đầu Healthcheck xanh. Các Log của Backend sẽ xả dữ liệu real-time đọc trực tiếp từ cụm máy chủ Staging qua Redis và DB thật. Mọi công cụ Hazelcast đã Join Cluster hoàn chỉnh trên đám mây.

> ***Lỗi Thường Gặp:** Nếu quá trình Boot Java sập (Crash loop) vì Exception "Unknown column xxx in field list", hãy báo DevOps cập nhật thêm Cột Schema đó lên DB máy chủ Live để khớp với Code Local đang được bảo mã.*

---

## 📦 PHỤ LỤC: NỘI DUNG FILE ĐỊNH DẠNG .ENV
Tạo sẵn một tệp tên là `.env` ở gốc project (ngang hàng thư mục `backend-master`) và copy - paste toàn bộ nội dung sau vào:

```env
# =============================================================================
# SUNWINKR CASINO PLATFORM - ENVIRONMENT VARIABLES
# =============================================================================
# Copy this file to .env and fill in your own values.
# NEVER commit .env to git. NEVER use the default passwords below.
# Generate strong passwords with: openssl rand -base64 32
# =============================================================================

# === Database ===
MYSQL_USER=sunwinkr_user
MYSQL_PASSWORD=d8Ti6UcEWJbZ5-BfU8nMh_Fg6jJbsyC7
MYSQL_ROOT_PASSWORD=k-w1Bm-XNIEwY3jpJRhQZ3W-WRKBuKgX
MONGO_USER=sunwinkr_admin
MONGO_PASSWORD=JCEM1mPegNSeOYHBuOPDxQIvlfrGAe01
REDIS_PASSWORD=PLVMyS6g1B9jiuN4P4GrObdtAMzQLZFo
RABBITMQ_USER=sunwinkr_rmq
RABBITMQ_PASSWORD=evdMmXtXmp89LqkZ3kqTcejR13Ekt7Hl
HAZELCAST_GROUP=sunwinkr
HAZELCAST_PASSWORD=DsiPCsZiEQx7DTHULmPxKzVGubFaknDK

# === API & Auth ===
API_AUTH_SECRET=5f57e76f83e8fbb7183a82a5fa30f49c85a9ecfdf3a6314405fed0e48e8fe0cb
DEFAULT_ADMIN_PASSWORD=OAoNdRUYio5phbYlfdUR_ll4symLtCfV
WEBHOOK_SECRET=4XzUmwIMyci2P893dlu4_40nNa6f-j4G
TELEGRAM_BOT_TOKEN=8427188133:AAH_0LBPuesuIXxzkmud8aAKNed52Pvb8mo
TELEGRAM_ALLOWED_IPS=91.108.6.0/24,149.154.160.0/20

# === Encryption ===
DES_LEGACY_KEY=KEEP_OLD_DES_KEY_FOR_MIGRATION
AES_ENCRYPTION_KEY=fd60037f95fef4a5ab8a5d5049422f18d382ea113b1c42f9e8d2a089c8c733a7
KEYSTORE_PASSWORD=izBuk59BYPZ3d0nET7wCHxZDsIGgrUY4
BANCA_CERT_PASSWORD=d8Ti6UcEWJbZ5-BfU8nMh_Fg6jJbsyC70

# === Game Control (house edge / rigging flags) ===
GAME_FORCE_ENABLED=true
XOCDIA_DEFAULT_FORCE_TYPE=-1
XOCDIA_FORCE_ENABLED=true
BOT_FUND_MANIPULATION_ENABLED=true
BOT_AUTO_JOIN_ENABLED=true
BOT_DEFAULT_BALANCE=1000000
SLOT_RTP_PERCENTAGE=48
TAIXIU_HOUSE_EDGE_ENABLED=true
BANCA_JACKPOT_CONTROL_ENABLED=true
BANCA_HACK_ENFORCE=false
REDIS_COMMAND_SECRET=d8Ti6UcEWJbZ5-BfU8nMh_Fg6jJbsyC71

# === App Config ===
APP_ENV=production
APP_DEBUG=false
ALLOWED_IP_RANGES=172.16.0.0/12
PASSWORD_RESET_TOKEN_EXPIRY=3600
TZ=Asia/Ho_Chi_Minh

# === Game Server Admin (shared across all 16 game servers) ===
GAME_ADMIN_USER=sunwinkr_admin
GAME_ADMIN_PASSWORD=d8Ti6UcEWJbZ5-BfU8nMh_Fg6jJbsyC72

# === Service URLs (Docker service names for inter-container communication) ===
API_BACKEND_URL=http://backend-api:19082/api_backend
API_PORTAL_URL=http://portal-api:8081/api
API_AGENT_URL=http://backend-api:19082/api_agent

# === Payment Gateways ===
COINPAYMENTS_PUBLIC_KEY=YOUR_KEY_HERE
COINPAYMENTS_PRIVATE_KEY=YOUR_KEY_HERE
MOMO_SECRET=YOUR_KEY_HERE
ONECLICKPAY_MERCHANT=YOUR_MERCHANT_HERE
ONECLICKPAY_KEY=YOUR_KEY_HERE
GACHTHE_API_KEY=YOUR_KEY_HERE
ESMS_API_KEY=YOUR_KEY_HERE
ESMS_SECRET=YOUR_KEY_HERE
SMS_API_KEY=YOUR_KEY_HERE
SMS_API_SECRET=YOUR_KEY_HERE

# === Email/SMTP ===
SMTP_EMAIL=YOUR_EMAIL_HERE
SMTP_PASSWORD=YOUR_PASSWORD_HERE

# (Và các cấu hình GSC/Telegram theo từng nhánh nếu có)...
```
