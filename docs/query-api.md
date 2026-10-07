# 事件查询与轨迹接口

所有接口需管理员 JWT，并按调用者所属园区强制隔离。

## 条件检索

```bash
curl -s "http://localhost:8080/api/events?from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z\
&type=access.denied&source=gate-01&severity=MAJOR&location=Gate%201&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
# => {"items":[...],"total":N,"page":0,"size":20}
```

支持的时间、类型、来源、严重级别、园区/位置参数均可选。

## 按唯一标识读取

```bash
curl -s http://localhost:8080/api/events/<eventId> \
  -H "Authorization: Bearer $TOKEN"
```

## 完整处理轨迹

```bash
curl -s http://localhost:8080/api/events/<eventId>/traces \
  -H "Authorization: Bearer $TOKEN"
# => [{"stage":"INGESTED","status":"RECEIVED",...},
#     {"stage":"VALIDATED",...},{"stage":"DEDUPLICATED",...},
#     {"stage":"PERSISTED",...},{"stage":"PUBLISHED",...},
#     {"stage":"ROUTED",...},{"stage":"DELIVERED",...}]
```

轨迹覆盖从接入、校验、去重、持久化、发布到分发/通知的完整链路；审计记录保存在 `audit_log` 表。
