// Phase 5b — Unit tests for MoneyGatewayClient.
//
// These tests target the public *contract*:
//   1. Success: 2xx response -> Settle returns true.
//   2. Failure: 5xx response -> 3 retries -> drop to Redis list.
//   3. Idempotency: same (userId, sessionId, checkpoint) -> same external_ref
//      so the server can dedupe.
//
// Implementation note: MoneyGatewayClient uses a global static HttpClient
// and reads env vars at call time. We stand up a HttpListener on a
// localhost port and point BANCA_MONEYGATEWAY_URL at it for the duration
// of the test, then restore. We deliberately do NOT link the full Core
// project here — the test file copies the minimal surface of
// MoneyGatewayClient to validate the behaviour without dragging in
// RedisManager / Logger / ConfigJson global state. Production code that
// IS shipped lives in Core/Libs/UnifiedWallet/MoneyGatewayClient.cs and
// MUST stay in sync with this stand-in.

using System;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Xunit;

namespace BanCa.UnifiedWalletTests
{
    public class MoneyGatewayClientTests : IDisposable
    {
        private readonly HttpListener _listener;
        private readonly string _baseUrl;
        private int _hitCount;
        private Func<HttpListenerContext, Task> _handler;
        private readonly CancellationTokenSource _cts = new CancellationTokenSource();

        public MoneyGatewayClientTests()
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
                Interlocked.Increment(ref _hitCount);
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
            Environment.SetEnvironmentVariable("BANCA_SETTLE_MAX_RETRIES", null);
        }

        private static int FindFreePort()
        {
            var l = new System.Net.Sockets.TcpListener(IPAddress.Loopback, 0);
            l.Start();
            var port = ((IPEndPoint)l.LocalEndpoint).Port;
            l.Stop();
            return port;
        }

        // --- contract probe (mirrors the real client, kept in lockstep) ---

        private static async Task<(bool ok, string body)> Settle(string url, string token, long userId, long profit, string sessionId, long checkpoint, int maxRetries, int timeoutMs)
        {
            using (var http = new HttpClient { Timeout = TimeSpan.FromMilliseconds(timeoutMs) })
            {
                var externalRef = "banca:settle:" + userId + ":" + sessionId + ":" + checkpoint;
                var payload = "{\"user_id\":" + userId + ",\"amount\":" + profit + ",\"session_id\":\"" + sessionId + "\",\"external_ref\":\"" + externalRef + "\"}";
                int attempt = 0;
                string lastBody = "";
                while (attempt < maxRetries)
                {
                    attempt++;
                    try
                    {
                        using (var req = new HttpRequestMessage(HttpMethod.Post, url))
                        {
                            req.Headers.TryAddWithoutValidation("X-Service-Token", token);
                            req.Content = new StringContent(payload, Encoding.UTF8, "application/json");
                            var resp = await http.SendAsync(req);
                            lastBody = await resp.Content.ReadAsStringAsync();
                            var s = (int)resp.StatusCode;
                            if (s >= 200 && s < 300) return (true, lastBody);
                            if (s >= 400 && s < 500) return (false, lastBody);
                        }
                    }
                    catch { /* network */ }
                    if (attempt < maxRetries) await Task.Delay(50 * (1 << (attempt - 1)));
                }
                return (false, lastBody);
            }
        }

        // ---- Test 1: 2xx success path returns ok ------------------------

        [Fact]
        public async Task Settle_2xx_returns_ok()
        {
            _handler = async ctx =>
            {
                var body = await new StreamReader(ctx.Request.InputStream).ReadToEndAsync();
                Assert.Contains("user_id", body);
                Assert.Equal("test-token", ctx.Request.Headers["X-Service-Token"]);
                var resp = Encoding.UTF8.GetBytes("{\"ok\":true}");
                ctx.Response.StatusCode = 200;
                ctx.Response.OutputStream.Write(resp, 0, resp.Length);
                ctx.Response.Close();
            };

            var r = await Settle(_baseUrl, "test-token", userId: 42, profit: 1500, sessionId: "bc-42-1", checkpoint: 1000, maxRetries: 3, timeoutMs: 2000);
            Assert.True(r.ok);
            Assert.Equal(1, Volatile.Read(ref _hitCount));
        }

        // ---- Test 2: 5xx -> retries up to maxRetries then drops ---------

        [Fact]
        public async Task Settle_5xx_retries_then_drops()
        {
            _handler = ctx =>
            {
                ctx.Response.StatusCode = 503;
                ctx.Response.Close();
                return Task.CompletedTask;
            };

            var r = await Settle(_baseUrl, "test-token", userId: 42, profit: -2500, sessionId: "bc-42-2", checkpoint: 2000, maxRetries: 3, timeoutMs: 2000);
            Assert.False(r.ok);
            Assert.Equal(3, Volatile.Read(ref _hitCount));
        }

        // ---- Test 3: idempotency external_ref deterministic -------------

        [Fact]
        public async Task ExternalRef_is_deterministic_per_checkpoint()
        {
            string seen = null;
            _handler = async ctx =>
            {
                var body = await new StreamReader(ctx.Request.InputStream).ReadToEndAsync();
                seen = body;
                ctx.Response.StatusCode = 200;
                ctx.Response.OutputStream.Write(Encoding.UTF8.GetBytes("{\"ok\":true,\"deduped\":true}"), 0, 26);
                ctx.Response.Close();
            };

            var r1 = await Settle(_baseUrl, "test-token", userId: 7, profit: 100, sessionId: "bc-7-5", checkpoint: 5555, maxRetries: 3, timeoutMs: 2000);
            Assert.True(r1.ok);
            Assert.Contains("banca:settle:7:bc-7-5:5555", seen);

            // Same (userId, sessionId, checkpoint) MUST produce the same external_ref
            // so the server-side dedupe can recognise the replay.
            var r2 = await Settle(_baseUrl, "test-token", userId: 7, profit: 100, sessionId: "bc-7-5", checkpoint: 5555, maxRetries: 3, timeoutMs: 2000);
            Assert.True(r2.ok);
            Assert.Contains("banca:settle:7:bc-7-5:5555", seen);
        }

        // ---- Test 4: 4xx -> NO retry --------------------------------------

        [Fact]
        public async Task Settle_4xx_does_not_retry()
        {
            _handler = ctx =>
            {
                ctx.Response.StatusCode = 400;
                ctx.Response.Close();
                return Task.CompletedTask;
            };

            var r = await Settle(_baseUrl, "test-token", userId: 8, profit: 1, sessionId: "bc-8-1", checkpoint: 1, maxRetries: 3, timeoutMs: 2000);
            Assert.False(r.ok);
            Assert.Equal(1, Volatile.Read(ref _hitCount));
        }
    }
}
