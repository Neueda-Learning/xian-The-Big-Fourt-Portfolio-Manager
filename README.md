# 投资组合管理系统

## 1. 项目介绍与开发背景

本项目是一个基于 **Spring Boot 4** 构建的多资产投资组合管理系统，提供完整的 REST API 后端服务与原生 JavaScript 单页前端仪表盘，面向个人投资者和小型团队提供统一的持仓与业绩管理能力，并集成智谱 AI 金融知识问答功能。

### 开发背景与痛点

传统的个人投资管理通常依赖电子表格，存在以下典型痛点：

- **数据分散、版本混乱**：持仓、交易、价格数据分散于不同文件，难以构建统一的资产视图。
- **账实不符**：买入、卖出操作与持仓数量、现金余额之间缺乏一致性约束，手工维护极易出错。
- **交易不可篡改**：缺乏对已记录交易的保护机制，历史数据容易被意外修改或删除。
- **业绩分析依赖手工**：总市值、收益率、资产配置比例等指标需要人工计算，效率低且难以追溯。
- **行情数据管理薄弱**：缺乏统一的历史价格与实时报价数据源，业绩统计缺乏可靠基础。
- **AI 建议与业务操作混淆**：用户希望获得 AI 辅助的金融知识讲解，但需要确保 AI 不干预实际的持仓与交易操作。

本系统通过结构化数据模型、严格的服务层业务规则（买入扣款、卖出增款、交易不可修改），以及日度快照机制，有效解决上述问题，提升投资数据的准确性、可追踪性与分析效率。

---

## 2. 核心系统功能

### 2.1 投资组合管理（Portfolio）

- 支持组合的新增、查询、更新、删除。
- 维护每个组合的名称、描述、初始现金与当前现金余额。
- 提供组合汇总接口，聚合展示总资产价值、总收益、现金余额及日度涨跌幅。

### 2.2 持仓管理（Holding）

- 按组合维度查询持仓列表，支持持仓详情查看。
- 记录资产类型（`STOCK` / `BOND` / `CASH`）、证券代码、数量、平均成本价、当前价格、买入日期与币种。
- **持仓数量变更仅允许通过交易接口触发**，不支持直接编辑或删除，保障数据一致性。
- 支持单独更新持仓当前价格（用于手动行情刷新）。

### 2.3 交易管理（Transaction）

- 支持买入（BUY）和卖出（SELL）交易记录的创建。
- **交易一旦创建即不可修改或删除**，保障交易历史的不可篡改性。
- 买入时自动校验现金余额是否充足，不足则拒绝；卖出时自动校验持仓数量。
- 买入扣减、卖出增加组合现金余额，交易完成后自动更新持仓数量与平均成本价。
- 支持现金存入接口，允许向组合增加现金余额。
- 支持按组合 ID 或持仓 ID 查询交易记录。

### 2.4 业绩分析（Performance）

- 提供组合层面的总市值、总成本、总收益（绝对值与百分比）计算。
- 输出每个持仓的市值、成本、当前价格及持仓明细。
- 当前价格优先级：本地价格历史 → 外部 Yahoo Finance 接口 → 持仓成本价兜底。

### 2.5 组合日度快照（Portfolio Snapshot）

- 支持按需捕获当日组合快照，记录总价值、现金余额与持仓价值。
- 支持按日期范围查询历史快照序列，为前端业绩图表提供数据源。
- 唯一性约束保证同一组合同一日期仅保留一条快照记录。

### 2.6 价格历史管理（Price History）

- 支持按证券代码和日期精确查询、按日期范围批量查询收盘价。
- 支持手动新增与删除价格记录。
- 价格历史数据用于持仓估值与业绩趋势计算。

### 2.7 实时行情接入（Quote）

- 通过 Yahoo Finance 外部接口获取证券实时报价。
- 若外部接口不可用，自动回退至本地价格历史中最新一条记录。

### 2.8 AI 金融知识问答（AI Chat）

- 集成智谱 AI（Zhipu / GLM）大模型，提供金融知识教育性问答。
- 支持前端模型选择（`glm-4.7-flash`、`glm-4.7`、`glm-5.2`），可用模型列表由后端配置管控。
- 支持用户在当前会话中保存个人 API Key，优先级高于系统默认 Key。
- **AI 仅用于知识问答，不执行任何交易、持仓或数据库变更操作**，内置系统提示词强制约束。
- API Key 仅存储于服务端 `HttpSession`，不落库、不写入配置文件、不回传明文至前端。

---

## 3. 数据库表说明

数据库使用 **MySQL**，表结构定义于 `src/main/resources/schema.sql`，初始数据定义于 `src/main/resources/data.sql`。

### 3.1 `portfolio` — 投资组合主表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT AUTO_INCREMENT | 主键 |
| `pro_name` | VARCHAR(100) | 组合名称 |
| `pro_description` | VARCHAR(500) | 组合描述 |
| `initial_cash` | DECIMAL(18,4) | 初始现金金额 |
| `cash_balance` | DECIMAL(18,4) | 当前可用现金余额 |
| `create_at` | TIMESTAMP | 创建时间 |
| `update_at` | TIMESTAMP | 最后更新时间 |

**用途**：定义投资组合基本信息与现金基线，是持仓、交易、快照等所有业务表的根实体，级联删除时会清除所属全部子数据。

---

### 3.2 `holding` — 持仓表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT AUTO_INCREMENT | 主键 |
| `portfolio_id` | INT | 所属组合 ID（外键，级联删除） |
| `asset_type` | VARCHAR(10) | 资产类型（`STOCK` / `BOND` / `CASH`） |
| `ticker` | VARCHAR(20) | 证券代码（现金持仓可为空） |
| `quantity` | DECIMAL(18,4) | 当前持仓数量 |
| `average_price` | DECIMAL(18,4) | 加权平均成本价 |
| `current_price` | DECIMAL(18,4) | 当前市场价格 |
| `purchase_date` | DATE | 初始买入日期 |
| `currency` | VARCHAR(3) | 币种，默认 `USD` |
| `create_at` | TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | 最后更新时间 |

**用途**：描述组合内各资产的当前头寸与价格信息，数量和成本价由交易服务层在每次买卖后自动重新计算。

---

### 3.3 `transaction` — 交易流水表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT AUTO_INCREMENT | 主键 |
| `portfolio_id` | INT | 所属组合 ID（外键，级联删除） |
| `holding_id` | INT | 关联持仓 ID（外键，可为空，级联删除） |
| `type` | VARCHAR(4) | 交易类型（`BUY` / `SELL`） |
| `quantity` | DECIMAL(18,4) | 交易数量 |
| `price` | DECIMAL(18,4) | 成交价格 |
| `trade_date` | TIMESTAMP | 交易时间 |

**用途**：完整记录所有买卖操作，是持仓数量变化的唯一合法来源。交易不可修改或删除，确保历史审计链完整。

---

### 3.4 `portfolio_snapshot` — 组合日度快照表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT AUTO_INCREMENT | 主键 |
| `portfolio_id` | INT | 所属组合 ID（外键，级联删除） |
| `snapshot_date` | DATE | 快照日期 |
| `total_value` | DECIMAL(18,4) | 当日组合总价值 |
| `cash_balance` | DECIMAL(18,4) | 当日现金余额 |
| `holdings_value` | DECIMAL(18,4) | 当日持仓市值 |
| `created_at` | TIMESTAMP | 记录创建时间 |

**约束**：`(portfolio_id, snapshot_date)` 唯一，每个组合每天仅保留一条快照。

**用途**：沉淀历史绩效截面数据，为前端业绩走势图表提供逐日数据点，支持日度对比与长期趋势分析。

---

### 3.5 `price_history` — 证券价格历史表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT AUTO_INCREMENT | 主键 |
| `ticker` | VARCHAR(20) | 证券代码 |
| `price_date` | DATE | 价格日期 |
| `close_price` | DECIMAL(18,4) | 当日收盘价 |
| `create_at` | TIMESTAMP | 记录创建时间 |

**用途**：存储证券历史收盘价序列，为持仓估值、业绩计算和图表展示提供标准化价格数据；同时作为实时行情不可用时的本地价格回退源。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 4.1 |
| 数据访问 | Spring JDBC / JdbcTemplate |
| 数据库 | MySQL 8（生产）/ H2（测试） |
| 前端 | 原生 JavaScript（ES Module）+ Chart.js |
| AI 服务 | 智谱 AI（Zhipu GLM 系列模型） |
| 构建工具 | Maven |
| 运行时 | Java 17+ |

## 快速启动

**前提**：本机已安装并启动 MySQL，数据库 `portfolio_db` 可自动创建（连接 URL 含 `createDatabaseIfNotExist=true`）。

```powershell
# 构建并启动（默认端口 9009）
.\mvnw.cmd spring-boot:run
```

浏览器访问：[http://localhost:9009](http://localhost:9009)

**配置 AI 功能**（可选）：在 IntelliJ IDEA Run Configuration 的 Environment Variables 中添加：

```
ZHIPU_API_KEY=your_zhipu_api_key
```

## 运行测试

```powershell
.\mvnw.cmd clean test
```

