# Task 13 实现报告：多提问、记忆和推荐默认桥接

## 范围

- Base commit：`e4d75ee592fa24810eea2e748c7b081985de388e`
- 分支：`feature/20260727/rework-ai-askdata-core-starter`
- 未修改 Core AskData 模块、OpenSpec 进度或既有无关脏文件。

## RED

先新增 `AskDataMultiQuestionFlowTest`，再运行：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=DefaultAskDataFlowExecutorTest,AskDataResumeFlowTest,AskDataMultiQuestionFlowTest test
```

结果为预期失败，测试可编译且出现 4 个断言失败、0 个错误：

- 显式两问题请求未暴露已审计的 `childResults`。
- 部分失败时未保留独立子结果。
- 未提供长期记忆默认桥接，无法按项目和操作人检索。
- 未提供推荐默认桥接，无法按项目和操作人保存及读取。

## GREEN

实现最小默认桥接：

- `options.questions` 明确含有两个及以上非空问题时，`DefaultAskDataFlowExecutor` 为每个问题复制相同的项目、操作人和请求参数，独立执行并在子结果添加无敏感信息的 `child-audit` 步骤；聚合结果保留所有子结果，部分失败不会中断其他子问题。
- 没有显式多问题列表时仍走原单问题流程，既有步骤顺序和成功语义不变。
- 新增 `AskDataMemoryRecallAdapter` 和 `AskDataRecommendationAdapter`，直接将完整 `AskDataProjectContext` 传给 Core SPI；自动配置提供可替换的单节点内存默认 Bean。
- `AskDataResult` 新增空列表默认的 `childResults`，避免对单问题调用方引入空值。

再次运行相同命令，构建成功：

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Surefire 实际运行了 `DefaultAskDataFlowExecutorTest` 的 12 项及 `AskDataMultiQuestionFlowTest` 的 4 项；筛选参数中的 `AskDataResumeFlowTest` 未被 Surefire 发现可执行用例，故本任务未将其计入通过数量。

## 风险与边界

- 多提问：已覆盖两个已审计子结果和一个成功、一个失败的局部失败场景；未实现复杂问题自动拆解或多智能体编排。
- 记忆隔离：已覆盖同项目不同操作人的长期记忆过滤，适配器不提供跨操作人或项目回退。
- 推荐隔离：已覆盖同项目不同操作人的推荐读写，默认适配器仅使用个人推荐接口。
- 公共 flow：既有单问题测试继续通过；多问题聚合不合并子问题 SQL、模型响应或异常细节。
- 监控：每个子问题仍经过原单问题生命周期事件路径；新增审计步骤只记录通用状态文本。
