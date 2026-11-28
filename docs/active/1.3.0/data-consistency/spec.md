# 客户数据一致性需求约定

**归属版本**：1.3.0。输入为[版本范围](../release.md)与[公共 API 契约](../../../design-docs/arch-api-contract.md)；实现选择见[设计](design.md)，执行边界见[计划](plan.md)。

## 目标与范围

防止客户更新假成功和静默覆盖，保留迁移前的数据，统一审计记录并消除分页的重复总数查询。公共成功与错误信封沿用既有契约；本项增加客户版本的读取与更新要求。

## Behavior B1：读取与创建客户版本

### Scenario：创建后可以直接编辑

Given 输入合法且邮箱未被占用；When 创建客户；Then 返回 HTTP 200、code=00000，客户数据包含整数 version=0；And 随后的详情和列表中该客户版本均为 0。

### Scenario：历史客户可继续使用

Given 升级前存在客户，电话为空且已记录创建、更新时间；When 完成升级并读取客户；Then ID、业务字段、原有两个时间值不变，版本为 0；And 创建、更新主体均补为 system。

### Scenario：读取不存在的客户

Given 客户已不存在；When 读取其详情；Then 返回 HTTP 200、code=20001、data=null，不生成新客户。

## Behavior B2：带版本更新客户

### Scenario：匹配版本更新

Given 客户版本为 3；When 提交 version=3 和合法电话修改；Then 返回 HTTP 200、code=00000、version=4；And 未提交字段不变。提交相同业务值仍是一次更新，版本增加 1。

### Scenario：两个编辑者使用同一版本

Given 两个编辑者都读到版本 3；When 第一个修改姓名成功、第二个仍以 version=3 修改电话；Then 第二个返回 HTTP 200、code=21002、data=null；And 第一个的修改被保留，电话不被第二个覆盖，不自动重试。

### Scenario：读取后被删除

Given 更新操作已经读取客户，但另一操作在其写入前删除该客户并完成提交；When 原更新继续写入；Then 返回 HTTP 200、code=20001、data=null，不能返回成功或重新创建客户。

### Scenario：版本或更新字段无效

Given 请求的 version 缺省、null、负数、非整数（含小数、数字字符串、布尔值）或大于 2147483646，或者只有 version 而无任何可更新业务字段；When 提交更新；Then 返回 HTTP 400 和校验/绑定错误，不执行写入。只传 version 的请求保留 OBJECT/$ 空更新错误；字段约束错误对应 FIELD/version，请求体解析错误沿用绑定错误映射。

### Scenario：直接调用与既有业务错误

Given 直接调用更新用例而非 HTTP 校验入口；When 版本无效或没有更新字段；Then 抛出 code=00004 的业务错误且不写入。客户不存在为 20001，客户存在但版本已旧为 21002；版本匹配后邮箱占用仍为 21001，其他长度、空值约束沿用既有契约。

## Behavior B3：维护审计记录

### Scenario：新建记录

Given 使用模板默认审计主体；When 创建合法客户；Then 创建、更新主体均为 system，创建、更新时间非空，版本为 0。

### Scenario：更新及失败隔离

Given 客户存在且已有创建时间和创建主体；When 带匹配版本成功更新；Then 原创建时间、创建主体不变，更新时间记录本次更新，更新主体为 system。When 旧版本更新被拒绝；Then 该失败不改变数据库中的任何业务字段、版本或审计值。

### Scenario：升级前置不成立

Given 数据库并非预期的上一版结构，或者执行账号没有结构变更权限；When 尝试升级；Then 报告失败，应用不作为可用的新版本开放写流量；And 不删除旧数据、不忽略失败继续运行。恢复流程以实际数据库状态为依据。

## Behavior B4：客户分页

### Scenario：过滤与元数据一致

Given 过滤后有 23 条客户；When 请求第 2 页、每页 10 条；Then 返回 10 条、totalCount=23、pageIndex=2、pageSize=10、totalPages=3、hasNext=true、hasPrevious=true；And 记录按 ID 降序，包含 version，姓名/邮箱关键字与状态过滤语义不变。

### Scenario：空结果与输入边界

Given 没有匹配客户；When 请求默认第一页；Then data 为 []，totalCount/totalPages 为 0，前后页标记均为 false。When 请求页码为 0、页大小为 1001；Then 使用纠正后的 1/1000 查询并回包；页码超过 2147483647 或非数字仍返回 HTTP 400。

### Scenario：查询失败

Given 查询数据源不可用；When 请求列表；Then 返回安全的系统错误响应，不伪造空列表或 totalCount=0，不回显 SQL、连接信息或凭据。

## Constraints

- version 为非负整数，存储/输出范围 0..2147483647；更新输入上限 2147483646，为下一次递增保留空间，禁止溢出。到达上限的记录不再接受更新，不自动清零。
- 一次更新只接受客户端所读版本；删除接口不新增 version 参数，重复删除的既有语义不变。
- 列表沿用默认 1/10、页大小上限 1000、页码上限 2147483647、Long 总数；本项不增加排序参数或修改旧参数兼容策略。
- 保留原有 50/100/20 字段长度限制、电话 null/空串和部分更新语义。公共信封及 HTTP/业务码分工不变。
- 本项目只维护单元、standalone MockMvc、序列化和架构测试；真实数据库迁移、并发与完整应用验收单列，不新增集成测试。
- 不修改 Asgard/Bifrost，不引入认证、全量 COLA 清理或发布加固；前端必须另行携带并刷新 version。
