$game = 'D:\Minecraft choi chung'
$jar = 'E:\Document\New project 5\Claude\1\cavehorror\dist\cavemind-1.8.1.jar'
$bak = Join-Path $game 'sao_luu_cavemind_1.8.0'
Wait-Process -Id 28384 -ErrorAction SilentlyContinue
Start-Sleep -Seconds 5
New-Item -ItemType Directory -Force $bak | Out-Null
$old = Join-Path $game 'mods\cavemind-1.8.0.jar'
if (Test-Path $old) { Move-Item -Force $old $bak }
Copy-Item -Force $jar (Join-Path $game 'mods\cavemind-1.8.1.jar')
$cfg = Join-Path $game 'config\cavemind-common.toml'
Copy-Item -Force $cfg (Join-Path $bak 'cavemind-common.toml')
$t = [IO.File]::ReadAllText($cfg)
$t = [regex]::Replace($t, '(\[herobrine\][\s\S]*?\bstart_day = )3\b', '${1}2')
[IO.File]::WriteAllText($cfg, $t, (New-Object Text.UTF8Encoding $false))
Get-ChildItem (Join-Path $game 'mods') -Filter 'cavemind*' | ForEach-Object { $_.Name }
