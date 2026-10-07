# Front/back decoupling verification over HTTP APIs only (PowerShell).
# Usage: pwsh ./scripts/api-e2e.ps1
$ErrorActionPreference = 'Stop'

$BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { 'http://localhost:8080' }
$Username = if ($env:USERNAME) { $env:USERNAME } else { 'admin' }
$Password = if ($env:PASSWORD) { $env:PASSWORD } else { 'admin123' }
$Json = @{ 'Content-Type' = 'application/json' }

Write-Host '== 1. login =='
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -Headers $Json `
  -Body (@{ username = $Username; password = $Password } | ConvertTo-Json)
$Auth = @{ Authorization = "Bearer $($login.token)" }

Write-Host '== 2. register source =='
$source = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/sources" -Headers $Auth `
  -Body (@{ name = "e2e-source-$([guid]::NewGuid())"; sourceType = 'IOT' } | ConvertTo-Json) `
  -ContentType 'application/json'
$sourceHeaders = @{ 'X-Source-Id' = $source.id; 'X-Source-Secret' = $source.secret }

Write-Host '== 3. ingest event =='
$event = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/ingest/events" -Headers $sourceHeaders `
  -ContentType 'application/json' `
  -Body (@{
    eventType = 'e2e.signal'; occurredAt = '2026-01-02T03:04:05Z'
    severity = 'MAJOR'; location = 'Gate 1'; payload = @{ value = 7 }
  } | ConvertTo-Json)
$eventId = $event.eventId
Write-Host "eventId=$eventId accepted=$($event.accepted)"

Write-Host '== 4. notification channel + template + subscription =='
$channel = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/notification-channels" -Headers $Auth `
  -ContentType 'application/json' `
  -Body (@{ name = 'e2e-channel'; type = 'WEBHOOK'; config = @{ url = 'http://127.0.0.1:9/notify' } } | ConvertTo-Json -Depth 5)
$template = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/notification-templates" -Headers $Auth `
  -ContentType 'application/json' `
  -Body (@{ name = 'e2e-template'; channelType = 'WEBHOOK'; subject = '[${severity}] ${eventType}'; body = 'Event ${eventType} at ${location}' } | ConvertTo-Json)
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/subscriptions" -Headers $Auth `
  -ContentType 'application/json' `
  -Body (@{
    name = 'e2e-sub'; eventType = 'e2e.*'; targetType = 'NOTIFICATION'
    notificationChannelId = $channel.id; templateId = $template.id; maxRetries = 1; backoffMillis = 0
  } | ConvertTo-Json) | Out-Null

Write-Host '== 5. trigger dispatch =='
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/events/$eventId/dispatch" -Headers $Auth | ConvertTo-Json

Write-Host '== 6. query event and trace =='
Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/events/$eventId" -Headers $Auth | ConvertTo-Json -Depth 3
$traces = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/events/$eventId/traces" -Headers $Auth
Write-Host "traces=$($traces.Count)"

Write-Host '== 7. notification deliveries =='
Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/notifications?eventId=$eventId" -Headers $Auth | ConvertTo-Json -Depth 3

Write-Host '== 8. dashboard data =='
Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/dashboard/statistics?dimension=SEVERITY" -Headers $Auth | ConvertTo-Json -Depth 3
Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/dashboard/situation" -Headers $Auth | ConvertTo-Json -Depth 2

Write-Host '== 9. create event via management API =='
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/events" -Headers $Auth -ContentType 'application/json' `
  -Body (@{ eventType = 'manual.e2e'; occurredAt = '2026-01-02T03:04:05Z'; severity = 'INFO'; payload = @{} } | ConvertTo-Json) `
  | ConvertTo-Json

Write-Host 'OK: all capabilities are reachable through HTTP APIs without the front-end.'
