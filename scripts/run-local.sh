#!/usr/bin/env bash
# Starts the backend on the local demo profile: embedded SQLite only, no Docker
# and no external PostgreSQL/Redis/Kafka. Data file defaults to
# backend/admin-api/data/eventcenter.db.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
data_dir="$root/backend/admin-api/data"
mkdir -p "$data_dir"
export BEC_SQLITE_PATH="$data_dir/eventcenter.db"
cd "$root/backend"
mvn -q -DskipTests install
mvn -pl admin-api spring-boot:run -Dspring-boot.run.profiles=local
