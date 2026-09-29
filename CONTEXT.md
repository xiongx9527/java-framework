# fw-plus Context

fw-plus 是 Java 开发框架，本文件只记录本项目领域语言中已精确定义的术语；实现细节见代码与 spec 文档。

## Web 栈

**宿主栈（Host Stack）**:
一个应用运行的 Web 栈，要么 servlet、要么 reactive；单个应用只属于其中一种，不存在双栈并存的应用。
_Avoid_: 环境、web 类型、混合栈

**栈支线（Stack Variant）**:
同一能力按宿主栈分裂出的两条实现支线，按应用所属栈二选一激活。
_Avoid_: 双实现、适配层

**双栈一致（Stack Parity）**:
一项能力在两条栈支线上行为等价的验收标准：共享同一套核心逻辑，仅 HTTP 出入口随栈适配。
_Avoid_: 兼容 webflux、支持 webflux

## swagger 插件

**文档切面（Doc Endpoint）**:
swagger 插件对外暴露接口元数据的 HTTP 端点，供外部文档系统消费。
_Avoid_: swagger 接口、doc 接口

**控制台切面（Console Tab）**:
swagger 插件在 magic-api 控制台中呈现的页签与前端资源。
_Avoid_: 前端页签、插件页面
