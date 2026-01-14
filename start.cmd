@echo off
REM start.cmd - Launch MP3Org (backend + frontend) on Windows
REM
REM Usage: start.cmd
REM
REM Starts both the Spring Boot backend (port 9090) and
REM React frontend (port 5173), then opens the browser.

setlocal

echo ==========================================
echo   Starting MP3Org
echo ==========================================
echo.

cd /d "%~dp0"

REM Check if frontend dependencies are installed
if not exist "frontend\node_modules" (
    echo Installing frontend dependencies...
    cd frontend
    call npm install
    cd ..
    echo.
)

REM Start backend in a new window
echo Starting backend (port 9090)...
start "MP3Org Backend" cmd /c "gradle21.cmd bootRun"

REM Wait for backend to be ready
echo Waiting for backend...
:wait_backend
timeout /t 2 /nobreak >nul
curl -s http://localhost:9090/api/v1/music/count >nul 2>&1
if errorlevel 1 goto wait_backend
echo Backend ready.

REM Start frontend in a new window
echo Starting frontend (port 5173)...
cd frontend
start "MP3Org Frontend" cmd /c "npm run dev"
cd ..

REM Wait for frontend
echo Waiting for frontend...
timeout /t 5 /nobreak >nul

echo.
echo ==========================================
echo   MP3Org is running!
echo.
echo   Open: http://localhost:5173
echo.
echo   Close the backend and frontend windows to stop
echo ==========================================

REM Open browser
timeout /t 2 /nobreak >nul
start http://localhost:5173

echo.
echo Press any key to stop both services...
pause >nul

REM Kill processes on ports
echo Shutting down...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :9090') do taskkill /PID %%a /F >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173') do taskkill /PID %%a /F >nul 2>&1

endlocal
