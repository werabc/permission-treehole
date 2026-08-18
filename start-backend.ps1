$env:JAVA_HOME = "C:\Users\Lenovo\.jdks\ms-17.0.17"
$env:M2_HOME = "C:\Users\Lenovo\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16"
$env:PATH = "$env:JAVA_HOME\bin;$env:M2_HOME\bin;$env:PATH"
Set-Location "D:\s1\permission-admin"

Write-Host "=== Packaging Project ===" -ForegroundColor Green
mvn package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "Package failed, retrying with output..." -ForegroundColor Red
    mvn package -DskipTests
    exit 1
}
Write-Host "Package successful!" -ForegroundColor Green

Write-Host ""
Write-Host "=== Starting Spring Boot Application ===" -ForegroundColor Green
$jarPath = "D:\s1\permission-admin\permission-api\target\permission-api-1.0.0.jar"
& "$env:JAVA_HOME\bin\java.exe" -jar $jarPath --spring.profiles.active=local
