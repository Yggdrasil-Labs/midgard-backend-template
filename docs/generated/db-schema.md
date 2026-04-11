# Database Schema Snapshot

状态：placeholder

本文件预留给数据库结构快照或自动生成摘要。

当前仓库事实：

- 已存在 [db/README.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/db/README.md)
- 但仓库中尚未提交可供汇总的正式 `schema/`、`migration/`、`data/` 脚本内容
- 因此这里暂时不伪造数据库结构

## 未来建议

当以下任一条件成立时，应把本文件改为生成产物：

1. `db/schema/` 中已经存在初始化 DDL
2. 项目开始稳定使用 Flyway 或 Liquibase
3. 需要让 Agent 快速获取表、索引、迁移历史摘要

## 目标格式

推荐至少包含：

- 表名
- 主键
- 关键索引
- 审计字段
- 关联的聚合或 Repository
- 来源脚本路径

在没有真实来源前，本文件只充当生成位，不承载事实断言。
