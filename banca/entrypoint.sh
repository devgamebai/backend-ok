#!/bin/sh
# GitLab #30 — template BanCa config.json from .env at container start.
#
# Reason: the .NET build at banca/BanCaLiteNet/out/config.json on the host
# used to hold baked MySQL + Redis credentials that silently drifted from
# .env on every rotation (2026-04-22 outage). Every other container reads
# .env via env_file; BanCa didn't because the .NET code reads config.json.
# This entrypoint rewrites the credential fields from env each boot so the
# container is single-source-of-truth on .env (never the checked-in file).
#
# Fields we patch (all optional — left alone if env var not set):
#   MYSQL_USER / MYSQL_PASSWORD  → mysql-connection uid=/pwd=
#   REDIS_PASSWORD               → redis-password
#   BANCA_XXENG_BACKEND          → xxeng-backend (main portal URL)
#
# sed is POSIX; no jq required — keeps the image minimal.

set -eu

CONFIG=/app/config.json

if [ ! -f "$CONFIG" ]; then
  echo "[banca/entrypoint] WARN: $CONFIG not found — nothing to template. Launching dotnet anyway."
  exec dotnet BanCaLiteNet.dll "$@"
fi

if [ -n "${MYSQL_USER:-}" ]; then
  sed -i -E "s|(mysql-connection\"[[:space:]]*:[[:space:]]*\"[^\"]*uid=)[^;]+|\1${MYSQL_USER}|" "$CONFIG"
fi
if [ -n "${MYSQL_PASSWORD:-}" ]; then
  sed -i -E "s|(mysql-connection\"[[:space:]]*:[[:space:]]*\"[^\"]*pwd=)[^;]+|\1${MYSQL_PASSWORD}|" "$CONFIG"
fi
if [ -n "${REDIS_PASSWORD:-}" ]; then
  sed -i -E "s|(\"redis-password\"[[:space:]]*:[[:space:]]*\")[^\"]*|\1${REDIS_PASSWORD}|" "$CONFIG"
fi
if [ -n "${BANCA_XXENG_BACKEND:-}" ]; then
  sed -i -E "s|(\"xxeng-backend\"[[:space:]]*:[[:space:]]*\")[^\"]*|\1${BANCA_XXENG_BACKEND}|" "$CONFIG"
fi

echo "[banca/entrypoint] config.json templated from env. Launching dotnet BanCaLiteNet.dll."
exec dotnet BanCaLiteNet.dll "$@"
