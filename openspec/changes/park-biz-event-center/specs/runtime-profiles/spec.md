# Spec Delta

## Purpose

定义可插拔的基础设施后端与运行时 profile，使系统能在生产后端（PostgreSQL/Kafka/Redis）与本地嵌入式后端（SQLite）之间切换，并保持对外 API 行为一致。

## ADDED Requirements

### Requirement: 可插拔基础设施后端

系统 MUST 通过端口隔离关系型持久化、事件总线与幂等存储，使上层业务逻辑不直接绑定具体中间件实现。

#### Scenario: 生产配置使用外部中间件

- **WHEN** 系统以生产配置启动
- **THEN** 持久化、事件总线与幂等存储分别使用 PostgreSQL、Kafka 与 Redis 适配器

#### Scenario: 本地配置使用嵌入式后端

- **WHEN** 系统以本地演示配置启动
- **THEN** 持久化、事件总线与幂等存储均使用嵌入式 SQLite 后端且无需外部中间件

### Requirement: 对外行为一致性

系统 MUST 在本地嵌入式后端与生产后端下提供相同的对外 REST API 行为（相同契约、相同可观察结果）。

#### Scenario: 同一契约测试在两种配置下一致

- **WHEN** 同一组 API 契约测试分别在本地配置与生产配置下执行
- **THEN** 两者返回一致的、符合契约的可观察行为

### Requirement: 本地事件可靠投递

系统 MUST 在本地嵌入式后端下保持事件的持久化缓冲、重试与死信语义，不因替换事件总线而丢失事件。

#### Scenario: 本地投递失败重试并进入死信

- **WHEN** 本地配置下向订阅目标的投递失败
- **THEN** 系统在本地持久化缓冲中按策略重试，达到上限后进入本地死信并记录失败原因

### Requirement: 生产配置保持不变

系统 MUST 在生产配置下保持既有的 PostgreSQL、Kafka、Redis 架构与行为，本地适配层不得改变生产运行路径。

#### Scenario: 生产路径仍走原架构

- **WHEN** 以生产配置启动并处理事件
- **THEN** 事件经由 Kafka 分发、持久化于 PostgreSQL、幂等状态存于 Redis
