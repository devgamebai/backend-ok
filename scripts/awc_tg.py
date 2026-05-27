#!/usr/bin/env python3
"""
AWC Telegram relay (MTProto userbot).

Reads AWC_TG_API_ID + AWC_TG_API_HASH from .env. Stores session at
~/.awc-tg.session (chmod 600). Logs incoming messages from a target
chat to /tmp/awc-tg.log so the human (or assistant) can read them
without staying connected to Telegram.

Subcommands:
  login                 Interactive first-time auth (phone + OTP + 2FA).
  list-chats [N]        Print recent dialogs (id + name) — find AWC group.
  resolve <name>        Find chat id by partial name match.
  listen <chat_id>      Listen forever; append new messages to /tmp/awc-tg.log.
  tail [N]              Print last N lines of /tmp/awc-tg.log (default 30).
  send <chat_id> <msg>  Send a message to a chat.

Security:
  - Session file = full account access. chmod 600, root-owned.
  - Run on staging server only.
  - Use --send only when explicitly asked. Do not auto-send.
"""

from __future__ import annotations
import argparse
import asyncio
import os
import sys
from datetime import datetime
from pathlib import Path

from telethon import TelegramClient, events  # type: ignore
from telethon.errors import SessionPasswordNeededError  # type: ignore


# ─────────────────────────────────────────────────────────────────────────
# Config
# ─────────────────────────────────────────────────────────────────────────
SESSION_PATH = Path.home() / ".awc-tg"
LOG_PATH = Path("/tmp/awc-tg.log")


def load_creds() -> tuple[int, str]:
    env = Path(__file__).resolve().parent.parent / ".env"
    api_id = api_hash = None
    if env.exists():
        for line in env.read_text().splitlines():
            line = line.strip()
            if line.startswith("AWC_TG_API_ID="):
                api_id = line.split("=", 1)[1]
            elif line.startswith("AWC_TG_API_HASH="):
                api_hash = line.split("=", 1)[1]
    api_id = api_id or os.environ.get("AWC_TG_API_ID")
    api_hash = api_hash or os.environ.get("AWC_TG_API_HASH")
    if not api_id or not api_hash:
        sys.exit("AWC_TG_API_ID/AWC_TG_API_HASH missing in .env or env")
    return int(api_id), api_hash


def make_client() -> TelegramClient:
    api_id, api_hash = load_creds()
    return TelegramClient(str(SESSION_PATH), api_id, api_hash)


# ─────────────────────────────────────────────────────────────────────────
# Subcommand: login
# ─────────────────────────────────────────────────────────────────────────
async def cmd_login() -> None:
    client = make_client()
    await client.connect()
    if await client.is_user_authorized():
        me = await client.get_me()
        print(f"Already logged in as: {me.first_name} (@{me.username}, id={me.id})")
        await client.disconnect()
        return

    phone = input("Phone (with +country code): ").strip()
    await client.send_code_request(phone)
    code = input("OTP code Telegram sent: ").strip()
    try:
        await client.sign_in(phone=phone, code=code)
    except SessionPasswordNeededError:
        pwd = input("Two-factor password: ").strip()
        await client.sign_in(password=pwd)

    me = await client.get_me()
    print(f"Logged in as: {me.first_name} (@{me.username}, id={me.id})")

    # Lock session file
    if SESSION_PATH.with_suffix(".session").exists():
        os.chmod(SESSION_PATH.with_suffix(".session"), 0o600)
        print(f"Session: {SESSION_PATH}.session (chmod 600)")
    await client.disconnect()


# ─────────────────────────────────────────────────────────────────────────
# Subcommand: list-chats
# ─────────────────────────────────────────────────────────────────────────
async def cmd_list_chats(limit: int) -> None:
    async with make_client() as client:
        n = 0
        async for dialog in client.iter_dialogs():
            print(f"  id={dialog.id:<15} name={dialog.name!r}")
            n += 1
            if n >= limit:
                break


# ─────────────────────────────────────────────────────────────────────────
# Subcommand: resolve
# ─────────────────────────────────────────────────────────────────────────
async def cmd_resolve(query: str) -> None:
    async with make_client() as client:
        async for dialog in client.iter_dialogs():
            if query.lower() in (dialog.name or "").lower():
                print(f"id={dialog.id} name={dialog.name!r}")


# ─────────────────────────────────────────────────────────────────────────
# Subcommand: listen
# ─────────────────────────────────────────────────────────────────────────
def fmt_msg(sender: str, text: str) -> str:
    ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    text = (text or "").replace("\n", "\n    ")
    return f"[{ts}] {sender}: {text}\n"


MEDIA_DIR = Path("/tmp/awc-tg-media")


async def cmd_listen(chat_id: int) -> None:
    LOG_PATH.touch(exist_ok=True)
    os.chmod(LOG_PATH, 0o644)
    MEDIA_DIR.mkdir(exist_ok=True)
    client = make_client()
    await client.start()
    target = await client.get_entity(chat_id)
    name = getattr(target, "title", None) or getattr(target, "first_name", str(chat_id))
    print(f"Listening on chat: {name} (id={chat_id}) — log: {LOG_PATH} media: {MEDIA_DIR}")

    @client.on(events.NewMessage(chats=chat_id))
    async def handler(event):
        try:
            sender = await event.get_sender()
            sender_name = getattr(sender, "first_name", None) or getattr(sender, "title", "?") or "?"
            text = event.message.message or ""

            media_path = None
            if event.message.media is not None:
                # Download attachment (photo, document, voice, etc.)
                ts_prefix = datetime.now().strftime("%Y%m%d-%H%M%S")
                target_dir = MEDIA_DIR / ts_prefix
                target_dir.mkdir(parents=True, exist_ok=True)
                try:
                    media_path = await event.message.download_media(file=str(target_dir) + "/")
                except Exception as me:
                    media_path = f"<download-failed: {me}>"

            line_text = text if text else ("<media>" if media_path else "<empty>")
            if media_path:
                line_text += f"\n[attachment saved: {media_path}]"

            with LOG_PATH.open("a") as f:
                f.write(fmt_msg(sender_name, line_text))
        except Exception as e:
            with LOG_PATH.open("a") as f:
                f.write(f"[handler-error] {e}\n")

    await client.run_until_disconnected()


# ─────────────────────────────────────────────────────────────────────────
# Subcommand: tail
# ─────────────────────────────────────────────────────────────────────────
def cmd_tail(n: int) -> None:
    if not LOG_PATH.exists():
        print(f"(no log at {LOG_PATH} — run `listen` first)")
        return
    lines = LOG_PATH.read_text().splitlines()
    for line in lines[-n:]:
        print(line)


# ─────────────────────────────────────────────────────────────────────────
# Subcommand: send
# ─────────────────────────────────────────────────────────────────────────
async def cmd_fetch_recent(chat_id: int, limit: int) -> None:
    """Pull last N messages from a chat (including media). Useful if listener
    missed older messages or wasn't running yet."""
    MEDIA_DIR.mkdir(exist_ok=True)
    async with make_client() as client:
        target_dir = MEDIA_DIR / datetime.now().strftime("history-%Y%m%d-%H%M%S")
        target_dir.mkdir(parents=True, exist_ok=True)
        msgs = []
        async for msg in client.iter_messages(chat_id, limit=limit):
            sender = await msg.get_sender()
            sender_name = getattr(sender, "first_name", None) or getattr(sender, "title", "?") or "?"
            text = msg.message or ""
            media_path = None
            if msg.media is not None:
                try:
                    media_path = await msg.download_media(file=str(target_dir) + "/")
                except Exception as e:
                    media_path = f"<download-failed: {e}>"
            ts = msg.date.astimezone().strftime("%Y-%m-%d %H:%M:%S")
            msgs.append((ts, sender_name, text, media_path))

        msgs.reverse()
        for ts, sender_name, text, media_path in msgs:
            print(f"[{ts}] {sender_name}: {text or ('<media>' if media_path else '<empty>')}")
            if media_path:
                print(f"  → attachment: {media_path}")


async def cmd_send(chat_id: int, text: str) -> None:
    # SQLite session is locked while a `listen` process holds it.
    # Pause the listener, send, then restart it.
    import subprocess, time
    listener_was_running = False
    try:
        out = subprocess.run(["pgrep", "-f", "awc_tg.py listen"], capture_output=True, text=True)
        pids = [int(p) for p in out.stdout.split() if p.isdigit()]
    except Exception:
        pids = []

    for pid in pids:
        try:
            os.kill(pid, 9)
            listener_was_running = True
        except ProcessLookupError:
            pass
    if pids:
        time.sleep(1)

    async with make_client() as client:
        await client.send_message(chat_id, text)
        print(f"sent → chat={chat_id}, len={len(text)}")

    if listener_was_running:
        # Re-launch the listener — daemon-style with nohup so it survives this process.
        subprocess.Popen(
            ["nohup", "python3", str(Path(__file__).resolve()), "listen", str(chat_id)],
            stdout=open("/tmp/awc-tg-listener.out", "ab"),
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
        print(f"listener restarted (chat={chat_id})")


# ─────────────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────────────
def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)

    sub.add_parser("login")

    pl = sub.add_parser("list-chats")
    pl.add_argument("limit", nargs="?", type=int, default=30)

    pr = sub.add_parser("resolve")
    pr.add_argument("query")

    pls = sub.add_parser("listen")
    pls.add_argument("chat_id", type=int)

    pt = sub.add_parser("tail")
    pt.add_argument("n", nargs="?", type=int, default=30)

    pf = sub.add_parser("fetch-recent")
    pf.add_argument("chat_id", type=int)
    pf.add_argument("limit", nargs="?", type=int, default=10)

    ps = sub.add_parser("send")
    ps.add_argument("chat_id", type=int)
    ps.add_argument("text")

    args = p.parse_args()

    if args.cmd == "login":
        asyncio.run(cmd_login())
    elif args.cmd == "list-chats":
        asyncio.run(cmd_list_chats(args.limit))
    elif args.cmd == "resolve":
        asyncio.run(cmd_resolve(args.query))
    elif args.cmd == "listen":
        asyncio.run(cmd_listen(args.chat_id))
    elif args.cmd == "tail":
        cmd_tail(args.n)
    elif args.cmd == "fetch-recent":
        asyncio.run(cmd_fetch_recent(args.chat_id, args.limit))
    elif args.cmd == "send":
        asyncio.run(cmd_send(args.chat_id, args.text))


if __name__ == "__main__":
    main()
