# Design

## Context

园区业务事件中心是全新工程，技术栈由本次决策确定；首版已按本设计落地基础实现，本次修订在其上增加本地演示 profile（见决策 10、11）并新增业务事件类型注册表与负载校验（见决策 12、13），不改变生产架构。上游是多来源的异构事件（IoT、门禁、能耗、停车、安防、第三方系统、人工/API），下游需要可靠的订阅分发、通知和运营看板；且后端与前端需解耦，全部能力可通过 API 直接操作。约束见 `proposal.md`（影响范围）；具体行为见各 `specs/*/spec.md`，本文件不重复。

## Goals / Non-Goals

**Goals:**

- 用一条标准化的"接入 → 存储 → 订阅匹配 → 分发 → 通知/看板"主链路支撑全部六项能力。
- 规范事件模型作为跨模块单一契约，来源与消费者解耦。
- 前后端解耦：后端 API 独立完整，管理前端仅是可替换的 API 客户端。
- 保证事件不丢失：可持久化缓冲、重试、死信。
- 支持多园区数据隔离与鉴权。
- 在绿色项目中控制初期复杂度，同时保留后续拆分微服务的边界。

**Non-Goals:**

- 不实现完整工作流/BPM（事件的工单流转、审批、关闭处置流程不在本次范围）。
- 不实现复杂规则引擎（不引入 Drools 等重型引擎）。
- 不实现跨园区的联合分析/大数据平台。
- 不做高可用多活部署设计，初期以单集群容器化部署为准。

## Decisions

### 决策 1：模块化单体，按能力分模块

后端采用 Java 17 + Spring Boot 3，以 Maven 多模块的模块化单体组织：`event-model`、`ingestion`、`subscription`、`dispatch`、`notification`、`store-query`、`dashboard`、`admin-api`、`common`。各模块只通过 `event-model` 契约和领域服务接口交互，禁止跨模块直接访问对方数据库表。

- 理由：绿色项目、团队规模未知，微服务会放大运维与联调成本；模块化单体可先跑通主链路，边界清晰便于后续按模块拆分。
- 备选：直接微服务（拆分过早、通信与事务复杂）；传统分层单体（边界模糊，易腐化）。

### 决策 2：规范事件模型为统一契约

规范事件采用固定信封字段 + 可变负载：`eventId`、`eventType`、`source`、`sourceType`、`occurredAt`、`severity`、`parkId`、`location`、`payload`、`traceId`。以 JSON 承载，Java 侧用 record + JSON Schema 校验。

- 理由：接入、分发、存储、通知、看板共用同一模型，避免各环节各写一套 DTO；保留原始负载便于追溯。
- 备选：各来源独立模型（下游需要重复适配）；强类型 Avro/Protobuf（引入额外 schema registry 复杂度，初期收益有限）。

### 决策 3：REST + 消息中间件，同步接入、异步处理

接入层对所有调用方（第三方系统、脚本、管理前端表单）提供统一的 REST 管理 API；对 IoT/设备类提供可扩展适配器，首版实现 REST 适配器并预留 MQTT/消息适配器接口。受理后事件写入消息中间件（Kafka）作为持久化缓冲，分发模块异步消费，并支持通过 API 触发分发/重投。消息中间件置于 `EventBus` 端口之后，具体实现由运行时 profile 选择（生产 Kafka、本地 SQLite 发件箱）。

- 理由：同步受理给来源即时反馈，异步投递保证削峰与不丢失；适配器模式便于新增来源类型。
- 备选：纯同步直投（来源抖动即丢事件）；RabbitMQ（重试/死信模型成熟但与团队生态未知，Kafka 更适合回放与多消费者）。

### 决策 4：存储选型

关系型数据库 PostgreSQL 存订阅、规则、通知配置与事件元数据/轨迹；Redis 作缓存与幂等去重键。事件检索初期用 PostgreSQL 索引与分区表，看板统计用聚合表/物化视图。预留 Elasticsearch 作为后续全文检索与大规模查询的替换项。关系库与缓存同样置于端口之后，由运行时 profile 选择实现（生产 PostgreSQL/Redis、本地 SQLite）；本地 SQLite 无分区表/物化视图，看板聚合退化为普通 SQL。

- 理由：单库起步降低运维面；事件量增长后按需引入检索引擎，接口保持不变。
- 备选：直接上 Elasticsearch + 时序库（初期运维成本高）；只存消息中间件（无法满足审计与条件检索）。

### 决策 5：过滤/转换规则用受限表达式，不用重型规则引擎

订阅的过滤条件与转换规则用受限的表达式语言（如 SpEL 白名单子集的包装器），只暴露事件字段与安全内置函数，转换规则用声明式字段映射。

- 理由：满足过滤/映射需求且易于管理，避免 Drools 的复杂度与学习/运维成本。
- 备选：Drools/自研 DSL（成本高或表达力不足）；纯硬编码（失去可配置性）。

### 决策 6：鉴权与多租户隔离

Spring Security + JWT（对接园区统一身份或内置账号），所有事件、订阅、通知资源带 `parkId`，请求上下文强制注入并过滤，禁止跨园区访问。

- 理由：园区平台天然多园区，隔离必须在数据访问层强制而非仅前端。
- 备选：单园区假设（后期改造成本高）；仅靠前端菜单隔离（不安全）。

### 决策 7：管理前端技术栈

前端采用 Vue 3 + TypeScript + Vite + Element Plus，图表用 ECharts，实时态势用 WebSocket/SSE 推送。

- 理由：园区管理台在国内生态成熟、组件齐全、上手快；大屏与实时态势需求由 ECharts + 推送覆盖。前端仅消费后端公开 API，不依赖私有接口，可独立部署与替换。
- 备选：React + Ant Design（同样可行，团队偏好不同）。

### 决策 8：仓库布局

采用 monorepo：`backend/`（Maven 多模块）与 `frontend/`（Vue）并列；根目录保留 OpenSpec 目录与说明文件。

- 理由：前后端同仓便于本变更协同开发与统一 CI；后续如需要可拆分仓库。

### 决策 9：前后台解耦与 API 优先

后端以 REST API 为一等交付物，用 OpenAPI 契约先行描述并生成/校验接口；创建事件、分发/重投、订阅与通知管理、查询、看板数据全部通过公开 API 暴露。管理前端作为无状态 API 客户端，仅调用公开 API，不与后端共享会话、服务端模板或私有接口。

- 理由：解耦后前端可独立部署与替换，脚本或第三方系统也能直接创建与分发事件；契约先行便于前后端并行开发与契约测试。
- 备选：前后端同源紧耦合（前端依赖私有接口，难以独立演进和被外部复用）。

### 决策 10：基础设施端口/适配器分层

在 `common` 模块定义基础设施端口：事件总线 `EventBus`（发布/订阅）、幂等 `IdempotencyStore`（已存在）与持久化方言约定；业务模块只依赖端口，不直接依赖 Kafka/Redis/PostgreSQL 客户端。生产实现为 Kafka/Redis/PostgreSQL 适配器。

- 理由：把中间件替换收敛到适配器一处，业务与存储解耦，本地演示与生产共用同一套业务逻辑与 API 契约。
- 备选：以 `@Profile` 直接替换各模块实现（散落、易漏）；直接内嵌 H2 而不做端口（仍需解决消息总线与幂等缓存，未真正解耦）。

### 决策 11：本地演示 profile（SQLite）

通过 Spring profile `local` 激活本地嵌入式后端：数据源 `jdbc:sqlite:` + Hibernate SQLite 方言 + 方言化 Flyway 迁移（如 `classpath:db/migration/sqlite`）；事件总线用 SQLite 持久化发件箱表 + 定时轮询投递（保留退避重试与死信）；幂等用 SQLite 表（TTL 过期清理）。`local` profile 不改动任何对外 REST 契约，生产 profile 保持现状。

- 理由：本地 PC 演示无需 Docker 与外部中间件，单文件 SQLite 即承载持久化、队列与幂等；发件箱保证事件不丢失，语义接近 Kafka 路径。
- 备选：进程内 `ApplicationEvent`（无持久化缓冲，重启丢事件）；嵌入式 Kafka（未真正切换到 SQLite）。

### 决策 12：业务事件类型注册表与负载契约

事件类型定义持久化于 `event_type` 表：类型标识（主键）、名称、来源类别、默认严重级别、payload schema（JSON Schema，文本）、enabled、version、时间戳；全局共享（不按园区隔离）。payload schema 采用 JSON Schema draft 2020-12，引入 `com.networknt:json-schema-validator` 校验，schema 在保存时先做元校验。预置园区典型类型目录由 Flyway 迁移种子写入（postgresql 与 sqlite 两套），如 `iot.temperature`、`iot.device.offline`、`access.granted`、`access.denied`、`energy.consumption.spike`、`facility.equipment.alarm`、`parking.enter`、`parking.exit`、`security.intrusion`、`security.face.match`、`thirdparty.workorder.created`、`manual.inspection` 等。管理 API 提供列表、详情、新增、修改与启停。

- 理由：类型与负载契约集中、可演进、可被生产方与消费方查询；备选：硬编码枚举（不可定义、不可演进）、仅信封校验（无法表达各类型差异化的 payload）。
- 备选：把 schema 存为文件而非常量表（无法运行时定义与热更新）；用 Avro/Protobuf + registry（初期运维成本高）。

### 决策 13：接入时非阻塞的类型与负载校验

接入在必填字段校验之后、去重/持久化之前识别事件类型并以其 payload schema 校验负载；结果（`VALIDATED` / `UNVALIDATED` + 原因，如 `UNKNOWN_TYPE`、`DISABLED_TYPE`、`SCHEMA_MISMATCH`）写入事件记录与处理轨迹。校验为非阻塞：类型未注册、已停用或负载不合规均不影响受理，仅标记，便于观测与后续治理。`CanonicalEvent` 信封字段保持不变，校验状态作为事件元数据持久化并通过查询/轨迹暴露，避免破坏跨能力契约。

- 理由：兼容存量与第三方来源、支持契约灰度演进，不因 schema 变更中断接入；备选：强校验直接拒绝（对异构来源与迁移期过于刚性）。
- 备选：在信封上增加校验字段（改动跨能力契约与 JSON Schema，影响面大）；不记录校验结果（无法观测脏数据）。

## Risks / Trade-offs

- 模块化单体后续可能演变为"分布式单体" → 通过强制模块边界与禁止跨库访问，按能力预留拆分点。
- Kafka 与 PostgreSQL 的引入抬高了本地开发门槛 → 提供 Docker Compose 一键起中间件与数据库，集成测试使用 Testcontainers。
- 受限表达式仍可能被滥用 → 白名单函数、禁止反射与 IO、设置执行超时。
- 多租户隔离遗漏导致数据越权 → 在数据访问层统一注入 `parkId` 过滤，并用集成测试覆盖跨园区访问被拒。
- 事件量超出预期导致库表膨胀 → 分区表 + 归档策略；检索与聚合接口与存储解耦，便于替换为检索/时序组件。
- 绿色项目缺少既有约定，前端/后端风格易漂移 → 首版任务中固定代码规范与 CI 校验（格式化、静态检查）。
- 前后端解耦后 API 契约漂移导致前端或外部客户端失效 → 以 OpenAPI 契约为准，前后端共用契约测试并纳入 CI。
- SQLite 单写者与并发能力弱于 PostgreSQL，本地与生产行为可能分化 → 本地 profile 仅用于演示与开发，契约测试在两种 profile 下运行，容量/并发基线仅在 PostgreSQL 下评估。
- 引入适配层后生产路径可能回归 → 端口默认实现即生产适配器，并以双 profile 测试覆盖，确保生产仍经 Kafka/PostgreSQL/Redis。
- 预置事件类型目录随业务演进需同步维护 → 提供管理 API 扩展/覆盖，预置项可停用，schema 变更走版本化。
- 非阻塞校验可能掩盖脏数据 → 校验状态与原因在事件详情/轨迹中可见，并可纳入看板与审计治理。
- JSON Schema 校验引入依赖与运行开销 → 仅对已注册且启用的类型校验，保存 schema 时做元校验并限制复杂度。

## Migration Plan

全新项目，无历史数据迁移。部署：Docker Compose 起 PostgreSQL、Redis、Kafka，后端与前端作为独立服务分别部署（前端仅依赖后端公开 API）；数据库结构由 Flyway 管理。本地演示以 `local` profile 单进程运行，使用嵌入式 SQLite、不启动任何外部中间件；生产部署不变。预置业务事件类型目录作为数据迁移种子随 Flyway 一并写入（两种方言各一套）。回滚：本变更不涉及线上存量，回退即还原仓库与部署编排。

## Open Questions

- 短信/企业微信等通知通道的具体供应商与凭证获取方式（不影响接口与任务拆分，实施前确认即可）。
- 事件与审计记录的保留周期与归档策略（先按可配置保留期实现，具体数值后续按合规要求调整）。
