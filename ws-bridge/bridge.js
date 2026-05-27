const WebSocket = require('ws');
const http = require('http');
const fs = require('fs');

// Pino logger — writes JSON to stdout (docker logs) + file
const pino = require('pino');
const logDir = '/app/logs/ws-bridge';
try { fs.mkdirSync(logDir, { recursive: true }); } catch(e) {}
const log = pino({
  level: 'debug',
  transport: {
    targets: [
      { target: 'pino/file', options: { destination: 1 } },
      { target: 'pino/file', options: { destination: `${logDir}/ws-bridge.log`, mkdir: true } }
    ]
  }
});

// === BitZero Binary Protocol Helpers ===
function encodeString(str) {
  const buf = Buffer.from(str, 'utf8');
  const result = Buffer.alloc(2 + buf.length);
  result.writeUInt16BE(buf.length, 0);
  buf.copy(result, 2);
  return result;
}

function buildPacket(controllerId, cmdId, payloads) {
  const payload = Buffer.concat(payloads || []);
  const packet = Buffer.alloc(3 + payload.length);
  packet.writeUInt8(controllerId, 0);
  packet.writeUInt16BE(cmdId, 1);
  payload.copy(packet, 3);
  return packet;
}

function writeInt64BE(buf, value, offset) {
  const big = BigInt(value || 0);
  const hi = Number((big >> 32n) & 0xffffffffn);
  const lo = Number(big & 0xffffffffn);
  buf.writeInt32BE(hi, offset);
  buf.writeUInt32BE(lo, offset + 4);
}

// JSON command → Binary packet mapping (for SocketJson text-mode clients)
// Minigame command IDs (from compiled TaiXiuModule.class bytecode):
//   TaiXiuModule registered at handler 2000. Compiled switch cases:
//     2000 = subscribe (payload: gameId=short, roomId=short)
//     2001 = unsubscribe
//     2002 = change room
//     2110 = bet (payload: BetTaiXiuCmd = int userId + long referenceId + long betValue + short moneyType + short betSide + short inputTime)
//     2116 = get history
//   NOTE: source says newer ids like 21110, but the precompiled Minigame.jar in this stack uses the bytecode above.
//   NOTE: 2003 is not bet in the compiled jar, so sending 2003 for Tai Xiu is wrong.
const CMD_MAP = {
  // Login via SystemController: controller=0, cmdId=9002 (LoginWebsocket)
  // This properly creates the User in globalUserManager so broadcasts reach the client
  'AUTHORIZE_TOKEN': (data) => buildPacket(1, 1, [
    encodeString(data.nickname || ''), encodeString(data.token || '')
  ]),
  // Subscribe to TaiXiu: cmd=2000, gameId=2, roomId=1 (VIN)
  'SUBSCRIBE_TAI_XIU': (data) => {
    const buf = Buffer.alloc(4);
    buf.writeInt16BE(2, 0);  // gameId=2 (TaiXiu)
    buf.writeInt16BE(data && data.roomId != null ? data.roomId : 1, 2);
    return buildPacket(1, 2000, [buf]);
  },
  // Unsubscribe TaiXiu
  'UNSUBSCRIBE_TAI_XIU': (data) => {
    const buf = Buffer.alloc(4);
    buf.writeInt16BE(2, 0);
    buf.writeInt16BE(data && data.roomId != null ? data.roomId : 1, 2);
    return buildPacket(1, 2001, [buf]);
  },
  // Bet TaiXiu: compiled BetTaiXiuCmd expects 26-byte payload.
  'BET_TAI_XIU': (data) => {
    const buf = Buffer.alloc(26);
    const userId = data.userId || 0;
    const referenceId = data.referenceId || 0;
    const betValue = data.betMoney != null ? data.betMoney : (data.betValue || 0);
    const moneyType = data.moneyType != null ? data.moneyType : ((data.roomId === 0) ? 0 : 1);
    const betSide = data.betSide || 0;
    const inputTime = data.inputTime || 0;

    buf.writeInt32BE(userId, 0);
    writeInt64BE(buf, referenceId, 4);
    writeInt64BE(buf, betValue, 12);
    buf.writeInt16BE(moneyType, 20);
    buf.writeInt16BE(betSide, 22);
    buf.writeInt16BE(inputTime, 24);
    return buildPacket(1, 2110, [buf]);
  },
  // Subscribe SicBo (TaiXiu MD5): handler 22000
  'SUBSCRIBE_SICBO': (data) => {
    const buf = Buffer.alloc(4);
    buf.writeInt16BE(2, 0);
    buf.writeInt16BE(data && data.roomId != null ? data.roomId : 1, 2);
    return buildPacket(1, 22000, [buf]);
  },
  'GET_MINI_GAME_SICBO_STATE': (data) => {
    const buf = Buffer.alloc(4);
    buf.writeInt16BE(2, 0);
    buf.writeInt16BE(1, 2);
    return buildPacket(1, 22000, [buf]);
  },
  // Bet SicBo MD5: cmd=22110
  'BET_MINI_GAME_SICBO': (data) => {
    const buf = Buffer.alloc(10);
    const val = data.betMoney || 0;
    buf.writeInt32BE(Math.floor(val / 0x100000000), 0);
    buf.writeUInt32BE(val >>> 0, 4);
    buf.writeInt16BE(data.betSlot || 0, 8);
    return buildPacket(1, 22110, [buf]);
  },
  'GET_LIST_PLAYER_MINI_GAME_SICBO': () => buildPacket(1, 22003, []),
  'GET_LOG_SESSION_MINI_GAME_SICBO': () => buildPacket(1, 22116, []),
  'GET_LOG_TRANSACTION_MINI_GAME_SICBO': () => buildPacket(1, 22117, []),
};

// Game server endpoints — by path (ws-bridge internal routing)
const GAME_MAP = {
  '/ws/minigame': 'game-minigame:1641',
  '/ws/slot': 'game-slot:1844',
  '/ws/poker': 'game-poker:1744',
  '/ws/xocdia': 'game-xocdia:2344',
  '/ws/bacay': 'game-bacay:3044',
  '/ws/baicao': 'game-baicao:1144',
  '/ws/binh': 'game-binh:1244',
  '/ws/caro': 'game-caro:1344',
  '/ws/cotuong': 'game-cotuong:1444',
  '/ws/lieng': 'game-lieng:1544',
  '/ws/sam': 'game-sam:1944',
  '/ws/tlmn': 'game-tlmn:2144',
  '/ws/xizach': 'game-xizach:2244',
  '/ws/coup': 'game-coup:2444',
  '/ws/pokertour': 'game-pokertour:1740',
};

// Subdomain → game server mapping (client connects via wmini.domain, wslot.domain, etc.)
const SUBDOMAIN_MAP = {
  'wmini':     'game-minigame:1641',
  'wmini2':    'game-minigame:1641',
  'wslot':     'game-slot:1844',
  'wpirateking':'game-slot:1844',  // Chiêm Tinh / Pirate King — handler 6000
  'wthantai':  'game-slot:1844',   // Thần Tài / God of Wealth — handler 7000
  'wavenger':  'game-slot:1844',   // Siêu Anh Hùng / Avenger — handler 4000
  'wkimcuong': 'game-slot:1844',   // Kim Cương / Galaxy — handler 8000
  'wdragonball':'game-slot:1844',   // Dragon Ball / Candy — handler 9000
  'wtaydu':    'game-slot:1844',   // Tây Du Thần Khỉ / Audition — handler 11000
  'wngulong':  'game-slot:1844',   // Kho Tàng Ngũ Long / TamHung — handler 15000
  'wwitcher':  'game-slot:1844',   // The Witcher / ThanBai — handler 17000
  'wthuycung': 'game-slot:1844',   // Thủy Cung / Aquarium — handler TBD (not yet implemented)
  'wpoker':    'game-poker:1744',
  'wpokertour':'game-pokertour:1740',
  'wxocdia':   'game-xocdia:2344',
  'wxocdiatulinh': 'game-xocdiatulinh:1651',  // Xóc Đĩa Tứ Linh — dedicated server (no TàiXỉu noise)
  'wminipoker':  'game-minigame:1641',   // Mini Poker — handler 4000
  'wtrenduoi':   'game-minigame:1641',   // Trên Dưới (Cao Thấp) — handler 6000
  'wbaucua':     'game-minigame:1641',   // Bầu Cua — handler 5000
  'wtaixiu':     'game-minigame:1641',   // Tài Xỉu — handler 2000
  'wsicbo':      'game-minigame:1641',   // Sicbo — handler 28000
  'wbacay':    'game-bacay:3044',
  'wbaicao':   'game-baicao:1144',
  'wbinh':     'game-binh:1244',
  'wcaro':     'game-caro:1344',
  'wcotuong':  'game-cotuong:1444',
  'wcoup':     'game-coup:2444',
  'wlieng':    'game-lieng:1544',
  'wsam':      'game-sam:1944',
  'wtlmn':     'game-tlmn:2144',
  'wxizach':   'game-xizach:2244',
  'overunder': 'game-minigame:1641',
};

// === Debug Traffic Capture ===
// Captures all WebSocket commands in/out for debugging.
// Usage:
//   GET /debug/capture/slot     → capture all slot game traffic
//   GET /debug/capture/minigame → capture all minigame traffic
//   GET /debug/capture/all      → capture everything
//   GET /debug/stop             → stop capturing
//   GET /debug/dump             → show captured traffic (JSON)
//   GET /debug/clear            → clear captured data
//   GET /debug/status           → show current state
//
// Traffic is stored in memory (last 500 entries) AND written to traffic.log
let captureFilter = null; // null = off, 'all' or game target substring
const captureBuffer = [];
const CAPTURE_MAX = 500;

function logTraffic(connId, dir, game, buf, extra) {
  if (!captureFilter) return;
  if (captureFilter !== 'all' && !game.includes(captureFilter)) return;
  const ts = new Date().toISOString().substring(11, 23); // HH:MM:SS.mmm
  let ctrl = -1, cmd = -1, errCode = -1;
  if (dir === 'CLIENT→SERVER' && buf.length >= 6) {
    // OutPacket: header(1) + size(2) + ctrl(1) + cmd(2)
    ctrl = buf[3]; cmd = buf.readUInt16BE(4);
  } else if (dir === 'SERVER→CLIENT' && buf.length >= 3) {
    // Response: ctrl(1) + cmd(2) + err(1)
    ctrl = buf[0]; cmd = buf.readUInt16BE(1); errCode = buf.length > 3 ? buf[3] : -1;
  }
  const entry = { ts, connId, dir, game, ctrl, cmd, err: errCode, len: buf.length,
    hex: buf.toString('hex').substring(0, 100), ...(extra || {}) };
  captureBuffer.push(entry);
  if (captureBuffer.length > CAPTURE_MAX) captureBuffer.shift();
  // Also write to file
  try {
    fs.appendFileSync(`${logDir}/traffic.log`, JSON.stringify(entry) + '\n');
  } catch(e) {}
}

const server = http.createServer((req, res) => {
  const url = req.url;
  if (url.startsWith('/debug/capture/')) {
    captureFilter = url.substring('/debug/capture/'.length) || 'all';
    captureBuffer.length = 0;
    log.info(`Traffic capture ON: filter=${captureFilter}`);
    res.writeHead(200); res.end(`Capturing: ${captureFilter}\n`);
  } else if (url === '/debug/stop') {
    captureFilter = null;
    log.info('Traffic capture OFF');
    res.writeHead(200); res.end('Capture stopped\n');
  } else if (url === '/debug/dump') {
    res.writeHead(200, {'Content-Type': 'application/json'});
    res.end(JSON.stringify(captureBuffer, null, 2) + '\n');
  } else if (url === '/debug/clear') {
    captureBuffer.length = 0;
    try { fs.writeFileSync(`${logDir}/traffic.log`, ''); } catch(e) {}
    res.writeHead(200); res.end('Cleared\n');
  } else if (url === '/debug/status') {
    res.writeHead(200, {'Content-Type': 'application/json'});
    res.end(JSON.stringify({ capturing: !!captureFilter, filter: captureFilter,
      entries: captureBuffer.length }) + '\n');
  } else if (url === '/debug/ipstats') {
    // Top-N IPs by active connection count — flag floods
    const top = [...ipConns.entries()]
      .map(([ip, set]) => ({ ip, conns: set.size }))
      .sort((a, b) => b.conns - a.conns).slice(0, 50);
    res.writeHead(200, {'Content-Type': 'application/json'});
    res.end(JSON.stringify({ total_unique_ips: ipConns.size,
      total_conns: top.reduce((s, e) => s + e.conns, 0),
      max_per_ip: MAX_CONNS_PER_IP, top }, null, 2) + '\n');
  } else {
    res.writeHead(200); res.end('WS Bridge OK\n');
  }
});

const wss = new WebSocket.Server({ server });
let connCounter = 0;

// === Per-IP connection cap (defense against flood/DoS) ===
// Cloudflare passes the real IP in CF-Connecting-IP header. Without a cap,
// one misbehaving client could open thousands of WS, exhausting fd / memory.
// 50 is generous for a casino client (typically 1 lobby + 1 game), and
// blocks scripted floods cleanly.
const MAX_CONNS_PER_IP = parseInt(process.env.WS_MAX_PER_IP || '50', 10);
const PONG_TIMEOUT_MS  = parseInt(process.env.WS_PONG_TIMEOUT_MS || '60000', 10);
const ipConns = new Map(); // ip → Set<connId>

function addIp(ip, connId) {
  let set = ipConns.get(ip);
  if (!set) { set = new Set(); ipConns.set(ip, set); }
  set.add(connId);
  return set.size;
}
function removeIp(ip, connId) {
  const set = ipConns.get(ip);
  if (!set) return;
  set.delete(connId);
  if (set.size === 0) ipConns.delete(ip);
}
function realIp(req) {
  // Cloudflare → cloudflared → nginx → ws-bridge: trust CF-Connecting-IP
  return req.headers['cf-connecting-ip']
      || (req.headers['x-forwarded-for'] || '').split(',')[0].trim()
      || req.socket.remoteAddress
      || 'unknown';
}

wss.on('connection', (clientWs, req) => {
  const rawPath = req.url;
  // Strip /websocket suffix — client's NetworkClient appends it automatically
  const path = rawPath.replace(/\/websocket\/?$/, '');
  let target = GAME_MAP[path] || GAME_MAP[rawPath];

  // Fallback: resolve by subdomain from Host header (e.g. wmini.staging-play.sunkr.bet)
  if (!target) {
    const host = (req.headers.host || '').split(':')[0]; // strip port
    const sub = host.split('.')[0]; // first segment = subdomain
    target = SUBDOMAIN_MAP[sub];
    if (target) {
      log.info(` Subdomain route: ${host} (${sub}) → ${target}`);
    }
  }

  if (!target) {
    log.warn(` Unknown path: ${path}, host: ${req.headers.host}`);
    clientWs.close(4004, 'Unknown game');
    return;
  }

  // === Per-IP cap check ===
  const clientIp = realIp(req);
  const connId = ++connCounter;
  const ipCount = addIp(clientIp, connId);
  if (ipCount > MAX_CONNS_PER_IP) {
    removeIp(clientIp, connId);
    log.warn(` [${connId}] IP cap exceeded: ip=${clientIp} count=${ipCount} max=${MAX_CONNS_PER_IP}`);
    clientWs.close(4029, 'Too many connections');
    return;
  }

  const gameName = target;
  log.info(` [${connId}] Client connected: ${path} → ${target} ip=${clientIp} ip_conns=${ipCount}`);
  logTraffic(connId, 'CONNECT', gameName, Buffer.alloc(0), { path, host: req.headers.host, ip: clientIp });

  // Slot server cmd ID translation: some server modules respond with their original cmd range
  // instead of the registered handler range. Track the client's subscribe cmd to detect this
  // and translate response cmds so the client can process them.
  // E.g. handler 14000 (AvengerModule) responds with 4000-range → translate to 14000-range
  // Slot cmd translation: some client games use cmd ranges that don't match their server handler.
  // CMD_TRANSLATE: when client subscribes with a specific cmd, translate between client and server ranges.
  // CMD_REDIRECT: when client sends cmds in one range, redirect them to a different handler range.
  const CMD_TRANSLATE = {
    // subscribeCmd: [serverResponseBase, clientExpectedBase]
    14003: [4000, 14000],  // Slot4/Slot3x3Gem: Avenger handler 14000 responds as 4000
    16003: [6000, 16000],  // Slot11-Bikini: ChiemTinh handler 16000 responds as 6000
    // 28000: DISABLED — TaiXiuSicboModule at 28000 works directly with full precompiled JARs
  };
  // CMD_REDIRECT: Pirate King (12xxx) → ThanTai (7xxx) — matches last working version.
  // ThanTai has 25 lines (matches Pirate King client), KhoBau/ChiemTinh have different line counts.
  const CMD_REDIRECT = {
    12003: [12000, 7000],  // Pirate King: redirect 12xxx → 7xxx (ThanTaiModule)
    // 28000: DISABLED — TaiXiuSicboModule game loop works with full precompiled JARs
  };
  let cmdTranslate = null;
  let cmdRedirect = null;

  // --- Minigame uses TCP (port 1640) to bypass Netty 3 WebSocket flush bug ---
  // --- Other games use WebSocket as before ---
  const useTcp = false; // disabled — TCP port not accessible from Docker
  let serverWs = null;
  let serverTcp = null;
  let tcpRecvBuf = Buffer.alloc(0);
  let serverReady = false;
  let clientClosed = false;
  let serverClosed = false;
  const queue = [];

  function forwardToClient(body) {
    if (clientClosed || clientWs.readyState !== WebSocket.OPEN) return;
    const buf = Buffer.isBuffer(body) ? body : Buffer.from(body);
    if (buf.length >= 3) {
      log.debug(` ${buf.length}B ctrl=${buf[0]} cmd=${buf.readUInt16BE(1)}`);
      if (cmdTranslate) {
        const cmd = buf.readUInt16BE(1);
        const [origBase, regBase] = cmdTranslate;
        if (cmd >= origBase && cmd < origBase + 1000) {
          buf.writeUInt16BE(cmd - origBase + regBase, 1);
        }
      }
      if (cmdRedirect) {
        const cmd = buf.readUInt16BE(1);
        const [fromBase, toBase] = cmdRedirect;
        if (cmd >= toBase && cmd < toBase + 1000) {
          buf.writeUInt16BE(cmd - toBase + fromBase, 1);
        }
      }
    }
    logTraffic(connId, 'SERVER→CLIENT', gameName, buf);
    clientWs.send(buf, { binary: true, fin: true }, (err) => {
      if (err) log.warn(` Forward error: ${err.message}`);
    });
  }

  function sendToServer(data) {
    if (useTcp && serverTcp && !serverTcp.destroyed) {
      serverTcp.write(data);
    } else if (serverWs && serverWs.readyState === WebSocket.OPEN) {
      serverWs.send(data, { binary: true });
    }
  }

  function closeServer() {
    if (useTcp && serverTcp && !serverTcp.destroyed) serverTcp.destroy();
    else if (serverWs && serverWs.readyState === WebSocket.OPEN) serverWs.close();
  }

  if (useTcp) {
    // TCP connection to minigame server (port 1640)
    // Uses EngineWriter (dedicated thread + SocketChannel.write) — no Netty 3 flush bug
    const net = require('net');
    const [tcpHost, tcpPort] = target.replace(':1641', ':11640').split(':');
    serverTcp = net.createConnection({ host: tcpHost, port: parseInt(tcpPort) }, () => {
      serverReady = true;
      log.info(` TCP ready: ${tcpHost}:${tcpPort}`);
      while (queue.length > 0) processClientMessage(queue.shift());
    });
    serverTcp.on('data', (chunk) => {
      tcpRecvBuf = Buffer.concat([tcpRecvBuf, chunk]);
      // Parse TCP frames: [header(1)][size(2 or 4)][body]
      while (tcpRecvBuf.length >= 3) {
        const header = tcpRecvBuf[0];
        const isBigSize = (header & 0x08) > 0;
        const sizeBytes = isBigSize ? 4 : 2;
        if (tcpRecvBuf.length < 1 + sizeBytes) break;
        const bodySize = isBigSize ? tcpRecvBuf.readUInt32BE(1) : tcpRecvBuf.readUInt16BE(1);
        const frameEnd = 1 + sizeBytes + bodySize;
        if (tcpRecvBuf.length < frameEnd) break;
        const body = tcpRecvBuf.slice(1 + sizeBytes, frameEnd);
        tcpRecvBuf = tcpRecvBuf.slice(frameEnd);
        forwardToClient(body);
      }
    });
    serverTcp.on('error', (e) => log.warn(` TCP error: ${e.message}`));
    serverTcp.on('close', () => {
      serverClosed = true;
      log.info(` TCP closed`);
      if (!clientClosed) { clientClosed = true; clientWs.close(); }
    });
  } else {
    // WebSocket connection for non-minigame servers
    serverWs = new WebSocket(`ws://${target}/websocket`, ['default-protocol']);
    serverWs.on('open', () => {
      serverReady = true;
      log.info(` Server ready: ${target}`);
      while (queue.length > 0) processClientMessage(queue.shift());
    });
    serverWs.on('message', (data) => forwardToClient(Buffer.isBuffer(data) ? data : Buffer.from(data)));
    serverWs.on('error', (e) => log.warn(` Server error: ${e.message}`));
    serverWs.on('close', (code, reason) => {
      serverClosed = true;
      log.info(` Server closed: ${code} ${reason || ''}`);
      logTraffic(connId, 'SERVER_CLOSE', gameName, Buffer.alloc(0), { code });
      if (!clientClosed) { clientClosed = true; clientWs.close(); }
    });
  }

  // === Keepalive + pong-timeout death detection ===
  // Why both: pings keep the WS alive through Cloudflare's idle timeout
  // (default 100s on Free, longer on paid). Pong-timeout detects clients
  // that silently went away (battery died, app force-quit, NAT dropped),
  // so we don't accumulate zombie connections leaking fd / heap.
  let lastPongAt = Date.now();
  clientWs.on('pong', () => { lastPongAt = Date.now(); });

  const keepalive = setInterval(() => {
    if (serverClosed || clientClosed) { clearInterval(keepalive); return; }
    // Death check first: if no pong since N seconds, force-close
    if (Date.now() - lastPongAt > PONG_TIMEOUT_MS) {
      log.warn(` [${connId}] pong-timeout (${PONG_TIMEOUT_MS}ms) — closing zombie`);
      try { clientWs.terminate(); } catch(e) {}
      clearInterval(keepalive);
      return;
    }
    try {
      if (clientWs.readyState === WebSocket.OPEN) clientWs.ping();
      if (!useTcp && serverWs && serverWs.readyState === WebSocket.OPEN) serverWs.ping();
    } catch(e) {}
  }, target.includes('1641') ? 1000 : 15000); // 1s pings for minigame (write-queue flush)

  // Process a client message
  function processClientMessage(msg) {
    const isBuf = Buffer.isBuffer(msg);
    const buf = isBuf ? msg : Buffer.from(msg);

    // Check if it looks like JSON (starts with '{' or '[')
    if (buf.length > 0 && (buf[0] === 0x7B || buf[0] === 0x5B)) {
      try {
        const json = JSON.parse(buf.toString('utf8'));
        const cmdName = json.id;
        const encoder = CMD_MAP[cmdName];
        if (encoder) {
          // Add 3-byte OutPacket header that server expects
          const innerPacket = encoder(json.data || {});
          const bodyLen = innerPacket.length;
          const packet = Buffer.alloc(3 + bodyLen);
          packet[0] = 0x90; // header flags
          packet.writeUInt16BE(bodyLen, 1);
          innerPacket.copy(packet, 3);
          log.debug(` JSON ${cmdName} → ${packet.length}B binary`);
          sendToServer(packet);
        } else {
          log.debug(` Unknown JSON cmd: ${cmdName}`);
        }
        return;
      } catch (e) {
        // Not valid JSON, treat as binary
      }
    }

    // Binary pass-through — detect cmd at correct offset
    // Cocos client sends raw [ctrl(1)][cmd(2)][payload] (no OutPacket header)
    // JSON→binary converter sends [0x90][size(2)][ctrl(1)][cmd(2)][payload] (with header)
    logTraffic(connId, 'CLIENT→SERVER', gameName, buf);
    let outBuf = buf;
    if (buf.length >= 3) {
      // Detect format: OutPacket header starts with 0x90, raw binary starts with ctrl (0x00 or 0x01)
      const hasHeader = buf[0] === 0x90 && buf.length >= 6;
      const cmdOffset = hasHeader ? 4 : 1;
      const ctrlOffset = hasHeader ? 3 : 0;
      const ctrl = buf[ctrlOffset];
      const cmd = buf.readUInt16BE(cmdOffset);
      log.debug(` Binary ${buf.length}B ctrl=${ctrl} cmd=${cmd} hdr=${hasHeader}`);
      // Detect subscribe cmd for response translation
      if (!cmdTranslate && CMD_TRANSLATE[cmd]) {
        cmdTranslate = CMD_TRANSLATE[cmd];
        log.info(` [${connId}] Cmd translate: ${cmdTranslate[0]}→${cmdTranslate[1]}`);
      }
      // Detect subscribe cmd for request redirect (client→server cmd rewrite)
      if (!cmdRedirect && CMD_REDIRECT[cmd]) {
        cmdRedirect = CMD_REDIRECT[cmd];
        log.info(` [${connId}] Cmd redirect: ${cmdRedirect[0]}→${cmdRedirect[1]}`);
      }
      // Redirect client cmds to different handler range
      if (cmdRedirect) {
        const [fromBase, toBase] = cmdRedirect;
        if (cmd >= fromBase && cmd < fromBase + 1000) {
          const newCmd = cmd - fromBase + toBase;
          buf.writeUInt16BE(newCmd, cmdOffset); // rewrite cmd at correct offset
          // Sicbo subscribe: rewrite payload gameId from 2 to 22000
          // Server doSubcribeMiniGame expects gameId=22000 in payload
          if (newCmd === 22000) {
            const payloadOffset = cmdOffset + 2; // right after cmd
            if (buf.length >= payloadOffset + 2) {
              buf.writeInt16BE(22000, payloadOffset); // gameId = 22000
              log.info(` [${connId}] Sicbo gameId rewrite: 2 → 22000`);
            }
          }
          log.debug(` [${connId}] Redirected cmd ${cmd} → ${newCmd}`);
        }
      }

      // MINI_CMD_MAP REMOVED — production Minigame.jar uses cmd 2110 for bet (not 2003)
      // The rewrite was only needed for Gradle-built JARs which had case 2003

      // Minigame WS expects the BitZero OutPacket envelope. Browser clients often send
      // raw [ctrl][cmd][payload], so wrap it before forwarding to the upstream minigame WS.
      if (!hasHeader && target.includes('1641')) {
        outBuf = Buffer.alloc(3 + buf.length);
        outBuf[0] = 0x90;
        outBuf.writeUInt16BE(buf.length, 1);
        buf.copy(outBuf, 3);
        log.debug(` [${connId}] Wrapped raw minigame packet ${buf.length}B → ${outBuf.length}B`);
      }
    } else {
      log.debug(` Binary ${buf.length}B`);
    }
    sendToServer(outBuf);
  }

  // Client sends message
  clientWs.on('message', (msg, isBinary) => {
    if (serverClosed) return;
    if (!serverReady) {
      queue.push(msg);
      log.debug(` Queued ${msg.length}B (server not ready)`);
      return;
    }
    processClientMessage(msg);
  });

  // MINIGAME POLL: re-subscribe every 1s to force the server to flush its
  // WebSocket write buffer. Works around Netty 3's flush bug — the response
  // path always flushes, so any request triggers all queued server-push
  // frames (UpdateSicboPerSecondMsg etc) to leave the buffer.
  //
  // Subscribe (cmd *000) is what ships countdown + room state on the
  // response, so the client stays time-synced while polling. A history-only
  // poll (cmd *116) flushes the channel but never re-emits the round timer
  // — the SicBo tester 2026-05-08 saw the countdown drift past round end
  // when polling history. The subscribe re-sync is heavier (joinRoom +
  // updateTaiXiuInfo every second) but is the only known path that keeps
  // the client UI honest.
  let pollInterval = null;
  function startPoll(subscribeCmd, roomId) {
    if (pollInterval) return;
    const subBuf = buildPacket(1, subscribeCmd, [(() => {
      const b = Buffer.alloc(4);
      b.writeInt16BE(2, 0);  // gameId=2
      b.writeInt16BE(roomId != null ? roomId : 1, 2);
      return b;
    })()]);
    const pkt = Buffer.alloc(3 + subBuf.length);
    pkt[0] = 0x90;
    pkt.writeUInt16BE(subBuf.length, 1);
    subBuf.copy(pkt, 3);
    pollInterval = setInterval(() => {
      if (serverClosed || clientClosed) { clearInterval(pollInterval); return; }
      sendToServer(pkt);
    }, 1000);
    log.info(` Started poll cmd=${subscribeCmd} (1s interval, subscribe re-sync)`);
  }
  // Keep legacy alias used by JSON path below
  function startTaiXiuPoll(roomId) { startPoll(2000, roomId); }

  // Detect when client subscribes to TaiXiu or SicBo, then start polling
  if (target.includes('1641')) {
    const origProcess = processClientMessage;
    processClientMessage = function(msg) {
      origProcess(msg);
      // Check if this is a SUBSCRIBE_TAI_XIU command
      try {
        const json = JSON.parse(msg);
        if (json.id === 'SUBSCRIBE_TAI_XIU' && !pollInterval) {
          startTaiXiuPoll(json.data?.roomId || 1);
        }
      } catch(e) { /* not JSON, ignore */ }

      const buf = Buffer.isBuffer(msg) ? msg : Buffer.from(msg);
      if (buf.length >= 7) {
        const hasHeader = buf[0] === 0x90 && buf.length >= 10;
        const cmdOffset = hasHeader ? 4 : 1;
        const payloadOffset = hasHeader ? 6 : 3;
        const cmd = buf.readUInt16BE(cmdOffset);
        // TaiXiu 2000 / TaiXiuMD5 22000 / SicBo 28000 all share SubcribeMinigameCmd
        // (gameId int16 + roomId int16). Same Netty-3 server-push flush bug
        // affects all three. BauCua 5001 / MiniPoker 4003 use a 1-byte
        // roomId-only payload, so they need a separate detection path —
        // not added until a player reports a freeze (re-subscribing those
        // every second re-runs joinRoom which has side effects).
        if ((cmd === 2000 || cmd === 22000 || cmd === 28000) && buf.length >= payloadOffset + 4) {
          const gameId = buf.readInt16BE(payloadOffset);
          const roomId = buf.readInt16BE(payloadOffset + 2);
          if (gameId === 2) {
            startPoll(cmd, roomId);
          }
        }
      }
    };
  }

  clientWs.on('close', (code, reason) => {
    clientClosed = true;
    if (pollInterval) clearInterval(pollInterval);
    removeIp(clientIp, connId);
    log.info(` Client closed: ${code} ${reason || ''}`);
    logTraffic(connId, 'CLIENT_CLOSE', gameName, Buffer.alloc(0), { code });
    if (!serverClosed) closeServer();
  });

  clientWs.on('error', (e) => {
    log.warn(` Client error: ${e.message}`);
  });
});

server.listen(8089, '0.0.0.0', () => {
  log.info('WS-BRIDGE started on port 8089');
});
