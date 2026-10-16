package com.xiongx9527.fw.plus.ai.sample.chat.agentic;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticHumanInTheLoop;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticPipelineRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticSupervisor;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticToolProviders;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticWorkflows;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store.InMemoryAgenticScopeStore;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.agentic.scope.AgenticScopeSerializer;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolProvider;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Agentic 智能体编排全模式演示（构建即文档 + 每模式可执行案例）。
 *
 * <p>本类是 {@code docs/langchain4j-agentic-guide.md} 的可执行对照：前半部分每个方法演示
 * 「如何构建」——编排构建不触达模型与网络，只有后续 {@code invoke(...)} 才真正执行；
 * 末尾的 {@code runXxx} 方法为**每个模式提供一个可运行案例**，与 {@link AgenticController}
 * 的端点、样例测试共用同一份代码。无模型模式（parallelMapper / conditional-offline /
 * human-in-the-loop / graph / observability / agent-as-tool）任何环境都能执行并断言结果；
 * 需要模型的模式接收注入的 {@link ChatModel}。</p>
 *
 * <p>所有构建都经由统一门面 {@link AgenticWorkflows}（无状态、可注入/复用），
 * 业务与脚本不直接接触 langchain4j-agentic 内部类型。</p>
 *
 * <h3>模式速查（含使用场景）</h3>
 * <ul>
 *     <li><b>scope store</b>：需要跨请求 / 跨进程保留编排运行状态、断点续跑或落库审计。</li>
 *     <li><b>typed agent</b>：agent 有稳定输入输出契约（接口 + {@code @Agent}），要类型安全且可复用。</li>
 *     <li><b>untyped agent</b>：只要「一个提示词 + 一个输出键」，或运行期动态拼装。</li>
 *     <li><b>sequence</b>：步骤与顺序都已知的固定流水线（如「抽取 → 回答」）。</li>
 *     <li><b>loop</b>：反复迭代同一 agent 直到满足退出条件（而非固定步数）。</li>
 *     <li><b>conditional</b>：按运行期状态在分支间择一（意图路由、多策略选择）。</li>
 *     <li><b>parallel</b>：多个互不依赖的子任务并行以缩短总耗时。</li>
 *     <li><b>parallelMapper</b>：对一批同类元素做同一种处理（列表逐项映射）。</li>
 *     <li><b>supervisor</b>：不确定调哪些 agent、什么顺序，交给监督者模型自主决策调度。</li>
 *     <li><b>planner</b>：同 supervisor，但更强调显式规划，适合多步骤、有依赖的自主编排。</li>
 *     <li><b>human-in-the-loop</b>：流程中需要人工审批 / 补充 / 确认后继续。</li>
 *     <li><b>declarative</b>：整条编排可用注解静态声明，「接口即系统」、少样板。</li>
 *     <li><b>A2A</b>：子 agent 部署在远端，需要跨进程 / 跨语言调用。</li>
 *     <li><b>observability</b>：需要观测 / 审计编排执行（输入输出、耗时、token、失败）。</li>
 *     <li><b>agent-as-tool</b>：把整条编排当作工具交给上层模型自主决定何时调用。</li>
 *     <li><b>graph</b>：流程是有向图（条件边 / 环 / 分支汇聚），需显式图结构或挂为工具。</li>
 *     <li><b>registry</b>：多条编排需统一注册 / 按 key 查找复用。</li>
 * </ul>
 */
public final class AgenticWorkflowsDemo {

	private AgenticWorkflowsDemo() {
	}

	// ------------------------------------------------------------------
	// 文档 §2 scope store：状态作用域与持久化
	// ------------------------------------------------------------------

	/**
	 * scope store（状态作用域持久化）：框架内置进程内实现 {@code InMemoryAgenticScopeStore}
	 * 经 {@code AgenticScopePersister} 全局接入；生产环境（跨进程 / 跨重启）换框架内置的
	 * {@code MongoAgenticScopeStore}（对齐 AgentScope2 Java 的 MongoDB 状态存储）——
	 * Spring Boot 下直接配置 {@code fw.plus.ai.langchain4j.agentic.scope-store.type=mongo}
	 * 即由自动装配完成「装配 + 全局安装 + 序列化白名单注册」，无需手写 {@code setStore}。
	 *
	 * <p>使用场景：需要把某次编排的运行状态（AgenticScope）跨请求 / 跨进程保留时，
	 * 例如多轮对话续跑、长流程断点恢复、多实例共享会话状态，或落库审计。</p>
	 */
	public static void scopeStoreDemo() {
		// 框架内置进程内 store；不调用 setStore 时 scope 仅在单次调用内存在（EPHEMERAL，调用结束即清理）
		InMemoryAgenticScopeStore store = new InMemoryAgenticScopeStore();
		AgenticScopePersister.setStore(store);

		// 序列化白名单：持久化后端反序列化时放行业务 DTO 的包前缀或具体类型；
		// Spring Boot 下亦可经 fw.plus.ai.langchain4j.agentic.scope-store.serialization.* 配置化注册
		AgenticScopeSerializer.allowDeserializationPackagePrefix(
				"com.xiongx9527.fw.plus.ai.sample.chat.agentic");
	}

	// ------------------------------------------------------------------
	// 文档 §3 单 agent（typed / untyped）
	// ------------------------------------------------------------------

	/** 带 {@code @Agent} 的接口：typed agent，可独立执行也可作为 subAgent。 */
	public interface EchoAgent {
		@Agent(name = "echo", description = "回显输入", outputKey = "out")
		@UserMessage("请回显：{{text}}")
		String echo(@V("text") String text);
	}

	/** 条件编排 echo 分支专用：与 {@link AnswerAgent} 写同一输出键，保证 conditional 的 outputKey 可用。 */
	public interface EchoAnswerAgent {
		@Agent(name = "echo", description = "回显输入", outputKey = "answer")
		@UserMessage("请回显：{{text}}")
		String echo(@V("text") String text);
	}

	/** 第一步：抽取问题。 */
	public interface ExtractAgent {
		@Agent(name = "extract", description = "抽取用户问题中的核心诉求", outputKey = "query")
		@UserMessage("抽取请求的核心诉求：{{request}}")
		String extract(@V("request") String request);
	}

	/** 第二步：依据抽取结果回答。 */
	public interface AnswerAgent {
		@Agent(name = "answer", description = "基于抽取的诉求给出回答", outputKey = "answer")
		@UserMessage("基于诉求回答：{{query}}")
		String answer(@V("query") String query);
	}

	/**
	 * 单 agent（typed）：传入带 {@code @Agent} 的接口，产物为该接口的可执行代理。
	 *
	 * <p>使用场景：agent 有稳定的输入 / 输出契约（接口 + {@code @Agent}），需要类型安全、
	 * 既能独立调用又能作为 subAgent 复用时。</p>
	 */
	public static EchoAgent typedAgentDemo(ChatModel chatModel) {
		return AgenticWorkflows.agent(EchoAgent.class)
				.chatModel(chatModel)
				.build();
	}

	/**
	 * 单 agent（untyped）：直接声明消息模板 + 输出键，产物为 {@link UntypedAgent} 句柄。
	 *
	 * <p>使用场景：只想要「一个提示词 + 一个输出键」的轻量 agent，或需要在运行期
	 * （脚本 / 配置）动态拼装、不便预先定义接口时。</p>
	 */
	public static UntypedAgent untypedAgentDemo(ChatModel chatModel) {
		// untyped：直接声明消息模板 + 输出键，产物为 UntypedAgent 句柄
		return AgenticWorkflows.agent()
				.chatModel(chatModel)
				.userMessage("回答：{{question}}")
				.outputKey("answer")
				.build();
	}

	// ------------------------------------------------------------------
	// 文档 §4 基础编排：sequence / loop / conditional / parallel
	// ------------------------------------------------------------------

	/**
	 * sequence 顺序编排：subAgents 依次执行，输出经 AgenticScope 按 {@code @V} 变量名流向下一个 agent。
	 *
	 * <p>使用场景：任务可拆成固定的先后步骤（如「抽取 → 回答」「分类 → 检索 → 生成」），
	 * 步骤与顺序都已知、无需模型自主决策时。</p>
	 */
	public static UntypedAgent sequenceDemo(ChatModel chatModel) {
		return AgenticWorkflows.sequence()
				.subAgents(
						AgenticWorkflows.agent(ExtractAgent.class).chatModel(chatModel).build(),
						AgenticWorkflows.agent(AnswerAgent.class).chatModel(chatModel).build())
				.outputKey("answer")
				.build();
	}

	/**
	 * loop 循环编排：反复执行 subAgents，直到 exitCondition 满足或达到 maxIterations。
	 *
	 * <p>使用场景：需要对同一 agent 反复迭代、直到满足某个退出条件（如内容合规 / 达到长度、
	 * 迭代修正），而**不是**固定步数时。</p>
	 */
	public static UntypedAgent loopDemo(ChatModel chatModel) {
		return AgenticWorkflows.loop()
				.subAgents(AgenticWorkflows.agent(AnswerAgent.class).chatModel(chatModel).build())
				.maxIterations(5)
				.outputKey("answer")
				.exitCondition(scope -> ((String) scope.readState("answer", "")).endsWith("。"))
				.build();
	}

	/**
	 * conditional 条件编排：按 AgenticScope 状态路由到满足条件的 agent 分支。
	 *
	 * <p>使用场景：执行哪条分支取决于<b>运行期状态</b>（意图、分类、标记），如意图路由、
	 * 多策略择一——分支与条件可预先声明，但选哪条要等运行时才知道。</p>
	 */
	public static UntypedAgent conditionalDemo(ChatModel chatModel) {
		return AgenticWorkflows.conditional()
				.name("intent-router")
				.subAgents(scope -> "query".equals(scope.readState("intent")),
						AgenticWorkflows.agent(AnswerAgent.class).chatModel(chatModel).build())
				.subAgents(scope -> "echo".equals(scope.readState("intent")),
						AgenticWorkflows.agent(EchoAnswerAgent.class).chatModel(chatModel).build())
				.outputKey("answer")
				.build();
	}

	/**
	 * parallel 并行编排：多个独立 agent 同时执行（fan-out），结果按各自 outputKey 写回。
	 *
	 * <p>使用场景：多个子任务<b>互不依赖</b>、可同时进行以缩短总耗时（如同一段文本同时翻译 +
	 * 写宣传语、多路检索后汇总）。有前后依赖的步骤不要放这里，改用 sequence。</p>
	 */
	public static UntypedAgent parallelDemo(ChatModel chatModel) {
		// 并行编排适合真正独立的任务：两个 untyped agent 各自从 scope 读自己的输入键，
		// 并行执行后各自把结果写回 scope（translation / slogan）。
		return AgenticWorkflows.parallel()
				.subAgents(
						AgenticWorkflows.agent()
								.chatModel(chatModel)
								.userMessage("把「{{title}}」翻译成英文")
								.outputKey("translation")
								.build(),
						AgenticWorkflows.agent()
								.chatModel(chatModel)
								.userMessage("为「{{title}}」写一句宣传语")
								.outputKey("slogan")
								.build())
				.outputKey("translation")
				.build();
	}

	// ------------------------------------------------------------------
	// 文档 §5 parallelMapper：列表并行映射
	// ------------------------------------------------------------------

	/** 非 AI 映射 agent：对逐项输入做确定性变换（不需要模型即可运行）。 */
	public static class UpperCaseMapper {
		@Agent(name = "upper", description = "把输入项转为大写", outputKey = "mapped")
		public String map(@V("item") String item) {
			return item.toUpperCase();
		}
	}

	/**
	 * parallelMapper 并行映射：对 {@code items} 列表逐项并行执行同一个映射 agent，
	 * 结果列表写回 {@code results}。开箱可跑、无需模型。
	 *
	 * <p>使用场景：对一批<b>同类元素做同一种处理</b>——列表逐项映射、批量抽取 / 翻译 / 分类，
	 * fan-out 后聚合为一个结果列表。</p>
	 */
	public static UntypedAgent parallelMapperDemo() {
		return AgenticWorkflows.parallelMapper()
				.itemsProvider("items")
				.subAgents(new UpperCaseMapper())
				.outputKey("results")
				.build();
	}

	// ------------------------------------------------------------------
	// 文档 §6 supervisor / planner：LLM 自主编排（必需注入 ChatModel）
	// ------------------------------------------------------------------

	public static AgenticSupervisor supervisorDemo(ChatModel chatModel) {
		return supervisorDemo(chatModel, null);
	}

	/**
	 * supervisor 监督者编排：监督者 LLM 依据请求自主选择并调用 subAgents，产物为
	 * {@link AgenticSupervisor} 句柄。
	 *
	 * <p>使用场景：事先不确定该调用哪些 agent、以什么顺序，需要让「监督者」模型依据每次请求
	 * 自主决策与调度一批 subAgents（开放域问答、多工具协作）。</p>
	 *
	 * <p>可选挂 {@link dev.langchain4j.agentic.observability.AgentListener} 观测执行。</p>
	 */
	public static AgenticSupervisor supervisorDemo(ChatModel chatModel,
			dev.langchain4j.agentic.observability.AgentListener listener) {
		com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticSupervisorBuilder builder =
				AgenticWorkflows.supervisor()
				.chatModel(chatModel)
				.subAgents(
						AgenticWorkflows.agent(ExtractAgent.class).chatModel(chatModel).build(),
						AgenticWorkflows.agent(AnswerAgent.class).chatModel(chatModel).build())
				.maxAgentsInvocations(6)
				.outputKey("answer");
		if (listener != null) {
			builder.listener(listener);
		}
		return builder.build();
	}

	/**
	 * planner 规划式编排：Planner LLM 依据 agent 拓扑自主编排 agents 执行，产物为 {@link UntypedAgent}。
	 *
	 * <p>使用场景：与 supervisor 同属「模型自主编排」，但更强调<b>显式规划</b>——适合多步骤、
	 * 步骤间有依赖、需要模型先给出执行计划的场景。</p>
	 */
	public static UntypedAgent plannerDemo(ChatModel chatModel) {
		Object stepExtract = AgenticWorkflows.agent(ExtractAgent.class).chatModel(chatModel).build();
		return AgenticWorkflows.planner()
				.chatModel(chatModel)
				.subAgents(stepExtract)
				.maxAgentsInvocations(4)
				.outputKey("answer")
				.build();
	}

	// ------------------------------------------------------------------
	// 文档 §7 human-in-the-loop：人工介入节点（无需模型，开箱可跑）
	// ------------------------------------------------------------------

	/**
	 * human-in-the-loop 人工介入：执行到该节点时采用外部 responseProvider 的响应继续流转。
	 *
	 * <p>使用场景：流程中需要人工审批 / 补充信息 / 确认后才能继续，如审批流、工单确认、
	 * 敏感操作的二次确认。</p>
	 */
	public static AgenticHumanInTheLoop humanInTheLoopDemo() {
		return AgenticWorkflows.humanInTheLoop()
				.responseProvider(() -> "人工响应")
				.outputKey("answer")
				.build();
	}

	/** humanInTheLoop 作为 sequence 的 subAgent 挂入（框架自动拆包）。 */
	public static UntypedAgent hitlMountedDemo() {
		return AgenticWorkflows.sequence()
				.subAgents(humanInTheLoopDemo())
				.outputKey("answer")
				.build();
	}

	// ------------------------------------------------------------------
	// 文档 §9 声明式 createAgenticSystem + declarative suppliers
	// ------------------------------------------------------------------

	public interface DeclExtract {
		@Agent(name = "extract", description = "第一步：抽取诉求", outputKey = "query")
		@UserMessage("抽取诉求：{{request}}")
		String run(@V("request") String request);
	}

	public interface DeclAnswer {
		@Agent(name = "answer", description = "第二步：回答", outputKey = "finalOut")
		@UserMessage("回答：{{query}}")
		String run(@V("query") String query);
	}

	/** 声明式顺序系统：注解声明整条编排，装配后接口代理直接调用。 */
	public interface DeclarativeSystem {
		@dev.langchain4j.agentic.declarative.SequenceAgent(
				subAgents = { DeclExtract.class, DeclAnswer.class }, outputKey = "finalOut")
		String run(@V("request") String request);
	}

	/**
	 * 声明式 createAgenticSystem：用 {@code @SequenceAgent} 等注解静态声明整条编排，
	 * 装配后以接口代理直接调用。
	 *
	 * <p>使用场景：整条编排可在编译期用注解写死，希望「接口即系统」、少样板、易读易维护时。</p>
	 */
	public static DeclarativeSystem declarativeDemo(ChatModel chatModel) {
		return AgenticWorkflows.createAgenticSystem(DeclarativeSystem.class, chatModel);
	}

	/** 声明式 suppliers：模型由 {@code @ChatModelSupplier} 静态方法提供，装配参数免传。 */
	public interface SupplierDeclarativeSystem {
		@dev.langchain4j.agentic.declarative.ChatModelSupplier
		static ChatModel model() {
			throw new UnsupportedOperationException("替换为 ChatModel 工厂实现（如 Ollama/OpenAI）");
		}

		@dev.langchain4j.agentic.declarative.SequenceAgent(
				subAgents = { DeclExtract.class, DeclAnswer.class }, outputKey = "finalOut")
		String run(@V("request") String request);
	}

	// ------------------------------------------------------------------
	// 文档 §8 A2A：远程 agent（惰性构建，仅 build() 才连接服务端）
	// ------------------------------------------------------------------

	/**
	 * A2A 远程 agent 客户端（untyped）：指定服务端地址后构建远程 agent，构建阶段不触网，
	 * {@code build()} 才连接服务端抓取 agent-card。
	 *
	 * <p>使用场景：某个子 agent 部署在远端（另一个服务 / 团队 / 语言），需要通过 A2A 协议
	 * 跨进程调用——把能力外包，或与异构系统协作时。</p>
	 */
	public static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticA2ABuilder a2aDemo(String serverUrl) {
		return AgenticWorkflows.a2a(serverUrl)
				.inputKeys("request")
				.outputKey("response")
				.async(false)
				.tenant("sample");
	}

	// ------------------------------------------------------------------
	// 文档 §10 observability：AgentListener 生命周期监听
	// ------------------------------------------------------------------

	/**
	 * observability：实现 {@code AgentListener} 挂到编排上，观察编排范围内每次 agent 调用、
	 * scope 创建/销毁、工具执行与错误。本类实现为基础空实现（方法签名即文档）；实际接入时可在
	 * 各回调中补日志 / 指标 / 追踪（与 ChatContextLogger 同思路），并挂到编排的 {@code listener(...)} 位。
	 *
	 * <p>使用场景：需要观测 / 审计编排执行——每个 agent 的输入输出、耗时、token、失败，
	 * 用于监控、日志、计费或根因分析时。</p>
	 */
	public static dev.langchain4j.agentic.observability.AgentListener observabilityDemo() {
		return new dev.langchain4j.agentic.observability.AgentListener() {
			@Override
			public void beforeAgentInvocation(
					dev.langchain4j.agentic.observability.AgentRequest request) {
			}

			@Override
			public void afterAgentInvocation(
					dev.langchain4j.agentic.observability.AgentResponse response) {
			}

			@Override
			public void onAgentInvocationError(
					dev.langchain4j.agentic.observability.AgentInvocationError error) {
			}

			@Override
			public void afterAgenticScopeCreated(dev.langchain4j.agentic.scope.AgenticScope scope) {
			}

			@Override
			public void beforeAgenticScopeDestroyed(dev.langchain4j.agentic.scope.AgenticScope scope) {
			}

			@Override
			public void onAgenticSystemSuspended(dev.langchain4j.agentic.scope.AgenticScope scope) {
			}

			@Override
			public void beforeAgentToolExecution(
					dev.langchain4j.agentic.observability.BeforeAgentToolExecution toolExecution) {
			}

			@Override
			public void afterAgentToolExecution(
					dev.langchain4j.agentic.observability.AfterAgentToolExecution toolExecution) {
			}

			@Override
			public boolean inheritedBySubagents() {
				return true;
			}
		};
	}

	// ------------------------------------------------------------------
	// 文档 §11 / §12 ToolProvider 包装 + AgenticPipelineRegistry
	// ------------------------------------------------------------------

	/**
	 * 编排注册中心（{@code AgenticPipelineRegistry}）：按 key 注册 / 查找 / 复用编排，
	 * 并支持把注册的编排包装为 agent-as-tool。
	 *
	 * <p>使用场景：多个编排需要统一管理，或脚本 / 配置希望按名字（key）取用某条编排、
	 * 再包装为工具挂入助手时。</p>
	 */
	public static ToolProvider toolProviderDemo(AgenticPipelineRegistry registry) {
		UntypedAgent pipeline = parallelMapperDemo();
		registry.register("nl2sql-pipeline", pipeline);

		// 按注册中心 key 取编排 → ToolProvider（未注册时抛 IllegalArgumentException 并列出已注册 key）
		return AgenticToolProviders.agenticToolProviderByKey(
				registry, "nl2sql-pipeline", "map_items", "对列表逐项并行映射", "results", "items");
	}

	/**
	 * agent-as-tool（不经注册中心）：直接包装编排产物为 {@link ToolProvider}。
	 *
	 * <p>使用场景：同 {@link #toolProviderDemo}，但编排不经过注册中心、就地包装时。</p>
	 */
	public static ToolProvider directToolProviderDemo() {
		return AgenticToolProviders.agenticToolProvider(
				parallelMapperDemo(), "map_items", "对列表逐项并行映射", "results", "items");
	}

	// ------------------------------------------------------------------
	// 每模式执行案例：每个编排模式一个可运行入口
	//
	// 无模型模式（parallelMapper / conditional-offline / HITL / graph /
	// observability / agent-as-tool）任何环境都能跑；
	// 需要模型的模式接收注入的 ChatModel——构建与执行分离，构建不触达模型，
	// invoke(...) 时才请求，故由调用方/端点按需触发。
	// ------------------------------------------------------------------

	// ---- 单 agent：typed / untyped ----

	/** typed agent 独立执行：{@code EchoAgent.echo(text)}。 */
	public static String runTypedAgentDemo(ChatModel chatModel, String text) {
		return typedAgentDemo(chatModel).echo(text);
	}

	/** untyped agent 独立执行：读 question → 返回 answer。 */
	public static String runUntypedAgentDemo(ChatModel chatModel, String question) {
		return String.valueOf(untypedAgentDemo(chatModel).invoke(Map.of("question", question)));
	}

	// ---- 基础编排 sequence / loop / conditional / parallel ----

	/** sequence 端到端：抽取 → 回答。 */
	public static String runSequenceDemo(ChatModel chatModel, String request) {
		return String.valueOf(sequenceDemo(chatModel).invoke(Map.of("request", request)));
	}

	/** loop 端到端：反复回答直到以「。」结尾或达到 maxIterations。 */
	public static String runLoopDemo(ChatModel chatModel, String query) {
		return String.valueOf(loopDemo(chatModel).invoke(Map.of("query", query)));
	}

	/** conditional 端到端：按 intent 路由到回答分支（query）或回显分支（echo）。 */
	public static String runConditionalDemo(ChatModel chatModel, String intent, String value) {
		Map<String, Object> input = Map.of("intent", intent, "query", value, "text", value);
		return String.valueOf(conditionalDemo(chatModel).invoke(input));
	}

	/** parallel 端到端：翻译与宣传语并行产出，返回 translation。 */
	public static String runParallelDemo(ChatModel chatModel, String title) {
		return String.valueOf(parallelDemo(chatModel).invoke(Map.of("title", title)));
	}

	// ------------------------------------------------------------------
	// supervisor / planner（LLM 自主编排）
	// ------------------------------------------------------------------

	/** supervisor 端到端：监督者自主选择并调用 subAgents 汇总答复。 */
	public static String runSupervisorDemo(ChatModel chatModel, String message) {
		return supervisorDemo(chatModel).invoke(message);
	}

	/** planner 端到端：Planner LLM 自主编排 agents 执行。 */
	public static String runPlannerDemo(ChatModel chatModel, String request) {
		return String.valueOf(plannerDemo(chatModel).invoke(Map.of("request", request)));
	}

	// ------------------------------------------------------------------
	// 声明式 createAgenticSystem
	// ------------------------------------------------------------------

	/** 声明式顺序系统端到端：注解声明的编排，接口代理直接调用。 */
	public static String runDeclarativeDemo(ChatModel chatModel, String request) {
		return declarativeDemo(chatModel).run(request);
	}

	// ------------------------------------------------------------------
	// parallelMapper（无模型，开箱可跑）
	// ------------------------------------------------------------------

	/** {@code parallelMapper} 端到端：{@code [a,b,c] → [A,B,C]}。 */
	public static List<String> runParallelMapperDemo() {
		Object result = parallelMapperDemo().invoke(Map.of("items", List.of("a", "b", "c")));
		return (List<String>) result;
	}

	// ------------------------------------------------------------------
	// human-in-the-loop（无模型，开箱可跑）
	// ------------------------------------------------------------------

	/** HITL 挂载 sequence 端到端：返回固定人工响应。 */
	public static String runHitlDemo() {
		return (String) hitlMountedDemo().invoke(Map.of("request", "是否需要继续？"));
	}

	// ------------------------------------------------------------------
	// conditional 的离线形态（非 AI 分支 agent，无模型）
	// ------------------------------------------------------------------

	/** 小写映射 agent：与 {@link UpperCaseMapper} 对称，用于离线 conditional 演示。 */
	public static class LowerCaseMapper {
		@Agent(name = "lower", description = "把输入项转为小写", outputKey = "mapped")
		public String map(@V("item") String item) {
			return item.toLowerCase();
		}
	}

	/** 非 AI 条件编排：按 mode 路由到大小写映射，无需模型。 */
	public static UntypedAgent conditionalOfflineDemo() {
		return AgenticWorkflows.conditional()
				.name("mode-router")
				.subAgents(scope -> "upper".equals(scope.readState("mode")), new UpperCaseMapper())
				.subAgents(scope -> "lower".equals(scope.readState("mode")), new LowerCaseMapper())
				.outputKey("mapped")
				.build();
	}

	/** 离线 conditional 端到端：{@code (upper,abc) → ABC}。 */
	public static String runConditionalOfflineDemo(String mode, String item) {
		Object result = conditionalOfflineDemo().invoke(Map.of("mode", mode, "item", item));
		return String.valueOf(result);
	}

	// ------------------------------------------------------------------
	// graph（langgraph4j StateGraph，节点为纯函数，无模型）
	// ------------------------------------------------------------------

	/**
	 * graph 端到端：建图 → 编译 → 执行（节点为确定性函数，不触达模型）。
	 *
	 * <p>使用场景：流程是<b>有向图</b>——含条件边、环、分支汇聚，需要显式图结构来表达；
	 * 或要把编译后的图挂为工具（{@link AgenticWorkflows#graphToolProvider}）交给模型调度时。</p>
	 */
	public static String runGraphDemo() {
		try {
			StateGraph<AgentState> graph = AgenticWorkflows.graph(AgentState::new)
					.addNode("upper", AsyncNodeAction.node_async(
							state -> Map.of("text", String.valueOf(state.value("text", "")).toUpperCase())))
					.addNode("shout", AsyncNodeAction.node_async(
							state -> Map.of("text", state.value("text", "") + "!")))
					.addEdge(StateGraph.START, "upper")
					.addEdge("upper", "shout")
					.addEdge("shout", StateGraph.END);
			CompiledGraph<AgentState> compiled = graph.compile();
			Optional<AgentState> finalState = compiled.invoke(Map.of("text", "hello"));
			return finalState.map(state -> String.valueOf(state.value("text", ""))).orElse("");
		}
		catch (Exception e) {
			throw new IllegalStateException("graph 执行失败", e);
		}
	}

	// ------------------------------------------------------------------
	// observability（AgentListener 切片监听，无模型）
	// ------------------------------------------------------------------

	/** 计数监听器：统计编排内 agent 调用次数（演示 AgentListener 切片挂载）。 */
	public static final class CountingAgentListener
			implements dev.langchain4j.agentic.observability.AgentListener {

		private final java.util.concurrent.atomic.AtomicInteger invocations = new java.util.concurrent.atomic.AtomicInteger();

		public int invocations() {
			return invocations.get();
		}

		@Override
		public void beforeAgentInvocation(dev.langchain4j.agentic.observability.AgentRequest request) {
			invocations.incrementAndGet();
		}

		@Override
		public void afterAgentInvocation(dev.langchain4j.agentic.observability.AgentResponse response) {
		}

		@Override
		public void onAgentInvocationError(dev.langchain4j.agentic.observability.AgentInvocationError error) {
		}

		@Override
		public void afterAgenticScopeCreated(dev.langchain4j.agentic.scope.AgenticScope scope) {
		}

		@Override
		public void beforeAgenticScopeDestroyed(dev.langchain4j.agentic.scope.AgenticScope scope) {
		}

		@Override
		public void onAgenticSystemSuspended(dev.langchain4j.agentic.scope.AgenticScope scope) {
		}

		@Override
		public void beforeAgentToolExecution(
				dev.langchain4j.agentic.observability.BeforeAgentToolExecution toolExecution) {
		}

		@Override
		public void afterAgentToolExecution(
				dev.langchain4j.agentic.observability.AfterAgentToolExecution toolExecution) {
		}

		@Override
		public boolean inheritedBySubagents() {
			return true;
		}
	}

	/** observability 端到端：给非 AI 编排挂计数监听器，返回结果与监听到的调用次数。 */
	public static String runObservabilityDemo() {
		CountingAgentListener listener = new CountingAgentListener();
		UntypedAgent pipeline = AgenticWorkflows.sequence()
				.subAgents(new UpperCaseMapper())
				.outputKey("mapped")
				.listener(listener)
				.build();
		Object result = pipeline.invoke(Map.of("item", "observe"));
		return result + " / agent 调用次数=" + listener.invocations();
	}

	// ------------------------------------------------------------------
	// agent-as-tool（ToolProvider 包装并执行，无模型）
	// ------------------------------------------------------------------

	/** agent-as-tool 端到端：注册 → 包装为 ToolProvider → 执行工具，返回映射结果。 */
	public static String runToolProviderExecutionDemo() {
		AgenticPipelineRegistry registry = new AgenticPipelineRegistry();
		ToolProvider provider = toolProviderDemo(registry);

		dev.langchain4j.service.tool.ToolProviderResult tools =
				provider.provideTools(new dev.langchain4j.service.tool.ToolProviderRequest(
						"demo", dev.langchain4j.data.message.UserMessage.from("映射列表")));
		dev.langchain4j.service.tool.AiServiceTool tool = tools.aiServiceTools().get(0);
		dev.langchain4j.agent.tool.ToolExecutionRequest request = dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
				.name(tool.toolSpecification().name())
				.arguments("{\"items\":[\"x\",\"y\"]}")
				.build();
		return tool.toolExecutor().execute(request, "demo");
	}

	/** 注册中心 key 集（示例复用）。 */
	public static Set<String> registeredKeys() {
		return Set.of("nl2sql-pipeline");
	}

	/**
	 * plannerBuilder 演示（无模型，开箱可跑）：使用自定义 Planner 实现固定顺序执行。
	 *
	 * <p>使用场景：不依赖 LLM 每步决策，由开发者提供确定性规划逻辑，适合
	 * 顺序执行、数据处理管道等不需要模型介入决策的场景。</p>
	 *
	 * <p>框架提供 {@link AgenticPlannerBasedBuilder} 配合 {@link AgenticWorkflows#plannerBuilder()}
	 * 入口，开发者只需提供 {@link Planner} 实现，其余流程（subAgents 挂载、outputKey 设置等）由框架完成。</p>
	 *
	 * <p>注：本演示使用样例自带的 {@link SequentialPlanner} 作为自定义规划器实现，
	 * 实际使用时请替换为业务逻辑。</p>
	 *
	 * @return 执行结果字符串
	 */
	public static String plannerBuilderDemo() {
		// 1. 构建两个子 agent（模拟业务步骤）
		Object stepA = AgenticWorkflows.agent(PriceQuoteAgent.class)
				.chatModel(DemoModels.SCRIPTED_MODEL)
				.build();
		Object stepB = AgenticWorkflows.agent(DiscountQuoteAgent.class)
				.chatModel(DemoModels.SCRIPTED_MODEL)
				.build();

		// 2. 使用 plannerBuilder 注入自定义规划器
		UntypedAgent agent = AgenticWorkflows.plannerBuilder()
				.planner(SequentialPlanner::new)  // 注入自定义规划器
				.subAgents(stepA, stepB)
				.outputKey("quoteResult")
				.build();

		// 3. 执行：传入订单 ID，由自定义规划器决定子 agent 调用顺序
		return String.valueOf(agent.invoke(Map.of("orderId", "ORD-2024")));
	}

	/**
	 * plannerBuilderWithCustomPlanner 演示（无模型，开箱可跑）：以 {@code Supplier}
	 * 形式注入自定义 {@link Planner} 实例。
	 *
	 * <p>演示 {@code planner(Supplier<Planner>)} 的用法：当规划器需要自带参数
	 * （顺序表、阈值等）时，用 lambda 构造实例即可。子 agent 列表由框架在
	 * {@code init()} 阶段通过 {@code ctx.subagents()} 注入，业务侧无需自行拼装
	 * agent proxy——{@code AgenticAgents} 属框架内部实现，样例不直接引用。</p>
	 *
	 * @return 执行结果字符串
	 */
	public static String plannerBuilderWithCustomPlannerDemo() {
		// 1. 构建两个子 agent
		Object stepA = AgenticWorkflows.agent(PriceQuoteAgent.class)
				.chatModel(DemoModels.SCRIPTED_MODEL)
				.build();
		Object stepB = AgenticWorkflows.agent(DiscountQuoteAgent.class)
				.chatModel(DemoModels.SCRIPTED_MODEL)
				.build();

		// 2. 使用 plannerBuilder 配合 planner() 方法注入自定义规划器实例
		//    SequentialPlanner 会在 init() 阶段从 ctx.subagents() 获取 agent 列表，
		//    并在 nextAction() 中按固定顺序返回 call 动作
		UntypedAgent agent = AgenticWorkflows.plannerBuilder()
				.planner(() -> new SequentialPlanner())
				.subAgents(stepA, stepB)
				.outputKey("quoteResult")
				.build();

		// 3. 执行
		return String.valueOf(agent.invoke(Map.of("orderId", "ORD-2025")));
	}

	// ------------------------------------------------------------------
	// 辅助：演示用的脚本模型（无模型演示使用常量）
	// ------------------------------------------------------------------

	private static final class DemoModels {
		/** 固定回显的脚本模型：仅用于「无模型可跑」的构建 / 执行演示，不访问网络。 */
		static final ChatModel SCRIPTED_MODEL = new ChatModel() {
			@Override
			public dev.langchain4j.model.chat.response.ChatResponse doChat(
					dev.langchain4j.model.chat.request.ChatRequest chatRequest) {
				return dev.langchain4j.model.chat.response.ChatResponse.builder()
						.aiMessage(dev.langchain4j.data.message.AiMessage.from("demo-scripted-output"))
						.build();
			}
		};
	}

	// ------------------------------------------------------------------
	// 演示用的 Agent 接口（模拟业务逻辑）
	// ------------------------------------------------------------------

	/** 报价单第一步：报价查询。 */
	public interface PriceQuoteAgent {
		@Agent
		@UserMessage("查询订单 %s 的报价")
		String quote(@V("orderId") String orderId);
	}

	/** 报价单第二步：折扣计算。 */
	public interface DiscountQuoteAgent {
		@Agent
		@UserMessage("计算订单 %s 的折扣报价")
		String quote(@V("orderId") String orderId);
	}

	/** 报单代理：顶层接口。 */
	public interface OrderPlannerAgent {
		@Agent
		@UserMessage("处理订单 %s 的报价单")
		String run(@V("orderId") String orderId);
	}

	/** plannerBuilderIntroDemo 演示入口演示（无模型）。
	 *
	 * <p>展示如何通过 {@link AgenticWorkflows#plannerBuilder(Class)} 入口注入自定义规划器。</p>
	 *
	 * @return 说明文字
	 */
	public static String plannerBuilderIntroDemo() {
		// 演示入口使用示例（伪码，实际以代码为准）
		// UntypedAgent agent = AgenticWorkflows.plannerBuilder()
		//         .planner(() -> new SequentialPlanner())
		//         .subAgents(/* sub agents */)
		//         .outputKey("quoteResult")
		//         .build();
		return "plannerBuilder 演示入口已就绪，详见代码注释";
	}

	/**
	 * 样例自带的确定性规划器：按 {@code init()} 阶段拿到的子 agent 顺序依次调用，
	 * 不依赖 LLM 决策——用于演示 {@link AgenticWorkflows#plannerBuilder()} 的自定义规划器注入。
	 */
	static final class SequentialPlanner implements Planner {

		private java.util.List<dev.langchain4j.agentic.internal.AgentExecutor> agents;
		private int cursor;

		@Override
		public void init(dev.langchain4j.agentic.planner.InitPlanningContext ctx) {
			this.agents = ctx.subagents().stream()
					.map(dev.langchain4j.agentic.internal.AgentExecutor.class::cast)
					.toList();
		}

		@Override
		public dev.langchain4j.agentic.planner.Action nextAction(
				dev.langchain4j.agentic.planner.PlanningContext ctx) {
			if (cursor >= agents.size()) {
				dev.langchain4j.agentic.scope.AgentInvocation previous = ctx.previousAgentInvocation();
				return previous != null ? done(previous.output()) : done();
			}
			return new dev.langchain4j.agentic.planner.Action.AgentCallAction(agents.get(cursor++));
		}
	}
}