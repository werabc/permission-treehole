@echo off
set JAVA_HOME=C:\Users\Lenovo\.jdks\ms-17.0.17
set MAVEN_HOME=C:\Users\Lenovo\apache-maven-3.9.9
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%

echo === 构建项目 ===
cd /d D:\s1\permission-admin
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo 构建失败!
    exit /b 1
)
echo === 构建成功 ===

echo === 停止旧进程 ===
taskkill /F /IM java.exe 2>nul
timeout /t 3 /nobreak >nul

echo === 启动后端 ===
cd permission-api
start /B java -jar target\permission-api-1.0.0.jar --spring.profiles.active=local --server.port=8083 > D:\s1\backend.log 2>&1
echo 后端启动中，等待30秒...
timeout /t 30 /nobreak >nul

echo === 测试 ===
curl -s -X POST http://localhost:8083/api/th/auth/register -H "Content-Type: application/json" -d "{\"username\":\"testuser\",\"password\":\"123456\"}"
echo.
curl -s -X POST http://localhost:8083/api/th/auth/login -H "Content-Type: application/json" -d "{\"username\":\"testuser\",\"password\":\"123456\"}"
echo.
