@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ===================================================
echo     KHOI TAO CO SO DU LIEU ATM (atm_db)
echo ===================================================
set /p MYSQL_PASS="Nhap mat khau root MySQL: "
"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p%MYSQL_PASS% < schema.sql
if %ERRORLEVEL% equ 0 (
    echo.
    echo [THANH CONG] Da khoi tao database atm_db va du lieu mau!
) else (
    echo.
    echo [THAT BAI] Khong the nap schema.sql. Vui long kiem tra lai mat khau!
)
pause
