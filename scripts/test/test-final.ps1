Write-Host "=== Final Service Test ===" -ForegroundColor Green

Write-Host "`n1. Backend (http://localhost:8081):" -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:8081/api/test/ping' -UseBasicParsing -TimeoutSec 5
    Write-Host "   OK - $($r.Content)" -ForegroundColor Green
} catch {
    Write-Host "   FAIL - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n2. Permission UI (http://localhost:5173):" -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:5173' -UseBasicParsing -TimeoutSec 5
    Write-Host "   OK - Status $($r.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "   FAIL - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n3. Treehole Web (http://localhost:3000):" -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:3000' -UseBasicParsing -TimeoutSec 5
    Write-Host "   OK - Status $($r.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "   FAIL - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n4. Login Test:" -ForegroundColor Yellow
try {
    $body = @{ username = "admin"; password = "Admin@1234" } | ConvertTo-Json
    $r = Invoke-WebRequest -Uri 'http://localhost:8081/api/auth/login' -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 10
    Write-Host "   OK - Login successful" -ForegroundColor Green
    $data = $r.Content | ConvertFrom-Json
    Write-Host "   Token: $($data.data.accessToken.Substring(0, 50))..." -ForegroundColor Cyan
} catch {
    Write-Host "   FAIL - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== Services Ready ===" -ForegroundColor Green
