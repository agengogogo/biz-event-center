# Starts the backend on the local demo profile: embedded SQLite only, no Docker
# and no external PostgreSQL/Redis/Kafka. Data file defaults to
# backend/admin-api/data/eventcenter.db.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dataDir = Join-Path $root "backend\admin-api\data"
New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
$env:BEC_SQLITE_PATH = Join-Path $dataDir "eventcenter.db"
Push-Location (Join-Path $root "backend")
try {
  & mvn -q -DskipTests install
  & mvn -pl admin-api spring-boot:run "-Dspring-boot.run.profiles=local"
}
finally {
  Pop-Location
}
