## Context

`fw-plus-boot-autoconfigure` 模块当前在 `com.xiongx9527.fw.plus.boot.autoconfigure` 包下并存三个 MyBatis 相关子包：

- `mybatis.datachange`：MyBatis 通用数据变更追踪自动配置（已存在，4 个类）
- `mp`：MyBatis-Plus 自动配置（7 个类）
- `mf`：MyBatis-Flex 自动配置（2 个类）

`mp` / `mf` 两个包中的类名均带 `Mp` / `Mf` 前缀，移动到 `mybatis` 包下不会与现有 `mybatis.datachange` 中的类名冲突。所有被移动类的内部 import 均为对 fw-plus-core / 第三方依赖的引用，与所在包无关。

仓库内对 `autoconfigure.mp` / `autoconfigure.mf` 包路径的外部引用仅 6 处：2 处 META-INF 自动配置注册文件（各 4 个类全限定名）、2 处测试类 import。详见 proposal.md - Impact。

## Goals / Non-Goals

**Goals:**

- 将 `mp`、`mf` 两个并列子包合并到 `mybatis` 包下，使 MyBatis 生态的自动配置类收敛到单一命名空间。
- 保持类名、Bean 注册顺序、条件注解、运行时行为完全不变。
- 同步更新所有外部引用，确保编译和测试通过。

**Non-Goals:**

- 不对 `MpAutoConfiguration` 内 `@Import` 的子配置类做功能重构。
- 不调整 `mybatis.datachange` 子包位置（保持现状）。
- 不重命名任何类（即使 `Mp*` / `Mf*` 前缀在合并到 `mybatis` 包下后语义略冗余，也保留原名以减少 diff 和下游影响）。
- 不调整 Spring Boot autoconfigure 注册顺序。
- 不修改 fw-plus-core 中对应的 `mp` / `mf` 模块（如 `com.xiongx9527.fw.plus.mp.*`、`com.xiongx9527.fw.plus.mf.*`），本次仅影响 boot-autoconfigure 层的包路径。

## Decisions

### D1：扁平合并到 `mybatis` 根包，不引入 `mybatis.mp` / `mybatis.mf` 子包

**选择**：把 `mp` / `mf` 两个包中的所有类直接放到 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis` 根包下，与现有的 `mybatis.datachange` 子包并列。

**理由**：

- 类名已通过 `Mp` / `Mf` 前缀自带框架归属信息，无需再用子包重复表达。
- 扁平结构使 `mybatis` 包成为一个清晰的"MyBatis 生态自动配置聚合包"，避免出现 `mybatis.mp.MpAutoConfiguration` 这种"包名+类名前缀"的双重前缀冗余。
- 改动最小：仅修改 package 声明 + 外部引用，类内部 import 完全不动。

**备选方案**：

- 保留子包（`mybatis.mp.*`、`mybatis.mf.*`）：能保留框架边界，但与 `mybatis.datachange` 这种"按功能而非框架分"的子包命名不一致，且产生 `mybatis.mp.MpAutoConfiguration` 冗余。已被用户否决。
- 按功能重组（`mybatis.encrypt`、`mybatis.datachange.adapter`、`mybatis.join` 等）：能消除 `Mp`/`Mf` 前缀冗余，但需要重命名大量类、处理跨框架合并（如 `MpEncryptAutoConfiguration` 与 `MfEncryptAutoConfiguration` 是否合并为一个），改动规模和风险显著上升。已被用户否决。

### D2：保留所有类名不变

**选择**：所有被移动类的类名（如 `MpAutoConfiguration`、`MfEncryptAutoConfiguration`）保持不变。

**理由**：

- 类名变更会扩大 diff，且 Spring autoconfigure 注册、`@Import` 引用、测试 import 都需要同步修改，增加出错面。
- `Mp` / `Mf` 前缀在合并后仍能正确表达"该配置类针对 MyBatis-Plus / MyBatis-Flex"，语义不冲突。
- 下游若显式引用全限定名，类名不变可降低迁移心智负担（只需替换包路径段）。

### D3：使用 `git mv` 而非"删除+新建"

**选择**：通过 `git mv` 移动文件，保留 git 历史追踪。

**理由**：让 git 能识别为 rename，便于后续 `git log --follow` 追溯单个文件的历史。

## Implementation Divergence（2026-08-09）

### D1：最终采用 `mybatis.mp` / `mybatis.mf` 子包方案（覆盖原 D1 扁平决策）

**选择**：把 `mp` / `mf` 两个包中的所有类分别放到 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp` 和 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf` 两个子包下，与现有的 `mybatis.datachange` 子包并列。

**原因**（覆盖原 D1 决策）：
- build 阶段用户明确要求在 `mybatis` 下创建 `mp`、`mf` 两个目录，保留按 ORM 框架的细粒度目录边界。
- 虽然产生 `mybatis.mp.MpAutoConfiguration` 这种"包名+类名前缀"的双重前缀，但与 `mybatis.datachange` 这种"按功能分目录"的模式并不冲突（`datachange` 是通用抽象层，`mp` / `mf` 是框架具体实现层）。
- 改动仍足够小：仅修改 package 声明 + 外部全限定名引用，类内部 import 完全不动。

**原 D1 扁平方案**不再执行。原 D1 段落中记录的"备选方案：保留子包"即为最终采用方案。原 `mybatis.mp.MpAutoConfiguration` 冗余问题作为取舍保留，后续按功能重组时再处理。

## Risks / Trade-offs

- **[风险] 下游项目显式引用全限定名失效** → 该包属于 fw-plus 内部 autoconfigure 实现，不对外暴露 public API；正常通过 Spring Boot autoconfigure 机制使用的项目无感知。在 proposal 中已明确标注 BREAKING，发版说明中提示包路径变更（`autoconfigure.mp` → `autoconfigure.mybatis.mp`，`autoconfigure.mf` → `autoconfigure.mybatis.mf`）即可。
- **[风险] 遗漏仓库内对 `autoconfigure.mp` / `autoconfigure.mf` 的引用** → 已通过仓库全量 Grep 确认仅 6 处外部引用（2 个 META-INF 文件、2 个测试类），全部纳入 tasks.md；build 阶段再次运行 Grep 复核，零残留。
- **[风险] `MpAutoConfiguration` 的 `@Import` 引用子配置类时使用相对引用** → 已确认 `@Import({MpBaseAutoConfiguration.class, ...})` 使用 `.class` 引用，且所有被引用类同在 `mybatis.mp` 子包下，与包路径无关，移动后自动生效，无需修改。
- **[取舍] `Mp` / `Mf` 类名前缀在 `mybatis.mp` / `mybatis.mf` 子包下双重冗余** → 换取最小 diff 和最低风险，可接受；后续若做按功能重组的重构再统一处理。
