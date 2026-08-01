# Brainstorm Summary

- Change: upgrade-springboot3-dependencies-and-sync-branches
- Date: 2026-07-30

## 确认的技术方案

- 主工作区固定在 dev，作为 Comet 状态、规划产物和最终证据的事实源。
- 成功获取三个最新远端分支后，先将本地 dev 提交重放到 origin/dev 并验证、非强制推送。
- springboot2 与 springboot3 使用独立 Git worktree。前者没有独立提交时允许快进；后者保留 Boot 3 独立历史并建立真实合并提交。
- springboot3 的分支同步与依赖升级使用独立提交；最终 Comet 纯文档提交从 dev 再同步到两个派生分支。
- dev 恢复 spring-ai.version=1.0.1 和 spring-ai-alibaba.version=1.0.1，不新增 Spring AI 模块。
- springboot3 的 Core BOM 与 AI Boot BOM 使用 langchain4j.version=1.18.1 并导入官方 BOM；受 BOM 管理的 LangChain4j artifact 删除显式版本。
- 核心稳定模块必须解析为 1.18.1，starter、向量库、解析器等集成模块必须解析为 1.18.1-beta28。
- 其他依赖只升级到兼容的最新 GA 稳定版；允许完成升级所必需的小范围源码或自动配置适配。

## 关键取舍与风险

- 独立 worktree 增加少量目录管理成本，但隔离跨分支未提交状态、构建产物和冲突处理，优于单工作区反复切换。
- Core BOM 与 AI Boot BOM 均保持自包含的 LangChain4j BOM 导入，存在同一版本属性的层级重复；通过固定版本断言和 effective POM 检查防止漂移。
- 若新依赖要求改变公开 API、固定 Spring 平台或大范围重构，则保留当前兼容版本并记录原因。
- 无最新远端对象、祖先关系不明确、更新不是快进或目标提交对象不在远端时，停止远端更新。
- LangChain4j beta 集成模块可能与固定 Spring Boot 3.5.8 存在自动配置差异，必须以实际编译、测试和上下文启动结果判定。

## 测试策略

- 在 change 的 .comet/evidence/ 下记录依赖审计矩阵、分支同步证据和构建结果。
- dev、springboot2 使用 Java 8；springboot3 使用 Java 17；每个分支按 Core → Boot → Cloud / AI → Sample 顺序构建。
- 验证 effective POM、依赖树、固定版本、LangChain4j 版本轨道、Spring AI 属性以及关键自动配置上下文。
- 将失败区分为远端传输、artifact 解析、编译 API、测试/上下文和依赖收敛五类，只修复本次范围内的问题。
- 推送后独立读取远端 SHA 并验证最终 dev 是两个派生分支的祖先；最后运行 OpenSpec 严格校验、git diff --check 和工作区检查。

## Spec Patch

- 已补充：springboot2 仍为 dev 祖先时允许快进同步。
- 已修正：Spring AI 要求为保留现有 1.0.1 版本属性和最新远端已有集成声明，不要求创建新模块。
- 已补充：兼容升级允许必要的小范围源码或自动配置适配；需要改变公开 API、固定平台或无关重构时保留旧版。
