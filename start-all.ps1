$env:JAVA_HOME = "C:\Users\Lenovo\.jdks\ms-17.0.17"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH

Write-Host "=== Checking Backend (port 8081) ===" -ForegroundColor Yellow
$backendRunning = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue
if (-not $backendRunning) {
    Write-Host "Backend not running! Please run start-backend.ps1 first." -ForegroundColor Red
    exit 1
}
Write-Host "Backend is running on port 8081" -ForegroundColor Green

Write-Host ""
Write-Host "=== Starting Permission UI (Admin) on port 5173 ===" -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location 'D:\s1\permission-ui'; npm run dev"

Write-Host "=== Starting Treehole Web on port 3000 ===" -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location 'D:\s1\treehole-web'; npm run dev"

Write-Host ""
Write-Host "=== All Services ===" -ForegroundColor Green
Write-Host "Backend API:      http://localhost:8081" -ForegroundColor Cyan
Write-Host "Permission UI:    http://localhost:5173" -ForegroundColor Cyan
Write-Host "Treehole Web:     http://localhost:3000" -ForegroundColor Cyan
Write-Host ""
Write-Host "=== Login Credentials ===" -ForegroundColor Green
Write-Host "Admin: admin / Admin@1234" -ForegroundColor Cyan
Write-Host "Tech:  tech / Admin@1234" -ForegroundColor Cyan
