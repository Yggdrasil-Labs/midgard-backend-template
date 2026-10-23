---
updated: 2026-10-23
---

# Tech Debt Tracker

本文件记录已核实的工程欠账、待验证风险及关闭依据。保留原有 ID，按当前事实更新状态；登记不代表已经立项或修复。

## 本轮基线与证据边界

- 复核基线：`c7043d2`，Java 17、Mimir Parent/BOM 2.3.0；覆盖当前后端源码、构建发布配置、文档与测试源码，跨项目仅核对 Asgard 错误消费契约及 Bifrost 的 Customer 样例。
- 本轮实际检查：源码与配置对照、默认/ci/precheck 有效 POM 核对、DDL 副本比对、文档路径与引用检查。没有执行 HTTP、数据库并发、Docker 构建或真实发布验证，也没有查询本轮远端 CI 结果。
- 既有验证：本次会话此前在 WSL / Java 17.0.19 / Maven 3.9.16 执行 `./mvnw -B -ntp clean verify -DskipITs` 成功，29 项单测及架构测试通过。容器集成测试未执行；日志明确跳过 JaCoCo 与 Spotless check，因此该结果不能证明覆盖率或格式门禁通过。
- 2026-09-22 在 `83423ca` 上登记的 TD-005～TD-010 已逐项重核；旧环境运行结果只作为历史证据。此前提出的进一步依赖升级未被采纳，本轮不将其列入实施队列。

## 状态总览

当前 **10 项开放（6 项 P1、4 项 P2）、1 项待验证、2 项关闭**。没有证据充分的 P0 事项。

- **P1**：存在明确功能缺口、数据一致性或构建发布风险，优先处理。
- **P2**：主要增加维护成本或削弱防回归保障，可排在 P1 之后。
- **待验证**：静态风险明确，但实际行为仍缺运行证据，不按已复现缺陷计数。

| ID | 优先级 | 状态 | 主题 | 处理边界 |
| --- | --- | --- | --- | --- |
| TD-006 | P1 | 开放 | 质量门禁入口与签名职责混杂 | 后端构建 / Mimir Parent |
| TD-007 | P1 | 开放 | 构建凭据写入 builder 层及缓存风险 | Dockerfile / build.sh / Release |
| TD-008 | P1 | 开放 | 旧版本重发可能倒退 main 版本及 latest 镜像 | Release |
| TD-005 | P1 | 开放 | 并发更新可能假成功或覆盖其他修改 | App / Repository / 数据库 |
| TD-009 | P1 | 开放 | 前后端错误响应契约不一致 | Midgard / Asgard 协同 |
| TD-010 | P1 | 开放 | 输入长度与数据库约束不一致 | 请求 / 应用与领域入口 / 持久化 |
| TD-001 | P2 | 开放 | 文档事实漂移且缺少一致性检查 | 文档 / CI |
| TD-002 | P2 | 开放 | 数据库结构参考文档缺失 | 数据源 / 生成流程 |
| TD-012 | P2 | 开放 | 数据库迁移脚本有两份手工副本 | 数据库迁移 / 构建 |
| TD-013 | P2 | 开放 | 架构测试未覆盖部分已声明边界 | ArchitectureTest |
| TD-011 | 待定 | 待验证 | 两个全局异常 Advice 的职责与顺序未核验 | Adapter / Mimir Exception Starter |

## 优先处理项

### TD-006：质量门禁入口与签名职责混杂

**证据与现状**：

- [根 POM](../../pom.xml) 的 `ci` profile 仍设置 `skipTests=true`、`maven.test.skip=true`；当前 Mimir Parent **2.3.0** 在该 profile 的 `verify` 阶段仍绑定 `gpg:sign`，有效配置为 `skip=false`。
- [CI](../../.github/workflows/ci.yml) 分为构建和测试两个 job，Test 确实执行默认 `verify`，不能表述为“整个 CI 不运行测试”。普通 PR 另有 `validate -Pci` 格式检查；Dependabot 分支跳过该步骤。
- 默认 dev 有效模型跳过 JaCoCo 和 Spotless check；根 POM 又在 `process-sources` 执行 Spotless apply，所以默认校验会自动格式化源码，而不是以格式错误阻断构建。
- **已有 precheck 候选入口**：有效模型开启测试与 JaCoCo，执行插件列表中无 GPG，但仍包含自动格式化；CI 未采用，尚未验证失败用例能否被门禁拒绝。无需为此预设新建一套 profile。

| 入口 | 测试配置 | 覆盖率配置 | 格式行为 | 签名执行 |
| --- | --- | --- | --- | --- |
| 默认 dev | 开启 | 跳过 | 自动 apply，check 跳过 | 无 |
| ci | 本项目显式跳过 | 开启 | validate 有 check，后续仍有 apply | verify 强制 sign |
| precheck | 开启 | 开启 | 仍有 apply；check 实际效果待验 | 无 |

**下一步与关闭条件**：优先评估并收敛现有 precheck，使 CI 采用无签名、只检查源码的质量入口；在具备 Docker 的干净环境中验证单测、集成测试、覆盖率和格式门禁均实际执行，工作区不变。故意引入失败测试、覆盖率不足和格式错误时均应返回非零。

**证据边界**：本轮执行 `help:effective-pom` 核对 ci/precheck，未重新执行签名；旧记录中 2.2.1 环境的 `No pinentry` 及 `-Dgpg.skip=true` 无效属于历史证据。`66a6bf3` 已处理空 client 模块 Javadoc 打包失败，该修复不能关闭本条质量门禁债务。

### TD-007：构建凭据写入 builder 层及缓存风险

**证据与影响**：[Dockerfile](../../Dockerfile) 通过 `ARG GITHUB_TOKEN` 将认证信息写入普通构建层的 `/root/.m2/settings.xml`；[build.sh](../../build.sh) 与 [Release](../../.github/workflows/release.yml) 均沿用 build arg，Release 还导出 `cache-to: type=gha,mode=max`。认证材料可能随 builder 层和缓存留存。

**下一步与关闭条件**：用 BuildKit secret mount 临时提供 Maven 认证配置，同步调整脚本和工作流。用专用测试标记验证日志、构建层、导出缓存及最终镜像均不包含标记，且依赖解析成功。

**证据边界**：本轮仅核对源码，未证实实际泄露；最终运行阶段只复制 JAR，不能推断最终运行镜像必然包含 Token。

### TD-008：旧版本重发可能倒退 main 版本及 latest 镜像

**证据与影响**：[Release](../../.github/workflows/release.yml) 支持手动指定旧 tag，随后仍根据该 tag 的 patch + 1 覆盖 main 的 `revision`，没有比较当前开发版本；Docker metadata 还无条件生成 `latest`，且缺少共享版本状态的并发协调。

例如 main 已为 `1.4.0-SNAPSHOT`，重发 `1.2.0` 会尝试写入 `1.2.1-SNAPSHOT`，并可能将 `latest` 指向旧镜像。实际执行还受权限及分支保护影响，本轮未触发发布。

**下一步与关闭条件**：隔离旧版重发与当前版本推进，对 main 及 latest 增加版本单调性、条件更新和并发协调。验证最新发布、旧 tag 重发、重复执行及并行发布；旧版重发不改 main/latest，重复执行不产生多余版本提交。

### TD-005：并发更新可能假成功或覆盖其他修改

**证据与影响**：[CustomerAppServiceImpl](../../app/src/main/java/io/yggdrasil/labs/midgard/app/customer/application/CustomerAppServiceImpl.java) 先读取实体，再修改字段并返回成功对象；[CustomerRepositoryImpl](../../infrastructure/src/main/java/io/yggdrasil/labs/midgard/infrastructure/persistence/impl/CustomerRepositoryImpl.java) 将完整实体转 DO 后调用 `updateById`，忽略影响行数，[CustomerDO](../../infrastructure/src/main/java/io/yggdrasil/labs/midgard/infrastructure/persistence/dataobject/CustomerDO.java) 无版本字段。

读取后被其他事务删除时，更新 0 行仍可能返回成功；两个请求基于旧实体修改不同字段时，也可能覆盖另一请求的修改。已有单测和集成测试未覆盖这些并发场景。

**下一步与关闭条件**：明确并发写入语义，检查影响行数，采用版本条件更新等策略。用真实 MySQL 和事务屏障稳定覆盖“读后被删”及两个并发更新，证明无假成功、无静默覆盖，同时保留正常更新行为。

**证据边界**：本轮为源码核实，未执行数据库并发复现；不额外把可成立的幂等删除语义登记为缺陷。

### TD-009：前后端错误响应契约不一致

**证据与影响**：[ErrorResponse](../../adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/dto/ErrorResponse.java) 和本地 [GlobalExceptionHandler](../../adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/GlobalExceptionHandler.java) 使用 `code/message`；[Asgard 拦截器](../../../asgard-frontend-template/src/utils/api/request.ts) 的非 2xx 分支读取 `errCode/errMessage`。后端格式的 400/409 响应进入该分支时，业务码及具体字段说明会丢失。

[CustomerApiIT](../../start/src/test/java/io/yggdrasil/labs/midgard/start/customer/CustomerApiIT.java) 断言 `$.code`，不能证明前端能正确消费。与 [Asgard 技术债清单](../../../asgard-frontend-template/docs/active/tech-debt-tracker.md) 第 10 项对应同一跨项目问题。

**下一步与关闭条件**：两端统一错误信封或集中适配；用后端真实序列化结果驱动前端消费测试，覆盖 400、404、409、500，保留业务码和安全说明，并完成至少一次真实 HTTP 链路验证。与 TD-011 一起核对实际启用的异常处理器，避免只验证其中一种响应来源。

**证据边界**：旧记录报告过前端受控执行复现；本轮仅重新核对两端源码，没有重跑该探针或真实 HTTP 请求。

### TD-010：输入长度与数据库约束不一致

**证据与影响**：[创建请求](../../adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/customer/dto/CreateCustomerRequest.java)、[更新请求](../../adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/customer/dto/UpdateCustomerRequest.java) 和 [Customer.validate()](../../domain/src/main/java/io/yggdrasil/labs/midgard/domain/customer/model/Customer.java) 均未限制邮箱、电话长度；[迁移 DDL](../../start/src/main/resources/db/migration/V1__create_customer.sql) 的上限分别为 100 和 20。

例如 21 字符电话能够通过当前边界校验，进入持久化；数据库拒绝时可能落入通用数据冲突处理，而非明确字段错误。数据库实际 SQL mode 及响应尚未运行验证。本轮将原 P2 调整为 **P1**，依据是已存在输入错误路径的功能缺口，而非单纯维护成本。

**下一步与关闭条件**：补齐创建/更新的长度校验，明确 App 直调及领域入口的保护范围。覆盖 email 100/101、phone 20/21 及电话缺省；超长输入返回 400 和明确字段说明，拒绝后数据库不变。

## 维护与防回归项

### TD-001：文档事实漂移且缺少一致性检查

**证据与影响**：[DOMAINS](../DOMAINS.md) 将 client 契约 DTO、App CmdExe/QryExe 标为完成，但 [client 源码](../../client/src/main/java) 只有 package-info，Customer 应用实现集中在 AppService；[质量评分卡](../QUALITY_SCORE.md) 仍称 db-schema 是占位文件，实际该文件不存在。[现有 CI](../../.github/workflows/ci.yml) 也没有文档索引、路径及状态检查。

**下一步与关闭条件**：先对齐实现状态和生成文档登记，再增加最小链接/索引检查；语义状态需结合源码人工复核，不能仅依赖 Markdown lint。所有当前文档中的“已实现”应能定位到实际代码，新增失效链接应被检查拒绝。

### TD-002：数据库结构参考文档缺失

**证据与影响**：[生成文档注册表](../generated/index.md) 登记了 `db-schema.md`，但 `docs/generated/db-schema.md` 实际不存在；Customer 迁移和 DO 已存在。旧“人工模板、等待脚本稳定”的描述已不准确。

**下一步与关闭条件**：在 TD-012 明确迁移来源后，由实际生效的迁移生成表、字段、约束及索引说明，记录生成命令并可重复生成。不得手工补一个长期漂移的 schema 副本；生成结果应与迁移一致。

### TD-012：数据库迁移脚本有两份手工副本

**证据与影响**：[仓库根目录迁移](../../db/migration/V1__create_customer.sql) 与 [启动模块迁移](../../start/src/main/resources/db/migration/V1__create_customer.sql) 当前内容完全相同；[应用配置](../../start/src/main/resources/application.yml) 只从 `classpath:db/migration` 加载，[db/README](../../db/README.md) 又引导维护根目录脚本。未发现两份脚本的同步或一致性检查，后续仅修改其中一处可能导致文档与实际建库行为分叉。

**下一步与关闭条件**：确定唯一维护位置，另一处删除、改为引用或由构建生成。通过一次迁移新增验证：开发、打包和测试使用相同来源，不依赖手工复制；保留已发布迁移的校验和，不重写执行过的 V1。

### TD-013：架构测试未覆盖部分已声明边界

**证据与影响**：[ARCHITECTURE](../../ARCHITECTURE.md) 明确禁止 Domain 依赖 client 或数据库映射；[ArchitectureTest](../../start/src/test/java/io/yggdrasil/labs/midgard/start/ArchitectureTest.java) 仅定义 Start、Adapter、App、Domain、Infrastructure 五层，未将 Client 作为被约束目标，也没有校验 Domain 的持久化注解或框架依赖。因此现有层间测试不能完整执行这些约束。

**下一步与关闭条件**：为已声明但未覆盖的边界增加针对性规则；用最小违规夹具分别证明 Domain→Client 和持久化映射注解能被拒绝，同时当前合法代码仍通过。此项描述的是防回归缺口，不代表当前 Domain 已存在违规。

## 待验证项

### TD-011：两个全局异常 Advice 的职责与顺序未核验

**已核实事实**：本地 [GlobalExceptionHandler](../../adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/GlobalExceptionHandler.java) 无显式 order，且 `Exception` 兜底返回 500，未专门处理请求体解析错误。发布构件 `mimir-boot-starter-exception:2.3.0` 的源码中，`ExceptionAutoConfiguration` 缺省启用，只有存在 **MimirExceptionHandler 类型** Bean 才退让；其 `MimirExceptionHandler` 同为最低优先级，并显式将 `HttpMessageNotReadableException` 映射为 400。本地不同类型的 Advice 不会让该条件自动退让。

**未知与影响**：当前完整应用实际注册顺序尚未验证。若本地 Advice 先匹配，空请求体或损坏 JSON 可能被兜底为 500；若 Mimir Advice 先匹配，则可返回 400。不能将“坏 JSON 必然返回 500”登记为已确认缺陷；同样需核对实际错误信封是否符合 TD-009 的统一契约。

**下一步与判定标准**：在完整 Spring 上下文中记录 Advice 顺序与实际异常处理方法，验证空 body、损坏 JSON、字段类型错误、405/415 和未知服务端异常。按结果明确两个 Advice 的职责与选择顺序，或关闭此项；确认客户端错误被误报为 500 后再定为 P1。单独装配本地 Advice 的 MockMvc 测试不能代替实际自动配置下的验证。

## 已关闭项

| ID | 关闭日期 | 主题 | 关闭依据与边界 |
| --- | --- | --- | --- |
| TD-003 | 2026-10-23 | 模板业务示例不足 | Customer 已具备 Domain/App/Infrastructure/Adapter 完整链路，[CustomerApiIT](../../start/src/test/java/io/yggdrasil/labs/midgard/start/customer/CustomerApiIT.java) 有 CRUD 生命周期样例，Bifrost 也有 Customer CRUD E2E 源码。仅关闭“缺少样例”，不代表容器集成或 E2E 本轮运行通过。 |
| TD-004 | 2026-06-07 | 前端相关文档仍为占位 | 沿用原关闭日期；本轮确认 FRONTEND.md 已不存在。 |

## 建议处理顺序

1. **先建立可信验证入口：TD-006。** 评估现有 precheck，明确单测、容器集成、覆盖率、格式和签名边界。
2. **发布侧可独立推进 TD-007、TD-008。** 真实发布前验收凭据留存及版本单调性，不能用本地编译结果替代。
3. **接口侧先核查 TD-011，并协同处理 TD-009、TD-010。** 异常处理器选择会影响错误契约的运行验证。
4. **处理 TD-005。** 先约定并发写入语义，再用真实数据库稳定验证。
5. **后续维护 TD-012 → TD-002，以及 TD-001、TD-013。** 数据源确定后再生成结构文档，避免继续维护重复事实。

## 复核与关闭规则

- 每次关闭保留日期、代码或验证证据；“配置已声明”“测试已存在”“命令已执行”和“实际通过”分开记录。
- 修复进入执行计划后再挂对应链接；当前条目均未因此自动立项。
- 源码静态结论不写成线上事故、实际泄露或运行复现；未知项必须有下一步核验方式。
