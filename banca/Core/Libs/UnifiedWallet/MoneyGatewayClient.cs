// Phase 5b — Wallet unification: BanCa main game loop session-batch settle.
//
// This client speaks HTTP to the Java backend MoneyGateway endpoint (single
// canonical wallet ledger PLAYER_VIN). It is gated by env var
// BANCA_USE_UNIFIED_WALLET — when 0 (default) the legacy Redis
// IncEpicCash code path runs in BanCaServer/GameBanCa unchanged.
//
// Design contract (see docs/WALLET_PHASE5_BANCA_DESIGN.md and
// WALLET_PHASE5B_BANCA_GAME_LOOP_IMPL.md):
//   - Hot path (Player.Cash / Player.Profit arithmetic) is NEVER touched.
//     Per-shot debits and per-kill credits remain pure in-memory.
//   - Settle is invoked at session boundaries: quit, periodic 5s flush,
//     big-bet threshold, openTx/closeTx cross-game, Revive crash recovery.
//   - Idempotency: external_ref encodes (userId, sessionId, checkpoint).
//   - Failure mode: after 3 retries (exponential backoff) the call is
//     dropped to Redis list `banca:failed_settle` so the legacy retry
//     worker can replay it. The caller never blocks on settle.
//
// Env vars consumed:
//   BANCA_USE_UNIFIED_WALLET     0|1 master kill switch (default 0)
//   BANCA_MONEYGATEWAY_URL       full URL of the Java MoneyGateway endpoint
//                                (e.g. http://backend-api:19082/money_gateway)
//   BANCA_SERVICE_TOKEN          shared secret for X-Service-Token
//                                (reused from SUN-1054 LogBetCommission)
//   BANCA_SETTLE_TIMEOUT_MS      HTTP timeout per attempt, default 5000
//   BANCA_SETTLE_MAX_RETRIES     retry count on 5xx/timeout, default 3

using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using BanCa.Redis;
using SimpleJSON;
using StackExchange.Redis;

namespace BanCa.Libs.UnifiedWallet
{
    public static class MoneyGatewayClient
    {
        public const string FAILED_SETTLE_LIST = "banca:failed_settle";

        private static readonly HttpClient _http = BuildHttpClient();

        private static HttpClient BuildHttpClient()
        {
            var timeoutMs = ReadInt("BANCA_SETTLE_TIMEOUT_MS", 5000);
            var c = new HttpClient
            {
                Timeout = TimeSpan.FromMilliseconds(timeoutMs)
            };
            c.DefaultRequestHeaders.ConnectionClose = false;
            return c;
        }

        // Phase 5b master kill switch. Default OFF; when OFF every caller
        // MUST fall back to the legacy Redis IncEpicCash path.
        public static bool IsEnabled()
        {
            var v = Environment.GetEnvironmentVariable("BANCA_USE_UNIFIED_WALLET");
            return v == "1" || string.Equals(v, "true", StringComparison.OrdinalIgnoreCase);
        }

        public static int SettleIntervalMs
        {
            get { return ReadInt("BANCA_SETTLE_INTERVAL_MS", 5000); }
        }

        public static long SettleThreshold
        {
            get { return ReadLong("BANCA_SETTLE_THRESHOLD", 10000); }
        }

        public static long BigBetThreshold
        {
            get { return ReadLong("BANCA_BIG_BET_THRESHOLD", 50000); }
        }

        // Idle-session settle window. When a player has not shot for this
        // many milliseconds and still carries a non-zero Profit, the next
        // settle-tick force-flushes that profit under a `bc-idle-*` session
        // tag so wager history closes cleanly at the silence boundary.
        // Set BANCA_IDLE_SETTLE_MS=0 to disable idle flushing.
        public static long IdleSettleMs
        {
            get { return ReadLong("BANCA_IDLE_SETTLE_MS", 15000); }
        }

        // When ON, the settle-tick ignores SettleThreshold + BigBetThreshold
        // entirely and only fires on idle (or RemovePlayer). Goal: exactly
        // ONE ledger row per play episode, so BanCa history aggregates by
        // session bracket instead of by mid-session flush.
        public static bool SessionOnlySettle
        {
            get
            {
                var v = Environment.GetEnvironmentVariable("BANCA_SESSION_ONLY_SETTLE");
                return v == "1" || string.Equals(v, "true", StringComparison.OrdinalIgnoreCase);
            }
        }

        // Settle a single (userId, profit) pair to MoneyGateway.
        //
        // profit > 0 -> WAGER_CREDIT_BANCA (player won, credit PLAYER_VIN)
        // profit < 0 -> WAGER_DEBIT_BANCA  (player lost, debit PLAYER_VIN)
        // profit = 0 -> no-op, returns true.
        //
        // external_ref = banca:settle:{userId}:{sessionId}:{checkpoint}
        // Re-posting the same external_ref returns success without
        // re-applying the ledger entry (server-side idempotency).
        //
        // Returns true on success, false if the call was dropped to the
        // failed_settle Redis list. The caller MUST NOT mutate
        // Player.Cash on failure — it zeros only on success.
        //
        // Phase 5c follow-up: on success we also refresh the legacy
        // Redis User_Cash:{userId} mirror with the new VND balance returned
        // by c=9998 (converted back to milli-VND to match the legacy units),
        // so cron / admin tools that still read via Redis observe a fresh
        // value instead of last-flag-off-write drift.
        public static async Task<bool> SettleAsync(long userId, long profit, string sessionId, string txType)
        {
            Logger.Info($"[trace] SettleAsync IN userId={userId} amount={profit} sid={sessionId} txType={txType}");
            if (profit == 0) return true;
            if (userId <= 0)
            {
                Logger.Warning("MoneyGatewayClient.SettleAsync: invalid userId=" + userId);
                return false;
            }
            var checkpoint = TimeUtil.TimeStamp;
            var externalRef = BuildExternalRef(userId, sessionId, checkpoint);

            // SUN-13xx Phase 5c unit fix: BanCa Player.Cash and `profit` are in VND.
            // Java c=9998 expects amount_milli in milli-VND (VND * 1000). Without
            // the *1000 a -17500 VND debit was being treated as -17.5 VND, so the
            // ledger barely moved while Player.Cash drifted far ahead.
            long profitMilli = profit * 1000L;
            var payload = new JSONObject();
            payload["user_id"] = userId;
            payload["amount_milli"] = profitMilli;
            payload["amount"] = profit;
            payload["tx_type"] = txType;
            payload["session_id"] = sessionId ?? "";
            payload["external_ref"] = externalRef;
            payload["checkpoint"] = checkpoint;
            payload["checkpoint_ms"] = checkpoint;
            payload["game_key"] = "banca";

            return await PostWithRetry(payload, externalRef, userId);
        }

        // Batch flush used by Revive() crash recovery. Each entry is
        // POSTed individually under its own external_ref so the call is
        // idempotent on retry. We loop sequentially (NOT in parallel)
        // because the backend MoneyGateway serializes by user_id anyway
        // and a parallel storm during revive would only worsen recovery.
        public static async Task BatchSettleAsync(IEnumerable<(long userId, long profit, string sessionId)> batch, string txType)
        {
            if (batch == null) return;
            foreach (var entry in batch)
            {
                try
                {
                    await SettleAsync(entry.userId, entry.profit, entry.sessionId, txType);
                }
                catch (Exception e)
                {
                    Logger.Error("MoneyGatewayClient.BatchSettleAsync entry failed user=" + entry.userId + " err=" + e.Message);
                }
            }
        }

        private static string BuildExternalRef(long userId, string sessionId, long checkpoint)
        {
            return "banca:settle:" + userId + ":" + (sessionId ?? "0") + ":" + checkpoint;
        }

        // Resolves base URL once per call so config reloads are picked up.
        private static string ResolveUrl()
        {
            var fromEnv = Environment.GetEnvironmentVariable("BANCA_MONEYGATEWAY_URL");
            if (!string.IsNullOrEmpty(fromEnv)) return fromEnv;
            // Fallback: derive from xxeng-backend in config.json
            try
            {
                if (Database.ConfigJson.Config != null)
                {
                    var backend = Database.ConfigJson.Config["xxeng-backend"].Value;
                    if (!string.IsNullOrEmpty(backend))
                        return backend.TrimEnd('/') + "/money_gateway";
                }
            }
            catch (Exception)
            {
                // ignore, fall through
            }
            return null;
        }

        private static string ResolveServiceToken()
        {
            return Environment.GetEnvironmentVariable("BANCA_SERVICE_TOKEN");
        }

        private static async Task<bool> PostWithRetry(JSONObject payload, string externalRef, long userIdForMirror = 0)
        {
            var url = ResolveUrl();
            var token = ResolveServiceToken();
            if (string.IsNullOrEmpty(url) || string.IsNullOrEmpty(token))
            {
                Logger.Warning("MoneyGatewayClient: url/token not configured, dropping ref=" + externalRef);
                QueueFailed(payload);
                return false;
            }

            var maxRetries = ReadInt("BANCA_SETTLE_MAX_RETRIES", 3);
            var body = payload.ToString();
            int attempt = 0;
            while (attempt < maxRetries)
            {
                attempt++;
                try
                {
                    using (var req = new HttpRequestMessage(HttpMethod.Post, url))
                    {
                        req.Headers.TryAddWithoutValidation("X-Service-Token", token);
                        req.Content = new StringContent(body, Encoding.UTF8, "application/json");
                        var resp = await _http.SendAsync(req);
                        var status = (int)resp.StatusCode;
                        if (status >= 200 && status < 300)
                        {
                            // Server-side idempotency: same external_ref -> 200
                            // (success body may carry {"deduped":true}). Either way
                            // we treat 2xx as final success. Phase 5c follow-up:
                            // refresh the legacy Redis User_Cash:{id} mirror from
                            // the response so cron/admin tools see fresh values.
                            string balVndForLog = "?";
                            if (userIdForMirror > 0)
                            {
                                try
                                {
                                    var respBody = await resp.Content.ReadAsStringAsync();
                                    try
                                    {
                                        var j = JSON.Parse(respBody);
                                        if (j != null) balVndForLog = j["balance_after_vnd"].Value;
                                    }
                                    catch (Exception) { /* ignore */ }
                                    UpdateRedisMirror(userIdForMirror, respBody);
                                }
                                catch (Exception mirrorEx)
                                {
                                    // Mirror refresh is best-effort. Never fail the settle on it.
                                    Logger.Warning("MoneyGatewayClient: mirror refresh failed userId=" + userIdForMirror + " err=" + mirrorEx.Message);
                                }
                            }
                            Logger.Info($"[trace] SettleAsync OUT userId={userIdForMirror} success=true balance_after_vnd={balVndForLog}");
                            return true;
                        }
                        if (status >= 400 && status < 500)
                        {
                            // 4xx is a hard rejection (validation, idempotency
                            // collision on a DIFFERENT payload). Do not retry.
                            var errBody = await resp.Content.ReadAsStringAsync();
                            Logger.Error("MoneyGatewayClient: 4xx ref=" + externalRef + " status=" + status + " body=" + errBody);
                            QueueFailed(payload);
                            return false;
                        }
                        Logger.Warning("MoneyGatewayClient: retryable status=" + status + " attempt=" + attempt + " ref=" + externalRef);
                    }
                }
                catch (Exception e)
                {
                    Logger.Warning("MoneyGatewayClient: attempt=" + attempt + " ref=" + externalRef + " err=" + e.Message);
                }
                if (attempt < maxRetries)
                {
                    // Exponential backoff: 100ms, 200ms, 400ms ...
                    var delayMs = 100 * (1 << (attempt - 1));
                    await Task.Delay(delayMs);
                }
            }

            Logger.Error("MoneyGatewayClient: dropping to failed_settle after " + maxRetries + " retries ref=" + externalRef);
            Logger.Error($"[trace] SettleAsync FAILED-RETRIES userId={userIdForMirror} amount={payload["amount"].AsLong} sid={payload["session_id"].Value}");
            QueueFailed(payload);
            return false;
        }

        // Phase 5c follow-up — parses balance_after_vnd out of the c=9998
        // response (VND) and writes back to Redis User_Cash:{id} as milli-VND
        // (the legacy units BanCa hot path used to store there).
        //
        // The Redis write is fire-and-forget through RedisManager so a slow
        // Redis cannot block the settle path. Non-2xx / missing-field payloads
        // are silently ignored — they just mean the mirror stays stale
        // (acceptable for cron/admin readers; the unified-wallet path no
        // longer depends on it).
        private static void UpdateRedisMirror(long userId, string respBody)
        {
            if (string.IsNullOrEmpty(respBody)) return;
            try
            {
                var json = JSON.Parse(respBody);
                if (json == null) return;
                if (!json["success"].AsBool) return;
                // c=9998 returns balance_after_vnd; tolerate either field name.
                var balVndNode = json["balance_after_vnd"];
                if (balVndNode == null) return;
                var balVndStr = balVndNode.Value;
                if (string.IsNullOrEmpty(balVndStr)) return;
                if (!long.TryParse(balVndStr, out var balVnd)) return;
                // SUN-13xx: legacy User_Cash:{id} units are VND, not milli-VND.
                // (Original cgame.users.cash held VND directly; BanCa hot path
                // treats Player.Cash as VND.) Write VND verbatim.
                RedisManager.SetUserCash(userId, balVnd);
            }
            catch (Exception e)
            {
                Logger.Warning("MoneyGatewayClient.UpdateRedisMirror parse failed userId=" + userId + " err=" + e.Message);
            }
        }

        // Phase 5c follow-up — query canonical PLAYER_VIN balance via c=9997.
        // Returns balance in milli-VND (matching legacy User_Cash:{id} units)
        // on success, or -1 on any failure. Single attempt, no retry — the
        // caller (GetUserCash) treats -1 as "fall back to legacy Redis".
        //
        // Used by RedisManager.GetUserCash when the unified wallet flag is on.
        // 5s in-memory cache lives in RedisManager (userCashCache) so per-frame
        // balance HUD updates are not a per-call HTTP round-trip.
        public static async Task<long> QueryBalanceMilliAsync(long userId)
        {
            if (userId <= 0) return -1L;

            var url = ResolveQueryUrl(userId);
            var token = ResolveServiceToken();
            if (string.IsNullOrEmpty(url) || string.IsNullOrEmpty(token))
            {
                Logger.Warning("MoneyGatewayClient.QueryBalance: url/token not configured userId=" + userId);
                return -1L;
            }

            try
            {
                using (var req = new HttpRequestMessage(HttpMethod.Get, url))
                {
                    req.Headers.TryAddWithoutValidation("X-Service-Token", token);
                    var resp = await _http.SendAsync(req);
                    var status = (int)resp.StatusCode;
                    var body = await resp.Content.ReadAsStringAsync();
                    if (status < 200 || status >= 300)
                    {
                        Logger.Warning("MoneyGatewayClient.QueryBalance: non-2xx status=" + status + " userId=" + userId + " body=" + body);
                        return -1L;
                    }
                    var json = JSON.Parse(body);
                    if (json == null || !json["success"].AsBool) return -1L;
                    var node = json["vin_balance_vnd"];
                    if (node == null) return -1L;
                    var raw = node.Value;
                    if (string.IsNullOrEmpty(raw)) return -1L;
                    if (!long.TryParse(raw, out var milli)) return -1L;
                    return milli;
                }
            }
            catch (Exception e)
            {
                Logger.Warning("MoneyGatewayClient.QueryBalance threw userId=" + userId + " err=" + e.Message);
                return -1L;
            }
        }

        // Derive c=9997 URL from the c=9998 URL by swapping the command id
        // suffix. Tolerates either ?c=NNNN or no command suffix (in which
        // case it appends ?c=9997).
        private static string ResolveQueryUrl(long userId)
        {
            var settleUrl = ResolveUrl();
            if (string.IsNullOrEmpty(settleUrl)) return null;
            // Settle URL typically ends with .../api_backend?c=9998 or
            // .../money_gateway. Either way we replace the path with the
            // backend-api base and append the query-params for c=9997.
            string baseUrl = settleUrl;
            int qIdx = settleUrl.IndexOf('?');
            if (qIdx > 0) baseUrl = settleUrl.Substring(0, qIdx);
            // Strip a trailing /money_gateway alias back to /api_backend if present.
            if (baseUrl.EndsWith("/money_gateway"))
            {
                baseUrl = baseUrl.Substring(0, baseUrl.Length - "/money_gateway".Length) + "/api_backend";
            }
            // BanCa passes its own cgame.users.user_id; Java resolves via
            // nickname-join (the cgame.vinplay_user_id FK and cgame.user_id-as-
            // vinplay-id are both unreliable for legacy accounts).
            return baseUrl + "?c=9997&cgame_user_id=" + userId;
        }

        // Async fire-and-forget push into the failed-settle Redis list.
        // A separate worker (out of scope for this phase) replays
        // entries against MoneyGateway with the same external_ref.
        private static void QueueFailed(JSONObject payload)
        {
            try
            {
                var redis = RedisManager.GetRedis();
                redis.ListRightPush(FAILED_SETTLE_LIST, payload.ToString(), flags: CommandFlags.FireAndForget);
            }
            catch (Exception e)
            {
                Logger.Error("MoneyGatewayClient: failed to enqueue failed_settle err=" + e.Message);
            }
        }

        private static int ReadInt(string name, int dflt)
        {
            var v = Environment.GetEnvironmentVariable(name);
            if (string.IsNullOrEmpty(v)) return dflt;
            return int.TryParse(v, out var parsed) ? parsed : dflt;
        }

        private static long ReadLong(string name, long dflt)
        {
            var v = Environment.GetEnvironmentVariable(name);
            if (string.IsNullOrEmpty(v)) return dflt;
            return long.TryParse(v, out var parsed) ? parsed : dflt;
        }
    }
}
