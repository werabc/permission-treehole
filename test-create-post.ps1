# Test creating a post and check if it gets logged
$body = @{ username = "admin"; password = "Admin@1234" } | ConvertTo-Json
$r = Invoke-WebRequest -Uri 'http://localhost:8081/api/auth/login' -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 10
$data = $r.Content | ConvertFrom-Json
$token = $data.data.accessToken

Write-Host "Logged in, token obtained"

# Create a post
$postBody = @{
    content = "Test post for logging verification"
    categoryId = 1
    isAnonymous = 0
} | ConvertTo-Json

Write-Host "`nCreating post..."
$r2 = Invoke-WebRequest -Uri 'http://localhost:8081/api/th/post' -Method POST -ContentType 'application/json' -Body $postBody -Headers @{ Authorization = "Bearer $token" } -UseBasicParsing -TimeoutSec 10
Write-Host "Response: $($r2.Content)"
