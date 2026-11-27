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

- 请求使用扁平查询参数：`GET /api/customers?pageIndex=1&pageSize=10&keyword=&status=`。Qry 自行声明 `pageIndex`、`pageSize`，经 `toPageRequest()` 转换为 Mimir `PageRequest`，复用其纠正与 offset 计算；不直接继承嵌套结构的 `PageQuery`。`error-contract-r` 阶段保留 `findAll + count`；`data-consistency` 改为 Repository.findPage，经 `PageConverters.toMybatisPage`、一次 `selectPage`、记录转领域与 `toPageResult` 返回结果，App 用 `PageResult.map` 转换视图并保留元数据。一次分页调用仍可能执行 count/records 两条 SQL，不再另查总数，也不承诺并发写入下二者的快照一致性。
- 响应为 `R<PageResult<T>>`：`data, totalCount, pageIndex, pageSize, totalPages, hasNext, hasPrevious`。

本次采用的具体边界：缺省或 null 使用 1/10；小于 1 的页码/大小分别纠正为 1/10；大小超过 1000 截为 1000。Qry 字段为 Long；协议页码上界仍为 2147483647，超过时在 HTTP 边界返回 400，直接用例调用抛 PARAM_INVALID，不因持久化分页参数支持 long 而扩大公开范围。查询与回包使用同一组纠正值；非数字绑定失败返回 400，status 保持 ACTIVE/INACTIVE/空值范围。旧 `page/size` 不设别名兼容，作为未知查询参数按现有绑定规则忽略。

五个现有 HTTP 接口及输入/输出为：

| 接口 | 输入 | 成功数据 | 业务错误 |
| --- | --- | --- | --- |
| POST `/api/customers` | JSON：name:string、email:string、phone:string? | CustomerCO | 21001 |
| GET `/api/customers/{id}` | id:number | CustomerCO | 20001 |
| GET `/api/customers` | pageIndex/pageSize:number?、keyword/status:string? | PageResult<CustomerCO> | 无新增业务码 |
| PUT `/api/customers/{id}` | id:number；JSON 必填 version:integer，可选 name/email/phone | CustomerCO | 20001、21001、21002 |
| DELETE `/api/customers/{id}` | id:number | null | 20001；重复删除仍为 20001 |

CustomerCO 字段为 `id:number,name:string,email:string,phone:string|null,status:string,version:integer`；version 由 `data-consistency` 新增，创建为 0，详情、列表及更新结果均携带版本。所有成功状态均为 HTTP 200 + `00000`，业务错误为 HTTP 200 + 对应码。列表总数不转换为 int，列表始终为数组；空列表第 1 页总数/总页数为 0，前后页标记 false。创建不承诺幂等，更新保持当前部分字段语义，不新增幂等键或重试保证。

### 并发更新

- version 为 Integer/INT，创建为 0，输出范围 0..2147483647。更新请求必须携带 JSON 整数 version，范围 0..2147483646；缺省/null/负数/超范围、小数、数字字符串或布尔值均返回 HTTP 400，不允许截断、溢出或清零。
- version 不算业务更新字段；只有 version 或业务字段全 null 仍是 400+00004、OBJECT/$ 空更新错误。version 的字段约束错误为 FIELD/version，无法解析为整数的请求体按既有绑定错误映射。
- App/Repository 直接调用显式防御无效版本，抛 PARAM_INVALID（00004）；HTTP 与直接调用的不同错误边界保持不变。
- HTTP 先执行既有 MVC 绑定与字段校验；进入 App 后的判定顺序为输入版本/空更新 → 加载客户（不存在 20001）→ 比较版本（旧版本 21002）→ 合并及校验业务字段/邮箱预查 → 条件写入；版本已旧时不再检查新邮箱是否被占用。
- 基于 MyBatis-Plus 乐观锁条件更新并检查影响行数，恰好 1 行才成功并返回 version+1；提交相同业务值也递增。0 行后同事务的 FOR UPDATE 当前读区分不存在（20001）与冲突（21002），不使用可能命中 RR 旧快照的普通读取。客户端提示刷新后重试，服务端不自动覆盖或重试；死锁/连接失败仍是系统错误。
- 新版需 V2 迁移及停写升级；前端必须保存并提交所读版本，旧请求不作无版本兼容。审计字段由服务端维护，不加入 CustomerCO 或可写请求。

上述并发更新、version 和分页内部单次调用由 [data-consistency](../active/1.3.0/data-consistency/plan.md) 交付。此前 `error-contract-r` 仅定义 21002 与信封映射；本项的本地验证、实际数据库/应用及消费者待验收边界分别见[版本记录](../active/1.3.0/release.md)，不能用隔离 SQL 生成测试代替真实并发验收。

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
