# Architecture Map

本文件描述 Midgard Backend Template 的稳定架构事实，用来回答三个问题：

1. 代码应该落在哪一层。
2. 层与层之间如何协作。
3. 哪些约束属于模板长期不变的骨架。

## 1. 系统定位

Midgard 是一个基于 COLA 5.0 的 DDD 后端服务模板，用于快速启动符合分层边界的 Spring Boot 微服务。

当前仓库更像“工程骨架”而不是“完整业务系统”，因此架构文档重点描述：

- 分层职责
- 包结构与命名
- 对象流与依赖方向
- 构建、运行、发布相关的工程约束

## 2. 模块总览

| 模块 | 角色 | 说明 |
| --- | --- | --- |
| `client` | 外部契约层 | 面向跨服务调用的接口与 DTO |
| `adapter` | 协议适配层 | Web、mobile、wap、RPC、MQ 等入站适配 |
| `app` | 应用层 | 用例编排、事务边界、命令查询执行 |
| `domain` | 领域层 | 领域模型、规则、Repository 接口 |
| `infrastructure` | 基础设施层 | Repository 实现、DO、外部 Gateway、技术配置 |
| `start` | 启动层 | Spring Boot 启动与全局装配 |

## 3. 依赖方向

允许的主要依赖方向：

```text
start -> adapter, infrastructure
adapter -> app
app -> domain
infrastructure -> domain
client -> 独立契约层
```

默认禁止：

```text
adapter -> domain
adapter -> infrastructure
app -> infrastructure
domain -> app/adapter/infrastructure/client
start 中写业务逻辑
```

这套约束比“能不能 import”更重要，因为模板价值来自长期可维护的边界，而不是单次实现速度。

## 4. 分层职责

### `client`

- 定义 Dubbo/Feign/跨服务调用契约。
- 放协议专属 DTO，不放内部编排 DTO。
- 只定义能力边界，不实现业务。

### `adapter`

- 处理协议输入输出和参数校验。
- 把 Request DTO 转为 App Command/Query。
- 返回面向协议的响应。
- 不直接编写业务规则或访问基础设施。

### `app`

- 组织用例流程。
- 负责事务边界、幂等、调用顺序和组合逻辑。
- 常见组织方式是 `AppService + CmdExe/QryExe`。

### `domain`

- 保存核心业务规则。
- 定义 Entity、VO、Domain Service、Repository 接口、领域事件。
- 不感知 Controller、DO、Mapper、第三方协议。

### `infrastructure`

- 实现领域层定义的接口。
- 管理持久化、外部调用和技术集成。
- 用 `InfraConvertor` 完成 DO 与 Entity 的转换。

### `start`

- 负责装配和启动。
- 允许放启动类与全局技术配置。
- 不承载业务行为。

## 5. 对象流

默认写入链路：

```text
Request DTO -> App Cmd/Qry -> Domain Entity -> DO -> Database
```

默认读取链路：

```text
Database -> DO -> Domain Entity -> CO -> Response
```

转换职责：

- Adapter Convertor: 协议 DTO -> App DTO
- App Convertor: Cmd/Qry -> Entity
- App Assembler: Entity -> CO
- Infra Convertor: Entity <-> DO

禁止的捷径：

- Controller 直接操作 Entity/DO
- Repository 直接返回 Request/CO
- DO 混入业务行为
- Entity 带数据库映射注解或协议字段

## 6. 推荐包结构

### Adapter

```text
adapter.web.{domain}
adapter.web.{domain}.dto
adapter.web.{domain}.convertor
adapter.rpc.provider
adapter.mq.consumer
```

### App

```text
{aggregate}
{aggregate}.application
{aggregate}.executor
dto.cmd
dto.qry
dto.co
dto.enums
convertor
assembler
```

### Domain

```text
{aggregate}.model
{aggregate}.service
{aggregate}.repository
{aggregate}.event
```

### Infrastructure

```text
infrastructure.persistence.dataobject
infrastructure.persistence.convertor
infrastructure.persistence.impl
infrastructure.gateway
infrastructure.config
```

## 7. 命名约束

- Controller: `{Domain}Controller`
- Request: `{Verb}{Domain}Request`
- AppService: `{Domain}AppService`
- Command: `{Verb}{Domain}Cmd`
- Query: `{Verb}{Domain}Qry`
- Output: `{Domain}CO`
- Domain Service: `{Domain}DomainService`
- Repository 接口: `{Domain}Repository`
- Repository 实现: `{Domain}RepositoryImpl`
- Infra Convertor: `{Domain}InfraConvertor`

`InfraConvertor` 是本模板明确的命名保护带，避免与 App 层 `Convertor` 冲突。

## 8. 事务、校验、异常

事务边界：

- 优先放在 App 层入口或 Executor。
- 不在 Domain 层放 Spring 事务注解。

参数校验：

- Adapter 层入口使用 `@Validated` / `@Valid`
- App 层 DTO 使用 Jakarta Validation
- Domain 层继续做业务规则校验

异常：

- 技术异常在合适边界转换
- 业务异常尽量靠近领域表达
- Controller 不用 `try/catch` 充当流程编排

## 9. 工程与构建约束

- Java 17
- Spring Boot 3.3.x
- Maven 聚合工程
- 根版本由 `pom.xml` 中的 `<revision>` 统一管理
- 优先使用 `./mvnw`
- Spotless 负责 Java 格式统一

常见验证：

- 全量校验：`./mvnw clean verify`
- 启动：`./mvnw spring-boot:run -pl start -am`
- 指定 profile：`./mvnw -Pdev clean verify`

## 10. 文档和架构的关系

本文件只描述稳定骨架。

如果你需要：

- 设计哲学：看 [docs/design-docs/core-beliefs.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/design-docs/core-beliefs.md)
- 计划与执行方式：看 [docs/PLANS.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/PLANS.md)
- 质量衡量：看 [docs/QUALITY_SCORE.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/QUALITY_SCORE.md)
- 可靠性与安全约束：看 [docs/RELIABILITY.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/RELIABILITY.md) 与 [docs/SECURITY.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/SECURITY.md)
