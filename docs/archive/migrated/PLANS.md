# Planning Guide

本文件定义执行计划在 Midgard 仓库中的角色。

## 为什么计划是第一等工件

当改动开始跨模块、跨层、跨会话时，仅靠即时上下文很容易丢失：

- 任务边界
- 已做决策
- 验证结论
- 剩余风险

执行计划解决的是“可接续性”，不是形式主义。

## 目录约定

- 进行中计划：`docs/exec-plans/active/`
- 已完成计划：`docs/exec-plans/completed/`
- 长期技术债：`docs/exec-plans/tech-debt-tracker.md`
- 可复制模板：`docs/exec-plans/active/_template.md`

旧计划文档已经迁移完成；历史记录统一以 `docs/exec-plans/completed/` 为准。

## 什么时候必须建计划

- 修改多个 Maven 模块
- 修改架构边界或模板规范
- 需要分阶段验证
- 预期超过一次会话完成

## 计划的最小模板

最小模板统一以 [docs/exec-plans/active/_template.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/exec-plans/active/_template.md) 为准。

`PLANS.md` 只负责解释何时建计划、计划放哪里、何时归档，不再复制模板字段，避免出现多个真相源。

## 完成标准

一个计划完成时，应至少满足：

- 目标已完成或已明确缩减
- 验证结果已记录
- 未完成项已转移
- 文档落点已更新
