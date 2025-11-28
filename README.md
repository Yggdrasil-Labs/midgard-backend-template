# midgard-backend-template

> 中庭 - DDD 架构模板项目

## 项目简介

Midgard（中庭）是基于 Cola5.0 DDD 架构的微服务模板项目，用于快速创建符合 DDD 最佳实践的后端服务。

**项目定位**：这是一个模板项目，用于快速开始其他具体的业务项目。开发者可以基于此模板快速搭建符合 DDD 架构规范的后端服务。

## 技术栈

- **基础框架**: Spring Boot 3.3.13 + Java 17
- **架构模式**: Cola5.0 DDD 分层架构
- **服务注册**: Nacos（推荐使用 Nacos 2.x）
- **服务间通信**: 
  - **对外接口**: OpenFeign（HTTP，供 Gateway 调用）
  - **内部调用**: Dubbo 3.x（RPC，微服务间调用）
- **数据库**: MySQL 8.4 + MyBatis-Plus
- **缓存**: Redis
- **消息队列**: Kafka / RocketMQ（可选）
- **接口文档**: Apifox（API 设计工具）

## 架构设计原则

### 核心架构原则

项目遵循以下核心架构原则，确保各层职责清晰、边界明确：

1. **Start 层原则**：Start 层只能做"启动 + 配置"，不写业务逻辑
   - 仅包含 SpringBoot 启动类和框架配置
   - 不包含任何业务逻辑代码

2. **Adapter 层原则**：Controller、RPC、Feign、MQ 全部在 Adapter 层
   - 所有 IO 通道的适配都在 Adapter 层
   - 包括 HTTP、RPC、Feign、MQ、OSS、定时任务等所有外部接口适配

3. **Client 层原则**：Client 层只存"契约模型"，不存实现
   - 仅包含对外契约定义（API 接口、DTO、Command、Query）
   - 不包含任何实现代码

4. **App 层原则**：App 层不放规则，只做编排
   - 负责业务流程编排和用例流程控制
   - 业务规则应在 Domain 层实现

5. **Domain 层原则**：Domain 层才是规则中心
   - 包含所有业务规则和领域逻辑
   - 不依赖任何外部框架，纯 Java 对象

6. **Infra 层原则**：Infra 层负责所有底层技术实现
   - 实现 Domain 层定义的接口
   - 包含所有技术细节和外部依赖

## 项目结构

项目采用 Cola5.0 DDD 分层架构，主要模块包括：

- **adapter**: 适配层，所有 IO 通道的适配（HTTP、RPC、Feign、MQ 等）
- **app**: 应用服务层，业务流程编排
- **domain**: 领域层，核心业务逻辑和领域模型
- **infrastructure**: 基础设施层，技术实现和外部依赖
- **client**: 客户端层，对外契约定义（API 接口、DTO、Command、Query）
- **start**: 启动层，应用启动类和框架配置

### 包结构规范

项目采用统一的包结构规范，每个模块下使用模块名作为包名前缀。详细包结构如下：

```
com.yggdrasil.labs
│
├── start                                # 启动层：仅包含启动类 & 框架配置
│   ├── Application                      # SpringBoot 启动类
│   ├── config                           # 全局配置（非业务）
│   │   ├── web                          # WebMvc/CORS/拦截器配置
│   │   ├── jackson                      # JSON序列化配置
│   │   ├── swagger                      # API文档配置
│   │   ├── security                     # SpringSecurity配置
│   │   ├── exception                    # 全局异常处理
│   │   ├── validation                   # 全局校验器
│   │   └── properties                   # @ConfigurationProperties
│   ├── aspect                           # 全局AOP：日志、鉴权、traceId
│   └── initializer                      # 应用启动监听器、初始化器
│
├── adapter                              # 适配层：所有 IO 通道的适配
│   ├── web                              # HTTP 接口适配
│   │   ├── controller                   # REST 控制器（输入适配）
│   │   ├── filter                       # Servlet Filter
│   │   ├── interceptor                  # HandlerInterceptor
│   │   └── handler                      # Web 异常/返回处理器
│   ├── rpc                              # RPC（如 Dubbo/GRPC）
│   │   ├── provider                     # RPC 服务提供者（暴露服务）
│   │   └── consumer                     # RPC 服务消费者（调用其他服务）
│   ├── feign                            # Feign 适配层
│   │   ├── client                       # Feign client（外部调用）
│   │   └── fallback                     # Feign 降级处理
│   ├── mq                               # 消息队列适配
│   │   ├── producer                     # MQ Producer（事件发送）
│   │   └── consumer                     # MQ Consumer（事件监听）
│   ├── oss                              # MinIO/OSS 文件上传适配
│   ├── gateway                          # 访问第三方 API 的适配器
│   ├── schedule                         # 定时任务适配
│   └── convert                          # DTO/VO 与 Command 的 MapStruct 转换
│
├── client                               # 客户端层：对外契约层（不包含实现）
│   ├── api                              # 公开 API 接口（非 Controller）
│   ├── dto                              # 外部可见的数据结构
│   │   ├── request                      # 请求 DTO
│   │   ├── response                     # 响应 DTO
│   │   └── common                       # 通用 DTO
│   ├── command                          # Command（写模型）
│   ├── query                            # Query（读模型）
│   ├── enums                            # 响应/请求相关的枚举
│   └── vo                               # Value Object（对外展示值对象）
│
├── app                                  # 应用服务层：业务流程编排
│   ├── service                          # 应用服务（流程控制，用例编排）
│   │   ├── command                      # 写操作（含事务）
│   │   └── query                        # 查询流程
│   ├── executor                         # COLA 风格的执行器
│   │   ├── command                      # CommandExecutor
│   │   └── query                        # QueryExecutor
│   ├── assemble                         # 数据组装（DTO ↔︎ Domain）
│   ├── convert                          # MapStruct 转换（Command ↔︎ Entity）
│   └── validator                        # 业务校验（非领域规则）
│
├── domain                               # 领域层：核心业务模型
│   ├── model                            # 领域模型
│   │   ├── entity                       # 实体（含业务行为）
│   │   ├── aggregate                    # 聚合根
│   │   ├── vo                           # 值对象（不可变）
│   │   └── enums                        # 领域枚举
│   ├── service                          # 领域服务（复杂业务规则）
│   ├── repository                       # 仓储接口（仅接口）
│   ├── event                            # 领域事件
│   │   ├── publisher                    # 事件发布接口
│   │   └── subscriber                   # 事件订阅接口
│   └── spec                             # 领域规约（Specification）
│
└── infra                                # 基础设施层：所有技术实现
    ├── config                           # MyBatis/Redis 等技术配置
    ├── db                               # 数据库访问
    │   ├── mapper                       # MyBatis Mapper
    │   ├── entity                       # DO（数据库对象）
    │   └── convert                      # DO ↔︎ Entity 转换
    ├── repository                       # 仓储接口实现
    ├── cache                            # 缓存
    │   ├── redis                        # Redis 实现
    │   └── local                        # 本地缓存（Caffeine）
    ├── mq                               # MQ 实现（Kafka/RocketMQ）
    │   ├── producer                     # Producer
    │   └── consumer                     # Consumer
    ├── oss                              # 文件存储实现
    ├── gateway                          # HTTP 调用第三方服务实现
    ├── http                             # HttpClient 封装
    ├── util                             # 工具类（仅技术相关）
    └── error                            # Infra 专用异常
```

**包命名规则**：
- Start 层：`com.yggdrasil.labs.start.*` - 仅包含启动类和框架配置
- Adapter 层：`com.yggdrasil.labs.adapter.*` - 所有 IO 通道适配
- Client 层：`com.yggdrasil.labs.client.*` - 对外契约定义
- App 层：`com.yggdrasil.labs.app.*` - 业务流程编排
- Domain 层：`com.yggdrasil.labs.domain.*` - 核心业务规则
- Infrastructure 层：`com.yggdrasil.labs.infra.*` - 技术实现

## 模块依赖关系示意图

整体依赖关系遵循“内核稳定、外层依赖内层”的原则，禁止反向依赖和环依赖：

```text
          ┌───────────────┐
          │    start      │  启动层：聚合所有业务模块，仅做启动与配置
          └──────┬────────┘
                 │ 依赖
        ┌────────┼───────────────────────────┐
        │        │                           │
        ▼        ▼                           ▼
   ┌────────┐ ┌────────┐               ┌──────────────┐
   │adapter │ │  app   │               │infrastructure│
   └───┬────┘ └────┬───┘               └──────┬───────┘
       │ 依赖      │ 依赖                       │ 依赖
       ▼           ▼                            ▼
   ┌────────┐  ┌────────┐                 ┌────────┐
   │ client │  │ domain │ ◀────────────── │  infra │
   └────────┘  └────────┘   只依赖 COLA 组件      （同一模块内的包结构）
```

- **client**：对外契约层，**不依赖任何业务实现模块**（app / domain / infra / adapter）。
- **domain**：领域层，**不依赖 client / app / infra / adapter**，仅依赖 COLA 领域组件。
- **app**：应用服务层，依赖 `client`（用例契约）和 `domain`（领域模型与仓储接口），**不依赖 infrastructure 实现**。
- **infrastructure**：基础设施层，依赖 `domain`，实现仓储等技术细节，**不被上层直接依赖为业务入口**。
- **adapter**：适配层，只依赖 `client`，使用其中的 API/Command/Query/DTO 作为入参和出参。
- **start**：启动层，显式依赖 `adapter`、`app`、`domain`、`infrastructure`，负责聚合模块与装配 Bean，但**不写业务逻辑**。

## 对象转换规范

项目遵循以下对象转换规范，确保各层职责清晰：

### 参数校验
- **位置**: Client 层
- **方式**: 在 DTO（包括 RequestDTO、Command、Query）上使用 JSR 303 注解（如 @NotNull、@NotEmpty、@Valid 等）进行参数校验

### 对象转换
- **App 层 Assembler**: 
  - Command → Domain Entity
  - Domain Entity → ResponseDTO
- **Infrastructure 层 Mapper**: 
  - Domain Entity ↔ DO（使用 MapStruct 进行双向转换）

### 转换工具
- **App 层**: 使用 Assembler（手动转换或 MapStruct）
- **Infrastructure 层**: 使用 MapStruct 进行 Domain Entity ↔ DO 的转换

## Client 命名规范（COLA 5.0 风格）

client/api:
  - {Aggregate}CmdService      // 写接口
  - {Aggregate}QueryService    // 读接口

client/dto/command:
  - Create{Aggregate}Cmd
  - Update{Aggregate}Cmd
  - Delete{Aggregate}Cmd
  - Add{SubResource}Cmd
  - Modify{SubResource}Cmd

client/dto/query:
  - {Aggregate}Query
  - {Aggregate}ListQuery
  - {Aggregate}PageQuery

client/dto/response:
  - {Aggregate}DTO
  - {Aggregate}ListResponse
  - {Aggregate}PageResponse

## 使用方式

### 基于模板创建新服务

1. 克隆模板项目
2. 修改包名和项目名
3. 配置数据库和 Nacos
4. 开始开发业务功能

## 文档

- 各包的 `package-info.java` 文件包含详细的包职责和架构定位说明
- 查看 `openspec/` 目录了解项目规范和架构设计

## 状态

🚧 开发中
