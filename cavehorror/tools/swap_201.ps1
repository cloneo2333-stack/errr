$game = 'D:\Minecraft choi chung'
$jar = 'E:\Document\New project 5\Claude\1\cavehorror\dist\cavemind-2.0.1.jar'
while (Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.WorkingSet64 -gt 500MB -or $_.Id -eq 10056 }) { Start-Sleep -Seconds 5 }
Start-Sleep -Seconds 5
Remove-Item -Force (Join-Path $game 'mods\cavemind-2.0.0.jar') -ErrorAction SilentlyContinue
Copy-Item -Force $jar (Join-Path $game 'mods\cavemind-2.0.1.jar')
Remove-Item -Force (Join-Path $game 'CaveHorror_TLauncher_1.8\cavemind-2.0.0.jar') -ErrorAction SilentlyContinue
Copy-Item -Force $jar (Join-Path $game 'CaveHorror_TLauncher_1.8\cavemind-2.0.1.jar')
Get-ChildItem (Join-Path $game 'mods') -Filter 'cavemind*' | ForEach-Object { $_.Name }
