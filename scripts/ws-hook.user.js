// ==UserScript==
// @name         Sunwinkr WS Frame Logger
// @namespace    sunwinkr
// @version      1.0.0
// @description  Decode every BitZero binary frame on staging-play. Pairs with docs/WS_FRAME_DEBUGGING.md.
// @author       sunwinkr ops
// @match        https://staging-play.sunkr.bet/*
// @match        https://staging-play.sunkr.club/*
// @match        https://*.staging-play.sunkr.bet/*
// @match        https://*.staging-play.sunkr.club/*
// @match        https://staging-admin.sunkr.bet/*
// @match        https://staging-admin.sunkr.club/*
// @run-at       document-start
// @grant        none
// ==/UserScript==

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
  console.log("%cSunwinkr WS hook active", "color:#a00;font-weight:bold");
})();
