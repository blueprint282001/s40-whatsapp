@echo off
title Nokia S40 WhatsApp Bridge Server & Cloudflare Tunnel
echo ====================================================
echo  Starting Nokia S40 WhatsApp Cloud Bridge & Tunnel
echo ====================================================
echo.

cd /d "%~dp0server"

:: 1. Start Server in background if not running
echo [1/2] Starting Node.js Bridge Server on port 3000...
start /b node server.js

:: 2. Wait 3 seconds
timeout /t 3 /nobreak >nul

:: 3. Run Cloudflare Tunnel
echo [2/2] Starting High-Speed Cloudflare Tunnel...
echo.
echo ====================================================
echo  Active Public URL for Nokia C2-01:
echo  http://cast-deposit-vic-riding.trycloudflare.com
echo ====================================================
echo.

cd /d "%~dp0tools"
cloudflared.exe tunnel --url http://localhost:3000
