Write-Host "=== Testing Login API ===" -ForegroundColor Green

$loginBody = @{
    username = "admin"
    password = "Admin@1234"
} | ConvertTo-Json

Write-Host "`n1. Admin Login (http://localhost:8081/api/auth/login):" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8081/api/auth/login' -Method POST -ContentType 'application/json' -Body $loginBody -UseBasicParsing -TimeoutSec 10
    Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   Response: $($response.Content)" -ForegroundColor Cyan
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n2. Get Captcha (http://localhost:8081/api/auth/captcha):" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8081/api/auth/captcha' -UseBasicParsing -TimeoutSec 10
    Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Green
    $data = $response.Content | ConvertFrom-Json
    Write-Host "   Captcha Key: $($data.data.captchaKey)" -ForegroundColor Cyan
} catch {
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== All Services Running ===" -ForegroundColor Green
Write-Host "Backend API:     http://localhost:8081" -ForegroundColor Cyan
Write-Host "Permission UI:   http://localhost:5173" -ForegroundColor Cyan
Write-Host "Treehole Web:    http://localhost:3000" -ForegroundColor Cyan
Write-Host "LAN Proxy:       http://localhost:3081" -ForegroundColor Cyan
