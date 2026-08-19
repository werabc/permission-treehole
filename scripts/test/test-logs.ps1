$body = @{ username = "admin"; password = "Admin@1234" } | ConvertTo-Json
$r = Invoke-WebRequest -Uri 'http://localhost:8081/api/auth/login' -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 10
$data = $r.Content | ConvertFrom-Json
$token = $data.data.accessToken

Write-Host "Token: $($token.Substring(0, 30))..."

Write-Host "`nTesting Operation Log API..."
$r2 = Invoke-WebRequest -Uri 'http://localhost:8081/api/log/operation/page?pageNum=1&pageSize=10' -Headers @{ Authorization = "Bearer $token" } -UseBasicParsing -TimeoutSec 10
Write-Host $r2.Content
