# atun-brain 个人第二大脑系统设计

## 📋 一、项目概述

### 1.1 项目背景
基于 `atun-brain-component` 框架，开发一个个人智能助手系统 "atun-brain"，作为用户的第二大脑，辅助日常决策、记忆管理和财务规划。

### 1.2 核心定位
- **智能决策助手**：基于个人历史数据和偏好，提供决策建议
- **个人记忆仓库**：自动记录、整理和检索个人知识与经历
- **财务智能管家**：自动化记账、分析和预算管理
- **个性化 AI Agent**：越用越懂你的个人助手

### 1.3 设计原则
- **隐私优先**：所有数据本地存储，可选云端同步
- **渐进式增强**：从 MVP 开始，逐步完善功能
- **高度可定制**：支持用户自定义工具、规则和偏好
- **离线优先**：核心功能支持离线使用

---

## 🏗️ 二、系统架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                     atun-brain 应用层                             │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │智能决策  │  │记忆仓库  │  │个人记账  │  │系统管理  │       │
│  │模块     │  │模块     │  │模块     │  │模块     │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
├─────────────────────────────────────────────────────────────────┤
│                 atun-brain-component 框架层                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │            AgentOrchestrator (四阶段 Pipeline)           │   │
│  │  ┌──────────┬──────────┬──────────┬──────────┐          │   │
│  │  │意图分类  │工具编排  │响应合成  │记忆持久  │          │   │
│  │  │          │          │          │          │          │   │
│  │  └──────────┴──────────┴──────────┴──────────┘          │   │
│  └─────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                    工具提供者层 (ToolProvider)                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │决策工具  │  │记忆工具  │  │记账工具  │  │第三方工具 │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
├─────────────────────────────────────────────────────────────────┤
│                       数据存储层                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │MySQL     │  │Qdrant    │  │文件系统  │  │Redis     │       │
│  │(结构化)  │  │(向量)    │  │(文档)    │  │(缓存)    │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 技术栈选型

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| **运行时** | Java 21 + Spring Boot 3.2.2 | 与框架保持一致 |
| **AI框架** | LangChain4j 1.0.0-beta2 | 原生支持 |
| **向量数据库** | Qdrant 1.11.0 | 原生支持 |
| **关系数据库** | MySQL 8.0 | 事务性数据存储 |
| **缓存** | Redis 7.x | 会话缓存、短期记忆 |
| **文档存储** | MinIO / 本地文件系统 | 个人文档、附件 |
| **前端** | Vue 3 + TypeScript | 可选的 Web 管理界面 |
| **移动端** | Flutter (可选) | 跨平台移动应用 |
| **部署** | Docker + Docker Compose | 容器化部署 |

---

## 🧠 三、核心功能模块设计

### 3.1 智能决策模块

#### 3.1.1 功能范围
```
智能决策模块
├── 决策场景识别
│   ├── 生活决策（购物、出行、娱乐选择）
│   ├── 财务决策（投资、消费、预算分配）
│   ├── 时间决策（任务优先级、日程安排）
│   └── 职业决策（学习方向、工作选择）
├── 偏好学习系统
│   ├── 显式偏好（用户直接设置的规则）
│   ├── 隐式偏好（从历史行为中学习）
│   ├── 权重调整（动态优化决策因子）
│   └── 偏好冲突检测与解决
├── 决策分析引擎
│   ├── 多方案对比（成本、收益、风险）
│   ├── SWOT分析自动化
│   ├── 决策树生成
│   └── 概率评估与置信度计算
├── 建议生成器
│   ├── 推荐最优方案
│   ├── 备选方案说明
│   ├── 风险预警与规避建议
│   └── 执行步骤分解
└── 决策记录与反馈
    ├── 决策历史追溯
    ├── 结果反馈收集
    └── 决策质量评估与优化
```

#### 3.1.2 数据模型

```java
// 决策请求
public record DecisionRequest(
    Long userId,
    String requestId,
    DecisionScenario scenario,      // 决策场景
    String description,             // 问题描述
    List<DecisionOption> options,   // 可选方案
    List<DecisionFactor> factors,   // 决策因子
    Instant timestamp
) {}

// 决策场景枚举
public enum DecisionScenario {
    PURCHASE,       // 购物决策
    TRAVEL,         // 出行决策
    FINANCE,        // 财务决策
    TIME_MANAGE,    // 时间管理
    CAREER,         // 职业发展
    HEALTH,         // 健康相关
    ENTERTAINMENT   // 娱乐选择
}

// 决策因子
public record DecisionFactor(
    String name,        // 因子名称（如：价格、质量、时间）
    Double weight,      // 权重（0-1）
    FactorType type     // 类型：COST(成本类) / BENEFIT(收益类)
) {}

// 决策结果
public record DecisionResult(
    String requestId,
    DecisionOption recommendedOption,   // 推荐方案
    List<OptionScore> optionScores,     // 各方案评分
    DecisionConfidence confidence,      // 置信度
    List<String> considerations,        // 考虑因素
    List<String> risks,                 // 风险提示
    String advice                       // 具体建议
) {}
```

#### 3.1.3 核心工具接口设计

```java
/**
 * 决策工具提供者
 */
@Component
public class DecisionToolProvider implements ToolProvider {

    private final DecisionAnalysisService analysisService;
    private final PreferenceLearningService preferenceService;

    @Override
    public String getGroupName() {
        return "decision-tools";
    }

    @Override
    public List<Object> getToolObjects() {
        return List.of(
            new DecisionAnalyzerTool(analysisService),
            new PreferenceManagerTool(preferenceService)
        );
    }

    @Override
    public int getOrder() {
        return 50; // 高优先级
    }
}

/**
 * 决策分析工具
 */
public class DecisionAnalyzerTool {

    private final DecisionAnalysisService service;

    /**
     * 分析决策问题并提供建议
     *
     * @param userId 用户ID
     * @param question 决策问题描述
     * @param options 可选方案列表，格式：方案1,方案2,方案3
     * @param factors 决策因子及权重，格式：因子1(权重),因子2(权重)
     * @return 决策分析结果
     */
    @Tool("分析用户的决策问题，对比各方案优劣，提供建议")
    public String analyzeDecision(
            @V("userId") Long userId,
            @V("question") String question,
            @V("options") String options,
            @V("factors") String factors
    ) {
        // 解析输入并调用分析引擎
        // 返回结构化建议
    }

    /**
     * 获取用户的决策偏好历史
     *
     * @param userId 用户ID
     * @return 偏好历史摘要
     */
    @Tool("查询用户的历史决策偏好和选择模式")
    public String getDecisionHistory(@V("userId") Long userId) {
        // 从数据库查询历史决策
    }
}
```

#### 3.1.4 实现要点
- 使用 RAG 从历史决策记录中检索相似案例
- 基于用户反馈动态调整决策因子权重
- 支持用户对建议进行确认/拒绝，持续优化模型

---

### 3.2 个人记忆仓库模块

#### 3.2.1 功能范围
```
个人记忆仓库
├── 记忆类型管理
│   ├── 对话记忆（与AI的所有对话）
│   ├── 事件记忆（重要经历、成就）
│   ├── 知识记忆（学习的技能、知识点）
│   ├── 人物记忆（社交关系、重要他人）
│   ├── 地点记忆（常去地点、旅行记录）
│   └── 媒体记忆（照片、文档、链接收藏）
├── 记忆存储与索引
│   ├── 短期记忆（最近对话，ChatMemory）
│   ├── 长期记忆（向量化存储，RAG）
│   ├── 元数据管理（时间、地点、人物、情绪）
│   └── 自动摘要生成
├── 记忆检索
│   ├── 语义搜索（自然语言查询）
│   ├── 时空过滤（时间范围、地理位置）
│   ├── 人物/标签筛选
│   └── 关联记忆推荐
├── 记忆整理
│   ├── 自动分类与打标
│   ├── 记忆摘要生成
│   ├── 记忆合并与去重
│   └── 记忆遗忘策略（可配置）
└── 记忆可视化
    ├── 时间线视图
    ├── 关系图谱
    ├── 统计图表
    └── 记忆地图（地理分布）
```

#### 3.2.2 数据模型

```java
// 记忆条目
public record MemoryEntry(
    Long id,
    Long userId,
    MemoryType type,            // 记忆类型
    String content,             // 记忆内容
    String summary,             // 摘要（自动生成）
    List<String> tags,          // 标签
    Instant createdAt,          // 创建时间
    Instant occurredAt,         // 事件发生时间
    Location location,          // 地理位置
    List<Person> relatedPersons,// 相关人物
    Map<String, Object> metadata, // 元数据
    Integer importance,         // 重要性（1-10）
    String embeddingId          // 向量ID
) {}

public enum MemoryType {
    CONVERSATION,   // 对话
    EVENT,          // 事件
    KNOWLEDGE,      // 知识
    PERSON,         // 人物
    LOCATION,       // 地点
    MEDIA,          // 媒体
    NOTE            // 笔记
}

// 记忆查询请求
public record MemoryQuery(
    Long userId,
    String keyword,             // 关键词
    MemoryType type,            // 类型过滤
    Instant timeRangeStart,     // 时间范围开始
    Instant timeRangeEnd,       // 时间范围结束
    Integer limit,              // 结果数量
    Double similarityThreshold  // 相似度阈值
) {}
```

#### 3.2.3 核心工具接口设计

```java
@Component
public class MemoryToolProvider implements ToolProvider {

    private final MemoryService memoryService;
    private final MemorySearchService searchService;

    @Override
    public String getGroupName() {
        return "memory-tools";
    }

    @Override
    public List<Object> getToolObjects() {
        return List.of(
            new MemoryQueryTool(searchService),
            new MemoryRecordTool(memoryService)
        );
    }
}

public class MemoryQueryTool {

    private final MemorySearchService searchService;

    /**
     * 查询个人记忆
     *
     * @param userId 用户ID
     * @param query 查询关键词或问题
     * @param type 记忆类型过滤（可选），如：event, knowledge, conversation
     * @param timeRange 时间范围（可选），如：today, yesterday, this_week, last_month, 2026-02
     * @return 记忆查询结果
     */
    @Tool("根据关键词或问题从用户的个人记忆仓库中检索相关信息")
    public String queryMemory(
            @V("userId") Long userId,
            @V("query") String query,
            @V("type") String type,
            @V("timeRange") String timeRange
    ) {
        // 执行语义搜索 + 过滤
        // 返回结构化的记忆片段
    }

    /**
     * 获取最近的记忆摘要
     *
     * @param userId 用户ID
     * @param days 天数
     * @return 记忆摘要
     */
    @Tool("获取用户最近几天的记忆摘要，用于上下文参考")
    public String getRecentMemorySummary(
            @V("userId") Long userId,
            @V("days") Integer days
    ) {
        // 汇总最近对话和事件
    }
}

public class MemoryRecordTool {

    private final MemoryService memoryService;

    /**
     * 记录重要事件或知识
     *
     * @param userId 用户ID
     * @param content 事件或知识内容
     * @param type 记忆类型：event, knowledge, note
     * @param tags 标签（逗号分隔）
     * @return 记录结果
     */
    @Tool("当用户提到重要事件、新知识或需要记录的信息时，自动保存到记忆仓库")
    public String recordMemory(
            @V("userId") Long userId,
            @V("content") String content,
            @V("type") String type,
            @V("tags") String tags
    ) {
        // 自动提取元数据（时间、人物等）
        // 生成摘要并存入向量库
    }
}
```

#### 3.2.4 实现要点
- **自动化**：通过监听对话自动识别重要信息并记录
- **隐私保护**：敏感信息自动识别和加密存储
- **记忆压缩**：定期对相似记忆进行合并和摘要
- **时空索引**：支持基于时间和地理位置的记忆检索

---

### 3.3 个人记账模块

#### 3.3.1 功能范围
```
个人记账模块
├── 交易管理
│   ├── 快速记账（语音/文本输入）
│   ├── 批量导入（银行账单、CSV）
│   ├── 交易编辑与删除
│   └── 交易分类与标签
├── 分类管理
│   ├── 收入分类（工资、投资、其他）
│   ├── 支出分类（餐饮、交通、购物、娱乐等）
│   ├── 自定义分类
│   └── 分类规则（自动分类）
├── 统计分析
│   ├── 收支趋势（日/周/月/年）
│   ├── 分类占比（饼图）
│   ├── 同比/环比分析
│   ├── 消费习惯分析
│   └── 财务健康度评估
├── 预算管理
│   ├── 月度/年度预算设置
│   ├── 预算使用进度
│   ├── 超支预警
│   └── 预算建议
├── 报表生成
│   ├── 月度报表
│   ├── 年度总结
│   ├── 税务相关报表
│   └── 自定义报表
└── 智能辅助
    ├── 异常消费检测
    ├── 节省建议
    ├── 投资建议
    └── 财务目标追踪
```

#### 3.3.2 数据模型

```java
// 交易实体
@Table("atb_transaction")
public class Transaction {
    @Id
    private Long id;
    private Long userId;
    private TransactionType type;       // EXPENSE/INCOME
    private BigDecimal amount;          // 金额
    private String description;         // 描述
    private Long categoryId;            // 分类ID
    private String categoryName;        // 分类名称（冗余）
    private LocalDateTime transactionTime; // 交易时间
    private LocalDateTime createdAt;    // 记录时间
    private String tags;                // 标签（逗号分隔）
    private String location;            // 交易地点
    private String payMethod;           // 支付方式
    private String remark;              // 备注
}

public enum TransactionType {
    EXPENSE,    // 支出
    INCOME      // 收入
}

// 分类实体
@Table("atb_category")
public class Category {
    @Id
    private Long id;
    private Long userId;
    private String name;
    private CategoryType type;          // EXPENSE/INCOME
    private Long parentId;              // 父分类
    private String icon;                // 图标
    private Integer sort;               // 排序
    private Boolean isDefault;          // 是否默认
    private String color;               // 颜色
}

// 预算实体
@Table("atb_budget")
public class Budget {
    @Id
    private Long id;
    private Long userId;
    private String name;                // 预算名称
    private BudgetPeriod period;        // 周期：MONTH/QUARTER/YEAR
    private Integer year;               // 年份
    private Integer month;              // 月份（月度预算）
    private BigDecimal amount;          // 预算金额
    private BigDecimal spent;           // 已花费
    private LocalDateTime startDate;    // 开始时间
    private LocalDateTime endDate;      // 结束时间
}

public enum BudgetPeriod {
    MONTH, QUARTER, YEAR
}

// 统计查询参数
public record TransactionStatsQuery(
    Long userId,
    TransactionType type,           // 支出/收入
    Instant startTime,              // 开始时间
    Instant endTime,                // 结束时间
    List<Long> categoryIds,         // 分类筛选
    StatsGroupBy groupBy            // 按天/周/月/年分组
) {}

public enum StatsGroupBy {
    DAY, WEEK, MONTH, YEAR, CATEGORY
}
```

#### 3.3.3 核心工具接口设计

```java
@Component
public class FinanceToolProvider implements ToolProvider {

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final StatsService statsService;

    @Override
    public String getGroupName() {
        return "finance-tools";
    }

    @Override
    public List<Object> getToolObjects() {
        return List.of(
            new TransactionManagerTool(transactionService, categoryService),
            new FinanceStatsTool(statsService)
        );
    }
}

public class TransactionManagerTool {

    private final TransactionService transactionService;
    private final CategoryService categoryService;

    /**
     * 记录一笔交易
     *
     * @param userId 用户ID
     * @param amount 金额
     * @param type 交易类型：expense（支出）或 income（收入）
     * @param description 交易描述
     * @param category 分类名称（可选），如：餐饮、交通、工资
     * @param time 交易时间（可选），格式：yyyy-MM-dd HH:mm 或 today/yesterday
     * @return 记账结果
     */
    @Tool("记录用户的收支交易，支持自动分类")
    public String recordTransaction(
            @V("userId") Long userId,
            @V("amount") BigDecimal amount,
            @V("type") String type,
            @V("description") String description,
            @V("category") String category,
            @V("time") String time
    ) {
        // 智能识别分类
        // 保存交易记录
        return "已记录：" + type + " " + amount + " 元 - " + description;
    }

    /**
     * 查询交易记录
     *
     * @param userId 用户ID
     * @param type 交易类型：expense, income, all（可选）
     * @param timeRange 时间范围（可选），如：today, yesterday, this_week, this_month, last_month, 2026-02
     * @param category 分类筛选（可选）
     * @param limit 返回条数（可选，默认10）
     * @return 交易记录列表
     */
    @Tool("查询用户的交易记录，支持按时间、类型、分类筛选")
    public String queryTransactions(
            @V("userId") Long userId,
            @V("type") String type,
            @V("timeRange") String timeRange,
            @V("category") String category,
            @V("limit") Integer limit
    ) {
        // 解析时间范围
        // 查询并格式化结果
    }

    /**
     * 搜索交易分类
     *
     * @param userId 用户ID
     * @param keyword 分类关键词
     * @return 匹配的分类列表
     */
    @Tool("根据关键词搜索交易分类")
    public String searchCategories(
            @V("userId") Long userId,
            @V("keyword") String keyword
    ) {
        return categoryService.search(keyword);
    }
}

public class FinanceStatsTool {

    private final StatsService statsService;

    /**
     * 获取收支统计摘要
     *
     * @param userId 用户ID
     * @param timeRange 统计时间范围，如：today, this_week, this_month, this_year
     * @return 统计摘要
     */
    @Tool("获取用户的收支统计摘要，包括总支出、总收入、结余等")
    public String getStatsSummary(
            @V("userId") Long userId,
            @V("timeRange") String timeRange
    ) {
        // 计算统计指标
        // 返回格式化的摘要
    }

    /**
     * 获取分类统计
     *
     * @param userId 用户ID
     * @param timeRange 时间范围
     * @param type 统计类型：expense（支出）或 income（收入）
     * @return 分类占比统计
     */
    @Tool("按分类统计用户的支出或收入分布")
    public String getCategoryStats(
            @V("userId") Long userId,
            @V("timeRange") String timeRange,
            @V("type") String type
    ) {
        // 按分类聚合
        // 返回占比数据
    }

    /**
     * 计算交易总金额
     *
     * @param userId 用户ID
     * @param type 交易类型：expense 或 income
     * @param timeRange 时间范围
     * @param category 分类筛选（可选）
     * @return 总金额
     */
    @Tool("计算指定条件下交易的总金额")
    public String calculateTotalAmount(
            @V("userId") Long userId,
            @V("type") String type,
            @V("timeRange") String timeRange,
            @V("category") String category
    ) {
        return statsService.calculateTotal(type, timeRange, category).toString();
    }
}
```

#### 3.3.4 现有代码适配
当前框架中的 `DefaultToolOrchestrator` 已经包含了财务助手的基础配置，需要：

1. **更新 System Prompt**：从单一的财务助手扩展为多功能 Agent
2. **增加工具**：除了记账工具，增加决策工具和记忆工具
3. **意图分类优化**：扩展 `DefaultIntentClassifier` 支持更多场景识别

---

## 📅 四、项目实施路线

### 4.1 阶段划分

#### **Phase 1: MVP 核心功能（2-3周）**

**目标**：搭建基础框架，实现核心记账功能

**任务清单**：
- [ ] 创建新项目 `atun-brain`，集成 `atun-brain-component`
- [ ] 配置数据库（MySQL）和向量库（Qdrant）
- [ ] 实现交易管理基础 CRUD
- [ ] 实现分类管理
- [ ] 实现基础统计查询（日/周/月）
- [ ] 创建 `FinanceToolProvider` 工具
- [ ] 测试基本对话记账功能
- [ ] 开发简单 Web 管理界面（可选）

**验收标准**：
- 用户可以通过自然语言记账："今天花了50元吃午饭"
- 用户可以查询交易："这个月花了多少钱？"
- 基础统计报表可查看

---

#### **Phase 2: 记忆仓库集成（2-3周）**

**目标**：实现个人记忆的自动记录和检索

**任务清单**：
- [ ] 设计记忆数据模型和存储方案
- [ ] 实现 `MemoryService` 和 `MemorySearchService`
- [ ] 创建 `MemoryToolProvider` 工具
- [ ] 实现对话自动记忆功能
- [ ] 实现语义搜索功能
- [ ] 集成 RAG 到响应合成流程
- [ ] 实现记忆摘要生成
- [ ] 测试记忆检索："我上周说了什么？"

**验收标准**：
- 对话自动保存到记忆仓库
- 可以通过自然语言检索历史记忆
- 记忆能够增强对话上下文

---

#### **Phase 3: 智能决策模块（3-4周）**

**目标**：实现基于个人数据的智能决策辅助

**任务清单**：
- [ ] 设计决策分析引擎架构
- [ ] 实现决策因子权重管理系统
- [ ] 创建 `DecisionToolProvider` 工具
- [ ] 实现偏好学习算法
- [ ] 集成历史数据检索（财务、记忆）
- [ ] 实现多方案对比分析
- [ ] 实现决策建议生成
- [ ] 测试决策场景："我应该买iPhone还是华为？"

**验收标准**：
- 可以处理简单的决策问题
- 建议会参考用户历史偏好
- 支持用户反馈优化

---

#### **Phase 4: 高级功能与优化（3-4周）**

**目标**：完善功能，提升用户体验

**任务清单**：
- [ ] 预算管理和预警功能
- [ ] 消费异常检测
- [ ] 记忆可视化界面（时间线、关系图）
- [ ] 数据导出和备份功能
- [ ] 用户偏好配置界面
- [ ] 性能优化（缓存、索引）
- [ ] 安全加固（数据加密、权限控制）
- [ ] 完整测试和文档

**验收标准**：
- 所有核心功能稳定运行
- 用户界面友好易用
- 性能满足日常使用需求

---

#### **Phase 5: 扩展与集成（可选）**

**目标**：扩展生态和第三方集成

**任务清单**：
- [ ] 移动端应用开发
- [ ] 浏览器插件（网页收藏记忆）
- [ ] 第三方服务集成（银行账单导入、日历同步）
- [ ] 语音助手集成
- [ ] 数据分析和报告生成
- [ ] 社交分享功能（可选）

---

### 4.2 技术实施方案

#### **第1步：项目初始化**
```bash
# 创建新项目
mkdir atun-brain
cd atun-brain

# 初始化 Maven 项目
mvn archetype:generate -DgroupId=com.atun.brain -DartifactId=atun-brain -Dversion=1.0-SNAPSHOT

# 添加依赖到 pom.xml
# - atun-brain-component 模块
# - Spring Boot Starter Web
# - MyBatis Plus
# - MySQL Connector
# - Redis
```

#### **第2步：数据库设计**
```sql
-- 创建数据库
CREATE DATABASE atun_brain DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 用户表
CREATE TABLE atb_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100),
    password_hash VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 交易表
CREATE TABLE atb_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL, -- EXPENSE/INCOME
    amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(500),
    category_id BIGINT,
    category_name VARCHAR(100),
    transaction_time DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    tags VARCHAR(500),
    location VARCHAR(200),
    pay_method VARCHAR(50),
    remark TEXT,
    INDEX idx_user_time (user_id, transaction_time),
    INDEX idx_user_category (user_id, category_id)
);

-- 记忆表
CREATE TABLE atb_memory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL, -- CONVERSATION/EVENT/KNOWLEDGE
    content TEXT NOT NULL,
    summary TEXT,
    tags VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    occurred_at DATETIME,
    location VARCHAR(200),
    importance INT DEFAULT 5,
    embedding_id VARCHAR(100), -- 向量ID
    metadata JSON,
    INDEX idx_user_type_time (user_id, type, created_at),
    INDEX idx_embedding (embedding_id)
);

-- 决策记录表
CREATE TABLE atb_decision (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    scenario VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    options JSON,
    factors JSON,
    result JSON,
    feedback INT, -- 1:满意, -1:不满意, 0:未反馈
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, created_at)
);
```

#### **第3步：核心代码结构**
```
atun-brain/
├── src/main/java/com/atun/brain/
│   ├── AtunBrainApplication.java          # Spring Boot 启动类
│   ├── config/                            # 配置类
│   │   ├── DatabaseConfig.java
│   │   ├── RedisConfig.java
│   │   └── AiModelConfig.java
│   ├── controller/                        # Web控制器
│   │   ├── ChatController.java
│   │   ├── TransactionController.java
│   │   ├── MemoryController.java
│   │   └── StatsController.java
│   ├── service/                           # 业务服务
│   │   ├── finance/                       # 记账服务
│   │   │   ├── TransactionService.java
│   │   │   ├── CategoryService.java
│   │   │   └── StatsService.java
│   │   ├── memory/                        # 记忆服务
│   │   │   ├── MemoryService.java
│   │   │   ├── MemorySearchService.java
│   │   │   └── MemorySummaryService.java
│   │   └── decision/                      # 决策服务
│   │       ├── DecisionAnalysisService.java
│   │       └── PreferenceLearningService.java
│   ├── repository/                        # 数据访问
│   │   ├── TransactionMapper.java
│   │   ├── CategoryMapper.java
│   │   ├── MemoryMapper.java
│   │   └── DecisionMapper.java
│   ├── tools/                             # 工具提供者
│   │   ├── FinanceToolProvider.java
│   │   ├── MemoryToolProvider.java
│   │   ├── DecisionToolProvider.java
│   │   └── ToolRouter.java                # 工具路由增强
│   └── model/                             # 数据模型
│       ├── entity/                        # 实体类
│       ├── dto/                           # 数据传输对象
│       └── vo/                            # 视图对象
├── src/main/resources/
│   ├── application.yml                    # 应用配置
│   ├── application-dev.yml                # 开发环境配置
│   ├── mapper/                            # MyBatis映射文件
│   └── static/                            # 静态资源
└── pom.xml
```

---

### 4.3 风险评估与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| **AI模型成本高** | 高 | 1. 支持多模型切换（阿里云百炼性价比高）<br>2. 实现缓存机制减少重复调用 |
| **隐私数据安全** | 高 | 1. 本地化部署为主<br>2. 敏感数据加密存储<br>3. 严格的权限控制 |
| **数据量增长快** | 中 | 1. 定期归档旧数据<br>2. 向量索引优化<br>3. 支持数据导出和清理 |
| **决策准确性** | 中 | 1. 明确标注"建议仅供参考"<br>2. 支持用户反馈优化模型 |
| **用户粘性低** | 中 | 1. 持续优化用户体验<br>2. 增加有价值的功能（预算预警、消费分析） |

---

## 🎯 五、总结与建议

### 5.1 项目可行性评估
**✅ 完全可行**

基于 `atun-brain-component` 框架，该项目具备以下优势：
- 框架已提供完整的 Pipeline 基础设施
- 支持工具扩展和自定义配置
- 技术栈成熟且有良好文档
- 可以从 MVP 开始逐步迭代

### 5.2 关键成功因素
1. **聚焦核心功能**：先做好记账和记忆，再扩展决策
2. **用户体验优先**：对话流畅性、响应速度是关键
3. **数据质量**：准确的分类、标签系统
4. **持续优化**：基于用户反馈迭代改进

### 5.3 下一步行动建议

**立即可以开始的工作**：
1. 创建 `atun-brain` 项目，搭建基础框架
2. 实现用户系统和数据库初始化
3. 开发基础的交易管理功能
4. 集成第一个工具 `FinanceToolProvider`
5. 测试端到端的对话记账流程

**需要决策的事项**：
1. **部署方式**：纯本地部署还是支持云端同步？
2. **前端形态**：Web界面、桌面应用还是移动端优先？
3. **模型选择**：阿里云百炼还是 OpenAI？是否支持本地模型？
4. **开源策略**：是否开源？如何保护用户数据？

---

## 📝 附录：快速启动检查清单

- [ ] 安装 Java 21 和 Maven
- [ ] 安装 MySQL 8.0
- [ ] 安装 Qdrant（Docker 运行）
- [ ] 安装 Redis（可选，用于缓存）
- [ ] 准备 AI 模型 API Key（阿里云百炼或 OpenAI）
- [ ] 创建项目基础结构
- [ ] 配置数据库连接
- [ ] 运行第一个测试对话

---

**设计完成时间**：2026年2月25日

**下一步**：根据本设计方案开始编码实现，建议从 Phase 1 的 MVP 核心功能开始。
