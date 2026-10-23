$game = 'D:\Minecraft choi chung'
$jar = 'E:\Document\New project 5\Claude\1\cavehorror\dist\cavemind-4.5.3.jar'
$log = 'E:\Document\New project 5\Claude\1\cavehorror\dist\swap_453.log'
while (Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.WorkingSet64 -gt 500MB }) { Start-Sleep -Seconds 5 }
Start-Sleep -Seconds 5
Move-Item -Force (Join-Path $game 'mods\cavemind-4.5.1.jar') 'E:\Document\New project 5\Claude\1\cavehorror\dist\old_installed\' -ErrorAction SilentlyContinue
Copy-Item -Force $jar (Join-Path $game 'mods\cavemind-4.5.3.jar')
Get-ChildItem (Join-Path $game 'mods') -Filter 'cavemind*' | ForEach-Object { $_.Name } | Out-File $log -Encoding utf8
