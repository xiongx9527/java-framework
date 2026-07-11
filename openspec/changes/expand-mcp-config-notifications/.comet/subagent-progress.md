# Subagent Progress

- Change: expand-mcp-config-notifications
- Phase: build
- Build mode: subagent-driven-development
- TDD mode: tdd
- Review mode: standard

## Current Task

- Plan task: Task 1: 固定新增精确 key 与忽略边界测试
- OpenSpec mapping: 2.1 为新增覆盖 key 编写 `McpConfigurationChangeNotifierTest` 失败用例，覆盖对应 tools/resources/prompts 或能力视图刷新通知。
- Stage: done
- Implementer: DONE
- Reviewer: skipped (standard review; no risk signal)
- Review/fix round: 0
- Risk signals: none
- RED evidence: `McpConfigurationChangeNotifierTest` failed as expected; Tests run: 13, Failures: 3; endpoint/session-transport/trusted-origins actual methods were empty.
- GREEN evidence: deferred to Task 2 by design; Task 1 is RED test task.
- Commits: 1947dfb
