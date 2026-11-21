---
updated: 2026-11-21
---

# Tech Debt Tracker

本文件记录已核实的工程欠账、待验证风险及关闭依据。保留原有 ID，按当前事实更新状态；登记不代表已经立项或修复。

## 2026-10-24 复核基线与证据边界

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
| TD-006 | P1 | 开放（本地验证通过，待 CI 验收） | 质量门禁入口与签名职责混杂 | 后端构建 / Mimir Parent |
| TD-007 | P1 | 开放 | 构建凭据写入 builder 层及缓存风险 | Dockerfile / build.sh / Release |
| TD-008 | P1 | 开放 | 旧版本重发可能倒退 main 版本及 latest 镜像 | Release |
| TD-005 | P1 | 开放 | 并发更新可能假成功或覆盖其他修改 | App / Repository / 数据库 |
| TD-009 | P1 | 开放（后端已实现，待跨项目验收） | 前后端错误响应契约不一致 | Midgard / Asgard 协同 |
| TD-010 | P1 | 开放 | 输入长度与数据库约束不一致 | 请求 / 应用与领域入口 / 持久化 |
| TD-001 | P2 | 开放 | 文档事实漂移且缺少一致性检查 | 文档 / CI |
| TD-002 | P2 | 开放 | 数据库结构参考文档缺失 | 数据源 / 生成流程 |
| TD-012 | P2 | 开放（副本已清理，待验收） | 数据库迁移脚本有两份手工副本 | 数据库迁移 / 构建 |
| TD-013 | P2 | 开放 | 架构测试未覆盖部分已声明边界 | ArchitectureTest |
| TD-011 | 待定 | 待验证 | 两个全局异常 Advice 的职责与顺序未核验 | Adapter / Mimir Exception Starter |

## 优先处理项

### TD-006：质量门禁入口与签名职责混杂

**原问题（Mimir Parent 2.3.0）**：根 POM 的 `ci` 显式跳过测试，且该模式有效模型仍要求 GPG 签名；默认/precheck 生命周期又自动执行 Spotless apply。原 CI 的独立 Test job 实际执行默认 `verify`，并非整个 CI 不运行测试，但未统一采用严格质量入口，Dependabot 的构建路径仅做编译。

**实施进展（2026-11-20）**：按 [1.3.0 升级门禁方案](1.3.0/mimir-upgrade-gates/plan.md)升级 Parent/BOM 至 2.4.0，移除测试跳过、自动 apply 及重复插件配置。CI 对所有 PR 使用同一 `-Pci clean verify`；PMD/CPD 显式启用违规阻断与分析错误阻断，签名默认跳过。补齐必要模块测试及存量规则违规，未降低阈值。

**升级初次本地证据**：7 模块的 dev/precheck/ci 有效模型核对通过；`-Pci verify -DskipITs` 在原工作区及无旧构建产物的临时普通副本通过，57 项单元/架构测试零失败、错误、跳过。四业务模块实际通过覆盖率阈值，生产/测试静态报告零违规；当时的 9 组独立故障夹具均由预期检查拒绝。结果集中在 [版本记录](1.3.0/release.md#最终验证摘要)。

**范围调整与关闭条件**：用户最新要求不再维护项目集成测试，原容器及失败 IT 夹具要求已取消。移除相关源码/依赖并取消 Failsafe 生命周期绑定后，复验无需 `-DskipITs` 的单元/架构及质量门禁，再于获准推送后核验远端 CI；本条保持开放。真实数据库与完整应用行为不属于单元测试证明范围，删除集成测试不自动关闭迁移或运行时相关债务。

**清理后本地复验**：`-Pci verify` 未传 `-DskipITs` 通过，57 项单元/架构测试全部执行；默认 verify 与依赖树检查通过，实际依赖无 Testcontainers，Failsafe 无生命周期执行。当前仅剩远端 CI 待验收，不再把已取消的集成测试列作关闭前置。

**历史证据边界**：2.2.1 环境的 `No pinentry` 及 `-Dgpg.skip=true` 无效属于历史结果；`66a6bf3` 处理空 client 模块 Javadoc 打包失败，不单独构成本条关闭依据。

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

读取后被其他事务删除时，更新 0 行仍可能返回成功；两个请求基于旧实体修改不同字段时，也可能覆盖另一请求的修改。现有单测未证明真实数据库的并发行为，后续按需人工运行验证，不新增集成测试。

**下一步与关闭条件**：明确并发写入语义，检查影响行数，采用版本条件更新等策略。用真实 MySQL 和事务屏障稳定覆盖“读后被删”及两个并发更新，证明无假成功、无静默覆盖，同时保留正常更新行为。

**证据边界**：本轮为源码核实，未执行数据库并发复现；不额外把可成立的幂等删除语义登记为缺陷。

### TD-009：前后端错误响应契约不一致

**原问题与影响**：历史本地 ErrorResponse/GlobalExceptionHandler 使用 code/message，而 [Asgard 拦截器](../../../asgard-frontend-template/src/utils/api/request.ts) 的非 2xx 分支读取 errCode/errMessage，业务码及字段说明可能丢失。

**后端进展（2026-11-21）**：[error-contract-r](1.3.0/error-contract-r/plan.md) 已将五接口统一为 Mimir R，移除本地 Advice/ErrorResponse，业务异常为 HTTP 200 + 业务码、MVC 校验为 HTTP 400；70 个 standalone/序列化/追踪场景及包含 Mockito 套件的完整本地门禁已通过（209 项测试）。前端未改、真实消费链路未验证，不能关闭本条。

历史 `CustomerApiIT` 曾断言 `$.code`，不能证明前端能正确消费；该测试已按用户要求移除。与 [Asgard 技术债清单](../../../asgard-frontend-template/docs/active/tech-debt-tracker.md) 第 10 项对应同一跨项目问题。

**下一步与关闭条件**：两端统一错误信封或集中适配；用后端真实序列化结果驱动前端消费测试，覆盖成功、HTTP 200 + 20001/21001、400 校验、404 协议及 500 系统错误，保留业务码和安全说明，并完成至少一次真实 HTTP 链路验证。与 TD-011 一起核对实际启用的异常处理器，避免只验证其中一种响应来源。

**证据边界**：本轮是后端隔离 HTTP/序列化测试，不是实际部署 HTTP 或 Asgard 消费验证；旧前端受控复现只作历史证据。

### TD-010：输入长度与数据库约束不一致

**原问题**：创建/更新请求与 Domain 曾未限制邮箱、电话长度，而[迁移 DDL](../../start/src/main/resources/db/migration/V1__create_customer.sql) 上限分别为 100 和 20。该输入错误路径缺口曾由 P2 调整为 P1。

**当前进展（2026-11-21）**：请求 DTO/Cmd/Domain 已补齐 50/100/20 约束；HTTP 非法字段和空更新在进入 App 前拒绝，直接调用仍有领域防线。Domain、请求校验及 App/Infrastructure Mockito 回归均已通过完整本地门禁；真实应用/数据库运行证据未补充，保持开放，不将隔离测试等同实际环境验收。

**下一步与关闭条件**：补齐创建/更新的长度校验，明确 App 直调及领域入口的保护范围。覆盖 email 100/101、phone 20/21 及电话缺省；超长输入返回 400 和明确字段说明，拒绝后数据库不变。

## 维护与防回归项

### TD-001：文档事实漂移且缺少一致性检查

**证据与进展**：此前 [DOMAINS](../DOMAINS.md) 将尚不存在的 client 契约 DTO、App CmdExe/QryExe 标为完成，旧评分卡也误称 db-schema 是占位文件。2026-10-24 的文件约束与项目纪律优化已纠正实现状态、淘汰陈旧评分卡，并补充文档总索引、文件归属和维护规则；当前规则由[文档治理](../standards/documentation-governance.md)及[开发指南](../engineering/development.md)维护。原记录中的持续自动检查缺口未在本轮处理；本轮未复核 CI，本条保持开放。

**下一步与关闭条件**：补齐最小链接/索引检查，持续核对文档状态；语义需结合源码人工复核，不能仅依赖 Markdown lint。所有当前文档中的“已实现”应能定位到实际代码，新增失效链接应被检查拒绝。本轮局部文档验证不等于已有持续自动检查。

### TD-002：数据库结构参考文档缺失

**证据与进展**：数据库结构参考文档仍不存在，Customer 迁移和 DO 已存在。原有虚列产物的空索引已移除，[文档治理](../standards/documentation-governance.md#生成文档)明确有实际产物后才登记；本轮尚未实现生成方式，本条保持开放。

**下一步与关闭条件**：在 TD-012 明确迁移来源后，由实际生效的迁移生成表、字段、约束及索引说明，记录生成命令并可重复生成。不得手工补一个长期漂移的 schema 副本；生成结果应与迁移一致。

### TD-012：数据库迁移脚本有两份手工副本

**原问题与进展**：根目录曾保留与 [Start 模块 V1](../../start/src/main/resources/db/migration/V1__create_customer.sql) 相同的手工副本，而实际 Flyway 配置只读取 `classpath:db/migration`，存在两处维护导致分叉的风险。2026-11-07 已按确认的方案删除根目录副本及旧说明，唯一迁移源保留在 Start 模块；维护规则统一进入[开发指南](../engineering/development.md#数据库迁移)，应用与测试的加载配置保持原样。

**验证与关闭边界**：清理前两份 V1 内容哈希一致，保留脚本的内容与文件名不变。本地构建、29 项单元及架构测试、文档引用检查通过；普通 JAR 和可执行 JAR 均只包含一份 V1，其内容与保留源文件一致。尚未执行真实数据库迁移新增验收，仍需确认实际迁移及 Flyway 校验行为；验收前保持开放。

### TD-013：架构测试未覆盖部分已声明边界

**证据与影响**：[ARCHITECTURE](../../ARCHITECTURE.md) 明确禁止 Domain 依赖 client 或数据库映射；[ArchitectureTest](../../start/src/test/java/io/yggdrasil/labs/midgard/start/ArchitectureTest.java) 仅定义 Start、Adapter、App、Domain、Infrastructure 五层，未将 Client 作为被约束目标，也没有校验 Domain 的持久化注解或框架依赖。因此现有层间测试不能完整执行这些约束。

**下一步与关闭条件**：为已声明但未覆盖的边界增加针对性规则；用最小违规夹具分别证明 Domain→Client 和持久化映射注解能被拒绝，同时当前合法代码仍通过。此项描述的是防回归缺口，不代表当前 Domain 已存在违规。

## 待验证项

### TD-011：两个全局异常 Advice 的职责与顺序未核验

**原问题**：本地 GlobalExceptionHandler 与历史 Mimir 2.3.0 Handler 均为最低优先级；本地不同类型的 Advice 不会让 Mimir 的条件自动退让，实际匹配顺序未验证，本地兜底又可能掩盖请求体错误的 400。

**当前进展（2026-11-21）**：本地 Advice 与 ErrorResponse 已删除，源码和打包结果不再包含它们；standalone 显式装配正式 2.4.0 Mimir Handler，已验证坏 JSON、字段错误与未知异常的映射。这不证明完整应用中自动配置已注册，实际运行选择仍未验收，保持待验证。

**下一步与判定标准**：按需人工启动应用记录实际 Handler 与响应，核验自动配置及协议错误；不新增集成测试。只有取得实际运行证据后关闭自动装配疑点，不能把显式 standalone 装配或移除本地源码当作运行时注册结果。

## 已关闭项

| ID | 关闭日期 | 主题 | 关闭依据与边界 |
| --- | --- | --- | --- |
| TD-003 | 2026-10-23 | 模板业务示例不足 | 当时 Customer 已具备 Domain/App/Infrastructure/Adapter 完整链路，历史 CustomerApiIT 提供 CRUD 样例；该集成测试现已按用户要求移除，Bifrost 示例不在本次清理范围。保留原关闭事实，不代表运行验收通过。 |
| TD-004 | 2026-06-07 | 前端相关文档仍为占位 | 沿用原关闭日期；本轮确认 FRONTEND.md 已不存在。 |

## 建议处理顺序

1. **先建立可信验证入口：TD-006。** 评估现有 precheck，明确单测、容器集成、覆盖率、格式和签名边界。
2. **发布侧可独立推进 TD-007、TD-008。** 真实发布前验收凭据留存及版本单调性，不能用本地编译结果替代。
3. **接口侧先核查 TD-011，并协同处理 TD-009、TD-010。** 异常处理器选择会影响错误契约的运行验证。
4. **处理 TD-005。** 先约定并发写入语义，再用真实数据库稳定验证。
5. **完成 TD-012 的迁移验收后推进 TD-002，以及 TD-001、TD-013。** 迁移来源已收敛，结构文档应由该唯一来源生成，避免继续维护重复事实。

## 复核与关闭规则

- 每次关闭保留日期、代码或验证证据；“配置已声明”“测试已存在”“命令已执行”和“实际通过”分开记录。
- 修复进入执行计划后再挂对应链接；当前条目均未因此自动立项。
- 源码静态结论不写成线上事故、实际泄露或运行复现；未知项必须有下一步核验方式。
