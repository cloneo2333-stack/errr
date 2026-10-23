@echo off
setlocal EnableExtensions
chcp 65001 >nul
set "MC="
set "TLP=%APPDATA%\.tlauncher\tlauncher-2.0.properties"
if exist "%TLP%" (
  for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "$l = Get-Content -LiteralPath $env:TLP | Where-Object { $_ -like 'minecraft.gamedir=*' } | Select-Object -First 1; if ($l) { $l.Substring(18).Replace('\:', ':').Replace('\\', '\') }"`) do set "MC=%%i"
)
if not defined MC set "MC=%APPDATA%\.minecraft"
if not exist "%MC%\versions" goto ask
goto found

:ask
echo.
echo  Không tìm thấy thư mục game của TLauncher ở: %MC%
echo  Mở TLauncher, bấm bánh răng, xem dòng "Thư mục game" rồi dán đường dẫn vào đây.
set /p "MC=  Đường dẫn thư mục game: "
set "MC=%MC:"=%"
if not exist "%MC%\versions" (
  echo  "%MC%" không phải thư mục game Minecraft. Dừng lại.
  pause
  exit /b 1
)

:found
echo.
echo  CAVE HORROR PROJECT 1.8 cho TLauncher
echo  Sẽ cài vào: %MC%
echo.
choice /c CK /n /m "  Bấm C để cài, K để thoát: "
if errorlevel 2 exit /b 0
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "TS=%%i"
set "BK=%MC%\sao_luu_truoc_CaveHorror_%TS%"
mkdir "%BK%" >nul 2>&1
echo  Đang cất đồ cũ vào: %BK%
for %%d in (mods config defaultconfigs kubejs resourcepacks shaderpacks fancymenu_data) do (
  if exist "%MC%\%%d" move "%MC%\%%d" "%BK%\" >nul
)
if exist "%MC%\options.txt" copy /y "%MC%\options.txt" "%BK%\" >nul
echo  Đang chép modpack, chờ một chút...
robocopy "%~dp0game" "%MC%" /E /NFL /NDL /NJH /NJS /NP >nul
if errorlevel 8 (
  echo  Chép bị lỗi. Hãy chép tay mọi thứ trong thư mục game vào "%MC%"
  pause
  exit /b 1
)
if exist "%BK%\mods\tl_skin_cape*.jar" copy /y "%BK%\mods\tl_skin_cape*.jar" "%MC%\mods\" >nul
echo.
echo  XONG. Mở TLauncher, chọn Forge 1.20.1 rồi bấm Vào game.
echo  Nhớ chỉnh RAM trong TLauncher lên 6144 MB trở lên.
echo.
pause
