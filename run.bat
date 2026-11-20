@echo off
chcp 65001 >nul
echo ===================================================================
echo     CampusRoom — Aplikasi Manajemen & Booking Ruangan Kampus
echo ===================================================================

REM 1. Coba jalankan via Apache Maven jika terpasang
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Menjalankan menggunakan Apache Maven...
    call mvn compile exec:java
    goto end
)

REM 2. Fallback: kompilasi dan jalankan dengan Java lokal + lib/*.jar
echo [INFO] Maven tidak terdeteksi, menjalankan mode native Java...
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin -cp "lib/*" src\main\java\com\campusroom\*.java src\main\java\com\campusroom\model\*.java src\main\java\com\campusroom\dao\*.java src\main\java\com\campusroom\service\*.java src\main\java\com\campusroom\ui\*.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Gagal mengompilasi kode program. Pastikan JDK 17+ telah terpasang.
    pause
    exit /b %ERRORLEVEL%
)

REM Salin resources images
if not exist "bin\images" mkdir bin\images
xcopy /E /I /Y src\main\resources\images\* bin\images\ >nul 2>nul

java -cp "bin;lib/*" com.campusroom.CampusRoomApp

:end
pause
