# 客户 R 契约统一实施计划

**归属版本**：1.3.0

**输入**：[需求约定](spec.md)、[设计](design.md)、[跨项目 API 契约](../../../design-docs/arch-api-contract.md)、[版本记录](../release.md)。关联 TD-009、TD-010、TD-011；TD-005 的真实并发行为仍归 data-consistency。

## 目标与已确定边界

五个客户接口显式返回 R；请求校验与业务异常分别遵循 HTTP 400、HTTP 200 约定，去除本地 Advice/ErrorResponse；统一分页和结构化明细，补齐长度约束。设计中的 JSON 空值输出、分页纠正/整数边界、唯一键识别与 21002 分期均为实施依据，不重新选型。

本项不改 V1/表结构/version/影响行数、不做全量 COLA 依赖清理、单次分页优化、运行时日志/CORS、前端或 E2E。不新增集成测试，不启动 Docker；不因删除测试而宣称真实数据库或自动装配已验证。远端 CI 仍待后续获准推送时验收。

## 任务依赖

按 T1 → T2 → T3 → T4 串行实施。各任务可能共同调整同一层测试与 App 接口，不安排并行写入；若独立委派只读调查，不得同时运行会改写相同构建目录的 Maven 生命周期。

### T1：领域错误、直接调用防线和仓储冲突翻译

**文件**：

- Modify：`domain/pom.xml` — 显式依赖 mimir-boot-common；保留 COLA domain starter 等其余依赖至 runtime-adoption。
- Create：`domain/src/main/java/io/yggdrasil/labs/midgard/domain/customer/exception/CustomerErrorCode.java` — EMAIL_ALREADY_EXISTS=21001、VERSION_CONFLICT=21002，实现 ErrorDefinition。
- Modify：`domain/src/main/java/io/yggdrasil/labs/midgard/domain/customer/model/Customer.java` — 替换 COLA BizException，补邮箱/电话长度并保持现有姓名、邮箱格式语义。
- Modify：`app/src/main/java/io/yggdrasil/labs/midgard/app/customer/application/CustomerAppServiceImpl.java` 与 `dto/cmd/{CreateCustomerCmd,UpdateCustomerCmd}.java` — 使用 Mimir 错误定义、保留空更新及直调校验；同步长度注解。
- Modify：`infrastructure/src/main/java/io/yggdrasil/labs/midgard/infrastructure/persistence/impl/CustomerRepositoryImpl.java` — 对 save/update 做设计限定的邮箱唯一键异常翻译；允许在同包提取私有识别 helper，不改 Repository 接口或 SQL。
- Modify：现有 `CustomerTest`、`CustomerAppServiceImplTest`、`CustomerRepositoryImplTest`；Create：同模块 `CustomerErrorCodeTest` — 错误、边界与异常链回归。

**验收**：

- email 合法格式 100 接受、101 拒绝，phone null/空串/20 接受、21 拒绝；同样覆盖创建和更新路径；失败无仓储写入。姓名、未变邮箱和缺失客户原有业务场景保留并更新错误码。
- 单元构造 save/update 的真实类型异常链，预期 idx_email 重复转 21001 且保留 cause；其他索引、idx_email_backup、未知完整性/连接错误不误译。
- 21002 枚举与异常工厂可用，但生产调用路径不生成真实版本冲突；测试报告不能将该映射夹具记为乐观锁交付。
- 对受影响模块执行针对性单元测试；命令若使用 Reactor 过滤，不因其他模块没有匹配测试而关闭真正的失败结果验证。

### T2：请求边界、分页和显式成功响应

**文件**：

- Modify：`adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/customer/dto/{CreateCustomerRequest,UpdateCustomerRequest}.java` — 字段校验及 Serializable。
- Create：`adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/customer/validation/ValidCustomerUpdate.java`、`CustomerUpdateValidator.java` — 三字段全 null 的类级校验；不在 Controller 手写局部错误信封。
- Modify：`app/src/main/java/io/yggdrasil/labs/midgard/app/customer/dto/qry/ListCustomerQry.java` — Long pageIndex/pageSize、toPageRequest、页码 Max 边界；旧 page/size 不增加别名兼容。
- Modify：`app/src/main/java/io/yggdrasil/labs/midgard/app/customer/dto/co/CustomerCO.java` 及两个 Cmd — Serializable；保留业务字段与数字 ID。
- Modify：`app/src/main/java/io/yggdrasil/labs/midgard/app/customer/application/{CustomerAppService,CustomerAppServiceImpl}.java` — list 返回 PageResult，删除公开 count，内部保留 Repository 双查询；使用同一组纠正参数。
- Modify：`adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/customer/CustomerController.java` — 五个 R 返回类型，移除 POST/DELETE 状态注解及 COLA DTO import。
- Modify：`CustomerControllerTest`、`CustomerAppServiceImplTest`；Create：所属模块 `CustomerUpdateValidatorTest`、`ListCustomerQryTest` — 对应约定及边界。

**验收**：

- 在独立 Bean Validation 与 standalone MVC 中验证空更新为 OBJECT/$，无效字段为 FIELD/字段名；请求错误不进入 App 写用例；App 直调 PARAM_INVALID 与 HTTP 校验 400 的不同边界均明确断言。
- 五接口成功均为 HTTP 200、R；空删除 data 为 null；详情不存在为 200+20001；保持请求字段 null/空字符串语义。
- 分页覆盖缺省、null、0、负数、1000/1001、2147483647/2147483648 及非数字输入；23 条第 2 页/空集合元数据正确；仓储参数与返回元数据一致，Long totalCount 不截断，旧过滤/降序不改变。
- 所有受影响调用方与测试适配新的 list 签名，不遗留旧 page/size/getPage/getSize 或 App count 使用；不修改其他项目调用方。

### T3：仅保留 Mimir Handler 并落实受管 JSON

**文件**：

- Modify：`start/src/main/resources/application.yml` — 开启结构化校验及 write-nulls；不触碰其他运行时配置。
- Delete：`adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/GlobalExceptionHandler.java`、`adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/dto/ErrorResponse.java`、对应 `GlobalExceptionHandlerTest.java`。
- Create：`adapter/src/test/java/io/yggdrasil/labs/midgard/adapter/web/customer/CustomerHttpContractTest.java` — 设计所述 standalone Controller + Mimir Handler + 受管 Jackson 装配，替代被删 Handler 测试的有效行为覆盖。
- Create：同测试包 `CustomerResponseSerializationTest.java` — 实际六字段信封、成功/失败派生值、空值、分页及追踪公开组件验证；可在该测试包增加共享 fixture 与 JSON 资源，禁止复制生产 Handler 或私有框架工厂。

**验收**：

- 检查仓库仅使用 Mimir Handler，无本地 Advice、ErrorResponse、COLA 异常 import 或 COLA 信封返回。COLA 的依赖声明暂留后续任务，不把“无源码引用”当作已完成组件清理。
- 明确断言 `code,message,data,timestamp,traceId,success`，无 fail/errCode/errMessage；delete/业务错误 data 节点存在且为 null，空电话节点存在，timestamp 为数字。
- standalone 装配真实 Mimir Handler，覆盖 20001、21001、21002 映射、400+00004 字段与对象错误、损坏 JSON 400 和安全 500；本地 Handler 原有邮箱冲突等有效场景有相应单元替代。
- TraceInterceptor/ResponseBodyEnhancer 公开组件在已知 traceId 夹具下 header/body 一致、MDC 无泄漏；不宣称已测试自动配置注册顺序。对当前只有一个生产 Handler 的结论使用源码/依赖核对，实际 Spring 注册由按需人工运行确认。
- 供后续 Asgard 使用的 fixture 标为“受管序列化/standalone 输出”，不标真实运行响应；若人工采集，分别记录环境与来源。

### T4：完整门禁与范围收尾

**文件**：

- Modify：`docs/DOMAINS.md` — 仅同步实际响应/异常及分页落点。
- Modify：`docs/active/1.3.0/release.md`、`docs/active/tech-debt-tracker.md` — 记录本项交付、实际本地证据及 TD-009/010/011 各自关闭条件；未取得人工/跨项目证据时保留待验收。
- Modify（仅必要时）：`docs/design-docs/arch-api-contract.md`、README 使用示例 — 同步经验证的最终字段与参数，不改变本次已确认行为，不重复维护另一份契约。

**验收与证据**：

- 用项目 Maven Wrapper / Java 17 执行 `./mvnw -B -ntp -Pprecheck verify` 与 `./mvnw -B -ntp -Pci verify`，无 skipTests/skipITs、无集成测试/数据库/容器前置；测试失败、覆盖率及生产/测试 PMD/CPD 不能被关闭。需无旧构建产物证据时，在符合项目约定的干净副本验证，临时报告不进入仓库。
- 保留既有 57 项单元/架构测试的业务意图；删除旧 Handler 测试时说明替代用例，不机械要求类名或总数不变，也不能靠减少有效覆盖换取通过。四业务模块实际通过覆盖率阈值，四项静态报告零违规。
- 检查数据库迁移、version、MyBatis 更新行数/分页优化、其他项目和发布配置均未被顺带修改；对外兼容变化只限本项。
- 文档本地链接/锚点、索引、示例与差异检查通过；独立复审实际代码与测试后再报告本地完成。真实 MySQL、框架自动装配、前端消费和远端 CI 的状态单列，不混成已验证。

## 决策边界

普通实现与测试 fixture 组织可由实施者决定。若需要改变 HTTP 语义、信封字段/空值策略、分页范围、请求空值含义、引入框架外处理器、改数据库/版本、升级依赖或扩展到其他项目，先重新对齐。方案就绪不产生实施、提交或推送授权；本项完成也不表示 data-consistency、runtime-adoption 或前端对接已完成。
