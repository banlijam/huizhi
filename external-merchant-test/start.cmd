@echo off
setlocal
cd /d "%~dp0"
if not exist .env (
  copy /y .env.example .env >nul
  echo Created .env from .env.example. Configure the HuizhiPay test API key before creating an order.
)
node --experimental-sqlite src\server.js
