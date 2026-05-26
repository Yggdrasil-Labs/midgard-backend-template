# Design Guide

本文件说明“什么样的设计信息应该进入仓库”。

## 设计分层

设计相关文档分三类：

1. 稳定架构骨架：见 [ARCHITECTURE.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/ARCHITECTURE.md)
2. 长期设计原则：见 `design-docs/`
3. 单次任务设计与执行：见 `exec-plans/`

## 写设计文档的标准

值得单独成文的设计应至少满足其一：

- 影响多个模块
- 引入新的横切能力
- 改变现有模板推荐方式
- 没有文档就很难让下一个 Agent 延续同一判断

## 不要这样写

- 用大段空泛原则替代具体约束
- 把一次实现细节写成永恒架构
- 在多个入口复制同一份说明

## 推荐结构

- 背景
- 目标与非目标
- 方案选择与取舍
- 对层间边界的影响
- 验证方式
- 风险与迁移策略
