$env:JAVA_HOME = "C:\Users\Lenovo\.jdks\ms-17.0.17"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
Set-Location "D:\s1\permission-admin"
$jarPath = "D:\s1\permission-admin\permission-api\target\permission-api-1.0.0.jar"
& "$env:JAVA_HOME\bin\java.exe" -jar $jarPath --spring.profiles.active=local
