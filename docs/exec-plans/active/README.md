# Active Execution Plans

本目录用于存放正在进行中的执行计划。

## 何时创建

满足任一条件时，优先创建计划文件：

- 涉及多个模块或多个层
- 需要分阶段提交
- 风险、约束或回滚点较多
- 任务可能跨多个会话完成

## 推荐文件名

`YYYY-MM-DD-<topic>.md`

例如：

- `2026-04-10-add-customer-aggregate.md`
- `2026-04-10-standardize-error-handling.md`

## 最少应包含

最小字段与结构统一以 [active/_template.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/exec-plans/active/_template.md) 为准。

完成后将文件移入 `../completed/`。
