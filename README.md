# midgard-backend-template

> 基于 COLA 5.0 的 DDD 后端服务模板，用于快速启动符合分层边界的 Spring Boot 微服务。

## 技术栈

- Java 17 + Spring Boot 3.3
- COLA 5.0 DDD 分层架构（adapter → app → domain ← infrastructure）
- MySQL 8.4 + MyBatis-Plus（`@AutoMybatis` 自动生成 Mapper）
- MapStruct 对象转换
- Nacos 服务注册 + OpenFeign/Dubbo 通信
- Redis 缓存

## 快速开始

```bash
# 1. 克隆项目
git clone <template-repo-url>

# 2. 配置本地环境
cp start/src/main/resources/application-local.yml.example start/src/main/resources/application-local.yml
# 编辑 application-local.yml，填入本地数据库连接信息

# 3. 启动
./mvnw spring-boot:run -pl start -am
```

## 开发命令

```bash
./mvnw clean verify                    # 全量校验
./mvnw spring-boot:run -pl start -am   # 启动服务
./mvnw -Pdev clean verify              # 指定 profile
```

## 模块结构

| 模块 | 角色 |
|------|------|
| `client` | 外部契约层（Dubbo/Feign 接口与 DTO） |
| `adapter` | 协议适配层（HTTP、RPC、MQ 入站） |
| `app` | 应用层（用例编排、事务边界） |
| `domain` | 领域层（业务规则、Repository 接口） |
| `infrastructure` | 基础设施层（持久化实现、外部 Gateway） |
| `start` | 启动层（Spring Boot 启动与装配） |

详细的分层职责、依赖方向、包结构与命名约束见 [`ARCHITECTURE.md`](./ARCHITECTURE.md)。

## 常见问题

<details>
<summary><b>Q: 为什么没有 Mapper 接口文件？</b></summary>

使用 `@AutoMybatis` 注解在编译期自动生成：

```java
@TableName("customer")
@AutoMybatis
public class CustomerDO { }
```
</details>

<details>
<summary><b>Q: Entity 与 DO 的区别？</b></summary>

- **Entity**：包含业务逻辑和行为方法（Domain 层）
- **DO**：只包含数据字段，对应数据库表（Infrastructure 层）

两者通过 `InfraConvertor` 转换。
</details>

<details>
<summary><b>Q: Convertor 与 Assembler 的区别？</b></summary>

- **Convertor**：Cmd → Entity（写入方向）
- **Assembler**：Entity → CO（读取方向）
</details>

## 使用模板

```bash
# 全局替换包名
io.yggdrasil.labs.midgard → io.your.company.yourservice

# 修改项目名
pom.xml: midgard-backend-template → your-service-name
```

## 提交规范

Conventional Commits：`<type>(<scope>): <中文描述>`

## 相关文档

- 架构与分层约束：[`ARCHITECTURE.md`](./ARCHITECTURE.md)
- AI Agent 导航：[`AGENTS.md`](./AGENTS.md)
- 设计哲学：`docs/design-docs/core-beliefs.md`
- 需求工作流：`docs/active/index.md`
