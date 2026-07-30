## 1. 远端状态与执行准备

- [x] 1.1 记录当前工作区、三个本地/跟踪分支 SHA、共同祖先和未推送提交，确认 change 产物与已有依赖清理提交的归属
- [x] 1.2 成功获取远端 `dev`、`springboot2`、`springboot3` 最新对象，重新计算每个分支的 ahead/behind 和祖先关系
- [ ] 1.3 建立固定版本断言与依赖升级排除清单，覆盖 Java/Spring 平台、Magic-API/Magic-Script、Spring AI 系列和两个 TinyEngine change

## 2. 整合、验证并推送 dev

- [x] 2.1 将本地 `dev` 提交重放到最新 `origin/dev`，解决冲突并保留远端 LangChain4j 自动配置迁移
- [x] 2.2 检查 `dev` 的依赖清理结果，确认 LangChain4j 0.35.0 仅使用 Java 8 兼容模块且固定平台版本未变化
- [x] 2.3 按 Core → Boot → Cloud / AI → Sample 顺序完成 `dev` 的 Java 8 Maven 测试与构建验证
- [x] 2.4 提交必要的整合结果，非强制推送 `dev`，并确认远端 SHA 等于本地 HEAD

## 3. 合并、验证并推送 springboot2

- [x] 3.1 将本地 `springboot2` 同步到最新远端历史，再合并已验证的最新 `dev`
- [x] 3.2 解决合并冲突，保持 Java 8、既有 Spring 平台和 LangChain4j 0.35.0 基线
- [x] 3.3 按 Core → Boot → Cloud / AI → Sample 顺序完成 `springboot2` 的 Maven 测试与构建验证
- [x] 3.4 提交合并结果，非强制推送 `springboot2`，确认远端 SHA 等于本地 HEAD 且包含最新 `dev`

## 4. 合并 dev 并升级 springboot3

- [x] 4.1 将本地 `springboot3` 同步到最新远端历史，再合并最新 `dev`，保留既有 Java 17 / Spring Boot 3 适配提交
- [x] 4.2 单独提交 `springboot3` 的分支同步结果，使合并冲突修复与依赖升级保持独立
- [ ] 4.3 审计固定范围外的 POM 属性、BOM 管理项、直接依赖和 Maven 更新报告，形成兼容升级与保留旧版清单
- [ ] 4.4 导入 LangChain4j BOM 1.18.1，移除冗余逐模块版本，并验证核心为 1.18.1、集成模块为 1.18.1-beta28
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
