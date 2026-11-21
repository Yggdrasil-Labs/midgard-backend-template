---
id: design-api-contract
status: draft
owner: ""
tags: [api, contract, mimir, error-code]
created: 2026-11-11
updated: 2026-11-21
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
| 业务错误（Mimir `BizException`） | 200 | 异常自带码；领域错误为 `2xxxx` | `null` |
| 参数缺失 / 校验失败 / 请求体不可解析 | 400 | `00003` / `00004` 等 Mimir 映射值 | 校验失败时为 `ValidationErrorData` |
| 404 / 405 / 406 / 413 / 415 等协议错误 | 对应状态 | Mimir 映射值 | `null` |
| 系统错误 | 500 | `1xxxx` | `null`，消息不含内部细节 |

客户端判定规则：HTTP 2xx **且** `code === "00000"` 为成功，其余均为失败。

公共 JSON 字段为 `code:string`、`message:string`、`data:T|null`、`timestamp:number`（毫秒时间戳）、`traceId:string|null`、`success:boolean`；`success` 与 code 派生一致，不输出 `fail`、`errCode` 或 `errMessage`。启用 `mimir.boot.web.serialization.write-nulls=true`，明确输出空 data 及业务 DTO 空字段；保留 `mimir.boot.web.json.include-legacy-fail=false`。客户端仍按 code 判定，不以新增 success 字段替代该规则。

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

`source` 取值以 Mimir 枚举为准；测试使用 MockMvc standalone 与序列化断言核对响应结构。需要真实响应样例时，按需在目标运行环境人工采集并记录环境，不以 mock 结果冒充真实响应。客户端按 `path` 映射到表单字段，`truncated=true` 时追加汇总提示。

长度约束以 DDL 为上限（`name` 50、`email` 100、`phone` 20），在请求 DTO（`@Size`）、`Customer.validate()` 与数据库列三处同时生效。

姓名长度下限为 2；创建姓名/邮箱必填；更新字段缺省或 null 表示不修改，姓名/邮箱提供空白值则拒绝。电话可选，null 表示不修改，字符串 "" 允许清空。更新三字段全缺省/全 null 返回 HTTP 400 + `00004`、对象级 `OBJECT/$` 错误；字段错误为 `FIELD/字段名`。请求边界保留原 `@Email` 校验，并覆盖现有领域邮箱格式/空白限制，使 HTTP 接受值满足领域防线；拒绝时不进入 App 写用例，不反向放宽原 HTTP 校验。

HTTP 400 来自 MVC 校验或绑定。App/Domain 直接调用的参数防线使用 `BizException` + `PARAM_INVALID`（`00004`），如经 HTTP Handler 暴露仍为 200；不能仅替换错误码便声称会返回 400。

### 分页

- 请求使用扁平查询参数：`GET /api/customers?pageIndex=1&pageSize=10&keyword=&status=`。Qry 自行声明 `pageIndex`、`pageSize`，经 `toPageRequest()` 转换为 Mimir `PageRequest`，复用其纠正与 offset 计算；不直接继承嵌套结构的 `PageQuery`。`error-contract-r` 保留现有 Repository 的 `findAll + count`，由 App 组装 `PageResult`；持久化层的 `PageConverters.toMybatisPage` / `toPageResult` 随 `data-consistency` 一并交付，与单次分页查询改造同阶段实施。
- 响应为 `R<PageResult<T>>`：`data, totalCount, pageIndex, pageSize, totalPages, hasNext, hasPrevious`。

本次采用的具体边界：缺省或 null 使用 1/10；小于 1 的页码/大小分别纠正为 1/10；大小超过 1000 截为 1000。Qry 字段为 Long；因当前 Repository 页码参数仍为 int，页码超过 2147483647 在 HTTP 边界返回 400，直接用例调用抛 PARAM_INVALID，禁止静默截断。查询与回包使用同一组纠正值；非数字绑定失败返回 400，status 保持 ACTIVE/INACTIVE/空值范围。旧 `page/size` 不设别名兼容，作为未知查询参数按现有绑定规则忽略。

五个现有 HTTP 接口及输入/输出为：

| 接口 | 输入 | 成功数据 | 业务错误 |
| --- | --- | --- | --- |
| POST `/api/customers` | JSON：name:string、email:string、phone:string? | CustomerCO | 21001 |
| GET `/api/customers/{id}` | id:number | CustomerCO | 20001 |
| GET `/api/customers` | pageIndex/pageSize:number?、keyword/status:string? | PageResult<CustomerCO> | 无新增业务码 |
| PUT `/api/customers/{id}` | id:number；JSON 可选 name/email/phone | CustomerCO | 20001、21001；21002 见下方分期 |
| DELETE `/api/customers/{id}` | id:number | null | 20001；重复删除仍为 20001 |

CustomerCO 字段为 `id:number,name:string,email:string,phone:string|null,status:string`；本项不新增 version。所有成功状态均为 HTTP 200 + `00000`，业务错误为 HTTP 200 + 对应码。列表总数不转换为 int，列表始终为数组；空列表第 1 页总数/总页数为 0，前后页标记 false。创建不承诺幂等，更新保持当前部分字段语义，不新增幂等键或重试保证。

### 并发更新

- 读取结果携带 `version`；更新请求必须携带 `version`。
- 基于 MyBatis-Plus 乐观锁条件更新并检查影响行数：记录不存在返回 `20001`，版本不一致返回 `21002`。客户端提示刷新后重试，不自动覆盖。

上述并发更新与 version 字段由 `data-consistency` 交付。`error-contract-r` 仅定义 `21002` 并验证错误信封映射，不生成真实版本冲突或声明已实现乐观锁；分页内部单次查询优化也留给 `data-consistency`。

### 实现约束

- Controller 显式返回 `R<T>` / `R<PageResult<T>>`，不使用全局响应包装。
- 对外 DTO 实现 `Serializable`，满足 `R<T extends Serializable>`。
- `traceId` 由 Mimir `ResponseBodyEnhancer` 填充，与 `X-Trace-Id` 响应头一致。
- ID 以数字输出，保持 `mimir.boot.web.serialization.write-large-integers-as-string` 为默认的 `false`。若改用 `ASSIGN_ID` 等可能超出 JS 安全整数的策略，应对 ID 字段统一显式按字符串序列化，而不依赖按值切换类型的全局开关。
- 删除本地 `GlobalExceptionHandler` 与 `ErrorResponse`，异常处理只保留 Mimir Handler。
- 邮箱预查及可识别的 MySQL `idx_email` 唯一键冲突均映射为 `21001`。删除本地 Advice 前，将定向翻译移至 Infrastructure save/update 边界；其他索引或未知数据库异常不得误报邮箱重复，不向客户端暴露 SQL 或底层异常消息。

## 影响范围与兼容性

- **Midgard**：Controller 返回类型、请求/响应 DTO、App 与 Domain 的异常抛出方式、Customer 表结构（`version` 列，见 1.3.0 `data-consistency`）。
- **Asgard**：请求拦截器、API 类型、Customer 页面分页与表单错误处理，见 Asgard 1.3.0 `api-contract-r`。
- **Bifrost**：现有 Customer CRUD spec 依赖旧契约，1.3.0 后将失败，由后续 E2E 迭代跟进。
- 这是不兼容的公共契约变更，基于本模板派生的项目升级时需同步前端。

## 验证依据

`error-contract-r` 已提供本地隔离合同与受管序列化证据，实际执行结果及仍受限的回归集中在[版本记录](../active/1.3.0/release.md#error-contract-r2026-11-21)。以下测试范围不能替代运行环境、数据库或消费者验收：

- Adapter 层单元测试与 MockMvc standalone 测试断言 JSON 结构，覆盖成功、`20001`、`21001`、`21002` 映射夹具、`00004`、请求体不可解析（400）及未知异常（500）的契约映射；使用 Mimir 受管 Jackson 配置核对字段形状。此类测试不证明完整 Spring 上下文中 Handler 的装配行为或真实并发能力。
- Asgard 拦截器与 Customer API 单元测试使用明确的契约 fixture；真实运行响应如有需要，按需人工采集，单独标注运行环境与来源。

## 参考

- [技术债清单](../active/tech-debt-tracker.md) TD-005、TD-009、TD-010、TD-011
- [1.3.0 版本记录](../active/1.3.0/release.md)
