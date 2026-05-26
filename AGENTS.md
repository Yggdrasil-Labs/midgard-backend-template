# AGENTS.md

## 项目概述

`midgard-backend-template` 是基于 COLA 5.0 + Spring Boot 3.3 的 DDD 后端服务模板。核心价值：严格的分层边界（adapter → app → domain ← infrastructure）、标准化的对象流与命名约束、让新服务快速启动并保持长期可维护。

## 全局规范

1. 项目约束优先于智能体默认行为。
2. 项目级 skill（`docs/skills/`）优先于用户级 skill。
3. Git 提交：`<type>(<scope>): <中文描述>`（Conventional Commits）。
4. 优先使用 `./mvnw`；WSL 下需要 Node 时先执行 `source ~/.nvm/nvm.sh`。

## 导航：我该去哪里找信息？

### A. 长期约束（只读，修改需架构 RFC）

- 系统分层与技术栈：`ARCHITECTURE.md`
- 业务领域划分：`docs/DOMAINS.md`
- 核心工程信条：`docs/design-docs/core-beliefs.md`
- 可靠性标准：`docs/RELIABILITY.md`
- 安全策略：`docs/SECURITY.md`

### B. 流转文档（每功能一份，可增改）

- 活跃需求：`docs/active/index.md`
- 已归档版本：`docs/archive/index.md`
- 技术债务清单：`docs/active/tech-debt-tracker.md`

### C. 元规范（方法论）

- 产品思维：`docs/PRODUCT_SENSE.md`
- 需求工作流：`docs/skills/project-workflow/SKILL.md`
- 质量评分：`docs/QUALITY_SCORE.md`

### D. 参考与产物

- 外部参考（只读）：`docs/references/`
- 自动生成文档（禁止手改）：`docs/generated/`

### E. 项目级 Skills

- 文档驱动迭代：`docs/skills/project-workflow/SKILL.md`

## 标准工作流（单任务）

1. **读上下文**：需求文档 + 长期约束
2. **先出计划**：非平凡任务在 `docs/active/{需求}/plan.md` 产出计划
3. **标注假设与风险**
4. **小步分层实施**：不违反依赖方向
5. **行为变化必加测试**
6. **收尾验证**：`./mvnw clean verify`
7. **同步文档**
8. **输出变更摘要**

## 开发命令

```bash
./mvnw clean verify          # 全量校验
./mvnw spring-boot:run -pl start -am  # 启动
./mvnw -Pdev clean verify   # 指定 profile
```
