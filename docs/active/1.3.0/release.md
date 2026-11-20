# 1.3.0 版本记录

## 版本摘要

- **版本**：1.3.0
- **状态**：规划中（需求范围已确认，尚未进入实施）
- **目标**：围绕消除技术债提升项目质量（文档、架构、测试、构建脚本、基础能力），以 Mimir Boot 2.4.0 能力替换本地重复实现。
- **前置条件**：Mimir Boot 2.4.0 已正式发布（2026-11-20 已核实 Release、发布流水线及 Central 主要制品可下载）。本版本所有需求以 2.4.0 正式版能力为准；本项目升级兼容性仍待验证。
- **跨项目协同**：Asgard 前端同步建立 1.3.0，对接本版本的 API 契约；契约规则见 [HTTP API 响应与错误契约](../../design-docs/arch-api-contract.md)。

## 需求交付

需求目录在编写具体方案时建立；方案就绪不代表已进入实施。

| 需求 | 路径 | 简述 | 关联 TD | 依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| mimir-upgrade-gates | [实施方案](mimir-upgrade-gates/plan.md) | 升级 Mimir 2.4.0；采用 parent 的 precheck/ci profile 与 PMD/CPD；移除本地 `ci` 跳过测试与重复 Spotless；CI 执行 `-Pci verify` | TD-006 | Mimir 2.4.0（已满足） | 方案已编写，未实施 |
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
- 本地 `./mvnw test` 提供无容器单测反馈；默认 `verify` 仍运行 Testcontainers IT，需要 Docker。`-Pprecheck clean verify` 为全量预检；CI 对所有 PR 统一执行 `-Pci clean verify`，覆盖单测、IT、JaCoCo、Spotless check、生产/测试 PMD/CPD 与 Enforcer。
- 补 Infrastructure 必要测试，逐模块核对覆盖率实际检查，避免无执行数据被跳过后误判达标。验收分别注入失败单测/IT、覆盖率不足、格式、生产/测试 PMD/CPD 违规及禁止依赖，均由预期门禁拒绝；正常代码通过且源码不被构建自动修改。具体范围见实施方案。

**error-contract-r**

- 按[契约](../../design-docs/arch-api-contract.md)实施：成功统一 200 + `R`；业务错误 200 + 业务码；`CustomerErrorCode`（`21001`、`21002`）；开启结构化校验详情；扁平分页参数转换为 Mimir `PageRequest`；Controller 显式返回 `R`；对外 DTO 实现 `Serializable`。
- `name`/`email`/`phone` 长度在请求 DTO、`Customer.validate()`、数据库三处一致（50/100/20）。
- 验收：`CustomerApiIT` 断言真实 JSON，覆盖成功、`20001`、`21001`、`21002`、`00004`、请求体不可解析 400、未知异常 500，并确认仅 Mimir Handler 生效；导出响应样例供 Asgard 测试使用；email 100/101、phone 20/21 边界拒绝后数据库不变。

**data-consistency**

- 新增 `V2__customer_audit_and_version.sql`：`created_at`/`updated_at` 改为 `create_time`/`update_time`，新增 `create_by`、`update_by`、`version`（默认 0）；已发布的 V1 不修改。
- `CustomerDO` 增加 `@Version` 与审计字段自动填充；仓储更新检查影响行数，区分不存在（`20001`）与版本冲突（`21002`）。
- 提供 `AuditorProvider` 示例 Bean（返回 `system`），注释说明接入认证时的替换点；不实现认证。
- 分页改为单次 `selectPage`，使用 mybatis starter 的 `PageConverters.toMybatisPage(PageRequest)` 与 `PageConverters.toPageResult(IPage)`，移除重复 count。
- 验收：真实 MySQL 上 V1→V2 迁移与 Flyway validate 通过（同时满足 TD-012 待验收部分）；事务屏障稳定覆盖“读后被删”与两个并发更新，无假成功、无静默覆盖；审计字段自动填充。

**mimir-runtime-adoption**

- 删除 `logback-spring.xml`，由 `mimir.boot.log.*` 配置；移除 `cola-component-catchlog-starter` 依赖与 `scanBasePackages` 中的 `com.alibaba.cola`；`allow-bean-definition-overriding` 设为 false。
- 删除本地 `CorsConfig`，改用 `mimir.boot.web.cors.*`，仅 dev/local 开启，暴露头包含 `X-Trace-Id`。
- 移除全部 COLA 组件依赖：`cola-component-dto`（client）、`cola-component-exception` 与 `cola-component-domain-starter`（domain）、`cola-component-catchlog-starter`（app），组件清空后移除根 POM 的 `cola-components-bom`；domain 改为依赖 `mimir-boot-common`。保留 COLA 分层架构理念。实施前核对 `cola-component-domain-starter` 无实际使用（当前 domain 源码仅引用 COLA `BizException`），且移除 dto 和 exception 须在 error-contract-r 完成替换之后。
- 验收：日志含 traceId，访问日志与脱敏生效；响应头与响应体 traceId 一致；应用上下文启动且无 Bean 覆盖。

**arch-test-adoption**

- `ArchitectureTest` 采用 `ColaArchRules.recommended("io.yggdrasil.labs.midgard")`，覆盖 Domain 无框架依赖、Client 层约束、无循环依赖。
- 测试按需采用 Mimir 测试基类；Testcontainers 配置保留在项目内。
- starter-test 中的 ArchUnit 为 optional，不会传递，`archunit-junit5` 仍以 test 范围显式声明（版本由 BOM 管理）。
- 验收：Domain→Client、Domain 使用持久化注解两类最小违规夹具均被拒绝；现有合法代码通过。

**release-hardening**

- Dockerfile 改为 `RUN --mount=type=secret` 提供 Maven 认证，`build.sh`、`release.yml` 同步，不再使用含 Token 的 build-arg。
- Release 仅在重发版本不低于 main 当前版本时回写 revision 和推送 `latest`；增加 `concurrency` 分组。
- 验收：在 fork 中以标记 Token 构建，日志、各镜像层及导出缓存均不含标记；演练最新发布、旧 tag 重发、重复执行三种场景。

**docs-consistency**

- CI 增加 Markdown 链接与索引检查，失效链接使检查失败。
- 增加数据库结构文档生成方式：以 Testcontainers MySQL 执行迁移后从 `information_schema` 导出到 `docs/generated/db-schema.md`，记录生成命令，可重复生成。
- 技术债清单：关闭本版本满足关闭条件的条目，登记 TD-014（运行时配置未采用 Mimir 能力：日志、CORS、COLA 组件、Bean 覆盖）与 TD-015（CO/Cmd/Qry 是否迁入 client 模块）。

## 独立交付

暂无。

## 最终验证摘要

尚未实施，无验证结果。

## 风险与遗留债务

- **外部前置已满足**：Mimir 2.4.0 已正式发布；各需求仍按内部依赖顺序推进。
- **已接受的跨项目破坏**：API 契约变更后，Bifrost E2E 现有 Customer CRUD spec 将失败。1.3.0 不修改 Bifrost，验收不包含 E2E 通过；该决定由用户于 2026-11-11 规划时确认。
- **不兼容契约变更**：基于本模板派生的项目升级 1.3.0 需同步前端。
- **本版本不处理**：认证鉴权（仅预留 `AuditorProvider`）、CO/Cmd/Qry 迁入 client 模块（TD-015）、nacos/dubbo/feign 引入、对 Mimir 的额外修改。
