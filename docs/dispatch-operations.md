# 分发、重试与死信运维说明

## 总线与分区

接入受理后事件写入 Kafka 主题 `park.events`，分区键为 `parkId:eventType`，保证同一园区同一类型事件
进入同一分区，从而实现同一目标的有序投递。异常消息在消费失败且超过重试后进入 `park.events.dlt`。

## 路由与投递

分发按订阅主题匹配 + 过滤表达式求值，命中后应用转换规则并投递到目标：

- `WEBHOOK` / `HTTP`：向 `targetUrl` 发送 `{"event": <规范事件>}`。
- `NOTIFICATION`：经订阅配置的通知通道发送通知。

投递状态记录在 `delivery` 表（`DELIVERED` / `FAILED` / `DEAD_LETTER`）。

## 重试与死信

投递失败按订阅的 `maxRetries` 与 `backoffMillis` 做指数退避重试；达到上限后该次投递进入死信并记录失败原因。
去重：同一订阅 + 同一 `eventId` 仅生效一次（Redis 幂等键），重复投递被跳过。

## 通过 API 触发分发与重投

```bash
# 对已受理事件触发分发
curl -s -X POST http://localhost:8080/api/events/<eventId>/dispatch -H "Authorization: Bearer $TOKEN"
# => {"eventId":"...","matched":1,"delivered":1,"failed":0,"deadLettered":0,"skipped":0}

# 对失败事件重投
curl -s -X POST http://localhost:8080/api/events/<eventId>/redeliver -H "Authorization: Bearer $TOKEN"

# 查看投递记录 / 死信
curl -s http://localhost:8080/api/deliveries               -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/deliveries/dead-letters  -H "Authorization: Bearer $TOKEN"

# 对某条死信投递重投
curl -s -X POST http://localhost:8080/api/deliveries/<id>/redeliver -H "Authorization: Bearer $TOKEN"
```
