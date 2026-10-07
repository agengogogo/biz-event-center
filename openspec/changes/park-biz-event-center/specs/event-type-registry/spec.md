# Spec Delta

## Purpose

定义园区业务事件类型目录与每种类型的 payload 契约，为接入校验、订阅与消费方提供统一、可演进的类型与负载 schema。

## ADDED Requirements

### Requirement: 事件类型定义管理

系统 MUST 支持事件类型定义的管理，每个定义至少包含类型标识、名称、来源类别、默认严重级别、payload 校验 schema 与启用状态，并支持新增、修改与启停。

#### Scenario: 定义自定义事件类型

- **WHEN** 管理员通过管理 API 提交一个新的业务事件类型及其 payload schema
- **THEN** 系统保存该类型定义并使其可用于后续接入校验

#### Scenario: 停用事件类型

- **WHEN** 管理员将某事件类型置为停用
- **THEN** 系统将该类型标记为停用，后续不再作为已注册启用类型参与校验

### Requirement: 预置典型园区业务事件类型

系统 MUST 预置覆盖园区典型来源（IoT 设备、门禁/人员通行、能耗/设施、停车/车辆、视频/安防、第三方业务系统、人工）的业务事件类型目录，且每个预置类型携带 payload schema。

#### Scenario: 预置类型开箱可用

- **WHEN** 系统完成首次初始化
- **THEN** 预置的典型业务事件类型及其 payload schema 已存在，并可被查询与用于接入校验

### Requirement: 事件类型查询

系统 MUST 提供与客户端无关的查询接口，返回事件类型定义及其 payload schema，使事件生产方与消费方可获取类型契约。

#### Scenario: 查询事件类型目录

- **WHEN** 已授权客户端请求事件类型列表或指定类型详情
- **THEN** 系统返回类型定义及其 payload schema
