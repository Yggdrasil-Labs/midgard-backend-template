# 文档总索引

按任务选择阅读入口。首次使用模板先看 [README](../README.md)，理解分层先看 [ARCHITECTURE](../ARCHITECTURE.md)。

## 项目规范

| 文档 | 何时阅读 |
| --- | --- |
| [文档治理](standards/documentation-governance.md) | 新增、修改、迁移文档，或决定文件、脚本与产物的归属 |
| [核心工程信条](standards/core-beliefs.md) | 判断模板能力准入、工程取舍及长期维护价值 |
| [安全约束](SECURITY.md) | 涉及输入、输出、凭据或敏感信息 |
| [可靠性约束](RELIABILITY.md) | 涉及事务、异常及外部依赖 |

规范说明长期约束，设计记录实际架构方案与技术取舍，工作指南说明如何操作。文档职责与版本记录格式统一由文档治理规范维护。

## 架构与开发

- [架构与分层](../ARCHITECTURE.md)：模块职责、依赖方向、对象流与命名。
- [领域现状](DOMAINS.md)：Customer 当前实现与扩展位置。
- [设计与 RFC](design-docs/index.md)：长期设计及变更依据。
- [开发指南](engineering/development.md)：任务范围、修改位置、验证和交付纪律。

## 工作记录与产物

- [活跃版本与需求](active/index.md)：通过版本 `release.md` 查阅需求入口、交付与最终验证。
- [技术债清单](active/tech-debt-tracker.md)：跨版本的问题、证据、实施关联及关闭依据。
- [历史归档](archive/index.md)：按版本保留交付记录及原有验证边界。

新增文档应从本页或最近的上级入口可达；没有实际内容时不预建目录或占位页。
