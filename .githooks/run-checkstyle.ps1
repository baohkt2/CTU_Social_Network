$ErrorActionPreference = "Stop"

$services = @(
  "api-gateway",
  "auth-service",
  "chat-service",
  "eureka-server",
  "media-service",
  "post-service",
  "recommend-service/java-api",
  "user-service"
)

$changed = git diff --cached --name-only -- "*.java"
if (-not $changed) {
  Write-Host "No staged Java files. Skipping Checkstyle."
  exit 0
}

$servicesToCheck = @{}
foreach ($file in $changed) {
  foreach ($service in $services) {
    if ($file.StartsWith($service + "/")) {
      $servicesToCheck[$service] = $true
    }
  }
}

if ($servicesToCheck.Count -eq 0) {
  Write-Host "No Java service changes detected for Checkstyle."
  exit 0
}

foreach ($service in $servicesToCheck.Keys) {
  Write-Host "Running Checkstyle in $service ..."
  Push-Location $service
  try {
    ./mvnw -q -DskipTests checkstyle:check
  }
  finally {
    Pop-Location
  }
}

Write-Host "Checkstyle passed for staged Java changes."
