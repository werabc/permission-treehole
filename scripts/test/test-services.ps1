Write-Host "=== Testing Services ===" -ForegroundColor Green

Write-Host "`n1. Backend API (http://localhost:8081):" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8081/api/test/ping' -UseBasicParsing -TimeoutSec 5
    Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   Response: $($response.Content)" -ForegroundColor Cyan
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n2. Permission UI (http://localhost:5173):" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:5173' -UseBasicParsing -TimeoutSec 5
    Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n3. Treehole Web (http://localhost:3000):" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:3000' -UseBasicParsing -TimeoutSec 5
    Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n4. LAN Proxy (http://localhost:3081):" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:3081/api/test/ping' -UseBasicParsing -TimeoutSec 5
    Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   Response: $($response.Content)" -ForegroundColor Cyan
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== Login Credentials ===" -ForegroundColor Green
Write-Host "Admin Username: admin" -ForegroundColor Cyan
Write-Host "Admin Password: Admin@1234" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test Username: tech" -ForegroundColor Cyan
Write-Host "Test Password: Admin@1234" -ForegroundColor Cyan
