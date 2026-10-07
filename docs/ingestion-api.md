# 事件接入模块接口与调用示例

所有接入能力通过统一 REST API 暴露，管理前端表单只是该 API 的一个客户端。

## 1. 注册来源并获取凭证

来源注册需要管理员 JWT（`POST /api/auth/login` 获取）。

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

curl -s -X POST http://localhost:8080/api/sources \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"gate-01","sourceType":"ACCESS"}'
# => {"id":"...","secret":"...","parkId":"PARK-A",...}  secret 只在创建时返回
```

## 2. 来源上报事件（统一接入接口）

来源使用 `X-Source-Id` / `X-Source-Secret` 鉴权，负载可为任意异构结构，系统会标准化并保留原始负载。

```bash
curl -s -X POST http://localhost:8080/api/ingest/events \
  -H "X-Source-Id: <sourceId>" \
  -H "X-Source-Secret: <secret>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "access.denied",
    "timestamp": "2026-01-02T03:04:05Z",
    "level": "MAJOR",
    "place": "Gate 1",
    "payload": {"personId": "p-9", "attempts": 3}
  }'
# 202 => {"eventId":"...","eventType":"access.denied","accepted":true,"duplicate":false,...}
```

字段别名：`type`/`event_type` → `eventType`；`timestamp`/`occurred_at` → `occurredAt`；
`level` → `severity`；`place`/`area` → `location`；`data`/`payload` → `payload`。

必填：`eventType` 与 `occurredAt`，缺失返回 400。相同 `eventId` 重复上报只受理一次（`duplicate=true`）。

## 3. 通过管理 API 直接创建事件

```bash
curl -s -X POST http://localhost:8080/api/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "manual.inspection",
    "occurredAt": "2026-01-02T03:04:05Z",
    "severity": "WARNING",
    "location": "Building A",
    "payload": {"note": "floor 3"}
  }'
# 201 => {"eventId":"...","accepted":true,...}
```

创建的事件与来源事件使用同一规范模型并进入同一分发流程。

## 4. 事件类型目录与负载校验

系统预置园区典型业务事件类型（IoT 设备、门禁/通行、能耗/设施、停车/车辆、视频/安防、第三方系统、人工），
每种类型携带 payload JSON Schema，可通过管理 API 维护。类型目录为全局共享。

```bash
# 列出事件类型（?enabled=true 仅启用的）
curl -s http://localhost:8080/api/event-types -H "Authorization: Bearer $TOKEN"

# 查看某类型及其 payload schema
curl -s http://localhost:8080/api/event-types/access.denied -H "Authorization: Bearer $TOKEN"

# 定义自定义类型
curl -s -X POST http://localhost:8080/api/event-types \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"custom.vip.arrival","name":"VIP 到达","sourceCategory":"ACCESS",
       "defaultSeverity":"INFO",
       "payloadSchema":"{\"type\":\"object\",\"properties\":{\"personId\":{\"type\":\"string\"}},\"required\":[\"personId\"]}"}'
```

接入校验为**非阻塞**：系统识别 `eventType` 并用其 schema 校验 payload，结果记录在事件的
`eventTypeValidation`（`VALIDATED` / `UNVALIDATED`）与 `validationReason` 字段，并写入处理轨迹（`TYPE_VALIDATED` 阶段）。

- 已注册且启用、payload 合规 → `VALIDATED`。
- 已注册但 payload 不合规 → `UNVALIDATED`，原因 `SCHEMA_MISMATCH: ...`（事件仍被受理）。
- 未注册或已停用 → `UNVALIDATED`，原因 `UNKNOWN_TYPE` / `DISABLED_TYPE`（事件仍被受理）。

校验状态可通过 `GET /api/events/{eventId}` 与 `GET /api/events/{eventId}/traces` 查询。

