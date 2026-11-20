# 1.3.0 版本记录

## 版本摘要

- **版本**：1.3.0
- **状态**：实施中（mimir-upgrade-gates 已实现；集成测试已按用户要求移除，远端 CI 待验收；其余需求未开始）
- **目标**：围绕消除技术债提升项目质量（文档、架构、测试、构建脚本、基础能力），以 Mimir Boot 2.4.0 能力替换本地重复实现。
- **前置条件**：Mimir Boot 2.4.0 已正式发布（2026-11-20 已核实 Release、发布流水线及 Central 主要制品可下载）。本版本所有需求以 2.4.0 正式版能力为准；本项目非容器门禁已验证，完整应用与数据库集成仍待验收。
- **跨项目协同**：Asgard 前端同步建立 1.3.0，对接本版本的 API 契约；契约规则见 [HTTP API 响应与错误契约](../../design-docs/arch-api-contract.md)。

## 需求交付

需求目录在编写具体方案时建立；方案就绪不代表已进入实施。

| 需求 | 路径 | 简述 | 关联 TD | 依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| mimir-upgrade-gates | [实施方案](mimir-upgrade-gates/plan.md) | 升级 Mimir 2.4.0；采用 parent 的 precheck/ci profile 与 PMD/CPD；项目仅维护单元/架构测试；CI 执行 `-Pci verify` | TD-006 | Mimir 2.4.0（已满足） | 已实现；集成测试范围已取消，清理后本地复验见下文，远端 CI 待验收 |
| error-contract-r | `1.3.0/error-contract-r/` | 按 API 契约统一为 Mimir `R`；领域错误码枚举；删除本地 Advice 与 `ErrorResponse`；结构化校验详情；补齐长度校验 | TD-009、TD-010、TD-011 | mimir-upgrade-gates | 未开始 |
| data-consistency | `1.3.0/data-consistency/` | V2 迁移（`version`、审计字段改为 Mimir 命名）；乐观锁与影响行数检查；`AuditorProvider` 示例；分页单次查询；真实 MySQL 迁移验收 | TD-005、TD-012 | error-contract-r | 未开始 |
| mimir-runtime-adoption | `1.3.0/mimir-runtime-adoption/` | 采用 Mimir 日志配置；移除全部 COLA 组件依赖与 cola 包扫描；关闭 Bean 覆盖；CORS 改用 Mimir 配置并暴露 `X-Trace-Id` | TD-014（待登记） | mimir-upgrade-gates；移除 COLA dto/exception 依赖 error-contract-r | 未开始 |
| arch-test-adoption | `1.3.0/arch-test-adoption/` | 架构测试改用 `ColaArchRules` 与 Mimir 测试基类；最小违规夹具证明规则生效 | TD-013 | mimir-upgrade-gates | 未开始 |
| release-hardening | `1.3.0/release-hardening/` | 构建凭据改用 BuildKit secret；Release 增加版本单调与并发协调；fork 演练验收 | TD-007、TD-008 | 无 | 未开始 |
| docs-consistency | `1.3.0/docs-consistency/` | 文档链接与索引检查接入 CI；由迁移生成数据库结构文档；同步技术债清单 | TD-001、TD-002 | data-consistency | 未开始 |

推荐顺序：mimir-upgrade-gates → error-contract-r → data-consistency；mimir-runtime-adoption、arch-test-adoption 在升级后并行（前者的 COLA 依赖清理放在 error-contract-r 之后）；release-hardening 可随时插入；docs-consistency 收尾。

### 各需求的范围与验收要点

**mimir-upgrade-gates**

- 根 POM 删除 `ci` profile 的 `skipTests`、`maven.test.skip`，删除本地 Spotless 插件配置（由 parent 在 `validate` 执行 check，不再 apply）。
- Parent 的 PMD/CPD 在 `precheck`、`ci` 下默认只报告不阻断（`pmd.failOnViolation`、`cpd.failOnViolation` 默认 `false`）。根 POM 须显式设置 `pmd.failOnViolation=true`、`cpd.failOnViolation=true`、`pmd.skipPmdError=false`，否则 PMD 违规不会使构建失败。
- 默认 `test`、`verify` 仅执行单元/架构测试；本项目取消 Parent 的 Failsafe 生命周期绑定。`-Pprecheck clean verify` 为全量预检；CI 对所有 PR 统一执行 `-Pci clean verify`，覆盖单元/架构测试、JaCoCo、Spotless check、生产/测试 PMD/CPD 与 Enforcer，无需 Docker 或 `-DskipITs`。
- 补 Infrastructure 必要单元测试，逐模块核对覆盖率实际检查，避免无执行数据被跳过后误判达标。验收注入失败单测、覆盖率不足、格式、生产/测试 PMD/CPD 违规及禁止依赖，共八类；用户已取消集成测试及其故障夹具验收范围。具体范围见实施方案。

**error-contract-r**

- 按[契约](../../design-docs/arch-api-contract.md)实施：成功统一 200 + `R`；业务错误 200 + 业务码；`CustomerErrorCode`（`21001`、`21002`）；开启结构化校验详情；扁平分页参数转换为 Mimir `PageRequest`；Controller 显式返回 `R`；对外 DTO 实现 `Serializable`。
- `name`/`email`/`phone` 长度在请求 DTO、`Customer.validate()`、数据库三处一致（50/100/20）。
- 验收：Controller standalone、Handler 和序列化单元测试覆盖 JSON 形状、成功、`20001`、`21001`、`21002`、`00004`、请求体不可解析 400、未知异常 500；email 100/101、phone 20/21 边界验证拒绝且不调用仓储写入。按需人工启动应用确认实际 Handler 生效、采集真实响应样例供 Asgard 使用；单元测试不证明数据库状态或自动装配行为，不新增集成测试。

**data-consistency**

- 新增 `V2__customer_audit_and_version.sql`：`created_at`/`updated_at` 改为 `create_time`/`update_time`，新增 `create_by`、`update_by`、`version`（默认 0）；已发布的 V1 不修改。
- `CustomerDO` 增加 `@Version` 与审计字段自动填充；仓储更新检查影响行数，区分不存在（`20001`）与版本冲突（`21002`）。
- 提供 `AuditorProvider` 示例 Bean（返回 `system`），注释说明接入认证时的替换点；不实现认证。
- 分页改为单次 `selectPage`，使用 mybatis starter 的 `PageConverters.toMybatisPage(PageRequest)` 与 `PageConverters.toPageResult(IPage)`，移除重复 count。
- 验收：在专用 MySQL 环境人工运行 V1→V2 迁移与 Flyway validate（对应 TD-012 待验收部分），通过两会话受控事务交错核验“读后被删”与并发更新，检查影响行数、版本及审计字段；仓储结果分支以单元测试验证，不新增集成测试，不以 mock 结果代替数据库运行证据。

**mimir-runtime-adoption**

- 删除 `logback-spring.xml`，由 `mimir.boot.log.*` 配置；移除 `cola-component-catchlog-starter` 依赖与 `scanBasePackages` 中的 `com.alibaba.cola`；`allow-bean-definition-overriding` 设为 false。
- 删除本地 `CorsConfig`，改用 `mimir.boot.web.cors.*`，仅 dev/local 开启，暴露头包含 `X-Trace-Id`。
- 移除全部 COLA 组件依赖：`cola-component-dto`（client）、`cola-component-exception` 与 `cola-component-domain-starter`（domain）、`cola-component-catchlog-starter`（app），组件清空后移除根 POM 的 `cola-components-bom`；domain 改为依赖 `mimir-boot-common`。保留 COLA 分层架构理念。实施前核对 `cola-component-domain-starter` 无实际使用（当前 domain 源码仅引用 COLA `BizException`），且移除 dto 和 exception 须在 error-contract-r 完成替换之后。
- 验收：日志含 traceId，访问日志与脱敏生效；响应头与响应体 traceId 一致；应用上下文启动且无 Bean 覆盖。

**arch-test-adoption**

- `ArchitectureTest` 采用 `ColaArchRules.recommended("io.yggdrasil.labs.midgard")`，覆盖 Domain 无框架依赖、Client 层约束、无循环依赖。
- 保留单元测试与 ArchUnit 架构规则；按需复用 Mimir 的纯测试工具，不采用依赖完整 Spring 上下文或外部数据库的测试基类。
- starter-test 中的 ArchUnit 为 optional，不会传递，`archunit-junit5` 仍以 test 范围显式声明（版本由 BOM 管理）。
- 验收：Domain→Client、Domain 使用持久化注解两类最小违规夹具均被拒绝；现有合法代码通过。

**release-hardening**

- Dockerfile 改为 `RUN --mount=type=secret` 提供 Maven 认证，`build.sh`、`release.yml` 同步，不再使用含 Token 的 build-arg。
- Release 仅在重发版本不低于 main 当前版本时回写 revision 和推送 `latest`；增加 `concurrency` 分组。
- 验收：在 fork 中以标记 Token 构建，日志、各镜像层及导出缓存均不含标记；演练最新发布、旧 tag 重发、重复执行三种场景。

**docs-consistency**

- CI 增加 Markdown 链接与索引检查，失效链接使检查失败。
- 增加数据库结构文档生成方式：在专用临时 MySQL 实例执行迁移后，从 `information_schema` 导出到 `docs/generated/db-schema.md`，记录生成命令；该生成流程不依赖项目集成测试或 Testcontainers。
- 技术债清单：关闭本版本满足关闭条件的条目，登记 TD-014（运行时配置未采用 Mimir 能力：日志、CORS、COLA 组件、Bean 覆盖）与 TD-015（CO/Cmd/Qry 是否迁入 client 模块）。

## 独立交付

暂无。

## 最终验证摘要

### mimir-upgrade-gates（2026-11-20）

以下为升级初次验收的历史结果；集成测试清理后的当前范围与验证单独记录，不将取消的测试记为通过。

- Parent/BOM 升至 2.4.0，继承四项 PMD/CPD 并启用严格阻断；移除本地测试跳过、Spotless apply 和 Start 重复 Failsafe 配置，保留 `exec` classifier。CI 统一为单个 `build` job，普通 PR 与 Dependabot 均执行 `./mvnw -B -ntp -Pci clean verify`。
- 存量修复仅补公开类型/方法 Javadoc、提取三个重复字面量常量；新增 Infrastructure 仓储/转换测试、现有异常 Handler 回归测试，补 App 更新边界。HTTP 契约、业务及数据库语义未调整。
- 环境：WSL、Temurin 17.0.19、Maven Wrapper 3.9.16。7 模块的 `dev`、`precheck`、`ci` 有效模型核对通过；全模块生产编译通过。CI YAML 解析、触发范围/权限/统一命令断言通过；未运行 actionlint 或远端 CI。
- 本地 `./mvnw -B -ntp -Pci verify -DskipITs` 与 `./mvnw -B -ntp -Pprecheck verify -DskipITs` 均通过。另从当前已跟踪及非忽略新增文件建立无旧构建产物的临时普通副本，同一 CI 命令通过；构建后副本内所有复制的源码、配置和文档内容保持一致。未清理原工作区构建目录，不将本次命令写作已执行 `clean verify`。
- 新副本实际执行 **57 项单元/架构测试，0 失败、0 错误、0 跳过**；6 个子模块的 24 份生产/测试 PMD/CPD XML 均存在且零违规、重复及处理错误。四个业务模块实际接受 JaCoCo 阈值检查，结果如下。

| 模块 | 指令覆盖率 | 分支覆盖率 |
| --- | --- | --- |
| Domain | 82.86% | 100.00% |
| App | 97.67% | 79.17% |
| Adapter | 72.49% | 78.57% |
| Infrastructure | 98.74% | 100.00% |

- Client 当前仅包结构，无测试执行数据；Start 的唯一生产类 Application 被 Parent 排除，覆盖率报告无可计量类。两者不记为覆盖率达标。
- 独立临时副本中先验证 Domain 正常基线，再分别注入失败单测、失败 IT、覆盖率不足、格式、生产/测试 PMD、生产/测试 CPD、Enforcer 禁止依赖，**9 组均返回非零且失败来自预期门禁**。IT 使用不依赖数据库的独立夹具，只证明 Failsafe 的运行与结果验证，不代替真实 MySQL IT。
- **当时的边界**：当时 Docker Desktop Linux 引擎不可用、WSL 无 Docker socket；用户先确认容器验收待验证，8 项 CustomerApiIT 未运行，真实应用/MySQL/Flyway 与远端 CI 不记为通过。初次升级已分阶段提交，未推送远端。

### 集成测试清理

- **范围调整**：用户随后明确要求清理集成测试，今后不在项目中编写集成测试；因此撤销此前的容器测试及失败 IT 夹具验收要求，保留单元、standalone MockMvc 和 ArchUnit 测试。真实运行或数据库迁移验证按需人工执行，不因删除测试而自动通过。
- 删除 CustomerApiIT 及专用容器数据源测试资源，移除 Start 的 Testcontainers 直接依赖，根 POM 将继承的 Failsafe `default` execution 绑定到 `none`。生产环境配置及数据库迁移保留。
- 本地 `./mvnw -B -ntp -Pci verify` 通过，**未传 `-DskipITs`**；57 项单元/架构测试全部通过，覆盖率、格式及四项 PMD/CPD 保持通过，日志无 Failsafe 生命周期执行。7 模块有效模型均确认集成测试绑定已取消。
- 默认 `./mvnw -B -ntp verify dependency:tree '-Dincludes=org.testcontainers:*'` 通过；各模块实际依赖树中无 Testcontainers。已删除测试源码和容器测试资源，没有将集成测试改名迁入 Surefire。
- 本次清理纳入本地提交，未推送；远端 CI 尚未验证。

## 风险与遗留债务

- **外部前置已满足**：Mimir 2.4.0 已正式发布；各需求仍按内部依赖顺序推进。
- **已接受的跨项目破坏**：API 契约变更后，Bifrost E2E 现有 Customer CRUD spec 将失败。1.3.0 不修改 Bifrost，验收不包含 E2E 通过；该决定由用户于 2026-11-11 规划时确认。
- **不兼容契约变更**：基于本模板派生的项目升级 1.3.0 需同步前端。
- **本版本不处理**：认证鉴权（仅预留 `AuditorProvider`）、CO/Cmd/Qry 迁入 client 模块（TD-015）、nacos/dubbo/feign 引入、对 Mimir 的额外修改。
