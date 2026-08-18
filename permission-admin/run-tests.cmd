@echo off
setlocal EnableDelayedExpansion
set JAVA_HOME=C:\Users\Lenovo\.jdks\ms-17.0.17
set M2_HOME=C:\Users\Lenovo\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%
cd /d D:\s1\permission-admin

echo ========================================
echo Java Version
echo ========================================
java -version
echo.

echo ========================================
echo Step 1: Compile Project
echo ========================================
call mvn compile -q
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Compilation failed!
    call mvn compile 2>&1
    exit /b 1
)
echo [PASS] Compilation successful

echo.
echo ========================================
echo Step 2: Run Unit Tests
echo ========================================
call mvn test -Dtest=JwtTokenProviderTest,GlobalExceptionHandlerTest -pl permission-framework
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Unit tests failed!
    exit /b 1
)
echo [PASS] Unit tests passed

echo.
echo ========================================
echo ALL TESTS PASSED
echo ========================================
endlocal
