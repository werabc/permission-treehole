$ErrorActionPreference = "Continue"
$env:JAVA_HOME = "C:\Users\Lenovo\.jdks\ms-17.0.17"
$env:PATH = "$env:JAVA_HOME\bin;C:\Users\Lenovo\apache-maven-3.9.9\bin;$env:PATH"

Write-Host "=== Building Project ===" -ForegroundColor Green
Set-Location "D:\s1\permission-admin"
& mvn clean package -DskipTests 2>&1 | Select-Object -Last 5

Write-Host "`n=== Stopping Old Processes ===" -ForegroundColor Yellow
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 3

Write-Host "`n=== Starting Backend ===" -ForegroundColor Green
Set-Location "permission-api"
Start-Process -FilePath "java" -ArgumentList "-jar", "target\permission-api-1.0.0.jar", "--spring.profiles.active=local", "--server.port=8083" -RedirectStandardOutput "D:\s1\backend.log" -RedirectStandardError "D:\s1\backend_error.log" -WindowStyle Hidden

Write-Host "Waiting for startup..."
Start-Sleep -Seconds 30

Write-Host "`n=== Testing ===" -ForegroundColor Cyan
$r1 = Invoke-RestMethod -Uri "http://localhost:8083/api/th/auth/register" -Method POST -ContentType "application/json" -Body '{"username":"testuser","password":"123456"}'
Write-Host "Register: $($r1 | ConvertTo-Json -Compress)"

$r2 = Invoke-RestMethod -Uri "http://localhost:8083/api/th/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"testuser","password":"123456"}'
Write-Host "Login: $($r2 | ConvertTo-Json -Compress)"
