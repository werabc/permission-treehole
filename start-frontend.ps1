$env:PATH = "C:\Program Files\nodejs;" + $env:PATH

Write-Host "=== Starting Permission UI (Admin) on port 5173 ===" -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location 'D:\s1\permission-ui'; npm run dev"

Write-Host "=== Starting Treehole Web on port 3000 ===" -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location 'D:\s1\treehole-web'; npm run dev"

Write-Host "=== Starting LAN Proxy on port 3081 ===" -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location 'D:\s1'; node dsh-lan-proxy.mjs"

Write-Host ""
Write-Host "All services starting..." -ForegroundColor Yellow
Write-Host "Backend: http://localhost:8081" -ForegroundColor Cyan
Write-Host "Permission UI: http://localhost:5173" -ForegroundColor Cyan
Write-Host "Treehole Web: http://localhost:3000" -ForegroundColor Cyan
Write-Host "LAN Proxy: http://localhost:3081" -ForegroundColor Cyan
