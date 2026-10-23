import os, re, shutil, zipfile

PROFILE = r'E:\Modrinth\profiles\Cave Horror Project 1'
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
NAME = 'CaveHorror_TLauncher_1.8'
STAGE = os.path.join(ROOT, 'dist', NAME)
GAME = os.path.join(STAGE, 'game')
SKIP_CONFIG = {os.path.join('ftbquests', 'quests_en_backup')}

BAT = r"""@echo off
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
"""

GUIDE = """CAVE HORROR PROJECT 1.8 (bản Việt hóa) CHO TLAUNCHER
=====================================================

Gói này gồm 166 mod, bản Việt hóa toàn bộ, sổ nhiệm vụ tiếng Việt và mod Cave Mind 1.8.0
(The Man From The Fog, The Damned, Herobrine được viết lại, có cốt truyện trong thế giới).

CẦN CÓ
• TLauncher bản mới.
• Forge cho Minecraft 1.20.1, bản 47.4.0 trở lên (khuyên dùng 47.4.23).
• Máy có từ 8 GB RAM trở lên, cấp cho game 6 GB.

CÀI ĐẶT (5 bước)
1. Mở TLauncher. Ở ô chọn phiên bản, chọn "Forge 1.20.1" rồi bấm "Vào game" một lần
   để TLauncher tự tải Forge. Khi vào tới màn hình chính của Minecraft thì thoát game.
   Nếu TLauncher chỉ có Forge cũ hơn 47.4.0: tải "Installer" của Forge 1.20.1 bản 47.4.x
   ở trang files.minecraftforge.net, chạy nó, chọn "Install client", trỏ vào thư mục
   .minecraft, bấm OK. Bản Forge đó sẽ hiện trong danh sách của TLauncher.
2. Giải nén file zip này ra một chỗ bất kỳ (ví dụ Desktop).
3. Chạy file CaiDat.bat. Nó tự tìm thư mục game mà TLauncher đang dùng (kể cả khi bạn đã
   chuyển sang ổ khác), hỏi lại cho chắc, cất mods, config, gói tài nguyên cũ vào thư mục
   sao_luu_truoc_CaveHorror_<ngày giờ> ngay trong thư mục game, rồi chép modpack vào.
   Mod skin của TLauncher được giữ lại.
   Không muốn chạy file .bat thì chép tay: mở thư mục "game", chép toàn bộ những gì bên
   trong vào thư mục game của TLauncher (xem trong phần Cài đặt của TLauncher, mặc định
   là %APPDATA%\\.minecraft), đồng ý ghi đè.
4. Trong TLauncher bấm biểu tượng bánh răng (Cài đặt), kéo "Bộ nhớ" lên 6144 MB
   (máy 16 GB có thể để 8192 MB).
5. Chọn Forge 1.20.1 và bấm "Vào game". Lần đầu mở mất khoảng 3 tới 6 phút, đừng tắt.

SAU KHI VÀO GAME
• Ngôn ngữ đã đặt sẵn tiếng Việt, gói "Cave Horror Tieng Viet" đã bật.
• Muốn bật shader: Tùy chọn > Tùy chọn hình ảnh > Gói shader, chọn Complementary
  (máy yếu thì để tắt).
• Độ dày đặc của các sinh vật chỉnh trong config\\cavemind-common.toml:
  intensity = 1.0 (0,5 là thưa hơn, 2 là dày gấp đôi), mercy = true (giảm ức chế:
  không hù khi bạn đang mở túi đồ, đang đánh nhau, vừa chết, treo máy...).

MẸO SỐNG SÓT (không tiết lộ hết)
• Đọc kỹ những gì bạn tìm thấy trong thế giới. Sổ ghi chép và tấm biển không nói dối.
• Có những thứ sợ tiếng chuông. Nhưng chúng học rất nhanh.
• Khi bị tìm, hãy ngồi thụp xuống ở chỗ tối và đừng nhìn.

GỠ CÀI ĐẶT
Xóa các thư mục mods, config, kubejs, defaultconfigs, resourcepacks, shaderpacks,
fancymenu_data trong .minecraft, rồi chép lại đồ cũ từ thư mục sao_luu_truoc_CaveHorror_...

CHƠI CÙNG BẠN
Mở thế giới cho mạng LAN (hoặc dùng phần mềm tạo LAN ảo như Radmin VPN). Mọi người
đều phải cài đúng gói này.
"""


def copytree(src, dst, skip=()):
    for root, dirs, files in os.walk(src):
        rel = os.path.relpath(root, src)
        if any(rel == s or rel.startswith(s + os.sep) for s in skip):
            continue
        os.makedirs(os.path.join(dst, rel), exist_ok=True)
        for f in files:
            shutil.copy2(os.path.join(root, f), os.path.join(dst, rel, f))


def main():
    shutil.rmtree(STAGE, ignore_errors=True)
    os.makedirs(GAME)
    os.makedirs(os.path.join(GAME, 'mods'))
    for f in os.listdir(os.path.join(PROFILE, 'mods')):
        if f.endswith('.jar'):
            shutil.copy2(os.path.join(PROFILE, 'mods', f), os.path.join(GAME, 'mods', f))
    copytree(os.path.join(PROFILE, 'config'), os.path.join(GAME, 'config'), SKIP_CONFIG)
    for d in ('defaultconfigs', 'kubejs', 'fancymenu_data'):
        if os.path.isdir(os.path.join(PROFILE, d)):
            copytree(os.path.join(PROFILE, d), os.path.join(GAME, d))
    opts = open(os.path.join(PROFILE, 'options.txt'), encoding='utf-8').read()
    packs = re.search(r'^resourcePacks:\[(.*)\]$', opts, re.M).group(1)
    names = [x.strip('"')[5:] for x in packs.split('","') if x.strip('"').startswith('file/')]
    rp = os.path.join(GAME, 'resourcepacks')
    os.makedirs(rp)
    for n in names:
        s = os.path.join(PROFILE, 'resourcepacks', n)
        if os.path.isdir(s):
            copytree(s, os.path.join(rp, n))
        elif os.path.exists(s):
            shutil.copy2(s, rp)
        else:
            print('missing pack', n)
    sp = os.path.join(GAME, 'shaderpacks')
    os.makedirs(sp)
    for f in os.listdir(os.path.join(PROFILE, 'shaderpacks')):
        if f.endswith('.zip'):
            shutil.copy2(os.path.join(PROFILE, 'shaderpacks', f), sp)
    opts = re.sub(r'^fullscreen:true$', 'fullscreen:false', opts, flags=re.M)
    open(os.path.join(GAME, 'options.txt'), 'w', encoding='utf-8', newline='\n').write(opts)
    for bad in ('claude', os.path.join('server_scripts', 'claude_bridge.js')):
        p = os.path.join(GAME, 'kubejs', bad)
        if os.path.exists(p):
            raise SystemExit('dev bridge still present: ' + p)
    open(os.path.join(STAGE, 'CaiDat.bat'), 'w', encoding='utf-8', newline='\r\n').write(BAT)
    open(os.path.join(STAGE, 'HUONG_DAN_CAI_DAT.txt'), 'w', encoding='utf-8-sig', newline='\r\n').write(GUIDE)
    out = os.path.join(ROOT, 'dist', NAME + '.zip')
    n = 0
    with zipfile.ZipFile(out, 'w') as z:
        for root, _, files in os.walk(STAGE):
            for f in files:
                p = os.path.join(root, f)
                arc = os.path.join(NAME, os.path.relpath(p, STAGE))
                comp = zipfile.ZIP_STORED if f.endswith(('.jar', '.zip', '.png', '.ogg')) else zipfile.ZIP_DEFLATED
                z.write(p, arc, compress_type=comp)
                n += 1
    print(out, n, 'files', os.path.getsize(out) // (1 << 20), 'MB')


main()
