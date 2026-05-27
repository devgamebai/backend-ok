// Phase 5c follow-up — Unit tests for the Redis-sync MoneyGatewayClient
// extensions (QueryBalance + Redis mirror refresh on settle).
//
// Same stand-in pattern as MoneyGatewayClientTests: we mirror the new
// behaviour with a minimal HTTP probe so the test does not need to pull
// in the entire Core project (heavy native deps + websocket server).
// The production code under test lives in
// banca/Core/Libs/UnifiedWallet/MoneyGatewayClient.cs and
// banca/Core/Libs/Database/RedisManager.cs — both MUST stay in lockstep
// with this stand-in (and the c=9997 / c=9998 contract).
//
// Test surface:
//   1. QueryBalance — calls HTTP GET, parses vin_balance milli-VND.
//   2. Settle 2xx — both the settle POST is observed AND the response
//      balance_after_vnd is written back to the Redis mirror (here
//      modelled as an in-memory dictionary the stand-in updates).

using System;
using System.Collections.Concurrent;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Xunit;

namespace BanCa.UnifiedWalletTests
{
    public class Phase5cRedisSyncTests : IDisposable
    {
        private readonly HttpListener _listener;
        private readonly string _baseUrl;
        private Func<HttpListenerContext, Task> _handler;
        private readonly CancellationTokenSource _cts = new CancellationTokenSource();
        private readonly ConcurrentDictionary<long, long> _redisMirror = new ConcurrentDictionary<long, long>();

        public Phase5cRedisSyncTests()
        {
            var port = FindFreePort();
            _baseUrl = "http://127.0.0.1:" + port + "/";
            _listener = new HttpListener();
            _listener.Prefixes.Add(_baseUrl);
            _listener.Start();
            _ = Task.Run(Loop);
        }

        private async Task Loop()
        {
            while (!_cts.IsCancellationRequested)
            {
                HttpListenerContext ctx;
                try { ctx = await _listener.GetContextAsync(); }
                catch { return; }
                try { if (_handler != null) await _handler(ctx); }
                catch { /* test cleanup race */ }
            }
        }

        public void Dispose()
        {
            _cts.Cancel();
            try { _listener.Stop(); } catch { }
            try { _listener.Close(); } catch { }
            Environment.SetEnvironmentVariable("BANCA_MONEYGATEWAY_URL", null);
            Environment.SetEnvironmentVariable("BANCA_SERVICE_TOKEN", null);
            Environment.SetEnvironmentVariable("BANCA_USE_UNIFIED_WALLET", null);
        }

        private static int FindFreePort()
        {
            var l = new System.Net.Sockets.TcpListener(IPAddress.Loopback, 0);
            l.Start();
            var port = ((IPEndPoint)l.LocalEndpoint).Port;
            l.Stop();
            return port;
        }

        // --- stand-in for QueryBalanceMilliAsync (mirrors production behaviour) ---
        private static async Task<long> QueryBalanceMilli(string url, string token, long userId)
        {
            using (var http = new HttpClient { Timeout = TimeSpan.FromMilliseconds(2000) })
            {
                using (var req = new HttpRequestMessage(HttpMethod.Get, url + "?c=9997&user_id=" + userId))
                {
                    req.Headers.TryAddWithoutValidation("X-Service-Token", token);
                    var resp = await http.SendAsync(req);
                    if (!resp.IsSuccessStatusCode) return -1L;
                    var body = await resp.Content.ReadAsStringAsync();
                    // Crude parse mirroring SimpleJSON in production
                    var key = "\"vin_balance\":";
                    var idx = body.IndexOf(key);
                    if (idx < 0) return -1L;
                    idx += key.Length;
                    var end = idx;
                    while (end < body.Length && (char.IsDigit(body[end]) || body[end] == '-')) end++;
                    if (end == idx) return -1L;
                    if (!long.TryParse(body.Substring(idx, end - idx), out var milli)) return -1L;
                    return milli;
                }
            }
        }

        // --- stand-in for SettleAsync + Redis mirror refresh ---
        private async Task<bool> SettleAndMirror(string url, string token, long userId, long profit, string sessionId)
        {
            using (var http = new HttpClient { Timeout = TimeSpan.FromMilliseconds(2000) })
            {
                var externalRef = "banca:settle:" + userId + ":" + sessionId + ":1";
                var payload = "{\"user_id\":" + userId + ",\"amount_milli\":" + profit + ",\"external_ref\":\"" + externalRef + "\"}";
                using (var req = new HttpRequestMessage(HttpMethod.Post, url))
                {
                    req.Headers.TryAddWithoutValidation("X-Service-Token", token);
                    req.Content = new StringContent(payload, Encoding.UTF8, "application/json");
                    var resp = await http.SendAsync(req);
                    if (!resp.IsSuccessStatusCode) return false;
                    var body = await resp.Content.ReadAsStringAsync();
                    // Mirror the production UpdateRedisMirror parse — pluck
                    // balance_after_vnd, write VND*1000 to our dictionary.
                    var key = "\"balance_after_vnd\":";
                    var idx = body.IndexOf(key);
                    if (idx >= 0)
                    {
                        idx += key.Length;
                        var end = idx;
                        while (end < body.Length && (char.IsDigit(body[end]) || body[end] == '-')) end++;
                        if (end > idx && long.TryParse(body.Substring(idx, end - idx), out var balVnd))
                        {
                            _redisMirror[userId] = balVnd * 1000L;
                        }
                    }
                    return true;
                }
            }
        }

        // ---- Test 1: QueryBalance returns vin_balance from c=9997 ------------

        [Fact]
        public async Task QueryBalance_returns_milli_from_response()
        {
            _handler = ctx =>
            {
                Assert.Equal("test-token", ctx.Request.Headers["X-Service-Token"]);
                // vin_balance = 1.500 VND × 1000 = 1_500_000_000 milli
                var resp = Encoding.UTF8.GetBytes("{\"success\":true,\"vin_balance\":1500000000,\"vin_balance_vnd\":1500000}");
                ctx.Response.StatusCode = 200;
                ctx.Response.ContentType = "application/json";
                ctx.Response.OutputStream.Write(resp, 0, resp.Length);
                ctx.Response.Close();
                return Task.CompletedTask;
            };

            var milli = await QueryBalanceMilli(_baseUrl, "test-token", 42);
            Assert.Equal(1500000000L, milli);
        }

        [Fact]
        public async Task QueryBalance_missing_field_returns_negative_one()
        {
            _handler = ctx =>
            {
                var resp = Encoding.UTF8.GetBytes("{\"success\":false,\"errorCode\":\"1002\"}");
                ctx.Response.StatusCode = 200;
                ctx.Response.OutputStream.Write(resp, 0, resp.Length);
                ctx.Response.Close();
                return Task.CompletedTask;
            };

            var milli = await QueryBalanceMilli(_baseUrl, "test-token", 99);
            Assert.Equal(-1L, milli);
        }

        // ---- Test 2: Settle 2xx -> Redis mirror updated with balance_after_vnd ----

        [Fact]
        public async Task Settle_2xx_updates_redis_mirror()
        {
            _handler = async ctx =>
            {
                var body = await new StreamReader(ctx.Request.InputStream).ReadToEndAsync();
                Assert.Contains("user_id", body);
                Assert.Contains("amount_milli", body);
                // balance_after_vnd = 2_000_000 VND  ->  mirror should be 2_000_000_000 milli
                var resp = Encoding.UTF8.GetBytes("{\"success\":true,\"ledger_tx_id\":0,\"balance_after_vnd\":2000000,\"reason\":\"posted\"}");
                ctx.Response.StatusCode = 200;
                ctx.Response.OutputStream.Write(resp, 0, resp.Length);
                ctx.Response.Close();
            };

            var ok = await SettleAndMirror(_baseUrl, "test-token", userId: 42, profit: -870000, sessionId: "bc-42-7");
            Assert.True(ok);
            Assert.True(_redisMirror.TryGetValue(42, out var milli));
            Assert.Equal(2000000000L, milli);
        }
    }
}
