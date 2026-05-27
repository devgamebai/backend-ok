FROM python:3.11-slim

WORKDIR /app

RUN pip install --no-cache-dir \
    mysql-connector-python==8.3.0 \
    requests==2.31.0

COPY audit-wallet-bot.py /app/audit-wallet-bot.py

# Health endpoint runs on 8889 inside container
EXPOSE 8889

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD python3 -c "import urllib.request,sys; r=urllib.request.urlopen('http://127.0.0.1:8889/').read(); print(r); sys.exit(0)" || exit 1

CMD ["python3", "/app/audit-wallet-bot.py"]
