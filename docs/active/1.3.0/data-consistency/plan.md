# 客户数据一致性实施计划

**归属版本**：1.3.0。输入：[需求约定](spec.md)、[设计](design.md)、[公共 API 契约](../../../design-docs/arch-api-contract.md)、[版本记录](../release.md)。关联 [TD-005、TD-012](../../tech-debt-tracker.md)；基线 `56a8716`。本文为静态实施输入，不表示已授权实施、提交或操作数据库。

## 目标与关键约束

新增 V2 审计/版本迁移，客户创建/详情/列表返回 version，更新必须带 version；以条件更新及影响行数防止假成功和覆盖；复用 Mimir AuditorProvider、PageConverters、PageResult.map 消除重复总数查询。

- version 采用 Integer/INT：创建为 0，更新输入 0..2147483646；每次成功递增 1，同值更新也递增。版本耗尽不清零。
- 版本/空更新防线 → 查客户 → 比较版本 → 合并及既有校验 → 条件写入；0 行后同事务当前读，缺失 20001、存在 21002，不用普通快照读分类。1 行成功才回填新版本，不重试失败 DO。
- version 不是可更新业务字段；仅 version 的请求仍为空更新。HTTP 校验 400、直调无效输入 BizException 00004；成功和业务错误的 R/HTTP 约定不变。
- V1 不改；旧时间列改为 create_time/update_time 且保留原值，历史主体 system、version=0。审计字段仅在 DO，创建信息禁止更新，严格填充使用新 DO/null 待填值。
- 分页只调用一次 selectPage，仍可能有 count+records 两条 SQL；保留参数边界/过滤/排序/元数据，不承诺并发下总数与记录的快照一致性。
- 本项不改认证、删除并发语义、COLA 清理、日志/CORS、依赖版本、发布配置或其他项目；不新增集成测试或长期专项脚本。

## 任务依赖与文件归属

T1 → T2 → T3 → T4 → T5 串行；模型、MapStruct 和跨层测试共享语义，不安排并行写入。只读 API 核对或独立审查可并行，不能同时运行改写相同 target 的 Maven 生命周期。

### T1：版本模型、迁移与审计

**文件**（路径均相对仓库根）：

- Create：`start/src/main/resources/db/migration/V2__customer_audit_and_version.sql` — 单条 ALTER，改时间列名、增加审计主体与 version；V1 内容/文件名不动。
- Modify：`domain/src/main/java/io/yggdrasil/labs/midgard/domain/customer/model/Customer.java` — Integer version、创建初始化 0、Serializable/serialVersionUID；不加入框架注解或审计字段。
- Modify：`infrastructure/src/main/java/io/yggdrasil/labs/midgard/infrastructure/persistence/dataobject/CustomerDO.java` — version/@Version、显式审计列及填充/更新策略，替换 createdAt/updatedAt。
- Modify：`infrastructure/src/main/java/io/yggdrasil/labs/midgard/infrastructure/persistence/convertor/CustomerInfraConvertor.java` — 双向 version、明确审计字段不从 Domain 复制。
- Create：`infrastructure/src/main/java/io/yggdrasil/labs/midgard/infrastructure/config/CustomerAuditConfiguration.java` — 返回 system 的 AuditorProvider 示例，说明认证替换点。
- Modify：现有 `CustomerTest`、`CustomerInfraConvertorTest`；Create：Infrastructure 同模块的 `CustomerAuditConfigurationTest` — 直接构造配置及真实 Mimir 审计 Handler 的纯单元验证。

**验收**：

- 领域创建 version=0；转换往返保留版本；版本缺失不能被更新路径静默当 0。生成 Mapper 不手改。
- 对 DO 构造 MyBatis 表元数据后调用真实 Handler，证明插入填充四字段、更新只填 updateBy/updateTime，创建字段更新策略为 NEVER；不以 mock Handler 证明填充能力。
- 迁移源仅 Start，SQL 无 DROP/TRUNCATE/修改 V1，保留默认时间、自增、原约束与索引；静态检查不记为数据库迁移通过。
- 核实正式 Mimir API、有效 MyBatis 依赖与项目覆盖配置；不存在 MybatisBaseDO 时不自造该依赖。

### T2：仓储条件更新与单次分页

**文件**：

- Modify：`domain/src/main/java/io/yggdrasil/labs/midgard/domain/customer/repo/CustomerRepository.java` — findPage(PageRequest,String,String): PageResult<Customer> 替换 findAll/count，保留其他方法签名并补版本语义。
- Modify：`infrastructure/src/main/java/io/yggdrasil/labs/midgard/infrastructure/persistence/impl/CustomerRepositoryImpl.java` — insert/update 行数检查、成功版本回填、0 行当前读分类、一次 selectPage 及框架转换；保留 EmailUniqueConstraint 定向翻译。
- Modify：`infrastructure/src/test/java/io/yggdrasil/labs/midgard/infrastructure/persistence/impl/CustomerRepositoryImplTest.java` — 覆盖 Mapper 行数、异常、分页参数和无额外 count；允许同包增加必要聚焦单元测试。

**验收**：

- update=1 才回填递增后版本；0 行分存在/不存在为 21002/20001，失败不回填；异常行数和未知数据库异常保持系统失败；insert=0 不能返回创建成功。
- Repository.update 自身拒绝 null/负数/超过 2147483646 的版本，抛 PARAM_INVALID 且无 Mapper 写入，防止直接调用绕过 App 后因 null 版本跳过乐观锁。
- 锁查询使用同事务连接、固定 ID 条件与固定 FOR UPDATE；静态/单元核对 wrapper，不声称 mock 证明 RR/current-read 行为。核实 DML 清缓存及锁查询不命中旧缓存，真实验证留 T5。
- 保留 save/update 邮箱唯一键的预期/非预期索引及异常链回归；死锁/连接异常不能翻译为 21002。
- findPage 经 PageConverters.toMybatisPage → 一次 selectPage → records 转领域 → PageConverters.toPageResult；Customer 的 Serializable 满足泛型，不强转绕过。
- 23 条第 2 页、空第 1 页、越界空页、超过 Integer.MAX_VALUE 的 Long 总数与纠正后参数均保持元数据；过滤、降序与 overflow=false 不变；verify 无额外 selectCount（邮箱检查不在列表路径）。

### T3：App 与 HTTP 版本契约

**文件**：

- Modify：`app/src/main/java/io/yggdrasil/labs/midgard/app/customer/dto/cmd/UpdateCustomerCmd.java`、`adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/customer/dto/UpdateCustomerRequest.java` — version 的必填/非负/递增上界校验。
- Create：`adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/customer/serialization/CustomerVersionDeserializer.java` — 仅 version 使用的严格 JSON 整数反序列化，拒绝小数、数字字符串、布尔值和超 Integer 范围；null 交 NotNull。
- Modify：`app/src/main/java/io/yggdrasil/labs/midgard/app/customer/dto/co/CustomerCO.java`、`app/src/main/java/io/yggdrasil/labs/midgard/app/customer/assembler/CustomerAssembler.java`、`adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/customer/convertor/CustomerWebConvertor.java` — 请求/响应 version 映射；字段名一致时验证 MapStruct 实际产物，不添加无用途映射。
- Modify：`app/src/main/java/io/yggdrasil/labs/midgard/app/customer/application/CustomerAppServiceImpl.java` — 显式直调防线、版本比较顺序、使用仓储返回的成功版本；list 改 findPage().map，保留页码 int 上界。
- Modify：App 的 `CustomerAppServiceImplTest`、`CustomerPaginationTest`，Adapter 的 `CustomerControllerTest`、`CustomerHttpContractTest`、`CustomerResponseSerializationTest`、`CustomerUpdateValidatorTest` 及当前 `StubCustomerAppService`/共享 fixture；仅适配必要样例，不改其他项目。
- Check/Modify（仅必要时）：`CustomerUpdateValidator.java` — version 不参与“至少一个业务字段”判断，保留 OBJECT/$；类级规则本身正确时不机械修改。

**验收**：

- HTTP/直接调用覆盖 null、缺省、-1、0、2147483646、2147483647、溢出及非整数字符串；JSON 小数不得被 Jackson 默认截断成有效 version，需聚焦验证并在该 DTO 局部严格拒绝，不改变全局数值反序列化策略。
- 只有 version、version+全 null 为 400+OBJECT/$；version+phone="" 可清空；沿用邮箱 100/101、电话 20/21、姓名、非法输入无写入等既有意图。
- 当前版本更新成功、新版本输出；旧版本在修改字段/仓储写前失败；不存在、旧版本且邮箱已占用的判定优先级按设计；0 行由仓储分类。
- create/get/list/update 的受管 JSON 都包含整数 version，仍为六字段 R 信封；delete/20001/21001/21002 等错误信封不增加客户数据。
- 列表 App 只调用一次 findPage，用 PageResult.map 保留元数据；不遗留 findAll/count 调用。

### T4：完整本地验证与文档同步

**文件**：

- Modify：`docs/design-docs/arch-api-contract.md` — 将本项的目标 version 字段/必填/范围/错误优先级写清，分开当前实现与计划/验收边界，不重写先前 error-contract-r 的历史结果。
- Modify：`docs/DOMAINS.md` — 仅实施后同步实际版本、迁移、分页和审计落点。
- Modify：`docs/active/1.3.0/release.md`、`docs/active/tech-debt-tracker.md` — 登记本项实现、本地证据与人工待验收；TD-005/TD-012 未满足运行条件不关闭。
- Modify（必要时）：`docs/engineering/development.md`、README 示例 — 只同步升级停写/迁移与 version 使用说明，优先链接设计，避免重复规则。

**验收**：

- 先用聚焦回归体现旧实现的假成功/版本缺失/双查询，再验证修改；全量采用 Wrapper 与 Java 17：`./mvnw -B -ntp -Pprecheck verify`、`./mvnw -B -ntp -Pci verify`。不得跳过测试、覆盖率、PMD/CPD，不引入数据库/容器前置。
- 保留现有有效业务场景；四业务模块覆盖率实际检查通过，生产/测试 PMD/CPD 零违规。报告按本次实际执行套件计数，避免 target 残留旧 XML 误计。
- 若 Mockito self-attach 再受环境限制，先报告具体限制并按实际权限处理，不改 MockMaker/全局环境来换取通过。远端 CI 需后续获准推送后另验收。
- 文档链接/锚点、索引、实际文件和 `git diff --check` 通过；无需为纯文档修订运行 Java 构建。

### T5：专用环境人工迁移、并发与消费验收

**归属**：不新增项目集成测试或长期脚本。专用目标数据库/应用启动与数据修改只在取得明确环境授权后执行；本次写方案不执行。一次性 SQL、日志、HTTP 采集放临时目录，结论归 release.md；未取得环境时列为待验收。

1. **两套迁移基线**：一套空库执行 V1+V2；一套 V1 库预置合法客户（含 null/空电话、两个不同历史时间）。保存行数、ID/字段/时间、SHOW CREATE TABLE、索引及 Flyway history。迁移前按设计清单逐项比较实际结构与 V1 预期 DDL，任何偏差均停止，不能以 Flyway validate 代替结构检查。停写执行 V2 后 validate，再逐项比对：历史值不变、version=0、主体 system、原索引/默认值保留，重启不重跑迁移。失败预演仅在可丢弃库模拟权限/结构不符，须包含“手工删除 idx_email、但 V2 ALTER 仍可执行”的漂移样例并验证前置检查拒绝；记录失败与恢复路径，禁止对业务库 clean/repair。
2. **实际装配与审计**：启动新版应用，确认实际 AuditorProvider/MetaObjectHandler/乐观锁插件；真实 POST/GET/list/PUT 输出正确 version，创建与更新主体 system，成功更新保留创建信息，失败请求全部列不变。用 HTTP 证明 version 的非法小数/缺省/上界、空更新仍按契约拒绝。
3. **旧版本冲突**：两个客户端读取 version=0，A 更新成功为 1，B 以 0 更新得到 21002；记录 HTTP 和数据库最终行，确认 A 的值保留。补一次同值更新，版本也递增。
4. **受控写入交错**：在专用环境用调试器暂停两个真实更新请求于“App 完成读取、Mapper 写入之前”，不增加生产睡眠/测试钩子。两者读同版本，先放行 A 并提交，再放行 B，断言恰一成功、另一 21002；保存 SQL 的版本条件和影响行数。另一次仅暂停 A，用第二连接删除并提交，再放行 A，断言 20001。必须记录隔离级别 REPEATABLE READ、事务提交顺序、零行后的 FOR UPDATE 当前读及同连接证据；普通顺序请求或 Mapper mock 不替代本项。
5. **分页与数据库错误边界**：采集实际列表 SQL，证明只有分页插件的 count/records、无第三次独立 count；空/越界页不强求固定 SQL 数。采集真实 idx_email 冲突形状仍映射 21001；在可丢弃环境确认锁等待/连接失败安全返回系统错误而非版本冲突，不暴露 SQL。
6. **交付判断**：分别记录本地门禁、数据库迁移、真实并发、完整应用、实际 HTTP、远端 CI 和 Asgard/E2E。后两项目不属于本次修改，真实 HTTP 不等于前端消费通过。仅取得 TD 对应证据后按原关闭条件关闭，不以“方案完成”或“测试通过”代替。

## 决策边界

实现者可在上述契约内选择私有方法、单元夹具与局部反序列化器内部写法。改变 version 类型/范围/必填性、错误优先级、锁/隔离策略、迁移恢复方式、引入新的公开 Repository 兼容层、扩大其他项目或新增外部 I/O 测试时先重新对齐。方案评审不产生实施、Git 写入、发布或目标数据库操作授权。
