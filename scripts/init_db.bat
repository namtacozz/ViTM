@echo off
chcp 65001 >nul
cd /d "%~dp0.."
echo ===================================================
echo     KHOI TAO CO SO DU LIEU ATM (atm_db)
echo ===================================================
set /p MYSQL_PASS="Nhap mat khau root MySQL: "
"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p%MYSQL_PASS% < database\schema.sql
if %ERRORLEVEL% neq 0 (
    echo [THAT BAI] Khong the nap database\schema.sql!
    pause
    exit /b 1
)
"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p%MYSQL_PASS% < database\seed.sql
if %ERRORLEVEL% equ 0 (
    echo [THANH CONG] Da khoi tao schema va du lieu mau atm_db!
) else (
    echo [THAT BAI] Khong the nap database\seed.sql!
)
pause
