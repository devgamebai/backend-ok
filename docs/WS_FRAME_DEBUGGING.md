# Sunwinkr — WebSocket Frame Debugging Guide

**Audience:** FE devs, ops, QA tester debugging a binary game session.
**Updated:** 2026-05-08
**Scope:** the BitZero binary protocol used by every minigame WS endpoint
(`wmini`, `wsicbo`, `wslot`, `wpoker`, `wbinh`, `wlieng`, `wsam`, `wxizach`,
`wbaicao`, `wbacay`, `wmini2`, `overunder`, `iportal`) plus the browser-bridge
JSON wrapping done by `ws-bridge`.

The challenge: Cocos Creator clients send/receive raw binary frames over wss.
Chrome's Network → Frames panel only shows hex, which is useless for
correlating cmd-ids and field values to a bug like "balance not updating
after bet". This doc lists every workable way to read what's on the wire,
ordered by effort.

---

## 1. Frame layout cheat sheet

The browser-side cocos client uses one of two envelopes:

```
RAW (most game cmds, after handshake):
  [ctrl:1][cmd:2 BE][payload:N]

WRAPPED (lobby + bridge JSON path):
  [0x90][bodyLen:2 BE][ctrl:1][cmd:2 BE][payload:N]
```

So byte offsets to extract `cmd` are:

```
offset(cmd) = (firstByte === 0x90) ? 4 : 1
cmd = (buf[offset] << 8) | buf[offset+1]
```

Common SicBo / TaiXiu / Sicbo MD5 cmd ids:

| Cmd | Direction | Meaning |
|---|---|---|
| 2000 / 22000 / 28000 | client→server | subscribe (gameId int16 + roomId int16) |
| 2001 / 22001 / 28001 | client→server | unsubscribe |
| 2110 / 22110 / 28110 | both | bet / bet-response (`BetSicboCmd` / `BetSicboMsg`) |
| 2111 / 22111 / 28111 | server→client | full room snapshot (`SicboInfoMsg`) |
| 2112 / 22112 / 28112 | server→client | per-second tick (`UpdateSicboPerSecondMsg`) |
| 2113 / 22113 / 28113 | server→client | dice reveal (`UpdateResultSicboDicesMsg`) |
| 2116 / 22116 / 28116 | client→server | history fetch (no payload) |

Source of truth for cmd ids: `backend-master/game/Minigame/src/main/java/game/MinigameServerHandleConfig.java`.

---

## 2. Option A — DevTools console snippet (zero install)

The recommended hook is **v2** — it replaces the global `WebSocket`
constructor so every socket created AFTER it loads gets wrapped. v1
(prototype patch) is included for reference, but v2 is what you should
use 99% of the time.

### 2.1 Why v2 (not v1)

Cocos opens its WebSocket connections during page boot, before you have
DevTools open and before you can paste anything. v1 patches
`WebSocket.prototype.send` and `prototype.addEventListener` AFTER those
connections were already wired, so they keep using the original handlers
and emit nothing. v2 replaces the constructor itself; you paste it then
**reload the page** and all subsequent sockets pass through the wrapper.

### 2.2 v2 hook — paste, then reload

```javascript
(function () {
  if (window.__wsHooked) return;
  window.__wsHooked = true;

  const Native = window.WebSocket;

  // Decode helper: returns "cmd=N len=N hex=XX XX..."
  const dec = (buf) => {
    const u = new Uint8Array(buf);
    const off = u[0] === 0x90 ? 4 : 1;
    if (u.length < off + 2) return `len=${u.length} (too short)`;
    const cmd = (u[off] << 8) | u[off + 1];
    const head = Array.from(u.slice(0, 24))
      .map((x) => x.toString(16).padStart(2, "0")).join(" ");
    return `cmd=${cmd} len=${u.length} hex=${head}${u.length > 24 ? "…" : ""}`;
  };

  const logOut = (d) =>
    typeof d === "string"
      ? console.log("%cWS→ JSON", "color:#0a0", d)
      : console.log("%cWS→ BIN", "color:#0a0", dec(d));
  const logIn = (d) => {
    if (typeof d === "string") console.log("%cWS← JSON", "color:#a0a", d);
    else if (d instanceof Blob)
      d.arrayBuffer().then((b) => console.log("%cWS← BIN", "color:#a0a", dec(b)));
    else console.log("%cWS← BIN", "color:#a0a", dec(d));
  };

  function Wrapped(url, protocols) {
    const ws = protocols ? new Native(url, protocols) : new Native(url);
    console.log("%cWS open", "color:#aa0", url);
    const origSend = ws.send.bind(ws);
    ws.send = function (d) { logOut(d); return origSend(d); };
    Native.prototype.addEventListener.call(ws, "message", (e) => logIn(e.data));
    return ws;
  }
  Wrapped.prototype = Native.prototype;
  Wrapped.CONNECTING = Native.CONNECTING;
  Wrapped.OPEN = Native.OPEN;
  Wrapped.CLOSING = Native.CLOSING;
  Wrapped.CLOSED = Native.CLOSED;
  window.WebSocket = Wrapped;
  console.log("%cWS hook v2 active — RELOAD the page now", "color:#a00;font-weight:bold");
})();
```

**Steps:**
1. F12 → **Console** tab.
2. Paste the block above and press Enter. You'll see
   `WS hook v2 active — RELOAD the page now`.
3. Press **F5** (or Ctrl+Shift+R for hard reload).
4. After the page comes back you'll see `WS open wss://…/websocket` for
   every socket the cocos client opens. Place a bet — every frame logs
   as `cmd=28110 len=42 hex=…` etc.

### 2.3 Persisting across reloads — TamperMonkey userscript (recommended)

DevTools' **Sources → Snippets** "Run" only patches the CURRENT JS
context. F5 reloads the page in a fresh context — your snippet is gone
before any cocos code runs and no frames log. To survive every reload,
install the userscript at [`docs/ws-hook.user.js`](ws-hook.user.js).

**Setup (one time):**

1. Install **Tampermonkey** for Chrome / Edge / Firefox.
2. Open the userscript file [`docs/ws-hook.user.js`](ws-hook.user.js)
   in any text viewer.
3. Tampermonkey icon → **Create a new script…** → erase the template,
   paste the file contents, **Ctrl-S** (or **File → Save**).
4. The script auto-runs at `document-start` for the matched hosts:
   - `https://staging-play.sunkr.bet/*`
   - `https://staging-play.sunkr.club/*`
   - `https://*.staging-play.sunkr.bet/*` (wmini, wsicbo, wslot, …)
   - `https://*.staging-play.sunkr.club/*`
   - `https://staging-admin.sunkr.bet/*` and `.club`

After install, refresh any matched page; the console shows
`Sunwinkr WS hook active` and every WebSocket open / send / receive
logs as `WS open …`, `WS→ JSON …`, `WS→ BIN cmd=… hex=…`,
`WS← BIN cmd=… hex=…`. No paste, no reload dance.

**To add another host** (e.g. production), open the Tampermonkey script
editor and add another `// @match https://your-host/*` line at the top.

**Alt extension:** "Custom JavaScript for websites 2" works the same
way (per-host JS injection). Tampermonkey is more popular and supports
the standard `// ==UserScript==` header verbatim, which is why
`docs/ws-hook.user.js` uses that format.

**One-shot fallback** (no extension): save the v2 hook code as a
DevTools Snippet (**Sources → Snippets → New**), paste, Ctrl-S. Run
the snippet by right-clicking → Run, then immediately navigate within
the app via in-app links (NOT F5 — F5 wipes the JS context). Frames
log for sockets opened in the same session.

### 2.4 v1 hook (reference only — patches prototype)

```javascript
(function () {
  // Decode helper: returns "cmd=N len=N hex=XX XX..."
  const dec = (buf) => {
    const u = new Uint8Array(buf);
    const off = u[0] === 0x90 ? 4 : 1;
    if (u.length < off + 2) return `len=${u.length} (too short)`;
    const cmd = (u[off] << 8) | u[off + 1];
    const head = Array.from(u.slice(0, 24))
      .map((x) => x.toString(16).padStart(2, "0")).join(" ");
    return `cmd=${cmd} len=${u.length} hex=${head}${u.length > 24 ? "…" : ""}`;
  };

  // Wrap send()
  const origSend = WebSocket.prototype.send;
  WebSocket.prototype.send = function (data) {
    if (typeof data === "string") {
      console.log("%cWS→ JSON", "color:#0a0", data);
    } else {
      console.log("%cWS→ BIN", "color:#0a0", dec(data));
    }
    return origSend.call(this, data);
  };

  // Wrap message events (only catches addEventListener — misses
  // onmessage = (e) => {…} assignments and any socket already opened
  // before this snippet ran. Use v2 for full coverage.)
  const origAdd = WebSocket.prototype.addEventListener;
  WebSocket.prototype.addEventListener = function (type, fn, ...rest) {
    if (type === "message") {
      const wrapped = (e) => {
        if (typeof e.data === "string") {
          console.log("%cWS← JSON", "color:#a0a", e.data);
        } else if (e.data instanceof Blob) {
          e.data.arrayBuffer().then((b) =>
            console.log("%cWS← BIN", "color:#a0a", dec(b))
          );
        } else {
          console.log("%cWS← BIN", "color:#a0a", dec(e.data));
        }
        return fn(e);
      };
      return origAdd.call(this, type, wrapped, ...rest);
    }
    return origAdd.call(this, type, fn, ...rest);
  };

  console.log("%cWS hook v1 active (prototype patch — may miss already-open sockets)", "color:#a00;font-weight:bold");
})();
```

v1 is shorter and works without a reload **iff** the page hasn't opened
any sockets yet (rare in this codebase). Keep v2 as your default.

---

## 3. Option B — Decode common cmds inline

Once you confirm the hook works, drop this into a snippet too — it
parses the SicBo `BetSicboMsg` and `SicboInfoMsg` (cmd 28110 and 28111)
into plain objects so you don't have to hex-read.

```javascript
// Append after the dec() definition.
const decoders = {
  28110: (u, off) => { // BetSicboMsg
    const dv = new DataView(u.buffer, u.byteOffset + off + 2);
    let p = 0;
    const currentMoney = Number(dv.getBigInt64(p)); p += 8;
    const referenceId  = Number(dv.getBigInt64(p)); p += 8;
    const betValue     = Number(dv.getBigInt64(p)); p += 8;
    const moneyType    = dv.getInt16(p); p += 2;
    const inputTime    = dv.getInt16(p); p += 2;
    const strLen       = dv.getInt16(p); p += 2;
    let betSide = "";
    for (let i = 0; i < strLen; i++) betSide += String.fromCharCode(dv.getUint8(p++));
    const pot = Number(dv.getBigInt64(p));
    return { cmd: 28110, currentMoney, referenceId, betValue, moneyType, inputTime, betSide, pot };
  },
  28111: (u, off) => { // SicboInfoMsg (truncated to interesting fields)
    const dv = new DataView(u.buffer, u.byteOffset + off + 2);
    let p = 0;
    const gameId       = dv.getInt16(p); p += 2;
    const moneyType    = dv.getInt16(p); p += 2;
    const referenceId  = Number(dv.getBigInt64(p)); p += 8;
    const remainTime   = dv.getInt16(p); p += 2;
    const bettingState = dv.getUint8(p); p += 1;
    return { cmd: 28111, gameId, moneyType, referenceId, remainTime, bettingState };
  },
  // Add 28112 / 28113 / 22110 / 22111 as needed — same shape as their
  // 28xxx siblings.
};

const richDec = (buf) => {
  const u = new Uint8Array(buf);
  const off = u[0] === 0x90 ? 4 : 1;
  const cmd = (u[off] << 8) | u[off + 1];
  if (decoders[cmd]) {
    try { return decoders[cmd](u, off); } catch { /* fall through */ }
  }
  return dec(buf);
};
```

Replace the `dec(buf)` call inside the hook with `richDec(buf)` once
you've added decoder entries for the cmds you care about. Source of truth
for each cmd's wire layout lives in
`backend-master/game/Minigame/src/main/java/game/modules/minigame/cmd/{rev,send}/...`.

---

## 4. Option C — Browser extension (replay / craft frames)

Install **Smart WebSocket Client** (Chrome / Edge) for one-off
hand-crafted frames against a known endpoint. Useful for replaying a
captured `BET_TAI_XIU` frame or testing an arbitrary cmd id without
running the cocos client. Doesn't intercept the live page traffic — it's
a standalone WS console.

The page-side equivalent is `wscat -c <url>` from a terminal.

---

## 5. Option D — Server-side frame log (already running)

`ws-bridge` logs every translated frame with hex when the log level is
`info` or higher. The logs are inside the container at:

```
/app/logs/ws-bridge/ws-bridge/ws-bridge.log
```

Tail filtered:

```bash
docker exec sunwinkr-ws-bridge tail -f /app/logs/ws-bridge/ws-bridge/ws-bridge.log \
  | grep -E "(zuestang|laviai|cmd=28|Started poll|BET_)"
```

Both directions surface, so you correlate "client sent X" with "server
received Y" without touching the browser. Pair this with **Option A** in
DevTools and you have both ends of the wire on screen.

---

## 6. Option E — mitmproxy with TLS intercept (full session capture)

Use when you need the full sequence persisted to disk for offline
analysis or a regression bug report.

```bash
brew install mitmproxy   # or apt / pip
mitmweb --listen-host 0.0.0.0 --listen-port 8080 \
        --set web_open_browser=false
```

Browser side:

1. Configure system proxy to `127.0.0.1:8080` (or use Chrome's `--proxy-server`).
2. Visit `http://mitm.it` and install the mitmproxy CA cert. **Trust it
   for SSL** (System keychain → Always Trust).
3. Reload `https://staging-play.sunkr.club` — mitmweb shows every flow
   including the WS upgrade and all frames.

Pre-canned binary protocol dissector: drop a `frames.py` next to
`mitmproxy.conf`:

```python
# mitmproxy addon — pretty-print BitZero binary frames.
from mitmproxy import ctx, websocket

CMD_NAMES = {
    2110: "BetTaiXiu", 22110: "BetTaiXiuMD5", 28110: "BetSicbo",
    2111: "TaiXiuInfo", 22111: "TaiXiuMD5Info", 28111: "SicboInfo",
    2112: "TaiXiuTick", 22112: "TaiXiuMD5Tick", 28112: "SicboTick",
    2113: "TaiXiuDice", 22113: "TaiXiuMD5Dice", 28113: "SicboDice",
    2000: "SubTaiXiu", 22000: "SubTaiXiuMD5", 28000: "SubSicbo",
}

def websocket_message(flow: websocket.WebSocketFlow):
    msg = flow.messages[-1]
    if msg.is_text:
        return  # JSON path printed by default
    b = msg.content
    off = 4 if b and b[0] == 0x90 else 1
    if len(b) < off + 2:
        return
    cmd = (b[off] << 8) | b[off + 1]
    name = CMD_NAMES.get(cmd, f"?({cmd})")
    direction = "→" if msg.from_client else "←"
    ctx.log.info(f"WS {direction} {name} len={len(b)} hex={b[:24].hex()}")
```

Run with `mitmweb -s frames.py`.

---

## 7. Option F — Cocos Creator dev build (best signal-to-noise)

If you control the build, set `cc.debug = true` in `Configs/init.ts` (or
the equivalent flag) and rebuild Web Mobile. The cocos runtime then logs
every `MiniGameNetworkClient` frame parse to the console along with the
parsed object — no hex required. Keeps your snippet hooks free to handle
just frames the build hasn't decoded yet.

This is the cleanest dev experience but requires a dev build per
session — not practical for triaging an issue on the deployed staging
page.

---

## 8. Suggested workflow for the "balance not subtracting" class of bugs

1. **DevTools snippet (Option A)** active before reload.
2. Open SicBo / TaiXiu / wherever the bug reproduces.
3. Filter the console for `cmd=28110` (bet response) — confirm the
   server returned the freshly debited balance.
4. Filter for `cmd=28111` (room snapshot) — does it carry the new
   balance too, or stale?
5. Check the cocos client code path that consumes that cmd. If the
   client receives correct money but the HUD doesn't update, the bug is
   the missing `BroadcastReceiver.register(USER_UPDATE_COIN, …)` in the
   scene's `start()` (this was the SicBo regression on 2026-05-08).
6. If steps 3–4 show stale money, server is the culprit — pair with
   `ws-bridge` log tail (Option D) and `MoneyGateway` debit log lines.

---

## 9. Cross-reference

- Frame layout authoritative source: `backend-master/game/Minigame/src/main/java/game/modules/minigame/cmd/{rev,send}/**`
- Bridge JSON ↔ binary translation: `ws-bridge/bridge.js` (CMD_TRANSLATE / CMD_REDIRECT / MSG_BUILDER)
- SicBo client receive handler: `sunwinkr-client/assets/SicboTaiPhu/scripts/TaiXiuFull.ts:296`
- Other minigames' USER_UPDATE_COIN observers (template pattern):
  `assets/XocDia/XocDiaScript/XocDia.Play.ts:418-428`,
  `assets/Slot1/Slot1Script/Slot1.Slot1Controller.ts:287`,
  `assets/Lieng/LiengScript/Lieng.Controller.ts:278`

---

## 10. Out of scope

- Hooking the cocos client at runtime via dev tools without the source map.
  Doable but the obfuscated `MiniGameNetworkClient` symbols make the
  effort larger than rebuilding with `cc.debug = true`.
- Encrypting / decrypting AES-GCM payloads. The minigame protocol is
  binary but **not encrypted at the application layer** — TLS is the
  only encryption. Once decoded by mitmproxy / DevTools, frames are
  plaintext per the layout in §1.
- Capturing on iOS / Android native client. Use `mitmproxy` over the
  device's proxy and trust the CA in iOS Settings → General → About →
  Certificate Trust Settings.
