# 订阅与过滤/转换规则配置说明

订阅包含：主题（事件类型）、投递目标、启停状态，以及可选的过滤表达式与声明式转换规则。

## 创建订阅

```bash
curl -s -X POST http://localhost:8080/api/subscriptions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "gate-webhook",
    "eventType": "access.*",
    "enabled": true,
    "filterExpression": "severity in {\"MAJOR\",\"CRITICAL\"} and contains(location, \"Gate\")",
    "transformRules": [
      {"from": "payload.personId", "to": "alert.person"},
      {"from": "eventType", "to": "alert.type"},
      {"from": "payload.camera", "to": "alert.camera", "defaultValue": "unknown"}
    ],
    "targetType": "WEBHOOK",
    "targetUrl": "https://example.com/hooks/events",
    "maxRetries": 3,
    "backoffMillis": 1000
  }'
```

`eventType` 支持：精确值、`*`（全部）、前缀通配 `access.*`。

## 生命周期

```bash
curl -s -X POST http://localhost:8080/api/subscriptions/<id>/disable -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8080/api/subscriptions/<id>/enable  -H "Authorization: Bearer $TOKEN"
curl -s -X DELETE http://localhost:8080/api/subscriptions/<id>       -H "Authorization: Bearer $TOKEN"
```

停用或删除后，新事件不再路由到该订阅。

## 过滤表达式（受限白名单）

- 可访问字段：`eventId`、`eventType`、`source`、`sourceType`、`occurredAt`、`severity`、
  `parkId`、`location`、`traceId`、`payload`（如 `payload.value > 5`）。
- 内置安全函数：`contains`、`startsWith`、`endsWith`、`matches`、`len`、`upper`、`lower`、`has`。
- 禁止反射/IO 类型引用（如 `T(java.lang.Runtime)`），并设置执行超时。非法表达式在保存时即被拒绝。

## 转换规则

每条规则将源字段（`from`，支持 `payload.a.b` 与信封字段）映射到目标字段（`to`，点号表示嵌套），
可选 `defaultValue` 作为缺省值，仅当满足过滤条件时在投递前应用。
