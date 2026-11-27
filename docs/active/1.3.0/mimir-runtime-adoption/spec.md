# Mimir 运行时能力接入需求约定

**归属版本**：1.3.0。输入：[版本范围](../release.md)、[公共 API 契约](../../../design-docs/arch-api-contract.md)。实现选择见[设计](design.md)，任务与验收见[计划](plan.md)。

## 目标与范围

统一模板的日志、开发跨域和运行时装配，移除已无业务用途的 COLA 组件。保留既有分层、客户 API、版本并发控制及数据库迁移。这里定义接入后的行为；当前交付状态只在版本记录维护。

## Behavior B1：应用日志与脱敏

| Scenario | Given | When | Then |
| --- | --- | --- | --- |
| 控制台输出 | 使用模板的 local/dev/test/prod 配置启动 | 产生符合当前级别的应用日志 | 日志输出到标准输出，带时间、级别、logger 和消息；本日志方案不创建日志目录或文件 |
| 请求关联 | 没有已保存追踪标识的新客户请求携带合法 `X-Trace-Id: runtime-42` | 在请求处理期产生一条应用日志并返回 R | 该日志含 runtime-42，响应头和 R.traceId 均为 runtime-42；请求结束后不污染后续请求 |
| 已声明凭据脱敏 | 日志消息或异常链含 `password=alpha token=beta secret=gamma api_key=delta` | 输出该日志 | 四个值均为 `****`，其余文本及异常结构仍可辨认 |
| 无请求上下文 | 应用启动或后台代码没有请求关联标识 | 输出日志 | 正常输出，不伪造 traceId；不要求后台日志含请求 ID |
| 配置错误 | 显式设置了无效日志格式或未知脱敏规则 | 初始化日志配置 | 报告配置错误，不以关闭日志或关闭脱敏继续正常启动 |

## Behavior B2：访问日志

| Scenario | Given | When | Then |
| --- | --- | --- | --- |
| 正常客户请求 | 客户请求不在排除路径内 | 同步请求完成 | 产生一条终态访问记录，包含方法、路径、最终 HTTP 状态及非负耗时；普通 2xx 为 INFO |
| 错误请求 | 请求最终 HTTP 状态为 400 或 500 | 请求完成 | 访问记录分别为 WARN 或 ERROR；业务错误仍按其实际 HTTP 200 记录，不改业务信封 |
| 排除边界 | 请求路径为 `/actuator/health` 或 `/favicon.ico` | 请求完成 | 不产生访问记录；原 HTTP 行为保持不变 |
| 敏感请求内容 | 请求携带 query、JSON body、Authorization 和 Cookie | 生成访问记录 | 不采集这些 query/body/认证头的内容；路径、直连 IP 和 User-Agent 属于记录范围，不承诺任意敏感文本都被识别 |

访问日志的关联标识与请求处理期日志分开验收：终态记录不承诺携带 traceId；本项不新增追踪桥接或补丁来保证它与响应一致。原因及框架时序见设计。

## Behavior B3：开发环境跨域

| Scenario | Given | When | Then |
| --- | --- | --- | --- |
| 允许开发前端 | 仅启用 local 或 dev，Origin 为 `http://localhost:5173` | 对客户接口发合法 GET 或 PUT 预检 | 允许该 Origin，携带凭据策略为 true；预检有效期 3600 秒，前端可读取 X-Trace-Id 和 Content-Disposition |
| 白名单外来源 | 同上，但 Origin 为 `http://localhost:5174`、`http://127.0.0.1:5173` 或 `https://untrusted.example`，未额外配置 | 发起跨域请求 | 拒绝跨域请求，不放行到业务写用例；需要其他端口或主机时须显式加入白名单 |
| 方法边界 | Origin 在白名单内 | 发 PATCH 预检 | 拒绝；允许的方法保持 GET/POST/PUT/DELETE/OPTIONS，不自动扩大到框架默认 PATCH |
| 关闭环境 | 使用模板的 test/prod 配置，或混入 test/prod 与 local/dev | 发跨域预检 | 不启用开发跨域策略、不返回允许跨域响应头；同源请求与普通非浏览器 HTTP 保持可用 |
| 无效策略 | 显式开启跨域但白名单为空，或凭据为 true 且 Origin 为 `*` | 创建运行配置 | 明确失败，不退回任意来源放行 |

该策略在启用时作用于所有 Servlet 路径，包括 `/api/**`、文档和监控路径；它不替代认证或路径授权。CORS 拒绝/预检可能在 MVC 之前结束，不要求它们返回 R 或 traceId。

## Behavior B4：去除重复组件与装配冲突

| Scenario | Given | When | Then |
| --- | --- | --- | --- |
| 无重复组件启动 | 已配置可用的专用数据源且迁移前置满足 | 启动应用 | 客户接口可用、审计与事务行为保留；业务运行不依赖 COLA 组件 |
| 重名 Bean | 配置引入非条件退让的同名 Bean | 启动应用 | 报告冲突并停止，不静默替换已有 Bean |
| 依赖移除回归 | 已移除框架组件，客户输入缺版本、版本过旧或触发业务错误 | 运行既有用例与 HTTP 契约 | 保留 400 校验与 200+业务码等既有结果，不因组件清理切回其他响应信封 |

## Constraints

- CORS 默认白名单为 1 个明确来源 `http://localhost:5173`，与当前 Asgard 默认开发端口一致；不保留 localhost 任意端口兼容。额外来源由明确配置提供，不从请求自动推导。
- 未提供外部 profile 选择时，模板默认激活 local：应用日志 DEBUG、开发 CORS 开启。基础 YAML 中的 INFO/CORS=false 是叠加 profile 前的值，不代表该默认启动结果。
- 默认文本日志、控制台输出；脱敏规则为 password/token/secret/api_key 四组。未承诺姓名、邮箱、电话号码、SQL 参数或任意异常文本均被遮蔽，也不新增请求体日志。
- 访问日志慢请求阈值保持 3000 ms；只记录实际事件，不设置未经测量的吞吐或延迟目标。
- 不新增认证、JSON 日志选型、日志文件滚动、OpenTelemetry/追踪桥接、Mimir 修改、架构规则升级或前端/Bifrost 改动。
- 自动测试仅使用单元、MockMvc standalone 与 ArchUnit；完整启动、实际日志输出和 HTTP/数据库运行证据通过专用环境人工验收，不能由隔离测试替代。
