---
updated: 2026-11-27
---

# 业务领域划分

本项目是遵循 COLA 分层理念的 DDD 后端模板。当前已实现第一个业务聚合（Customer），用于展示完整的分层链路。

## 领域清单

| 领域 | 职责说明 | 代码位置 | 关键实体 |
|------|----------|----------|----------|
| Customer（客户） | 客户信息管理，包含创建、查询、更新、删除 | `domain/.../customer/` | Customer, CustomerStatus |

## Customer 聚合详情

### 领域模型

| 类型 | 类名 | 说明 |
|------|------|------|
| Entity | `Customer` | 聚合根，包含 name/email/phone/status/version；实现 Serializable |
| Value Object | `CustomerStatus` | 枚举：ACTIVE, INACTIVE |
| Repository | `CustomerRepository` | 持久化接口（save/findById/findPage/update/deleteById/existsByEmail；分页返回 PageResult，更新带预期版本） |
| 错误定义 | `CustomerErrorCode` | 21001 邮箱重复；21002 版本冲突，真实并发结果仍需运行验收 |

### 业务规则

- `validate()`：name 非空白且长度 2-50，email 符合现有格式且不超过 100，phone 可选且不超过 20
- `initForCreate()`：新建客户默认状态为 ACTIVE、version=0
- HTTP 创建/更新请求通过字段和对象级校验；直接调用由 App/Domain 防线保护。更新必须带有效 version，版本不匹配拒绝写入，成功后返回递增版本；version 本身不算业务更新字段。更新字段的 null 表示不修改，电话空串可清空。
- 错误与分页信封统一为 Mimir R，字段和状态码以 [HTTP API 契约](design-docs/arch-api-contract.md) 为准；不再维护本地 GlobalExceptionHandler/ErrorResponse。

### 数据库映射

- 表名：`customer`
- 唯一迁移源：Start 模块的 [V1](../start/src/main/resources/db/migration/V1__create_customer.sql) 与 [V2](../start/src/main/resources/db/migration/V2__customer_audit_and_version.sql)，由应用配置中的 Flyway `classpath:db/migration` 加载；维护方式见[数据库迁移规则](engineering/development.md#数据库迁移)
- 唯一约束：email（idx_email）
- 索引：status（idx_status）
- 仓储仅将可识别的 idx_email 重复键错误翻译为 21001；插入/更新恰好影响 1 行才成功。更新 0 行后用同事务 FOR UPDATE 当前读区分不存在 20001 和冲突 21002，失败不回填新版本。
- App 使用同一组 PageRequest 纠正值调用 findPage，再通过 PageResult.map 转为 CO；Infrastructure 一次 selectPage 复用框架分页转换，不另查总数。一次调用仍可能产生 count/records 两条 SQL。
- 审计采用 Mimir Handler 与 AuditorProvider 示例（system），DO 字段 create_time/create_by 仅插入填充且禁止更新，update_time/update_by 插入与更新填充；审计字段不进入领域或客户视图。

### 各层实现状态

| 层 | 状态 | 说明 |
|----|------|------|
| domain | ✅ 完成 | Entity + VO + Repository 接口 + 业务规则 |
| infrastructure | ✅ 完成 | Repository 实现 + DO + InfraConvertor |
| app | 已实现 | AppService + Cmd/Qry/CO + Convertor/Assembler；当前未拆分 CmdExe/QryExe |
| adapter | ✅ 完成 | REST Controller + Request DTO + Convertor |
| client | 预留 | 当前只有 package-info，尚无跨服务接口与契约 DTO |
| 迁移 | ✅ 完成 | Flyway V1/V2 脚本就位；真实迁移待验收 |

表中状态仅描述源码落点。项目只维护单元/standalone/架构测试；真实应用、数据库、并发与前端消费证据单独说明，不能由隔离测试结果推断。

## 领域间关系

```mermaid
flowchart LR
  Customer["Customer（已实现）"]
```

> 当前仅有单一聚合，无跨聚合依赖。引入第二个聚合时在此注册关系。

## 领域通信规则

- 领域之间不允许循环依赖
- 跨领域通信只能通过 App 层 Service 编排
- Domain 层不感知其他聚合的内部实现
