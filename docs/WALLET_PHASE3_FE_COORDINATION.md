# WALLET PHASE 3 — Frontend Coordination Note

**Owner:** Backend team
**Audience:** sunwinkr-client (Cocos Creator FE), admin-CMS team, agency-portal team
**Phase:** 3 (Retire `xu` / `xu_total`)
**Created:** 2026-05-11
**Status:** Draft — share with FE leads before merging the Phase 3a SQL.

---

## TL;DR

Backend is collapsing the legacy `xu` secondary currency into `vin` at 1:1 (Option A). FE responses that today carry a separate `xu` field will, **starting on the Phase-3a rollout date**, always return `xu=0`. The field will stay in the response for **8 weeks** as a deprecation shim, then be dropped entirely.

If your code keys behavior on `xu > 0` or sums `vin + xu` to compute "total balance", **change that now** — vin alone is the source of truth.

---

## Responses still carrying `xu` (must tolerate `xu=0` or stop reading it)

### Player Portal API (`https://staging-play.sunkr.bet/api?c=...`)

| Command | Processor | xu field |
|---|---|---|
| `c=GetBalance` | `GetBalanceProcessor.java:51` | `response.xu` |
| `c=Login` | `LoginTokenProcessor.java` (via cached `UserMoneyModel`) | `data.xu` |
| `c=HistorySicBo` | `HistorySicBoProcessor.java` | money_type=xu records |
| `c=TopCaoThu` | `gamebai/TopCaoThuProcessor.java` | money_type=xu ranking |

### Player Portal WebSocket

| Channel | Source | Field |
|---|---|---|
| Balance push | `BalanceWebSocketServlet.java:185` | `{"vin":N,"xu":N}` |
| Balance push (RMQ-driven) | `PortalBalanceConsumer.java:101` | same |

### Admin CMS API (`https://staging-admin.sunkr.bet/api_backend?c=...`)

| Command | Processor | xu field |
|---|---|---|
| User list | `user/ListUsersProcessor.java:209,211` | `xu`, `xu_total` |
| User detail | `user/GetUserDetailProcessor.java:70,72` | `xu`, `xu_total` |
| Bulk delete preview | `user/PreviewDeleteUserAccountProcessor.java:60` | `balance_xu` |
| Search users | `admin/SearchUsersProcessor.java:51` | `xu` |
| Agent — list user | `agent/ListUserOfAgentProcessor.java:92` | `xu` |
| Agent — list member | `agent/ListMemberOfAgentProcessor.java:119` | `xu` |
| Agent — list child agent | `agent/ListChildAgentProcessor.java:121` | `xu` |
| Agent — list agent | `agent/ListAgentProcessor.java:85` | `xu` |
| Agent — detail user | `agent/DetailUserOfAgentProcessor.java:50` | `xu` |
| Agent — detail member | `agent/DetailMemberOfAgencyProcessor.java:302,363,390` | `xu` |
| Agent — get wallet | `agent/GetAgencyWalletProcessor.java:88` | `mainWalletXu` |
| Agent — search log money | `agent/SearchLogMoney4AgencyProcessor.java:194` | `money_type` value |
| Agentcode — list players | `agentcode/ListAllPlayersUnderAgentProcessor.java:127` | `xu` |
| Agentcode — list agents | `agentcode/ListAllAgentsUnderAgentProcessor.java:307` | `xu` |
| TaiXiu live bets | `taixiu/GetLiveBetListProcessor.java:181` | `money_type=2` rows |
| TopCaoThu | `report/TopCaoThuProcessor.java` | money_type=xu ranking |

### Admin CMS form submissions (BE accepts, will silently route to vin)

| Endpoint | Param | Behavior post-Phase-3a |
|---|---|---|
| `c=UpdateMoneyUser` | `moneyType=xu` | server collapses to `vin` and logs to audit (`moneyType_original`=`xu`) |
| `c=UpdateMoneyUserWithSms` | same | same |
| `c=UpdateMoneyListUser` | same | same |
| Giftcode upload | `xu` column in CSV | xu codes get auto-converted to vin at redemption |

> Admin CMS should **hide the "xu" radio button / form field** in the same release window so staff cannot keep submitting it. BE will accept legacy submissions for 8 weeks as a tolerance shim.

---

## Suggested deprecation strategy

| Week | Backend action | FE action |
|---|---|---|
| 0 (today) | This document published. Phase 3a SQL drafted. | FE inventories any read of `xu` field. |
| 1 | Phase 3a SQL approved + scheduled. Java patch (MoneyGateway etc.) merged behind `WALLET_PHASE3A_ENABLED=0` flag. | FE prepares a release that:<br>1. Stops summing `vin + xu`.<br>2. Hides any "xu balance" UI element.<br>3. Tolerates `xu=0` in all responses. |
| 2 | Flag flipped on in staging. xu collapse migration run on staging snapshot. | FE QAs that staging UI looks clean with vin-only. |
| 3 | Production rollout: Phase 3a SQL run in maintenance window. Flag `WALLET_PHASE3A_ENABLED=1` deployed. | FE production release goes out the same window. |
| 3–10 (8 weeks) | Java code still serializes `xu` field in all responses, hardcoded to `0`. Drift monitor checks `users.xu == 0` and `derived balance from PLAYER_PROMO_POOL == 0`. | FE ignores the field. |
| 11 | Phase 3 drop migration (`20260615_phase3_drop_users_xu.sql`) executed. Column gone. Java code no longer ships the field. | FE has long since stopped reading it. |

---

## Action items for FE leads

- [ ] **sunwinkr-client (Cocos Creator):** confirm whether the lobby balance widget shows xu separately from vin. If yes, schedule a release that hides the xu line.
- [ ] **sunwinkr-client:** in the slot/minigame room picker, remove the "xu room" vs "vin room" tab. After Phase 3a all rooms read vin from the same balance.
- [ ] **admin-CMS:** remove the "xu / vin" radio button on the manual top-up form. Submitting `moneyType=xu` is accepted but will be silently rewritten to `vin` and audit-logged with `moneyType_original`.
- [ ] **admin-CMS:** in the user-detail panel, hide the `xu` and `xu_total` columns. Replace with a tooltip explaining the legacy currency was retired.
- [ ] **admin-CMS:** in the bulk giftcode upload form, remove the xu column. Existing unredeemed xu giftcodes will auto-convert to vin at redemption (server-side change part of the Java patch).
- [ ] **agency-portal:** sort options "Sort by xu" must be removed; agency listings will only sort by vin going forward.
- [ ] **report dashboards:** any chart that displays xu separately needs a deprecation notice or removal.

---

## Communication template (Vietnamese, for team chat / Jira)

```
Đội FE thân mến,

Backend đang triển khai Phase 3 của RFC hợp nhất ví: gộp xu vào vin theo
tỉ lệ 1:1. Sau ngày triển khai:

  • Tất cả response đang trả về trường `xu` sẽ trả `xu = 0`.
  • Trường này sẽ tồn tại thêm 8 tuần để tương thích, sau đó xoá hẳn.
  • Vui lòng KHÔNG cộng `vin + xu` để tính "tổng số dư" — `vin` là nguồn
    duy nhất.
  • CMS quản trị: vui lòng ẩn radio "vin/xu" ở form nạp tiền tay; backend
    sẽ tự gộp về `vin` và log audit.

Chi tiết: docs/WALLET_PHASE3_FE_COORDINATION.md
RFC: docs/RFC_SINGLE_WALLET_UNIFICATION.md §Phase 3

Cần phối hợp ngày phát hành — vui lòng ack trong tuần này.
```

---

## Risk / fallback

- If a third-party API or partner integration we haven't audited reads `xu`, the 8-week deprecation window gives them time to migrate.
- If a player support workflow depends on "xu as separate balance" (e.g., comping non-withdrawable balance), escalate to PM before Phase 3a runs. Option B (separate PLAYER_PROMO_WALLET) is the alternate plan; see `20260601_phase3b_xu_to_promo_wallet.sql` for the path.
- BalanceGuard (`VbeeCommon/.../response/BalanceGuard.java`) is unaffected — it only guards against `vin_total` leaks, not xu balance.
