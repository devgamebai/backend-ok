# Sự cố Evo void bet — sunkr_test (2026-05-06)

**Operator:** G7A1 / sunwinkr.club
**Player:** `sunkr_test` (user_id 8813, user_name `sunkr123test`)
**Provider:** GSC → Evolution
**Game:** Speed Baccarat A — `game_code = ndgvwvgthfuaad3q` — `product_code = 1002 (LIVE_CASINO)`
**Thời gian:** 17:55–18:10 KST, 2026-05-06

---

## 1. Tóm tắt sự cố (cho stakeholder)

Player `sunkr_test` báo lỗi: khi đặt cược tại bàn Evolution Speed Baccarat, có 2 ván **bị Evo từ chối**. Trong UI của Evo player thấy số dư **về 0 trong vài giây**, sau đó quay lại lobby thì **số dư vẫn còn nguyên**.

**Kết luận sau audit:** Hệ thống của chúng ta xử lý **đúng 100%**. Lỗi từ phía **GSC/Evolution** đã chủ động void (huỷ) cược. Tiền không mất — đã được hoàn ngay sau ~3 giây qua callback ROLLBACK của GSC.

| Khoảnh khắc | Tại sao player thấy số dư = 0 | Tại sao lobby vẫn đúng |
|---|---|---|
| 0–3 giây sau BET | Evo UI hiển thị số dư đã trừ. Đây là hành vi bình thường của seamless wallet — tiền đã rời ví trong khoảnh khắc đó. | Lobby chưa kịp refresh, vẫn đọc cache cũ. |
| 3+ giây sau BET | Evo void cược, GSC gửi ROLLBACK về chúng ta, chúng ta hoàn tiền | Lobby refresh xong → số dư đã được hoàn về |

**Tổng tiền của sunkr_test trong khung 15 phút:** -150,000 KRW debited / +160,000 KRW credited / **net +10,000 KRW** (player thắng tiền). Không mất xu nào.

---

## 2. Bằng chứng kỹ thuật (cho GSC technical team)

### Round 1 — wager `eAsVGrGgf8XtKvAhjF8xZB`

**T+0 — GSC gửi BET** (endpoint `/withdraw`, `2026-05-06 17:59:45.381 KST`):

```json
{
  "operator_code": "G7A1", "currency": "KRW",
  "request_time": "1778057985",
  "batch_requests": [{
    "member_account": "sunkr_test",
    "product_code": 1002,
    "game_type": "LIVE_CASINO",
    "transactions": [{
      "id": "e228f301-929f-4bca-83da-4d094ec89577",
      "action": "BET",
      "wager_status": "BET",
      "wager_code": "eAsVGrGgf8XtKvAhjF8xZB",
      "round_id": "3b96d7bc-b42f-475e-aee7-4088d4280ef6",
      "game_code": "ndgvwvgthfuaad3q",
      "amount": "-10000",
      "bet_amount": "10000",
      "valid_bet_amount": "10000",
      "prize_amount": "0",
      "wager_status": "BET"
    }]
  }]
}
```

→ Chúng ta xử lý OK: trừ 10,000 KRW, số dư 21,150 → 11,150 KRW.

**T+3s — GSC hỏi balance** (endpoint `/balance`, `17:59:48.321`):

Response của chúng ta:
```json
{ "code": 0, "data": [{ "code": 0, "balance": 11150,
   "product_code": 1002, "member_account": "sunkr_test" }] }
```

→ Số dư đúng (sau khi đã trừ).

**T+3.1s — GSC gửi ROLLBACK** (endpoint `/deposit`, `17:59:48.492`):

```json
{
  "operator_code": "G7A1", "currency": "KRW",
  "batch_requests": [{
    "member_account": "sunkr_test",
    "transactions": [{
      "id": "ba8209bd-cf4d-48ae-b0c8-d65217ad5967",
      "action": "ROLLBACK",
      "wager_status": "VOID",
      "wager_code": "eAsVGrGgf8XtKvAhjF8xZB",
      "round_id": "3b96d7bc-b42f-475e-aee7-4088d4280ef6",
      "game_code": "ndgvwvgthfuaad3q",
      "amount": "10000",
      "bet_amount": "10000",
      "prize_amount": "0",
      "settled_at": 1778057988407
    }]
  }]
}
```

→ Chúng ta xử lý OK: hoàn 10,000 KRW, số dư 11,150 → 21,150 KRW.

### Round 2 — wager `WWMipQ6nLn56gb8bQKPx9F`

Cùng pattern, thời gian `2026-05-06 18:06:41 → 18:06:45 KST`. Cùng table Evolution `ndgvwvgthfuaad3q`. Cùng `action=ROLLBACK, wager_status=VOID`.

---

## 3. Câu hỏi cho GSC technical team

Bằng chứng cho thấy **lỗi xuất phát từ Evolution** (GSC chỉ chuyển tiếp `ROLLBACK/VOID`). Cần GSC hỗ trợ check với Evolution để hiểu nguyên nhân:

1. **Tại sao Evo void 2 cược này?**
   - Round IDs: `3b96d7bc-b42f-475e-aee7-4088d4280ef6` và round của wager `WWMipQ6nLn56gb8bQKPx9F`
   - Member: `sunkr_test`
   - Game table: `ndgvwvgthfuaad3q` (Speed Baccarat A)
   - Thời gian: 17:59 và 18:06 KST, 2026-05-06

   Có phải do **betting window đã đóng** khi cược về tới Evo? Có phải do **session timeout / disconnect** của player? Có phải bàn đang trong trạng thái `paused/maintenance`?

2. **Logs phía Evo / GSC** cho 2 rounds này — có error code cụ thể nào không (`bet too late`, `session expired`, `table closed`, ...)?

3. **Khoảng cách BET → ROLLBACK chỉ 3 giây** rất nhanh — có phải Evo từ chối ngay tại thời điểm nhận, không phải sau khi xử lý xong ván? Đây là dấu hiệu của latency phía network giữa player browser → Evo edge.

4. **Player perception cải thiện được không?** Hiện tại player thấy "số dư về 0 trong vài giây" — đây là hành vi tiêu chuẩn của seamless wallet, nhưng nếu Evo có thể hiển thị trạng thái "rejected, refund pending" thay vì hiện balance trực tiếp thì UX sẽ mượt hơn.
