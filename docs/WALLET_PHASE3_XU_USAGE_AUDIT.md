# WALLET PHASE 3 — Full `xu` / `xu_total` Usage Audit

**Phase:** 3 (Retire `xu` / `xu_total`)
**RFC:** [RFC_SINGLE_WALLET_UNIFICATION.md §6 Phase 3](RFC_SINGLE_WALLET_UNIFICATION.md) + [V2 Addendum §M7](RFC_SINGLE_WALLET_UNIFICATION_V2_ADDENDUM.md)
**Created:** 2026-05-11
**Methodology:** `Grep` across `backend-master/**.java`, `www/**.php`, plus thematic searches for `getXu/setXu/MONEY_XU/"xu"/moneyType` patterns.
**Status of upstream Phase 3 prerequisite:** PROMO_POOL and LEGACY_RECONCILIATION account_types already seeded (`2026_05_02b_money_ledger_seed_system.sql`, `2026_05_02d_money_ledger_backfill_initial_balances.sql`). `PLAYER_XU` ledger account exists per-user (`2026_05_02c_money_ledger_seed_users.sql`).

---

## 1. Headline numbers

| Metric | Value |
|---|---|
| Java files referencing `xu`/`xu_total`/`getXu`/`setXu`/`MONEY_XU` | **66 unique files** |
| Java callsite lines | **~280 lines** (counted across 2 grep dumps, deduplicated by file:line) |
| PHP files referencing xu | **0** (in `www/agency-php/`, `www/webhook/`, `www/webbuild/`) |
| C# (BanCa) files referencing xu | **0** (BanCa uses `cgame.users.cash*` only) |
| Active promo / test feature dependent solely on xu | **None observed** — gift codes can credit either vin or xu; minigame rooms with `moneyTypeStr="xu"` are user-selectable bet currency |
| Distinct callsite classes (read/write/display/logic) | 4 (see §3) |

The number "34 occurrences across 20+ files" from the critic audit was an undercount — the real surface area is ~3× larger once cache models (`UserMoneyModel`, `UserCacheModel`), test scaffolds (`MoneyGatewayDualWriteTest`), and bot helpers are included.

---

## 2. Master inventory by module

### 2.1 `VbeeCommon` — shared types & constants (highest-fanout)

These are the **type-level definitions**. Removing or stubbing them collapses every downstream caller.

| File | Lines | Class |
|---|---|---|
| `statics/DBFields.java` | 19 — `XU = "xu"` | Type/constant |
| `statics/Consts.java` | 274 — `MONEY_XU = "xu"` | Type/constant |
| `statics/HttpParams.java` | 56, 88 — `MONEY_XU`, `XU` | Type/constant |
| `models/UserModel.java` | 24, 63, 67, 80, 298–303, 410–414, 419, 426, 443, 458 | Read+Write (POJO) |
| `models/UserClientInfo.java` | 105–109 | Read+Write (DTO) |
| `models/UserAdminInfo.java` | 148–152 | Read+Write (admin DTO) |
| `models/cache/UserMoneyModel.java` | 16, 25, 31, 84–89, 100–104, 149–170 | Hazelcast cache POJO |
| `models/cache/UserCacheModel.java` | 62, 65–66 | Hazelcast cache constructor |
| `utils/UserUtil.java` | 33, 121, 129 | Maps ResultSet → UserMoneyModel |
| `response/BalanceGuard.java` | 10, 16, 57, 58 | Guard comment refs `xu_total` |
| `ledger/BalanceService.java` | 65, 131 — maps `"xu" → "PLAYER_XU"` | Mapping |

### 2.2 `VinPlayDAL` — gateway + DAOs

| File | Lines | Class |
|---|---|---|
| `service/MoneyGateway.java` | 350, 365, 366, 1324, 1330, 1344, 1358, 1606 | Write (atomic update + dual-write) |
| `dao/impl/UserDaoImpl.java` (DAL) | persists `xu`/`xu_total` in INSERT/UPDATE SP calls | Write |
| `dao/impl/AgentDAOImpl.java` | 1357 — `user.setXu(rs.getLong("xu"))` | Read |
| `dao/impl/LogMoneyUserDaoImpl.java` | 104, 157 — branches on `moneyType.equals("xu")` to pick `xu_total` column | Logic |
| `dao/impl/TaiXiuSicboDAOImpl.java` | references `xu` in moneyType branching | Logic |
| `dichvuthe/service/impl/RechargeServiceImpl.java` | xu recharge path | Write |
| `payment/utils/PayCommon.java` | only Vietnamese-text strings ("Dang xu ly") — **NOT a column ref**, false positive | – |
| `dichvuthe/service/impl/CashOutServiceImpl.java` | only Vietnamese-text strings — **false positive** | – |
| `test/.../MoneyGatewayDualWriteTest.java` | 242, 1230, 1262–1307, 1451–1515 — full coverage of xu credit/debit/dual-write | Test |

### 2.3 `VinPlayUserCore` — service + DAO

| File | Lines | Class |
|---|---|---|
| `service/impl/UserServiceImpl.java` | 331, 415, 710, 782, 873–910 | Write (admin recharge, xu add/debit, restore) |
| `service/impl/UserMissionServiceImpl.java` | 96–450 — initializes mission lists keyed by `"xu"`, reads `MATCH_MAX_XU` | Read+Logic |
| `service/impl/LuckyServiceImpl.java` | 159–160 — VQMM lucky-wheel xu prize path | Write |
| `dao/impl/UserDaoImpl.java` (user-core) | `xu`/`xu_total` in DB SP call (legacy `update_money_db`) | Write |
| `dao/impl/GiftCodeDAOImpl.java` (both v1 and `com.gamebase`) | 294, 313, 317, 1612 (v1); 313, 338, 1945 (gamebase): `moneyType = "xu"` for xu-denominated giftcodes | Write |
| `dao/impl/GiftCodeAgentDaoImpl.java` | giftcode flow uses moneyType selector | Write |
| `utils/LuckyUtils.java` | 80 — extracts `xu` JSON sub-object from prize config | Read |
| `dichvuthe/service/impl/CashOutServiceImpl.java` | xu cashout fallthrough | Write |
| `vbee/common/models/UserLive.java` | 82–86 — `getXuTotal/setXuTotal` on a live-stream user pojo | Read+Write |

### 2.4 `CardCoreLib` + `BitzeroMini` — framework

| File | Lines | Class |
|---|---|---|
| `entities/Constant.java` | 7, 9 — `MONEY_TYPE_XU = 0`, `MONEY_NAME_XU = "xu"` | Constant |
| `gameRoom/entities/GameMoneyInfo.java` | 43, 101 — `moneyType==1 ? "vin" : "xu"` | Logic (room currency selector) |
| `gameRoom/entities/GameRoomManager.java` | 288, 334 — same selector pattern | Logic |
| `bitzero/util/ExtensionUtility.java` | shared util uses moneyType selector | Logic |

### 2.5 `api/VinPlayBackend` — admin API processors (CMS)

Pure display + admin update paths. **No game-money mutation happens here outside `updateMoney*Processor`s.**

| Processor (file) | Line(s) | Class |
|---|---|---|
| `user/CreateUserProcessor.java` | 121 — inserts `xu, xu_total` columns at signup | Write |
| `user/ListUsersProcessor.java` | 174, 209, 211 — admin user list with xu+xu_total | Display |
| `user/GetUserDetailProcessor.java` | 70, 72 | Display |
| `user/DeleteUserAccountProcessor.java` | 23, 70, 77, 84 — pre-delete balance check (`vin==0 AND xu==0`) | Read+Logic |
| `user/PreviewDeleteUserAccountProcessor.java` | 52, 60 | Display |
| `agent/DetailUserOfAgentProcessor.java` | 36, 50 | Display |
| `agent/DetailMemberOfAgencyProcessor.java` | 282, 302, 350, 363, 390 | Display |
| `agent/ListUserOfAgentProcessor.java` | 76, 92 | Display |
| `agent/ListAgentProcessor.java` | 85 | Display |
| `agent/ListChildAgentProcessor.java` | 104, 121 | Display |
| `agent/ListMemberOfAgentProcessor.java` | 102, 119 | Display |
| `agent/GetAgencyWalletProcessor.java` | 88 — agency main-wallet xu balance | Display |
| `agent/SearchLogMoney4AgencyProcessor.java` | 57, 194 — `vn=vin|xu` query param + response field | Read+Display |
| `agentcode/ListAllAgentsUnderAgentProcessor.java` | 217, 218, 260, 307 — sortable + display | Read+Display |
| `agentcode/ListAllPlayersUnderAgentProcessor.java` | 52, 105, 127 | Read+Display |
| `admin/SearchUsersProcessor.java` | 51 | Display |
| `admin/UserGameListProcessor.java` | 25 — sort whitelist contains `"xu"` | Read |
| `admin/BulkLoadCacheProcessor.java` | 55 — bulk cache warm reads `xu` | Read |
| `admin/QuickRegisterProcessor.java` | inserts xu | Write |
| `money/UpdateMoneyUserProcessor.java` | 55, 127 (`// xu → legacy path`) — admin topup/deduct allows `moneyType=xu` | Write |
| `money/UpdateMoneyUserWithSmsProcessor.java` | 98 — same admin op | Write |
| `money/UpdateMoneyListUserProcessor.java` | 59 — same admin op, bulk | Write |
| `taixiu/GetLiveBetListProcessor.java` | 36, 181 — live TaiXiu bet listing has `money_type=2` (xu) branch reading `um.getXu()` | Display |
| `userMission/ResetUserMissionTask.java` | 39, 56 | Logic |
| `userMission/UpdateMatchWinMissionProcessor.java` | 48 | Logic |
| `report/TopCaoThuProcessor.java` | 33 | Read |
| `UploadGiftCodeProcessor.java` | 26 — admin uploads bulk giftcode with both `vin` & `xu` amounts | Write |

### 2.6 `api/VinPlayPortal` — player API + WS

| File | Lines | Class |
|---|---|---|
| `processors/GetBalanceProcessor.java` | 51 — `response.put("xu", user.getXu())` — **player-visible balance field** | Display |
| `processors/bot/CreateBotProcessor.java` | 33 — bot creation accepts xu seed | Write (bot only) |
| `processors/gamebai/TopCaoThuProcessor.java` | 34 | Read |
| `processors/minigame/HistorySicBoProcessor.java` | money_type=xu branch | Read |
| `processors/LoginTokenProcessor.java` | login cache load reads xu | Read |
| `ws/BalanceWebSocketServlet.java` | 177, 180, 185, 188 — **real-time balance push includes xu field** | Display (WS) |
| `ws/PortalBalanceConsumer.java` | 85–147 — same as above (RMQ consumer side) | Display (WS) |
| `utils/PortalUtils.java` | xu in admin/portal helpers | Read |

### 2.7 `api/vbee` — RabbitMQ log consumer

| File | Lines | Class |
|---|---|---|
| `rmq/log/processor/LogMoneyUserProcessor.java` | 37 — `if message.getMoneyType().equals("xu")` | Logic |
| `dao/impl/LogMoneyUserDaoImpl.java` | 55, 133 — picks `xu_total` column when moneyType=xu | Write |
| `dao/impl/UserDaoImpl.java` (vbee) | persists xu via SP | Write |
| `main/VBeeMain.java` | 60 — bootstrap reads last xu reference id | Read |

### 2.8 `game/*` — 17 game servers (read-only side; bets mutate via Java service)

All these are **bet-currency selectors**, not direct ledger writers. They pick `"vin"` or `"xu"` and pass it through to `UserService.updateMoney(...)` upstream. Removing xu collapses them to vin-only.

| File | Lines | Notes |
|---|---|---|
| `game/Minigame/**/room/MGRoom*.java` | TaiXiu, TaiXiuMD5, Sicbo, Candy, BauCua, Galaxy, CaoThap, MiniPoker, PokeGo, SlotExtend — all hardcode `moneyTypeStr="xu"` on some rooms (xu-denominated minigames) | Logic |
| `game/Minigame/**/{TaiXiuUtils,BauCuaModule,CaoThapModule,PokeGoModule,MiniPokerModule,Slot3x3ExtendModule,CandyModule,GalaxyModule}.java` | xu branches | Logic |
| `game/Minigame/**/entities/BotMinigame.java` | 560 — bots play xu rooms | Logic |
| `game/Minigame/src/main/java/game/modules/mission/MissionModule.java` | 69 | Logic |
| `game/Minigame/src/main/java/game/entities/Constant.java` | 7, 9 | Constant |
| `game/slot/**/Module.java` (Bikini, Spartan, ThanBai, ThanDen, ChiemTinh, ThanTai, RollRoy, TamHung, MayBach, ThuyCung, NuDiepVien, KhoBau, Audition, RangeRover, DragonBall, Avenger, Benley, Galaxy, VQV, Candy) | `moneyTypeStr="xu"` per-module | Logic |
| `game/slot/**/Room.java` (RollRoy, RangeRover, VQV, Audition, Slot, TamHung, KhoBau) | `moneyType==1?"vin":"xu"` selector | Logic |
| `game/slot/src/main/java/game/modules/slot/utils/Constant.java` | 7, 9 | Constant |
| `game/xocdia/server/XocDiaGameServer.java` | 251 | Logic |
| `game/binh/server/BinhGameServer.java` | 901 | Logic |
| `service/impl/BotServiceImpl.java` (DAL) | seeds bots with xu | Write |

### 2.9 Bots (`BotServiceImpl`, `BotMinigame`)

Bot accounts can be auto-funded with xu. Migration must **skip bots** (per RFC instruction — "for every non-bot user with `xu > 0`").

---

## 3. Callsite classification

| Class | Count (approx) | Risk |
|---|---|---|
| **Type/constant definitions** | 8 sites (VbeeCommon constants + CardCoreLib + slot Constants) | Low — change once, ripple compiles |
| **Pure read/display** (admin CMS user list, balance push) | ~55 sites | Low — returning 0 is safe |
| **Business-logic branching** on moneyType=xu (game-room currency selector, mission, log message routing) | ~80 sites | Medium — collapses to vin-only branch |
| **Write paths** (admin topup, giftcode credit, lucky wheel, recharge, mission reward, restore-balance) | ~30 sites | High — must route to vin path after Option A |

---

## 4. User-facing distinguishing flows

These are the only places where the player or admin can **see xu separately from vin**:

1. **Player balance — `c=GetBalance` portal API** (`GetBalanceProcessor.java:51`). Returns `vin` AND `xu`. FE renders both in lobby.
2. **Player balance WebSocket push** (`BalanceWebSocketServlet.java` + `PortalBalanceConsumer.java`). Real-time updates include `xu`.
3. **Game-room currency tab** (multiple slot/minigame rooms). UI shows two tabs: "Vin" / "Xu". Each room is configured with one currency. Switching collapses to 1 tab.
4. **Admin CMS user-detail panel** (`GetUserDetailProcessor.java`, `ListUsersProcessor`, agency processors). Shows `xu`, `xu_total` columns.
5. **Admin "Add money" form** (`UpdateMoneyUserProcessor.java` + variants). Admin can choose vin or xu when crediting/debiting.
6. **Top player ranking** (`TopCaoThuProcessor.java` — both portal and backend). Has `moneyType=xu` ranking variant.
7. **TaiXiu live-bet listing** (`GetLiveBetListProcessor.java:36, 181`). Has `money_type=2 → xu` filter.
8. **Giftcode redemption** (`GiftCodeDAOImpl.java`). A giftcode can carry an xu denomination.
9. **VQMM lucky wheel** (`LuckyServiceImpl.java:159–160`). Some wheel prizes are denominated in xu.
10. **User-mission rewards** (`UserMissionServiceImpl.java`). Mission bonuses can be xu.

---

## 5. Active promo / test features dependent on xu?

**No active promo with xu-only sourcing was found.**

What we did find:
- **Giftcodes**: support both `vin` and `xu` denominations (admin selects). Production data needed to confirm whether any active campaigns use xu (DB query: `SELECT COUNT(*) FROM gift_code WHERE money_type='xu' AND status='active'`). Defer to PM.
- **xu minigame rooms** (TaiXiu xu room, Sicbo xu room, slot xu rooms): present but **player-selectable** — not gated by a promo flag.
- **VQMM lucky wheel xu prizes**: configurable; if any prize entry is xu-denominated, Option A converts those to vin 1:1 going forward.
- **User missions**: `MATCH_MAX_XU` config key gates max-bet xu missions. Option A retires this branch.

There is **no `XU_ONLY_PROMO` / `XU_TEST_BUCKET` / `XU_BETA_FLAG`** in code. xu is a generic legacy second currency, not a live A/B test.

---

## 6. Phase-3 blockers / call-out for PM

1. **FE must coordinate before Java code returns `xu=0`** (see `WALLET_PHASE3_FE_COORDINATION.md`).
2. **Admin CMS forms** still let staff submit `moneyType=xu` — `UpdateMoneyUserProcessor` accepts it. Option A: server collapses xu→vin at request time and logs to audit; Option B: keep separate.
3. **Giftcode bulk-upload** lets admins distribute xu-denominated codes. Option A: those codes must be invalidated or auto-converted to vin at redemption. Option B: keep working unchanged.
4. **`MoneyGatewayDualWriteTest`** has ~10 xu test cases (`MoneyGatewayDualWriteTest.java` lines 1262–1515). They must either be **deleted** (Option A) or **kept and pointed at PLAYER_PROMO_WALLET** (Option B). Decision tied to PM choice.
5. **Bots are exempted from the migration** per RFC — the SQL `WHERE is_bot = 0` filter is critical to avoid synthetically inflating PLAYER_WALLET.
6. **`vin_total`/`xu_total` writes** happen via SP `update_money_db` AND via direct `creditUserWithCumulative` UPDATE in `MoneyGateway.java:384`. The Phase 4 drop migration of `xu_total` depends on these write paths being killed first — Phase 3 Java patch (see `MoneyGateway.java` deliverable) handles MoneyGateway; SP killing is Phase 4's job (already RFC'd).

---

## 7. Counts summary (for cap-at-500-words executive report)

- **66 Java files**, **~280 callsite lines**, **0 PHP files**, **0 C# files** touch `xu` / `xu_total` / `getXu` / `setXu`.
- **17 of 17 game servers** distinguish xu from vin via `moneyType` selector (low-risk; collapses to vin under Option A).
- **0 active promo or A/B-test feature** depends exclusively on xu.
- **No business-critical xu-only flow** blocks Option A.

---

## 8. Recommendation

**Option A (collapse xu → vin at 1:1, sourced from PROMO_POOL per V2 §M7).**

Justification:
- No code path requires xu/vin segregation for business reasons (regulatory, ringfencing, promo accounting).
- Eliminates 80+ branch points (`moneyType==1?"vin":"xu"`) immediately — every game server, mission, lucky wheel, recharge, gift-code, admin-update path simplifies.
- Drops 2 columns + 1 SP branch.
- PROMO_POOL sourcing means House P&L correctly absorbs the cost of converting promotional balance (the V2 §M7 fix).
- Option B's `PLAYER_PROMO_WALLET` adds a third per-player account that nothing else needs — pure complexity tax.

Option B is justified **only if** PM mandates that promotional balance must remain visibly segregated (e.g., for regulatory or future "withdrawable vs. non-withdrawable" semantics). Code-base evidence finds no such requirement today.
