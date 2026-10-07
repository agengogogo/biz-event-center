# 通知通道与模板配置文档

## 创建通知通道

```bash
curl -s -X POST http://localhost:8080/api/notification-channels \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"ops-webhook","type":"WEBHOOK","config":{"url":"https://example.com/notify"}}'
```

支持通道类型：

- `WEBHOOK`：向 `config.url` POST `{"subject","content","event"}`。
- `WECHAT_WORK`（企业微信机器人）：向 `config.url` POST markdown 消息。

## 创建通知模板

模板通过 `${字段}` 引用事件字段，支持 `payload.` 前缀访问负载。

```bash
curl -s -X POST http://localhost:8080/api/notification-templates \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "alert-template",
    "channelType": "WEBHOOK",
    "subject": "[${severity}] ${eventType}",
    "body": "事件 ${eventType} 发生在 ${location}，负载值=${payload.value}"
  }'
```

## 在订阅中选择通道与模板

创建/更新订阅时指定 `targetType=NOTIFICATION`、`notificationChannelId` 与 `templateId`：

```bash
curl -s -X POST http://localhost:8080/api/subscriptions \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"notify","eventType":"notify.*","targetType":"NOTIFICATION",
       "notificationChannelId":"<channelId>","templateId":"<templateId>","maxRetries":2}'
```

## 投递结果与重发

```bash
curl -s http://localhost:8080/api/notifications -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/notifications?eventId=<eventId>" -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8080/api/notifications/<id>/resend -H "Authorization: Bearer $TOKEN"
```

每次发送记录状态（`SENT`/`FAILED`）、失败原因与渲染内容；失败通知可通过 resend 接口重发。
