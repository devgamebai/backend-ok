# Swagger UI Guide

How to read, run, and update the Swagger UI for the sunwinkr backend + portal APIs.

**TL;DR:**
```bash
docker compose -f docker-compose.swagger.yml up -d
# open http://localhost:18080/swagger/
```
Click **Authorize** → paste `aat` token → pick a command → **Try it out** → **Execute**.

---

## What this is

The OpenAPI 3.0.3 spec at `docs/openapi/openapi.json` documents every command in `api_backend.xml` and `api_portal.xml` (≈640 operations across ~58 tags). It is **generated** from those XML files plus the processor source code under `backend-master/api/*/src/main/java/`. Don't edit it by hand — regenerate.

---

## Two ways to view it

| Mode | Command | URL | Try-it-out works? |
|---|---|---|---|
| **Local static** (read-only) | `bash scripts/serve-swagger.sh` | http://localhost:8000/ | ❌ Python http.server can't proxy to backend |
| **Docker proxy** (recommended) | `docker compose -f docker-compose.swagger.yml up -d` | http://localhost:18080/swagger/ | ✅ if `backend-api` & `portal-api` containers are healthy |

Static mode is fine for browsing the spec. Use the docker proxy mode whenever you actually want to call a command.

---

## Authentication: paste `aat` once, every backend call carries it

The spec exposes `aat` as a global **`apiKey` security scheme** (`AatAuth`, `in: query`). Swagger UI renders this as the **Authorize** button at the top right.

1. Get a fresh token by calling `POST /api_backend/c/701` with admin `un`/`pw` (the `c=701` operation has its security overridden to empty so it does not itself require an aat).
2. The response envelope's `data` field (double-decode the JSON string) contains an `adminToken`. That string is your `aat`.
3. Click **Authorize** in the Swagger UI, paste the token, click **Authorize** → **Close**.
4. Every subsequent backend call's "Try it out" automatically appends `&aat=<token>`.
5. The token has an **8-hour TTL** — when calls start failing with `errorCode 1001`, get a fresh one.

`c=701` (admin login) and **all portal commands** (`/api/c/*`) are explicitly marked `security: []` so they never demand an aat.

---

## Updating after API changes

Spec is generated, so changes to commands or processor params don't auto-propagate. After an API change, regenerate:

```bash
python3 scripts/gen_openapi.py
git add docs/openapi/openapi.json
git commit -m "openapi: regen after <change description>"
```

When to regenerate:

| Change | Regen needed? |
|---|---|
| Added / removed `<command>` in `api_backend.xml` or `api_portal.xml` | ✅ |
| Added / removed a `request.getParameter("xx")` in processor source | ✅ |
| Renamed `<name>` (the human description) | ✅ — updates `summary` and `operationId` |
| Moved a processor (FQN change) | ✅ |
| Internal Java change with no param impact | ❌ |
| New `.java` file but not registered in XML | ❌ — register first, then regen |

**No restart required.** The swagger-nginx container mounts `./docs/openapi:/var/www/swagger:ro` and the `openapi.json` location has `Cache-Control: no-cache`, so a browser F5 picks up the new spec immediately.

### CI guard (recommended)

To stop spec drift from sneaking past review, add a job that fails if `openapi.json` is out of date with the source. Append to `.gitlab-ci.yml`:

```yaml
validate-openapi:
  stage: validate
  image: python:3.11-alpine
  script:
    - python3 scripts/gen_openapi.py
    - git diff --exit-code docs/openapi/openapi.json
        || (echo "openapi.json is stale — run scripts/gen_openapi.py and commit" && exit 1)
```

Not yet wired — open task.

---

## Known limitations

- **Try-it-out URL ≠ real URL.** OpenAPI 3.0 forbids `?` in path templates, so the spec uses synthetic paths like `/api_backend/c/108`. The swagger-nginx proxy rewrites these back to the real `/api_backend?c=108` form. Without that proxy (e.g. when serving via `serve-swagger.sh`), Try-it-out hits the synthetic path and fails.
- **Response `data` is a JSON-encoded string.** Every command returns the same envelope: `{success, errorCode, data, message}`. The `data` field is itself a JSON-encoded string — clients must double-decode. Per-command schemas inside `data` are deferred (Phase 2 work).
- **Some commands ship with empty `params: []`.** ~78 processors live only in the precompiled fat JARs (`VinGameBase`, `VinOTP`, `VinPayment`) and have no source the regex extractor can read. Their commands appear in the spec with no documented params; consult the running backend or the team that owns them.
- **Required vs optional params is not detected.** All extracted params are emitted as optional `string` query params. The actual processor may reject a request that omits a param it requires.
- **No request body schema.** All commands are query-string only by convention; this matches actual usage.

---

## Troubleshooting

### `502 Bad Gateway` from swagger-nginx

Means the proxy is fine but the upstream Java API isn't responding. Check:

```bash
docker ps --format '{{.Names}}\t{{.Status}}' | grep -E 'backend-api|portal-api'
docker logs --tail 30 sunwinkr-backend-api | grep -vE 'TimerThread|^\s*at '
```

If the backend container shows `(health: starting)` for more than ~2 minutes or repeatedly crashes, look for `NoSuchMethodError`, MongoDB connection refused, or missing-table errors in the log. The `servlet-api-2.3.jar` classpath conflict was fixed in commit `1a5aabef` — if you see it again, ensure the Dockerfile still has the `rm -f /dist/libs/runtime/servlet-api-2.3.jar` line at line ~64.

### `Failed to fetch` in Swagger UI when serving locally

Don't open `docs/openapi/index.html` via `file://` — browsers block local-file fetches under CORS. Always go through HTTP, either `serve-swagger.sh` or the docker proxy.

### Port 18080 not bound after `docker compose ... up -d`

If `docker ps` shows `80/tcp` but no `0.0.0.0:18080->80/tcp`, Docker Desktop on Windows silently dropped the port mapping. Make sure `docker-compose.swagger.yml`'s `swagger-nginx` service lists **both** `default` and the external `backend` network. With only the external network, host port forwarding doesn't activate.

### `errorCode: 1001` on every backend call

The `aat` token is missing, expired, or invalid. Re-authorize via `c=701` and paste the fresh `adminToken` into the Authorize dialog.

---

## File map

| Path | Purpose |
|---|---|
| `scripts/gen_openapi.py` | Generator: parses XML + processor source → emits `openapi.json` |
| `scripts/test_gen_openapi.py` | Unit tests for the generator (`python3 scripts/test_gen_openapi.py -v`) |
| `scripts/serve-swagger.sh` | Local static-only viewer (no Try-it-out) |
| `docker-compose.swagger.yml` | Docker setup for full proxy mode (Try-it-out works) |
| `nginx/conf.d/swagger.conf` | Nginx config: `/swagger/` static + `/api_backend/c/<id>` rewrite to real URL |
| `docs/openapi/openapi.json` | Generated spec (committed for review/diff) |
| `docs/openapi/index.html` | Swagger UI 5.x loader (CDN-based) |

---

## Production deployment

The repo's `nginx/conf.d/default.conf` already has the `location /swagger/` block. The remaining piece is in the FE-team-managed compose at `/home/sunwinkr-dev/docker-compose.yml`: the `sunwinkr-fe-nginx` service must mount `docs/openapi` at `/var/www/swagger`. Add this to that file's `volumes:`:

```yaml
- ../sunwinkr/docs/openapi:/var/www/swagger:ro
```

After that mount lands and FE-nginx reloads, https://staging-admin.sunkr.bet/swagger/ will serve the docs (read-only — no rewrite proxy in production yet, so Try-it-out won't work against real staging until Phase 2 adds the rewrite to the FE-nginx config).
