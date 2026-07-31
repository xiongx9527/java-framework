## 1. 远端状态与执行准备

- [x] 1.1 记录当前工作区、三个本地/跟踪分支 SHA、共同祖先和未推送提交，确认 change 产物与已有依赖清理提交的归属
- [x] 1.2 成功获取远端 `dev`、`springboot2`、`springboot3` 最新对象，重新计算每个分支的 ahead/behind 和祖先关系
- [ ] 1.3 建立固定版本断言与依赖升级排除清单，覆盖 Java/Spring 平台、Magic-API/Magic-Script、Spring AI 系列和两个 TinyEngine change
- [x] 1.4 在 Core 父 POM 的 `pluginManagement` 统一管理 `exec-maven-plugin:3.1.0`，Boot、Cloud、AI 的消费 POM 不声明插件版本
- [ ] 1.5 按用户确认的方案 A 修订 Spring Boot 3 LangChain4j 契约：主版本统一为 1.18.0，Redis/Qianfan/DashScope 例外固定为 0.36.2，并将后续派生分支同步改为代码同步后重新验证

## 2. 整合、验证并推送 dev

- [x] 2.1 将本地 `dev` 提交重放到最新 `origin/dev`，解决冲突并保留远端 LangChain4j 自动配置迁移
- [x] 2.2 检查 `dev` 的依赖清理结果，确认 LangChain4j 0.35.0 仅使用 Java 8 兼容模块且固定平台版本未变化
- [x] 2.3 按 Core → Boot → Cloud / AI → Sample 顺序完成 `dev` 的 Java 8 Maven 测试与构建验证
- [x] 2.4 提交必要的整合结果，非强制推送 `dev`，并确认远端 SHA 等于本地 HEAD
- [ ] 2.5 移除 AskData Starter 对 LangChain4j Adapter Starter 的直接依赖，以中立依赖测试和 Java 8 Maven 构建锁定边界

## 3. 合并、验证并推送 springboot2

- [x] 3.1 将本地 `springboot2` 同步到最新远端历史，再合并已验证的最新 `dev`
- [x] 3.2 解决合并冲突，保持 Java 8、既有 Spring 平台和 LangChain4j 0.35.0 基线
- [x] 3.3 按 Core → Boot → Cloud / AI → Sample 顺序完成 `springboot2` 的 Maven 测试与构建验证
- [x] 3.4 提交合并结果，非强制推送 `springboot2`，确认远端 SHA 等于本地 HEAD 且包含最新 `dev`

## 4. 合并 dev 并升级 springboot3

- [x] 4.1 将本地 `springboot3` 同步到最新远端历史，再合并最新 `dev`，保留既有 Java 17 / Spring Boot 3 适配提交
- [x] 4.2 单独提交 `springboot3` 的分支同步结果，使合并冲突修复与依赖升级保持独立
- [ ] 4.3 审计固定范围外的 POM 属性、BOM 管理项、直接依赖和 Maven 更新报告，形成兼容升级与保留旧版清单
- [ ] 4.4 在 Core BOM 导入 LangChain4j BOM 1.18.0，AI BOM 通过导入 Core BOM 继承管理；验证 AI BOM 不直接声明 `langchain4j.version` 或导入 LangChain4j BOM，三项例外由 Core BOM 显式覆盖 0.36.2，且消费模块不直接写版本
- [ ] 4.5 升级其余与 Java 17 和固定 Spring 平台兼容的依赖，删除已证明未使用的直接依赖或旧拦截器配置
- [ ] 4.6 确认 Spring AI、Spring AI Alibaba、Magic-API 2.2.2、Magic-Script 1.9.0 和三个固定 Spring 平台版本均被保留

## 5. 验证并推送 springboot3

- [ ] 5.1 在 Java 17 下按 Core → Boot → Cloud / AI → Sample 顺序完成 `springboot3` Maven 测试与构建
- [ ] 5.2 验证 LangChain4j、Spring AI、Spring AI Alibaba 的有效 POM、依赖树、依赖收敛和关键自动配置上下文
- [ ] 5.3 检查固定版本断言、无冗余 LangChain4j 版本、无意外 TinyEngine 变更以及 `git diff --check`
- [ ] 5.4 提交 `springboot3` 依赖升级，非强制推送并确认远端 SHA 等于本地 HEAD

## 6. 最终一致性核验

- [ ] 6.1 独立读取三个远端分支 SHA，确认分别匹配已推送的本地提交
- [ ] 6.2 确认最终 `dev` 是 `springboot2` 与 `springboot3` 的祖先，且两个派生分支原有独立提交仍存在
- [ ] 6.3 确认工作区干净，并汇总三个分支的提交、构建结果、保留旧版原因和任何剩余风险
- [ ] 6.4 最终 dev 同步包含 AskData 生产代码、POM、测试和契约文档；springboot2 以 Java 8、springboot3 以 Java 17 重新完成 AskData 与受影响自动配置验证，springboot3 另验证 LangChain4j 1.18.0 与三项 0.36.2 管理，并核对远端 SHA、`0 0` 与祖先关系
