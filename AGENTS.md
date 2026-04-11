# Midgard Backend Template Agent Map

本文件是仓库入口地图，不是百科全书。

目标只有三个：

1. 帮助 Agent 在最少上下文下快速定位真实信息源。
2. 让仓库文档成为版本化的记录系统，而不是聊天式说明。
3. 让实现、计划、架构和质量约束都可以被后续 Agent 持续维护。

## 1. 首次进入先看哪里

按顺序阅读：

1. [ARCHITECTURE.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/ARCHITECTURE.md)
2. [docs/DESIGN.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/DESIGN.md)
3. [docs/PLANS.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/PLANS.md)
4. 任务相关的 `docs/design-docs/`、`docs/product-specs/` 或 `docs/exec-plans/active/`

当任务涉及质量、发布或风险时，再读：

- [docs/QUALITY_SCORE.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/QUALITY_SCORE.md)
- [docs/RELIABILITY.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/RELIABILITY.md)
- [docs/SECURITY.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/SECURITY.md)

## 2. 仓库事实

- 架构与模块事实统一以 [ARCHITECTURE.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/ARCHITECTURE.md) 为准。
- 本文件只保留协作入口，不再重复分层、模块、对象流和工程细节。
- 本地执行约束只补充两条最容易遗漏的事实：
  - 优先使用 `./mvnw`
  - WSL 下需要 Node 时先执行 `source ~/.nvm/nvm.sh`

## 3. 不可打破的架构边界

- 所有分层边界、依赖方向、对象流和命名约束统一以 [ARCHITECTURE.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/ARCHITECTURE.md) 为准。
- 如果某次改动会影响这些架构事实，应先更新 `ARCHITECTURE.md`，再继续实现。

## 4. 文档地图

仓库文档采用“渐进披露”：

- 顶层地图：
  - [ARCHITECTURE.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/ARCHITECTURE.md)
  - [docs/DESIGN.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/DESIGN.md)
  - [docs/PLANS.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/PLANS.md)
- 设计知识：
  - [docs/design-docs/index.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/design-docs/index.md)
  - [docs/design-docs/core-beliefs.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/design-docs/core-beliefs.md)
- 产品与用例：
  - [docs/product-specs/index.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/product-specs/index.md)
- 执行计划与债务：
  - [docs/exec-plans/active/README.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/exec-plans/active/README.md)
  - [docs/exec-plans/completed/README.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/exec-plans/completed/README.md)
  - [docs/exec-plans/tech-debt-tracker.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/exec-plans/tech-debt-tracker.md)
- 生成型参考：
  - [docs/generated/db-schema.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/generated/db-schema.md)
  - `docs/references/*.txt`

## 5. Agent 工作方式

先判断改动发生在哪一层，再实现，不要先写代码后补解释。

### 小改动

- 可以直接实现。
- 如果改动改变了规则、边界、模板示例或工作流，必须同步更新对应文档。

### 中大改动

- 先在 `docs/exec-plans/active/` 创建执行计划。
- 执行过程中持续记录关键决策、验证结果与未解决风险。
- 完成后移入 `docs/exec-plans/completed/`。

### 设计变更

出现以下情况时，先更新设计文档再继续：

- 修改层间依赖或对象流
- 新增横切能力，如鉴权、审计、缓存、消息、可靠性机制
- 变更模板推荐的包结构、命名规范或工程约束

## 6. 命名与实现约束

- 具体命名与实现边界统一看 [ARCHITECTURE.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/ARCHITECTURE.md)。

## 7. 验证与提交

- 优先使用与改动范围匹配的最小验证命令。
- 若修改 `pom.xml`、构建、公共契约或跨模块结构，优先运行更完整的 Maven 校验。
- 默认不执行 `git commit`、`git push`、打 tag 或发布，除非用户明确要求。
- 提交信息必须符合 Conventional Commits。

## 8. 文档维护规则

- `AGENTS.md` 只保留稳定地图与硬约束，不堆积细节。
- 细节写入对应文档，不重复散落在多个入口。
- 索引文档负责指路，专题文档负责解释，执行计划负责记录变化过程。
- 生成内容放在 `docs/generated/`，人工总结不要伪装成“自动生成”。
- 当前仓库尚无成熟业务域时，允许文档以模板和待补项形式存在，但必须明确状态。
