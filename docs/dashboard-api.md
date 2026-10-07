# 看板数据接口文档

看板数据 API 与具体前端无关，运营大屏或其他已授权客户端均通过同一 API 获取数据。

## 统计聚合

```bash
curl -s "http://localhost:8080/api/dashboard/statistics?dimension=SEVERITY\
&from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z&interval=hour" \
  -H "Authorization: Bearer $TOKEN"
# => {"from":"...","to":"...","dimension":"SEVERITY","total":12,
#     "points":[{"key":"MAJOR","total":7},{"key":"CRITICAL","total":5}]}
```

`dimension` 取值：`TYPE`、`SOURCE`、`SEVERITY`、`LOCATION`、`TIME`。
`TIME` 维度按 `interval=hour|day` 分桶。

## 实时态势

```bash
curl -s "http://localhost:8080/api/dashboard/situation?limit=50" \
  -H "Authorization: Bearer $TOKEN"
# => {"activeCount":N,"activeAlerts":[<未关闭的 MAJOR/CRITICAL 事件>]}
```

## 增量推送（SSE）

```bash
curl -N http://localhost:8080/api/dashboard/stream -H "Authorization: Bearer $TOKEN"
# event: situation
# data: {"activeCount":N,"activeAlerts":[...]}   （每 5 秒增量推送）
```

大屏前端监听 `situation` 事件即可实时刷新统计图表与态势列表。
