$ErrorActionPreference = "Stop"

Set-Location -LiteralPath $PSScriptRoot

if (-not (Test-Path -LiteralPath ".env")) {
    Copy-Item -LiteralPath ".env.example" -Destination ".env"
    Write-Warning "Created .env from .env.example. Change DB_PASSWORD before production use."
}

Write-Host "Building and testing all services..."
& .\gradlew.bat clean build --no-daemon
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE"
}

Write-Host "Building Docker images and starting the platform..."
docker compose -f docker-compose.yml up -d --build
if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose failed with exit code $LASTEXITCODE"
}

docker compose -f docker-compose.yml ps
Write-Host "Gateway: http://localhost:6767"
