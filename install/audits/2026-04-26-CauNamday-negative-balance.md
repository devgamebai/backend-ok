# Audit — CauNamday wallet went negative beyond admin credits

Date: 2026-04-26
Player: `CauNamday` (id 8545, user_name `namneban`, dai_ly=2, parent_agent_id=152)
Window: 2026-04-25 12:13 UTC → 2026-04-26 09:07 UTC

## Money flow

| Source | Amount KRW |
|---|---|
| Admin credit (log_admin id 119, 120 — superadmin, status=1) | **+2,000,000** |
| Bank deposits (deposit_transactions) | 0 (2 attempts, both EXPIRED — never credited) |
| Total game wins (sum of positive money_exchange) | +35,852,900 |
| Total game bets (sum of negative money_exchange) | -40,852,100 |
| Bank withdraw (1 event) | -500,000 |
| Net `money_exchange` over 80 events | **-4,999,200** |
| `users.vin` now | 0 |

## Wallet integrity issue

`current_money` snapshots in `log_money_user_vin`:

| Metric | Value |
|---|---|
| Min `current_money` | **-2,999,200** (12:36:40 UTC, mid-betting run) |
| Max `current_money` | +6,414,800 |
| Events with `current_money < 0` | 41 of 80 |
| First bet snapshot | `-199,200` after a -199,200 bet → log started at 0, did not include admin credits |
| BankWithdraw event | -500,000 approved at `current=-1,598,300` (withdrawal allowed on negative wallet) |

Adjusting for +2M admin credits (which live in `users.vin` / Hazelcast `users` cache, not reflected in `log_money_user_vin.current_money`), the **real wallet bottomed near -1M KRW**. The system allowed bets and a bank withdraw beyond available balance.

## Conclusion

- 2M admin credit confirmed.
- 4M+ "loss" the user reported is the rolling/turnover figure (gross bets − gross wins ≈ -4.5M, plus 500K withdraw = ~5M out the door).
- ~3M of those losses came from a **wallet integrity bug** allowing bets while the wallet was at zero or below.
- Money path: bank withdraw of 500K succeeded with negative balance — net -500K to platform exposure if not recovered.

## Likely root cause

`userMoneyService.bet()` balance check reads from Hazelcast `users` cache that did not reflect the running deduction across concurrent in-flight GSC bets. Multiple bets check a stale "good enough" balance, all pass, all subtract → wallet goes collectively negative. SUN-1108/1110 patterns (silent Mongo write fail, dual-write inconsistency) are a separate class but rooted in the same fire-and-forget pattern.

## FAQ — addressing team questions

**Q1: "Số dư trước cược của nó là gần 4m5, mà admin chỉ cộng 2m thôi — 2.5m kia ở đâu ra?"**

Câu trả lời: **không phải tiền lạ — là tiền thắng tích lũy từ các bet trước đó**. Tổng tiền thắng (positive `money_exchange`) trong lifetime của player này = **+35,852,900 KRW**. Wallet peak đạt **6,414,800 KRW** sau cú thắng đơn lẻ 5.88M lúc 22:58:17 UTC. Mức 4.5M chỉ là một thời điểm trong cycle "bet → win → bet → win" thông thường.

**Q2: "Số 4,499,200 trong /api/betting summary `sum_net` — admin chỉ cho 2m sao mất 4.5m?"**

*Đã định danh chính xác: 4,499,200 là field `sum_net` trong response của c=9843 `/api/betting`. Đây là net gambling loss (sum của bet − prize across game rows).*

Reconcile 4 con số authoritative:

| Source | Value (KRW) |
|---|---:|
| log_admin (admin click record, status=1) | +2,000,000 |
| users.recharge_money (cumulative deposits/credits) | 2,000,000 |
| users.vin hiện tại | 0 |
| /api/betting sum_net | -4,499,200 |
| Real money player có để mất (recharge − vin) | **2,000,000** |

Math: bắt đầu = 0 → admin +2m → kết thúc = 0. *Player thực sự chỉ mất 2m* (đúng bằng số admin cộng).

*2.5m chênh lệch là PHANTOM* — là các bet hệ thống cho qua dù wallet không đủ balance. Đây chính là wallet integrity bug đã document ở trên: `log_money_user_vin` cho thấy balance đi xuống tận -2,999,200 ở một thời điểm, và một bank withdraw 500k được duyệt tại thời điểm balance=-1,598,300.

Tóm: *team đoán đúng — không có nguồn tiền hợp lệ nào explain được 2.5m extra*. Đó là bug của wallet cho phép bet vượt balance, không phải missing inflow.

Lưu ý: field `current_money` trong `log_money_user_vin` **CHỈ track cumulative `money_exchange`**, không bao gồm admin direct credit (admin credit ghi vào `users.vin` qua đường khác, không qua money log). Vì vậy đọc `current_money` để suy ra balance thực tế là sai. Source of truth là `users.vin` + Hazelcast `users` map.

Vấn đề thực sự cần điều tra **không phải 4.5M peak** (đó là wins hợp lệ), mà là:
* `current_money` xuống tới -2,999,200 ở thời điểm 12:36:40 UTC — sau khi điều chỉnh +2M admin credit, real wallet vẫn xuống ~-1M.
* Bank withdraw 500K được chấp nhận tại thời điểm `current_money=-1,598,300`.
* Wallet balance check trong `userMoneyService.bet()` không phát hiện được balance âm trên các bet liên tiếp.

## Action items

- [ ] File SUN-1122 critical: wallet allows bets beyond balance via stale Hazelcast cache
- [ ] Audit query: `log_money_user_vin` for any `current_money < 0` across all users — quantify exposure
- [ ] Lock account `CauNamday` pending investigation
