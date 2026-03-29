# PowerShell script to run auth-service with .env.cloud variables

if (Test-Path ".env.cloud") {
    $envVars = Get-Content ".env.cloud" | Where-Object { $_ -match '=' -and $_ -notmatch '^#' }
    foreach ($line in $envVars) {
        $name, $value = $line -split '=', 2
        $value = $value.Trim().Trim('"').Trim("'")
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
    Write-Host "Env variables from .env.cloud loaded."
    $env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
    $env:EUREKA_CLIENT_SERVICEURL_DEFAULTZONE="http://localhost:8761/eureka/"
    Write-Host "Overriding Kafka and Eureka to localhost for host-based execution."
} else {
    Write-Error ".env.cloud not found!"
    exit 1
}

$env:SPRING_PROFILES_ACTIVE="cloud"
cd auth-service
java -jar target/auth-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=cloud
