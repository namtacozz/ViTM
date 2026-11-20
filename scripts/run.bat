@echo off
chcp 65001 >nul
cd /d "%~dp0.."
call mvn -q compile exec:java
if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the chay ung dung qua Maven. Kiem tra lai Java 21 va Maven 3.9+!
    pause
)
