#!/usr/bin/env bash
# Front/back decoupling verification: drives every capability only through HTTP APIs,
# without starting the admin front-end.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-admin123}"

command -v jq >/dev/null 2>&1 || {
  echo "jq is required" >&2
  exit 1
}

echo "== 1. login =="
TOKEN=$(curl -sf -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" | jq -r .token)
AUTH="Authorization: Bearer $TOKEN"

echo "== 2. register source =="
SOURCE=$(curl -sf -X POST "$BASE_URL/api/sources" -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"e2e-source-$RANDOM\",\"sourceType\":\"IOT\"}")
SRC_ID=$(echo "$SOURCE" | jq -r .id)
SRC_SECRET=$(echo "$SOURCE" | jq -r .secret)

echo "== 3. ingest event (external source) =="
EVENT=$(curl -sf -X POST "$BASE_URL/api/ingest/events" \
  -H "X-Source-Id: $SRC_ID" -H "X-Source-Secret: $SRC_SECRET" \
  -H 'Content-Type: application/json' \
  -d '{"eventType":"e2e.signal","occurredAt":"2026-01-02T03:04:05Z","severity":"MAJOR","location":"Gate 1","payload":{"value":7}}')
EVENT_ID=$(echo "$EVENT" | jq -r .eventId)
echo "eventId=$EVENT_ID accepted=$(echo "$EVENT" | jq -r .accepted)"

echo "== 4. notification channel + template + subscription =="
CHANNEL=$(curl -sf -X POST "$BASE_URL/api/notification-channels" -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d '{"name":"e2e-channel","type":"WEBHOOK","config":{"url":"http://127.0.0.1:9/notify"}}')
CHANNEL_ID=$(echo "$CHANNEL" | jq -r .id)
TEMPLATE=$(curl -sf -X POST "$BASE_URL/api/notification-templates" -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d '{"name":"e2e-template","channelType":"WEBHOOK","subject":"[${severity}] ${eventType}","body":"Event ${eventType} at ${location}"}')
TEMPLATE_ID=$(echo "$TEMPLATE" | jq -r .id)
curl -sf -X POST "$BASE_URL/api/subscriptions" -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"e2e-sub\",\"eventType\":\"e2e.*\",\"targetType\":\"NOTIFICATION\",\"notificationChannelId\":\"$CHANNEL_ID\",\"templateId\":\"$TEMPLATE_ID\",\"maxRetries\":1,\"backoffMillis\":0}" >/dev/null
echo "subscription created for channel=$CHANNEL_ID template=$TEMPLATE_ID"

echo "== 5. trigger dispatch via API =="
curl -sf -X POST "$BASE_URL/api/events/$EVENT_ID/dispatch" -H "$AUTH" | jq .

echo "== 6. query event and trace =="
curl -sf "$BASE_URL/api/events/$EVENT_ID" -H "$AUTH" | jq '{eventId,eventType,status,parkId}'
echo "traces=$(curl -sf "$BASE_URL/api/events/$EVENT_ID/traces" -H "$AUTH" | jq 'length')"

echo "== 7. notification deliveries =="
curl -sf "$BASE_URL/api/notifications?eventId=$EVENT_ID" -H "$AUTH" | jq '.'

echo "== 8. dashboard data =="
curl -sf "$BASE_URL/api/dashboard/statistics?dimension=SEVERITY" -H "$AUTH" | jq '{dimension,total}'
curl -sf "$BASE_URL/api/dashboard/situation" -H "$AUTH" | jq '{activeCount}'

echo "== 9. create event via management API =="
curl -sf -X POST "$BASE_URL/api/events" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"eventType":"manual.e2e","occurredAt":"2026-01-02T03:04:05Z","severity":"INFO","payload":{}}' \
  | jq '{eventId,accepted}'

echo "OK: all capabilities are reachable through HTTP APIs without the front-end."
