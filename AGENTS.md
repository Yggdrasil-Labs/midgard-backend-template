# AGENTS.md

本文件是智能体的工作入口，保持简短；具体规则和步骤通过链接按需读取。

## 项目概述

`midgard-backend-template` 是基于 COLA 5.0 + Spring Boot 3.3 的 DDD 后端服务模板。核心价值：严格的分层边界（adapter → app → domain ← infrastructure）、标准化的对象流与命名约束、让新服务快速启动并保持长期可维护。

## 全局规范

1. 项目约束优先于智能体默认行为。
2. 默认使用简体中文。分析、评估、方案认可、实施和提交分别核对用户意图；流程安排和验证通过不自动授权下一操作。Git 提交采用 `<type>(<scope>): <中文描述>`（Conventional Commits）；提交、推送、PR 操作、发布和归档各自按明确授权执行。
3. 先核对实际文件、源码和配置，再维护文档；区分当前实现、预留目录、建议和待验证项。实现偏离约束时应登记差异，不能为迁就实现而静默放宽约束。
4. 保留用户及其他任务的改动；控制当前任务范围，不顺带升级依赖、调整环境或改写历史。
5. 功能和技术债实施先明确归属版本，需要计划时使用 `docs/active/{version}/{requirement}/plan.md`；具体记录职责遵守[文档治理](docs/standards/documentation-governance.md)。版本未确定时先对齐归属，不建立无版本需求目录。
6. 实质性改变分层、依赖方向或架构原则时记录设计与取舍。文档组织和协作规则直接维护规范及指南，局部事实与链接修订直接验证，不为日常治理新建设计文档。已有明确授权的同一范围不重复询问。
7. 新增文件、文档、脚本或生成产物前，遵守[文件归属与准入规则](docs/standards/documentation-governance.md)。一次性排查与验证产物放临时目录。
8. 优先使用 `./mvnw`，先核对命令副作用，再按改动选择验证。结果须对应实际执行内容；跳过、未执行和环境阻塞不能记为通过。

## 导航

### 架构与规范

- [文档总索引与项目规范](docs/index.md)
- [系统分层、对象流与命名约束](ARCHITECTURE.md)
- [业务领域与当前实现](docs/DOMAINS.md)
- [工程信条](docs/standards/core-beliefs.md)、[可靠性](docs/RELIABILITY.md)、[安全](docs/SECURITY.md)
- [架构设计与 RFC](docs/design-docs/index.md)

### 工作指南与记录

- [开发指南](docs/engineering/development.md)：开始工作、定位文件、验证和交付
- [文档治理](docs/standards/documentation-governance.md)：新增、修改、迁移与归档
- [活跃版本与需求](docs/active/index.md)、[历史归档](docs/archive/index.md)、[技术债](docs/active/tech-debt-tracker.md)

## 决策地图

| 改什么 | 首先查阅 |
| --- | --- |
| 新增业务能力、对象或模块依赖 | [架构](ARCHITECTURE.md)与[领域](docs/DOMAINS.md)，再定位所属层 |
| 修改请求、响应或公共契约 | Adapter / Client 与实际调用方，核对兼容性和相关测试 |
| 修改数据库结构 | [数据库迁移规则](docs/engineering/development.md#数据库迁移)与 Start 模块的唯一迁移源 |
| 新增文档、脚本、报告或目录 | [文档治理](docs/standards/documentation-governance.md)，先说明持续用途及归属 |
| 版本、需求、技术债流转或归档 | [版本与需求](docs/standards/documentation-governance.md#版本与需求)，核对归属、记录及授权 |
| 选择验证和说明完成情况 | [开发指南](docs/engineering/development.md)，核对实际报告与未覆盖范围 |
