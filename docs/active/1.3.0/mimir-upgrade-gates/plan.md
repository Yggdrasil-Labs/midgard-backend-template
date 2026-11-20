# Mimir 升级与质量门禁实施方案

**归属版本**：1.3.0

**需求**：mimir-upgrade-gates

**关联**：[版本记录](../release.md)、[TD-006](../../tech-debt-tracker.md#td-006质量门禁入口与签名职责混杂)

## 目标与范围

升级至 Mimir Boot 2.4.0 正式版，直接采用 Parent 的构建与质量能力，使本地全量预检和 CI 有可信的测试、覆盖率、格式及静态检查门禁。本文同时承载设计取舍和实施步骤，不另建 Spec 或 Design。

本需求包含根 POM、Start 重复插件配置、CI、门禁暴露的必要源码修复及测试补充。保持现有 Customer HTTP 契约、数据结构和业务行为；`R` 改造、乐观锁、COLA 清理、运行时配置及发布加固由各自需求负责。构建 `revision` 保持现值，目标交付版本不自动改变构建版本。

## 已核实背景

- 2026-11-20 核实 [2.4.0 正式 Release](https://github.com/Yggdrasil-Labs/mimir-boot/releases/tag/v2.4.0) 已发布；[发布流水线](https://github.com/Yggdrasil-Labs/mimir-boot/actions/runs/44542857319) 中消费者验证和 Central 公开制品验证成功。Parent、BOM、质量插件及所需主要组件已从 Maven Central 下载成功；这不代表本项目升级构建已经通过。
- 当前根 POM 的 Parent 和显式 BOM 均为 2.3.0，本地 `ci` 跳过测试，Spotless 在 `process-sources` 自动 apply。CI 的独立 Test job 实际运行默认 `verify`，因此不能表述为“整个 CI 不运行测试”；Dependabot 构建路径目前只有编译检查。
- 正式 Parent 保持 Java 17、Spring Boot 3.3.13；`precheck`、`ci` 开启 JaCoCo、Spotless check、PMD/CPD，只有 `ci` 启用 Enforcer。PMD/CPD 默认仅报告；签名和部署默认跳过。以[公开 Parent POM](https://repo.maven.apache.org/maven2/io/github/yggdrasil-labs/mimir-boot-parent/2.4.0/mimir-boot-parent-2.4.0.pom)和实施时的有效模型为准，不从本地 Mimir 默认分支推断正式能力。
- 现有普通测试分布于 Domain、App、Adapter，架构测试和 `CustomerApiIT` 位于 Start。后者使用 Testcontainers MySQL。Infrastructure 尚无本模块测试；Start IT 的采集数据不会自动汇入 Infrastructure 的模块级 JaCoCo 检查。

## 设计决策与行为契约

### 依赖与继承

- Parent 与显式导入的 Mimir BOM 同步升至 `2.4.0`，保留现有显式 BOM 导入及属性入口，避免本批扩大依赖管理清理范围。Java、Spring Boot、springdoc 兼容基线保持现状。
- 删除本地整段 Spotless 配置及 `ci` 测试跳过属性，直接继承 Parent；空的模式 profile 声明可删除。正常生命周期只检查源码，不再隐式修复格式；需要格式化时显式执行 `spotless:apply` 并复核差异。
- 删除 Start 重复的 Failsafe 声明，继承 Parent 的测试选择和执行；保留 Spring Boot `exec` classifier，保证薄 JAR 主构件与 IT 类路径。
- 根属性显式设置 `pmd.failOnViolation=true`、`cpd.failOnViolation=true`、`pmd.skipPmdError=false`。继承独立插件 `io.github.yggdrasil-labs:mimir-boot-code-quality:2.4.0`，不重复接入官方 PMD 插件或复制规则。
- 插件与普通依赖分别核对解析仓库。标准 Maven Central 环境使用默认插件仓库；自定义镜像需覆盖质量插件及其官方实现依赖，不能把普通 `<repositories>` 可访问当作插件可解析的证据。

### 构建入口

以下为升级后的目标命令，不用于当前 2.3.0 配置的只读调查：

| 场景 | 命令 | 行为与边界 |
| --- | --- | --- |
| 无容器的单测反馈 | `./mvnw -B -ntp test` | 执行 Surefire；不包含 IT 或完整门禁 |
| 默认完整生命周期 | `./mvnw -B -ntp verify` | 单测和 IT；默认跳过覆盖率、格式和 PMD/CPD；需要 Docker |
| 本地全量预检 | `./mvnw -B -ntp -Pprecheck clean verify` | 单测、IT、覆盖率、格式、PMD/CPD；需要 Docker |
| CI 验收 | `./mvnw -B -ntp -Pci clean verify` | 全量预检加 Enforcer；需要 Docker，不要求签名密钥 |

CI 所有现有触发路径使用同一门禁，包括 Dependabot；取消按 actor 降为 compile-only 的路径。保留现有 push/PR 触发范围、只读仓库权限和 JDK 17；不调整分支保护。CI 采用单个质量 job，避免先 package 再重复 verify；保留 job ID `build`，显示名称表达完整质量检查。发布工作流不在本批调整。

### 覆盖率与存量违规

- 沿用 Parent 的模块级 BUNDLE 阈值：指令覆盖率至少 60%、分支覆盖率至少 50%。本批补 Infrastructure 仓储与转换器测试，使其生成本模块数据并实际接受阈值检查；不引入跨模块聚合覆盖率系统。
- Domain、App、Adapter、Infrastructure 均需有新生成的报告和成功的阈值检查。Client 当前仅包结构预留，Start 是装配与集成测试入口；逐模块记录是否存在可计量生产类，明确无源或无执行数据造成的跳过，不宣称这些模块覆盖率已达标。若这些模块存在未受检查的业务实现，需补相应测试后再验收。
- 新测试断言现有正常及异常行为，不通过空断言或只调用方法凑覆盖率。仓储单测可以 mock Mapper；真实 SQL、容器启动和 HTTP 回归仍由现有 MySQL IT 验证。并发、影响行数语义及重复 count 暂不改变。
- 格式、Javadoc、测试写法和重复代码等违规采用最小修复；行为等价的局部提取允许实施者决定。不得通过降低阈值、关闭生产/测试 execution、增加整模块排除或宽泛 suppress 换取通过。确有误报需提供规则、位置与证据，重新对齐处理方式。

## 任务与文件归属

按 T1 → T2 → T3 → T4 串行实施。根 POM 和各模块源码可能同时影响多项检查，不并行 clean 或修改同一配置。

### T1：升级依赖并收敛构建配置

**文件**：

- Modify：`pom.xml` — 升级 Parent/BOM，启用严格属性，移除重复 Spotless 与测试跳过配置。
- Modify：`start/pom.xml` — 移除重复 Failsafe 声明，保留 `exec` classifier。

**验收**：

- 在 Java 17 与项目 Maven Wrapper 环境生成各相关模块的有效 POM，输出放系统临时目录。确认 Parent/BOM/质量插件为 2.4.0，依赖实际可解析，编译目标为 Java 17。
- `dev`、`precheck`、`ci` 的测试开关均为开启；质量开关符合入口表，`ci` 含 Enforcer；签名及部署默认跳过。
- Surefire 接收 `*Test`/`*Tests`，排除 `*IT`/`*IntegrationTest`；Failsafe 接收后两者，绑定集成测试和结果验证；Spotless 仅有继承的 `spotless-check`，没有生命周期 apply。
- 质量插件包含 `pmd`、`test-pmd`、`cpd`、`test-cpd` 四个执行，均绑定 `verify`；主、测试报告目录互不覆盖。

### T2：修复门禁问题并补齐模块测试

**文件**：

- Create：`infrastructure/src/test/java/io/yggdrasil/labs/midgard/infrastructure/persistence/impl/CustomerRepositoryImplTest.java` — 验证保存回填 ID、查询存在/不存在、分页与筛选、计数、更新/删除调用和邮箱存在性判断。
- Create：`infrastructure/src/test/java/io/yggdrasil/labs/midgard/infrastructure/persistence/convertor/CustomerInfraConvertorTest.java` — 验证主要字段、状态转换、空值及现有非法状态输入行为。
- Modify（按实际报告）：`{adapter,app,domain,infrastructure,start}/src/main/java/`、对应 `src/test/java/` — 仅限门禁所需的格式、文档、行为等价修复及必要测试补充；已知回归入口包含 `CustomerTest`、`CustomerAppServiceImplTest`、`CustomerControllerTest`、`ArchitectureTest`、`CustomerApiIT`。
- Modify（确有测试依赖缺口时）：所属模块 `pom.xml` — 仅补 test 范围依赖，优先复用已声明的 Mimir Test Starter 和 BOM 版本。

**验收**：

- 保存原有测试覆盖的 HTTP 状态、JSON 形状、业务结果和数据库行为，不为升级提前采用新 API 契约。若正式版行为导致现有断言失败，先定位来源；需要改变公共行为时重新对齐，不直接改断言迁就新行为。
- 完整 `-Pprecheck clean verify` 通过；普通测试、架构规则和 8 个现有 `CustomerApiIT` 用例均实际运行，新增测试也实际运行，不能用 skip 或测试发现数下降替代通过。实施前重新核对已有用例数量。
- 核对四个 XML 报告：`target/mimir-quality/pmd.xml`、`cpd.xml` 和 `target/mimir-quality/tests/pmd.xml`、`cpd.xml`，所有有相应源码的模块零违规。
- 核对业务模块 `target/site/jacoco/jacoco.xml`、阈值检查日志及新生成的执行数据；Infrastructure 不再因缺少执行数据跳过检查。

### T3：统一 CI 门禁

**文件**：

- Modify：`.github/workflows/ci.yml` — 收敛至 `build` job 的一次 `-Pci clean verify`，清除重复构建及 actor 例外。

**验收**：

- YAML/工作流静态检查通过；现有触发范围和只读权限保留，普通 PR 与 Dependabot 没有不同质量命令或跳过参数。
- 本地执行同一 CI 命令，测试、JaCoCo、Spotless、四项 PMD/CPD 与 Enforcer 均生效，构建成功且不需要 GPG 私钥。
- 远端 CI 仅在后续获准推送/PR 后核验；本地验证和静态检查不能写成远端成功。未取得远端结果时，在交付记录中明确待验收。

### T4：故障注入验收与文档同步

**文件**：

- Modify：`docs/engineering/development.md` — 更新实际命令、默认模式、Docker 和格式化边界。
- Modify：`README.md` — 仅同步受影响的版本、构建及质量命令说明。
- Modify：`.github/Workflow.md` — 同步 CI 质量检查职责，移除“Dependabot PR 仅做编译”的例外说明，明确所有 PR 使用同一门禁；发布流程说明不在本批调整。
- Modify：`docs/active/tech-debt-tracker.md` — 更新 TD-006 的关联、进展及关闭证据，不提前关闭。
- Modify：`docs/active/1.3.0/release.md` — 集中登记最终结果及未覆盖项；本 Plan 不回写执行进度。
- 临时违规夹具、原始日志和有效 POM 不进入版本控制，不新增专项公共脚本。

**隔离与验收**：

- 故障夹具在实施产物的独立副本中运行，每个场景从无违规基线独立构造，互不遮挡。若需 worktree，遵守项目内已忽略目录约定；没有安全位置时不擅自建立项目外 worktree，可使用临时目录中的普通文件副本。包含本批未提交文件，不能仅复制 HEAD，也不能回退用户工作区。
- 先确认正常基线的 `-Pci clean verify` 返回 0，再按下表分别注入。每次除非零退出码外，还必须核对失败来自预期检查；下载、编译或 Docker 故障不算门禁拒绝违规的证据。

| 故障 | 预期拒绝证据 |
| --- | --- |
| 一个断言失败的 `*Test` | Surefire 测试失败，构建非零 |
| 一个断言失败的 `*IT` | Failsafe 运行并在结果验证时报错，构建非零 |
| 一个有生产类的模块覆盖率低于现有阈值 | 测试仍成功、采集数据存在，JaCoCo 明确报告阈值不满足 |
| 一处格式错误 | Spotless check 报错，源码没有被自动修复 |
| 生产 PMD 违规、测试 PMD 违规（分别执行） | 对应 `pmd`、`test-pmd` 检查及报告指向夹具 |
| 生产 CPD 重复、测试 CPD 重复（分别执行） | 重复片段超过 100 tokens，对应 `cpd`、`test-cpd` 拒绝；夹具避免同时触发 PMD |
| Enforcer 禁止的依赖 | `ci` 的 Enforcer 报出指定禁止项，构建非零 |

- 清除副本中的故障后重新执行正常全量门禁；对比构建前后的已跟踪文件和非忽略新文件，确认没有生命周期自动修改源码。`target/`、已忽略 Flatten 产物允许生成，不把“源码无变化”理解成构建不写任何文件。
- 文档链接、导航和 Markdown 差异检查通过。TD-006 仅在其运行关闭条件实际满足后关闭；远端尚未验证时保留待验收边界。本批不关闭后续 API、迁移、架构与运行时需求对应的技术债。

## 交付与决策边界

实施者可决定契约内的测试组织、局部等价提取和格式修复。以下变化需重新对齐：改变 HTTP/领域/持久化语义，提前修改其他需求能力，降低或规避门禁，升级兼容基线，建立覆盖率聚合体系，或存量违规需要大范围架构重构。

Docker、镜像或依赖不可用时记录具体环境阻塞，不跳过集成测试后宣称完整门禁通过。方案编写不授权实施；本地修改、提交、推送和发布继续按用户明确指令分别处理。
