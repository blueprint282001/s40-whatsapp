@echo off
title Nokia S40 WhatsApp Bridge Server & Auto-Tunnel
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

:: 3. Run Localtunnel with auto-restart loop
echo [2/2] Starting Public Internet Tunnel...
echo.
echo ====================================================
echo  Fixed Public URL for Nokia C2-01:
echo  http://nokia-c201-wa.loca.lt
echo ====================================================
echo.

:tunnel_loop
echo [%time%] Connecting tunnel...
call npx.cmd --yes localtunnel --port 3000 --subdomain nokia-c201-wa
echo [%time%] Tunnel disconnected. Reconnecting in 3 seconds...
timeout /t 3 /nobreak >nul
goto tunnel_loop
