---
id: design-api-contract
status: draft
owner: ""
tags: [api, contract, mimir, error-code]
created: 2026-11-11
updated: 2026-11-11
verified: null
---

# HTTP API 响应与错误契约

## 技术问题与约束

当前 Midgard 同时存在三种响应形态，Asgard 只能正确消费其中一种（见技术债 TD-009、TD-011）：

| 来源 | 形态 | 位置 |
| --- | --- | --- |
| 成功响应 | COLA `SingleResponse` / `PageResponse` / `Response`（`success/errCode/errMessage`） | `CustomerController` |
| 本地异常处理 | `ErrorResponse{code,message,timestamp,path}` | `GlobalExceptionHandler`、`ErrorResponse` |
| Mimir 异常处理 | `R{code,message,data,timestamp,traceId}` | `mimir-boot-starter-exception` 的 `MimirExceptionHandler` |

两个 Advice 均处于最低优先级，实际匹配顺序未定义；Mimir Handler 不识别 COLA `BizException`。Asgard 拦截器读取 `errCode/errMessage`，后端错误格式进入非 2xx 分支后业务码丢失。

约束：

- 以 Mimir 2.4.0 正式版能力为准，不在模板内重复实现框架已有的异常处理、分页、序列化能力。
- Domain 层只允许依赖不含框架的公共包；`mimir-boot-common` 为纯 POJO，不在 `ColaArchRules.domainIsFrameworkFree` 禁止范围内。
- 本契约同时约束 Asgard 前端；Bifrost E2E 在 1.3.0 不随之修改。

## 方案对比

| 方案 | 收益 | 代价与风险 |
| --- | --- | --- |
| 全面采用 Mimir `R` | 零适配代码；`traceId` 进入响应体；错误码分段与所有 Mimir 服务一致 | 前端拦截器、类型与测试需重写；既有 E2E 断言失效 |
| 统一为 COLA `Response`，经 `ExceptionResponseFactory` 适配 | 前端几乎不改 | 响应体缺少 `traceId`；在 Mimir 外长期维护一层自定义工厂 |
| 后端不改，前端兼容多种格式 | 改动最小 | 三套信封继续并存，问题转移到前端 |

业务错误的 HTTP 状态另行比较：遵循 Mimir（业务错误 200 + 业务码）、扩展 Mimir 支持语义化状态、本地 Advice 覆盖映射。后两者分别扩大框架契约范围或重新引入本地重复处理器。

## 采用方案及理由

采用 Mimir `R` 作为唯一信封，HTTP 状态只表达协议层结果，业务结果由 `code` 表达。

### 状态码与信封

| 场景 | HTTP | `code` | `data` |
| --- | --- | --- | --- |
| 全部成功（含创建、删除） | 200 | `00000` | 业务数据；删除为 `null` |
| 业务错误（Mimir `BizException`） | 200 | `2xxxx` | `null` |
| 参数缺失 / 校验失败 / 请求体不可解析 | 400 | `00003` / `00004` 等 Mimir 映射值 | 校验失败时为 `ValidationErrorData` |
| 404 / 405 / 406 / 413 / 415 等协议错误 | 对应状态 | Mimir 映射值 | `null` |
| 系统错误 | 500 | `1xxxx` | `null`，消息不含内部细节 |

客户端判定规则：HTTP 2xx **且** `code === "00000"` 为成功，其余均为失败。

注意：Mimir 对路由不存在（`NoResourceFoundException`、`NoHandlerFoundException`）返回 HTTP 404 + `20001`，与业务“数据不存在”（HTTP 200 + `20001`）同码。客户端需结合 `httpStatus` 区分接口路径错误与业务数据不存在。

### 错误码

- 通用语义复用 Mimir `ErrorCode`，如 `DATA_NOT_FOUND`（`20001`）、`DATA_ALREADY_EXISTS`（`20002`）、`PARAM_INVALID`（`00004`）。
- 需要客户端区别处理的领域错误，由 Domain 层枚举实现 `ErrorDefinition`，通过 `BizException.fromError(...)` 抛出；不再使用 COLA `BizException`。Domain 模块为此依赖 `mimir-boot-common`。
- 号段：`2xxxx` 为业务段，第二位为领域编号，后三位为领域内序号。

| 领域编号 | 领域 | 已分配 |
| --- | --- | --- |
| 1 | Customer | `21001` 邮箱已被占用；`21002` 数据已被他人修改，请刷新后重试 |

新增领域时在本表登记编号，避免冲突。

### 校验错误

开启 `mimir.boot.exception.validation.structured-details-enabled=true`。校验失败时 `data` 为：

```json
{
  "errors": [
    { "source": "<Mimir ValidationErrorSource>", "path": "email", "parameterIndex": 0, "constraint": "Size", "message": "..." }
  ],
  "totalCount": 1,
  "truncated": false
}
```

`source` 取值以 Mimir 枚举为准，实施时由集成测试抓取真实响应固化为样例。客户端按 `path` 映射到表单字段，`truncated=true` 时追加汇总提示。

长度约束以 DDL 为上限（`name` 50、`email` 100、`phone` 20），在请求 DTO（`@Size`）、`Customer.validate()` 与数据库列三处同时生效。

### 分页

- 请求使用扁平查询参数：`GET /api/customers?pageIndex=1&pageSize=10&keyword=&status=`。Qry 自行声明 `pageIndex`、`pageSize`，经 `toPageRequest()` 转换为 Mimir `PageRequest`，复用其纠正与 offset 计算；不直接继承嵌套结构的 `PageQuery`。持久化层用 `PageConverters.toMybatisPage` / `toPageResult` 完成 MyBatis-Plus 分页与 `PageResult` 的转换。
- 响应为 `R<PageResult<T>>`：`data, totalCount, pageIndex, pageSize, totalPages, hasNext, hasPrevious`。

### 并发更新

- 读取结果携带 `version`；更新请求必须携带 `version`。
- 基于 MyBatis-Plus 乐观锁条件更新并检查影响行数：记录不存在返回 `20001`，版本不一致返回 `21002`。客户端提示刷新后重试，不自动覆盖。

### 实现约束

- Controller 显式返回 `R<T>` / `R<PageResult<T>>`，不使用全局响应包装。
- 对外 DTO 实现 `Serializable`，满足 `R<T extends Serializable>`。
- `traceId` 由 Mimir `ResponseBodyEnhancer` 填充，与 `X-Trace-Id` 响应头一致。
- ID 以数字输出，保持 `mimir.boot.web.serialization.write-large-integers-as-string` 为默认的 `false`。若改用 `ASSIGN_ID` 等可能超出 JS 安全整数的策略，应对 ID 字段统一显式按字符串序列化，而不依赖按值切换类型的全局开关。
- 删除本地 `GlobalExceptionHandler` 与 `ErrorResponse`，异常处理只保留 Mimir Handler。

## 影响范围与兼容性

- **Midgard**：Controller 返回类型、请求/响应 DTO、App 与 Domain 的异常抛出方式、Customer 表结构（`version` 列，见 1.3.0 `data-consistency`）。
- **Asgard**：请求拦截器、API 类型、Customer 页面分页与表单错误处理，见 Asgard 1.3.0 `api-contract-r`。
- **Bifrost**：现有 Customer CRUD spec 依赖旧契约，1.3.0 后将失败，由后续 E2E 迭代跟进。
- 这是不兼容的公共契约变更，基于本模板派生的项目升级时需同步前端。

## 验证依据

计划执行（尚无结果）：

- `CustomerApiIT` 在真实 Spring 上下文与 MySQL 下断言 JSON 结构，覆盖成功、`20001`、`21001`、`21002`、`00004`、请求体不可解析（400）、未知异常（500），并确认仅 Mimir Handler 生效。
- 从上述测试的真实响应导出样例，作为 Asgard 拦截器与 Customer API 单元测试输入。

## 参考

- [技术债清单](../active/tech-debt-tracker.md) TD-005、TD-009、TD-010、TD-011
- [1.3.0 版本记录](../active/1.3.0/release.md)
