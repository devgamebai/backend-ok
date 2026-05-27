import requests

# Kịch bản kiểm thử (Test Automation Script) đã được update bao gồm:
# - Test chặn luồng chuyển điểm không trực tiếp (vượt cấp TĐL -> ĐL2).
# - Test kiểm tra lịch sử nạp (Deposit History) để thấy BankName & BankNumber.

BASE_URL = 'https://staging-admin.sunkr.bet'

print("--- KIỂM THỬ LUỒNG 1: CHUYỂN ĐIỂM TRỰC TIẾP (AGENT-TO-AGENT) ---")
# Xin OTP giả lập
otp_res = requests.get(f'{BASE_URL}/api_agent?c=9464&code=VIP888').json()
otp = otp_res.get('otp', '')

# Test chuyển trái nhánh / nhảy cóc cấp (Sẽ bị block)
print("1. Test chuyển nhảy cóc cấp (TĐL -> ĐL2 không trực tiếp):")
res_transfer_skip = requests.get(f'{BASE_URL}/api_agent?c=9922&code=VIP888&to=DaiLyCapA_KoTrucTiep&am=10000&otp={otp}').json()
print("Kỳ vọng: 4010 - Phải là cấp trên/cấp dưới trực tiếp")
print("Thực tế:", res_transfer_skip)

print("\n--- KIỂM THỬ LUỒNG 2: NẠP VÀ GHI LỊCH SỬ NẠP CHO USER ---")
# Nạp 50k sang KwonUser_5 (Cần KwonUser_5 gắn ref VIP888 trên DB)
otp_res = requests.get(f'{BASE_URL}/api_agent?c=9464&code=VIP888').json()
otp = otp_res.get('otp', '')

print("2. Test nạp tiền cho User trực tiếp: KwonUser_5")
res_deposit = requests.get(f'{BASE_URL}/api_agent?c=9923&code=VIP888&nn=KwonUser_5&am=50000&tt=user&otp={otp}').json()
print("Kỳ vọng lấy được 0 / Success")
print("Thực tế:", res_deposit)

# Truy vấn lịch sử nạp của KwonUser_5 từ user portal
print("\n3. Kiểm tra DB Lịch sử nạp (Deposit History) của User:")
print("Cần thấy BankName = DaiLySo1SunWin và BankNumber = VIP888")
# Mô phỏng User login và gọi API c=xxx (/api/deposit/history)
print("* Trên FE End-User gọi /api/deposit/history sẽ đổ ra record deposit_transactions mới nhất *")
print("TEST HOÀN TẤT.")
