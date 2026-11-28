# Mimir 运行时能力接入实施计划

**归属版本**：1.3.0。输入：[需求约定](spec.md)、[设计](design.md)、[版本记录](../release.md)。关联 [TD-014](../../tech-debt-tracker.md#td-014运行时配置与组件未收敛到-mimir)。基线 `5246d96`，mimir-upgrade-gates、error-contract-r 已本地完成；data-consistency 已提交，其真实环境验收仍待补。

## 目标与关键约束

按设计显式选 Mimir container/text 日志，保留控制台方式并启用四组凭据脱敏及访问日志；CORS 仅在 local/dev 且没有 test/prod 时开启，以明确 Origin 白名单暴露 X-Trace-Id；移除 COLA 组件与扫描，关闭 Bean 覆盖。

不改客户 API、版本/迁移/数据语义、认证、前端、架构测试框架或发布配置。不升级 Mimir/Boot/Java/依赖版本，不新增 Spring 上下文或外部 I/O 集成测试，不修改用户忽略的 application-local.yml。

当前源码无 COLA import/注解使用，Client 三处 package-info 及四个组件依赖仍有残留。不得仅搜索 import 就认定传递依赖、自动配置或运行行为已验证。日志选型必须显式设置 logging.config；不能仅删除本地 XML 后使用依赖根目录的旧配置。

## 顺序与归属

T1 → T2 → T3 → T4 串行。根 POM、Start 配置与测试 classpath 共享依赖，不并行改写。只读框架核对和独立审查可并行；不得同时运行会改写相同 target 的 Maven 生命周期。Plan 不作进度账本，实际结果集中进入 release.md。

### T1：组件与扫描清理

**文件**：

- Modify：`pom.xml` — 删除 COLA BOM/property；只在证据显示存在必要传递依赖缺口时补既有 BOM 管理的直接依赖。
- Modify：`client/pom.xml`、`domain/pom.xml`、`app/pom.xml` — 分别删除 dto、exception/domain-starter、catchlog-starter；保留 Domain 的 mimir-boot-common、事务/校验/转换依赖。
- Modify：`start/src/main/java/io/yggdrasil/labs/midgard/start/Application.java` — 只扫描应用包，保留 MapperScan。
- Modify：`client/src/main/java/io/yggdrasil/labs/midgard/client/dto/cmd/package-info.java`、`qry/package-info.java`、`co/package-info.java`（后两者相对同一 dto 目录）— 去除 COLA 基类及相关注解示例，不增加实际 Client DTO。

**验收**：

- 对源码/配置及实际依赖树核对 `com.alibaba.cola`/`cola-component`/`cola-components-bom`，业务代码和实际组件依赖均清零；历史需求文档和“COLA 分层理念”文字不机械删除。
- 生产/测试编译通过；没有因删除组件丢失当前实际使用的 API。保留当前 ArchUnit 与全部客户测试意图；架构规则升级留给 arch-test-adoption。
- 校验生成的 Mapper、MapStruct、普通 JAR 与 exec JAR；不提交 target 或 flattened POM。无来源证据时不引入替代组件或恢复 COLA。

### T2：日志与开发跨域配置

**文件**：

- Delete：`start/src/main/resources/logback-spring.xml`。
- Delete：`adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/CorsConfig.java`。
- Modify：`start/src/main/resources/application.yml` — 合并设计中的 logging.config、text/mask/access、Bean 覆盖 false、基础 CORS false 和 local/dev 条件文档；不覆盖现有 exception/serialization/Flyway 配置。
- Modify：`start/src/main/resources/application-dev.yml`、`application-test.yml`、`application-prod.yml` — 精确 logger 包前缀及设计级别表；不在这些文件再引入 CORS enabled 以绕过条件文档。
- Modify：`start/src/main/resources/application-local.yml.example` — 说明明确来源列表覆盖、默认 5173 和级别；不写入真实凭据、不覆盖实际 local 文件。

**验收**：

- 配置矩阵分别检查基础文档原始值、未提供外部 profile 选择的实际默认 local，以及显式 local/dev/test/prod/dev+prod/local+test。基础文档是应用 INFO/CORS=false；默认启动和显式 local/dev 为应用 DEBUG/CORS=true；test/prod 或混入它们的 profile 不开启开发 CORS。所有矩阵项均使用日志 preset/text/四组脱敏/access INFO、Bean 覆盖 false。激活配置必须按 profile 条件生效，不能把基础文档检查冒充默认启动，也不能把多文档 YAML 无条件合并。
- 应用 root/package 级别与设计表一致，dev/test/prod 原 `.*` 后缀已移除；access 排除列表沿用框架默认，不错误覆盖为空。
- CORS 具体属性为 allowed-origins、5 个原有方法、allowed-headers=*、credentials=true、max-age=3600s、两项暴露头；无 allowed-origin-patterns 幻想配置，无全来源凭据放行。
- exec JAR 的 BOOT-INF/classes 不包含本地 XML/CorsConfig；保留 starter 内的原生资源，不能把依赖根 logback-spring.xml 的存在误当清理失败。从 JAR 可解析显式 container.xml 及其 include 资源。

### T3：隔离回归与本地完整门禁

**文件**：

- Create：`start/src/test/java/io/yggdrasil/labs/midgard/start/runtime/RuntimeConfigurationTest.java` — 对实际 Start YAML 的 profile 选择、绑定、外部明确 Origin 覆盖及错误条件做纯单元验证；可在同测试包提取必要资源加载 helper。
- Create：同目录 `RuntimeCorsContractTest.java` — 直接构造 Mimir CorsConfig/Filter + standalone MVC 的最小测试控制器，使用绑定后的策略；不新增生产控制器。
- Create：同目录 `RuntimeLoggingContractTest.java` — 对真实 EPP/脱敏 converter/AccessLogFilter 的公开入口做内存验证；读取项目配置作为输入，不重新维护一份硬编码默认配置副本。
- Check/Modify（仅需要补本项边界时）：`adapter/src/test/java/io/yggdrasil/labs/midgard/adapter/web/customer/CustomerResponseSerializationTest.java` — 复用已有真实 TraceInterceptor/ResponseBodyEnhancer 测试，保留 MDC 恢复与头/体一致场景；不要复制到新测试。

**验收**：

- 对删除前的 CORS/default配置缺口选择有意义的聚焦回归再验证修改；配置/资源删除以静态检查为准，不为制造 Red 改坏正确测试。
- CORS：允许 localhost:5173 GET/PUT 预检与凭据；拒绝 localhost:5174、127.0.0.1:5173、外部 Origin 和 PATCH 预检；检查 Content-Disposition/X-Trace-Id 暴露、maxAge，非法空白名单/`*`+凭据失败。无 Origin/同源请求不被错误拦截；记录 `/**` 范围，不把测试限制于 `/api/**` 而遗漏范围变化。
- 日志：格式为 text，EPP 能识别显式 preset，未知 format/规则被拒绝；合成消息及 cause 链的四个凭据值均遮蔽。访问 Filter 正常/400/500/排除路径及不采集 query/body/认证头均以实际事件断言；不使用 sleep 验证 3000ms 阈值，阈值以绑定检查，慢请求运行验证按需进行。
- 仅用 Mock Servlet/standalone、真实公开配置方法、Binder/Environment 和内存 appender/converter；禁止 ApplicationContextRunner、SpringBootTest、容器/数据库或磁盘日志测试。配置资源读取不等于完整应用启动。正确保存/恢复静态 converter 的配置、logger 级别与 appender、MDC，禁止反射调用上游私有测试 reset；确实不能隔离的启动行为留 T4，不复制框架实现。
- 使用 Wrapper/Java 17 执行 `./mvnw -B -ntp -Pprecheck verify`、`./mvnw -B -ntp -Pci clean verify`，不跳过测试、覆盖率及静态检查。按实际 274 项基线的有效场景保留情况核对，不误计旧报告；Mockito attach 受限按当前权限处理，不改 MockMaker/javaagent。
- 另用原生 Maven 在 reactor 核对 `dependency:tree '-Dincludes=com.alibaba.cola:*'`，并检查 exec JAR 无 COLA 组件。不得仅依据 dependencyManagement 删除就宣称传递依赖为零。纯单元方法直接调用不证明框架条件注册和过滤器实际顺序。

### T4：运行验收与说明同步

**文档文件**：

- Modify：`README.md`、`AGENTS.md`、`ARCHITECTURE.md` — 仅同步运行栈/“COLA 分层理念”的当前事实与必要入口，不改分层或文档治理制度。
- Modify：`docs/engineering/development.md` — 增加运行 profile、具体 Origin 配置及日志边界的使用说明，链接本设计避免多处复制策略。
- Modify：`docs/active/1.3.0/release.md`、`docs/active/tech-debt-tracker.md` — 登记实际完成范围、本地命令及运行证据；未验收不关闭 TD-014。
- Check/Modify（仅相关描述需要同步）：`docs/DOMAINS.md`、`docs/design-docs/arch-api-contract.md` — 保留客户行为和既有追踪边界，不虚构终态 access traceId 保证。

**获准专用环境后人工执行**：

1. 配置已满足 data-consistency V1/V2 前置的专用 MySQL，记录实际 active profiles、JAR、依赖和必要配置来源，不输出凭据。检查用户已有 local 文件有无显式覆盖，本项不自动覆盖它。完整应用/数据库操作需要已有目标环境授权；没有环境时先交付本地实现并列待验收。
2. 分别冷启动 local/dev/test/prod（可顺序共用同一专用实例）；核对 Bean 覆盖 false、启动成功、无 COLA 组件、实际 Mimir Handler/审计/事务装配。通过受控临时启动配置制造同名 Bean 冲突，确认启动失败；夹具和日志放临时目录，不增加项目集成测试或生产故障开关。
3. 核对启动使用 container.xml、运行 appender 没有文件写入；合成应用日志/异常的凭据被遮蔽，prod root/app=WARN 但 access.log INFO 可见。不得仅在脱敏 converter 单元通过后声明启动期日志也已验证。
4. 用客户合法请求、400 校验和 HTTP200业务错误采集头、R.traceId 与实际状态。无已保存 traceId 的新请求以合法头 runtime-42 验证一致；缺头且无合法属性/MDC 时生成新 ID，缺头但已有合法 MDC 时复用；显式非法头在无合法请求属性时生成而非复用 MDC，已有合法请求属性时先复用属性。属性/MDC 预置分支可复用隔离测试，真实 HTTP 只报告实际可构造且观测到的路径。用调试器在已有请求处理线程内触发一次合成日志，验证处理期 MDC 输出；不新增生产演示端点或日志。终态 access 只验状态/路径/耗时/条数，单独记录其 traceId 实际有无，不将缺失标成客户响应追踪失败。
5. 直接对目标服务做预检与实际跨域请求，核对允许/拒绝、非 `/api/**` 路径、暴露头及 test/prod 关闭。使用浏览器手工请求读取 X-Trace-Id，区分“HTTP expose头存在”与“浏览器实际可读”；无需修改 Asgard/Bifrost，禁止以代理后的同源请求冒充跨域验收。拒绝/预检可能没有 R/traceId。
6. 记录访问日志、Web Trace、CORS 的实际注册与顺序；保留 query/body/认证头未采集及 URI/UA/IP 仍可包含敏感数据的边界。运行验收可与 data-consistency T5 同环境安排，但两个需求的证据和关闭条件分别判断。

**完成判断**：本地门禁、源码/JAR、文档链接/锚点和 `git diff --check` 分别记录；真实运行、远端 CI 与消费者验收单列。纯文档阶段不跑 Java 全量构建。TD-014 至少需本项本地证据及实际启动/日志/CORS/追踪验收才关闭，不因本方案就绪关闭其他技术债。

## 决策边界

私有测试 helper、配置布局中的等价组织及必要编译依赖声明可按项目习惯处理。改变默认 Origin/范围、启用 prod CORS、改 JSON/文件日志、增加日志内容或追踪依赖、修改 Mimir、改变客户协议/数据库/认证、放宽门禁或新增集成测试均需重新对齐。方案及审查只交付实施输入；实施、提交、推送和目标环境操作按会话已有授权判断。
