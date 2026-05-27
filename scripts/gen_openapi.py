#!/usr/bin/env python3
"""Generate OpenAPI 3.0.3 spec for sunwinkr backend and portal APIs.

See docs/superpowers/specs/2026-04-30-openapi-swagger-design.md.

Usage: python3 scripts/gen_openapi.py
       (writes docs/openapi/openapi.json from repo root)
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

CommandTuple = tuple  # (int_id, name_str, fqn_str)


def parse_commands(xml_text: str) -> list[CommandTuple]:
    """Extract (id, name, fqn) from XML, skipping commented-out blocks.

    Mirrors the comment-aware approach in scripts/check-api-commands.sh.
    On duplicate id, keeps the first occurrence and warns to stderr.
    """
    stripped = re.sub(r"<!--.*?-->", "", xml_text, flags=re.DOTALL)
    seen_ids: set[int] = set()
    out: list[CommandTuple] = []
    for m in re.finditer(r"<command>(.*?)</command>", stripped, flags=re.DOTALL):
        block = m.group(1)
        id_match = re.search(r"<id>(\d+)</id>", block)
        name_match = re.search(r"<name>([^<]*)</name>", block)
        path_match = re.search(r"<path>([^<]+)</path>", block)
        if not (id_match and path_match):
            continue
        cmd_id = int(id_match.group(1))
        if cmd_id in seen_ids:
            print(f"warning: duplicate command id {cmd_id} — keeping first", file=sys.stderr)
            continue
        seen_ids.add(cmd_id)
        name = name_match.group(1).strip() if name_match else ""
        out.append((cmd_id, name, path_match.group(1).strip()))
    return out


def resolve_source_path(fqn: str, source_roots: list[Path]) -> Path | None:
    """Convert FQN like 'com.vinplay.Foo' to first matching {root}/com/vinplay/Foo.java."""
    rel = Path(*fqn.split(".")).with_suffix(".java")
    for root in source_roots:
        candidate = root / rel
        if candidate.is_file():
            return candidate
    return None


def extract_params(source_text: str) -> list[str]:
    """Return query-param names from request.getParameter("...") calls,
    deduplicated in first-seen order."""
    seen: dict[str, None] = {}
    for m in re.finditer(r'request\.getParameter\(\s*"([^"]+)"\s*\)', source_text):
        seen.setdefault(m.group(1), None)
    return list(seen.keys())


ROOT_PROCESSOR_PARENTS = {
    "com.vinplay.api.backend.processors",
    "com.vinplay.api.processors",
}


def derive_tag(fqn: str) -> str:
    """Last sub-package before class name, or 'general' for root processors."""
    parts = fqn.rsplit(".", 1)
    if len(parts) < 2:
        return "general"
    package_part = parts[0]

    # Check if parent is a root processor package
    if package_part in ROOT_PROCESSOR_PARENTS:
        return "general"

    # Extract the last sub-package (tag) from the full package
    package_parts = package_part.split(".")
    if len(package_parts) > 0:
        return package_parts[-1]
    return "general"


BACKEND_LOGIN_IDS = {701}  # commands that skip aat


def _camel_case(name: str) -> str:
    """'update money user' -> 'updateMoneyUser'. Strips non-alnum."""
    parts = re.split(r"[^A-Za-z0-9]+", name.strip())
    parts = [p for p in parts if p]
    if not parts:
        return "unnamed"
    head, *tail = parts
    return head.lower() + "".join(p[:1].upper() + p[1:].lower() for p in tail)


def build_operation(api: str, cmd_id: int, name: str, fqn: str,
                    params: list[str]) -> dict:
    assert api in ("backend", "portal")
    parameters: list[dict] = [
        {
            "name": "c",
            "in": "query",
            "required": True,
            "schema": {"type": "integer", "enum": [cmd_id]},
        }
    ]
    for p in params:
        parameters.append({
            "name": p,
            "in": "query",
            "schema": {"type": "string"},
        })
    op: dict = {
        "summary": name or f"command {cmd_id}",
        "operationId": f"{api}_c{cmd_id}_{_camel_case(name) or 'unnamed'}",
        "tags": [derive_tag(fqn)],
        "parameters": parameters,
        "responses": {
            "200": {
                "description": "Envelope response",
                "content": {
                    "application/json": {
                        "schema": {"$ref": "#/components/schemas/Envelope"}
                    }
                },
            }
        },
    }
    if api == "portal" or cmd_id in BACKEND_LOGIN_IDS:
        op["security"] = []
    return op


INFO_DESCRIPTION = """\
Command-style RPC API. Real URL pattern is:

    POST /api_backend?c=<command_id>&<params>&aat=<token>   (backend)
    POST /api?c=<command_id>&<params>                       (portal)

The /c/{id} synthetic path templates below exist only to satisfy OpenAPI's
path-uniqueness constraint. Swagger UI's "Try it out" will hit the synthetic
URL and fail — use the real URL pattern when calling these commands.

The `data` field of every response is a JSON-encoded string; clients must
double-decode it.
"""


def build_doc(backend_ops: dict[int, dict], portal_ops: dict[int, dict]) -> dict:
    paths: dict[str, dict] = {}
    for cmd_id in sorted(backend_ops.keys()):
        paths[f"/api_backend/c/{cmd_id}"] = {"post": backend_ops[cmd_id]}
    for cmd_id in sorted(portal_ops.keys()):
        paths[f"/api/c/{cmd_id}"] = {"post": portal_ops[cmd_id]}
    return {
        "openapi": "3.0.3",
        "info": {
            "title": "SunwinKR Backend & Portal APIs",
            "version": "1.0.0",
            "description": INFO_DESCRIPTION,
        },
        "servers": [
            {"url": "/", "description": "Same origin as this Swagger UI (default — works for local serve and the FE-mounted nginx)"},
            {"url": "https://staging-admin.sunkr.bet", "description": "Staging admin (backend commands)"},
            {"url": "https://staging-play.sunkr.bet", "description": "Staging play (portal commands)"},
        ],
        "components": {
            "securitySchemes": {
                "AatAuth": {
                    "type": "apiKey",
                    "in": "query",
                    "name": "aat",
                    "description": "32-char hex admin auth token (8h TTL). Paste once via Authorize button — applies to every backend command except login (c=701) and all portal commands.",
                },
            },
            "schemas": {
                "Envelope": {
                    "type": "object",
                    "properties": {
                        "success": {"type": "boolean"},
                        "errorCode": {"type": "string"},
                        "data": {
                            "type": "string",
                            "description": "JSON-encoded string; double-decode on the client.",
                        },
                        "message": {"type": "string"},
                    },
                },
            },
        },
        "security": [{"AatAuth": []}],
        "paths": paths,
    }


def _collect_ops(api: str, xml_path: Path, source_root: Path) -> dict[int, dict]:
    cmds = parse_commands(xml_path.read_text(encoding="utf-8"))
    ops: dict[int, dict] = {}
    for cmd_id, name, fqn in cmds:
        src = resolve_source_path(fqn, [source_root])
        if src is None:
            print(f"warning: source not found for {fqn} (cmd {cmd_id})", file=sys.stderr)
            params: list[str] = []
        else:
            params = extract_params(src.read_text(encoding="utf-8", errors="replace"))
        ops[cmd_id] = build_operation(api=api, cmd_id=cmd_id, name=name,
                                      fqn=fqn, params=params)
    return ops


REPO_ROOT_GUESS = Path(__file__).resolve().parent.parent
DEFAULTS = {
    "backend_xml": REPO_ROOT_GUESS / "backend-master/api/VinPlayBackend/config/api_backend.xml",
    "portal_xml":  REPO_ROOT_GUESS / "backend-master/api/VinPlayPortal/config/api_portal.xml",
    "backend_src": REPO_ROOT_GUESS / "backend-master/api/VinPlayBackend/src/main/java",
    "portal_src":  REPO_ROOT_GUESS / "backend-master/api/VinPlayPortal/src/main/java",
    "output":      REPO_ROOT_GUESS / "docs/openapi/openapi.json",
}


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description="Generate OpenAPI spec from sunwinkr XML+sources.")
    p.add_argument("--backend-xml", type=Path, default=DEFAULTS["backend_xml"])
    p.add_argument("--portal-xml",  type=Path, default=DEFAULTS["portal_xml"])
    p.add_argument("--backend-src", type=Path, default=DEFAULTS["backend_src"])
    p.add_argument("--portal-src",  type=Path, default=DEFAULTS["portal_src"])
    p.add_argument("--output",      type=Path, default=DEFAULTS["output"])
    args = p.parse_args(argv)

    backend_ops = _collect_ops("backend", args.backend_xml, args.backend_src)
    portal_ops  = _collect_ops("portal",  args.portal_xml,  args.portal_src)
    doc = build_doc(backend_ops=backend_ops, portal_ops=portal_ops)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(doc, indent=2, sort_keys=False) + "\n",
                           encoding="utf-8")
    print(f"wrote {args.output} ({len(backend_ops)} backend + {len(portal_ops)} portal ops)",
          file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
