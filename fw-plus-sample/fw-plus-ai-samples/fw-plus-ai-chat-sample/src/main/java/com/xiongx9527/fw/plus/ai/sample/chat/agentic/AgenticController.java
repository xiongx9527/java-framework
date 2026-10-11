package com.xiongx9527.fw.plus.ai.sample.chat.agentic;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticPipelineRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticSupervisor;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agentic 智能体编排的 REST 入口：<b>每个编排模式一个可运行案例</b>（对应
 * {@code docs/langchain4j-agentic-guide.md} 的 API 事实）。
 *
 * <p>所有模式案例都调用门面 {@link AgenticWorkflowsDemo} 的 {@code runXxx} 执行方法，
 * 「端点 / 测试 / 文档」共用同一份代码，避免三处各写一套。每个模式「何时该用」在
 * {@link AgenticWorkflowsDemo} 类文档有完整「模式速查」，本类在每个端点上再就地写清
 * <b>使用场景 / 请求体 / 是否需模型 / 响应形态</b>四件事，便于直接对着本文件调试。</p>
 *
 * <h3>端点 → 模式 → 使用场景（速查）</h3>
 * <ul>
 *     <li>{@code POST /map} → <b>parallelMapper</b>：对一批同类元素做同一种处理（列表逐项映射）。</li>
 *     <li>{@code POST /supervisor} → <b>supervisor</b>：不确定调哪些 agent、什么顺序，交给监督者模型自主决策调度。</li>
 *     <li>{@code POST /typed-agent} → <b>typed agent</b>：agent 有稳定输入输出契约，要类型安全且可复用。</li>
 *     <li>{@code POST /untyped-agent} → <b>untyped agent</b>：只要「一个提示词 + 一个输出键」，或运行期动态拼装。</li>
 *     <li>{@code POST /sequence} → <b>sequence</b>：步骤与顺序都已知的固定流水线（如「抽取 → 回答」）。</li>
 *     <li>{@code POST /loop} → <b>loop</b>：反复迭代同一 agent 直到满足退出条件（而非固定步数）。</li>
 *     <li>{@code POST /conditional} → <b>conditional</b>：按运行期状态在分支间择一（意图路由、多策略选择）。</li>
 *     <li>{@code POST /parallel} → <b>parallel</b>：多个互不依赖的子任务并行以缩短总耗时。</li>
 *     <li>{@code POST /planner} → <b>planner</b>：模型自主编排且更强调显式规划，适合多步骤、有依赖的自主编排。</li>
 *     <li>{@code POST /declarative} → <b>declarative</b>：整条编排可用注解静态声明，「接口即系统」、少样板。</li>
 *     <li>{@code POST /conditional-offline} → <b>conditional（离线）</b>：分支路由与模型无关时可离线验证。</li>
 *     <li>{@code POST /human-in-the-loop} → <b>human-in-the-loop</b>：流程中需要人工审批 / 补充 / 确认后继续。</li>
 *     <li>{@code POST /graph} → <b>graph</b>：流程是有向图（条件边 / 环 / 分支汇聚），需显式图结构或挂为工具。</li>
 *     <li>{@code POST /observability} → <b>observability</b>：需要观测 / 审计编排执行（输入输出、耗时、token、失败）。</li>
 *     <li>{@code POST /tool-provider} → <b>agent-as-tool</b>：把整条编排当作工具交给上层模型自主决定何时调用。</li>
 *     <li>{@code GET /patterns} → <b>索引</b>：先拿到「离线可跑 / 需要模型」两份端点清单。</li>
 *     <li>{@code GET /pipelines} → <b>registry</b>：查看注册中心已注册的编排 key 全集。</li>
 * </ul>
 *
 * <h3>执行模型（构建与执行分离）</h3>
 * <p>所有需要模型的编排都在方法体内<b>现场构建、现场 invoke</b>：构建阶段只装配对象、
 * 不触达模型与网络，只有本端点被请求时才真正 {@code invoke(...)}。因此无 Ollama 时，
 * 需模型的端点会返回 500（构建成功、调用失败），而离线端点始终可跑。</p>
 *
 * <h3>是否需模型一览</h3>
 * <ul>
 *     <li><b>离线可跑</b>（无需 Ollama）：{@code /map}、{@code /conditional-offline}、
 *         {@code /human-in-the-loop}、{@code /graph}、{@code /observability}、
 *         {@code /tool-provider}、{@code /patterns}、{@code /pipelines}。</li>
 *     <li><b>需要模型</b>（需本机 Ollama）：{@code /typed-agent}、{@code /untyped-agent}、
 *         {@code /sequence}、{@code /loop}、{@code /conditional}、{@code /parallel}、
 *         {@code /supervisor}、{@code /planner}、{@code /declarative}。</li>
 * </ul>
 *
 * <h3>响应形态</h3>
 * <ul>
 *     <li>{@code /patterns}：{@code {"offline":[...],"modelRequired":[...]}}（两组端点名索引）</li>
 *     <li>{@code /map}：{@code {"pipeline":"item-map","items":[...],"results":[...]}}</li>
 *     <li>{@code /supervisor}：{@code {"pipeline":"supervisor","reply":"..."}}</li>
 *     <li>{@code /pipelines}：{@code {"keys":[...]}}（注册中心已注册的编排 key 全集）</li>
 *     <li>其余模式案例统一为：{@code {"pattern":"<模式名>","result":"<字符串结果>"}}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/chat/agentic")
public class AgenticController {

	private final AgenticPipelineRegistry registry;

	private final UntypedAgent itemMapPipeline;

	private final AgenticSupervisor supervisor;

	private final ChatModel chatModel;

	public AgenticController(AgenticPipelineRegistry registry, UntypedAgent itemMapPipeline,
			AgenticSupervisor supervisor, ChatModel chatModel) {
		this.registry = registry;
		this.itemMapPipeline = itemMapPipeline;
		this.supervisor = supervisor;
		this.chatModel = chatModel;
	}

	// ------------------------------------------------------------------
	// 索引与注册中心
	// ------------------------------------------------------------------

	/**
	 * 模式索引：返回两份端点清单——{@code offline}（无需模型、任何环境可跑）与
	 * {@code modelRequired}（需本机 Ollama 可用）。
	 *
	 * <p><b>使用场景</b>：客户端 / 调试者先取清单，据此决定「先跑哪些能立刻看到结果」，
	 * 或在 CI / 离线环境自动跳过需模型的端点。</p>
	 *
	 * <p><b>请求体</b>：无。<br>
	 * <b>是否需模型</b>：否。<br>
	 * <b>响应</b>：{@code {"offline":[...],"modelRequired":[...]}}</p>
	 */
	@GetMapping("/patterns")
	public ResponseEntity<Map<String, Object>> patterns() {
		return ResponseEntity.ok(Map.of(
				"offline", List.of("map", "conditional-offline", "human-in-the-loop", "graph",
						"observability", "tool-provider", "pipelines"),
				"modelRequired", List.of("typed-agent", "untyped-agent", "sequence", "loop",
						"conditional", "parallel", "supervisor", "planner", "declarative")));
	}

	/**
	 * 注册中心现状：返回 {@link AgenticPipelineRegistry} 中已注册的编排 key 全集。
	 *
	 * <p><b>使用场景</b>：确认某条编排是否已注册、排查按 key 取用（{@code agenticToolProviderByKey}）
	 * 时的「未注册」异常，或观察配置热更 / 启动装配后注册表是否符合预期。</p>
	 *
	 * <p><b>请求体</b>：无。<br>
	 * <b>是否需模型</b>：否。<br>
	 * <b>响应</b>：{@code {"keys":[...]}}</p>
	 */
	@GetMapping("/pipelines")
	public ResponseEntity<Map<String, Object>> pipelines() {
		Set<String> keys = registry.keys();
		return ResponseEntity.ok(Map.of("keys", keys));
	}

	// ------------------------------------------------------------------
	// parallelMapper（离线）与 supervisor（需模型）
	//
	// 这两个端点直接使用装配好的 Bean（itemMapPipeline / supervisor）：
	// - itemMapPipeline 演示「注册中心链路」：装配期注册、调用期按 Bean 复用；
	// - supervisor 演示「监督者句柄调用」：supervisor Bean 由 AgenticDemoConfig 装配。
	// 其余模式统一走 AgenticWorkflowsDemo.runXxx，避免重复装配代码。
	// ------------------------------------------------------------------

	/**
	 * parallelMapper：对 {@code items} 逐项并行执行同一映射 agent，返回映射后的列表。
	 *
	 * <p><b>使用场景</b>：对一批<b>同类元素做同一种处理</b>——列表逐项映射、批量抽取 / 翻译 /
	 * 分类，fan-out 后聚合为一个结果列表。是「注册中心链路」的最小验证：Bean 在装配期注册、
	 * 调用期直接复用，无需模型即可看到完整闭环。</p>
	 *
	 * <p><b>请求体</b>：{@code {"items":["a","b","c"]}}（{@code items} 为必填字符串数组）。<br>
	 * <b>是否需模型</b>：否（映射 agent 为确定性函数）。<br>
	 * <b>响应</b>：{@code {"pipeline":"item-map","items":[...],"results":[...]}}</p>
	 */
	@PostMapping("/map")
	public ResponseEntity<Map<String, Object>> map(@RequestBody Map<String, Object> body) {
		List<String> items = asStringList(body.get("items"));
		Object results = itemMapPipeline.invoke(Map.of("items", items));
		return ResponseEntity.ok(Map.of("pipeline", "item-map", "items", items, "results", results));
	}

	/**
	 * supervisor：监督者自主编排 subAgents 并给出最终答复。
	 *
	 * <p><b>使用场景</b>：<b>不确定该调哪些 agent、以什么顺序调</b>，希望把调度决策交给
	 * 监督者模型——例如开放式问答、需要按问题内容临时组合多个子能力时。</p>
	 *
	 * <p><b>请求体</b>：{@code {"message":"帮我查天气"}}（缺省空串）。<br>
	 * <b>是否需模型</b>：是（模型 Bean 由 {@code ChatAssistantConfig} 装配，需本机 Ollama）。<br>
	 * <b>响应</b>：{@code {"pipeline":"supervisor","reply":"..."}}</p>
	 */
	@PostMapping("/supervisor")
	public ResponseEntity<Map<String, Object>> supervisor(@RequestBody Map<String, Object> body) {
		String message = body.get("message") == null ? "" : String.valueOf(body.get("message"));
		String reply = supervisor.invoke(message);
		return ResponseEntity.ok(Map.of("pipeline", "supervisor", "reply", reply));
	}

	// ------------------------------------------------------------------
	// 单 agent（typed / untyped）——需要模型
	// ------------------------------------------------------------------

	/**
	 * typed agent：{@code EchoAgent.echo(text)}——带 {@code @Agent} 注解的接口代理调用。
	 *
	 * <p><b>使用场景</b>：agent 有稳定的输入 / 输出契约（接口 + {@code @Agent}），需要
	 * <b>类型安全</b>、既能独立调用又能作为 subAgent 复用时。相比 untyped，它把名称、
	 * 描述、输入键、输出键都固化在接口上，适合作为可复用积木。</p>
	 *
	 * <p><b>请求体</b>：{@code {"text":"你好"}}（缺省 {@code "你好"}）。<br>
	 * <b>是否需模型</b>：是。<br>
	 * <b>响应</b>：{@code {"pattern":"typed-agent","result":"..."}}</p>
	 */
	@PostMapping("/typed-agent")
	public ResponseEntity<Map<String, Object>> typedAgent(@RequestBody(required = false) Map<String, Object> body) {
		return reply("typed-agent", AgenticWorkflowsDemo.runTypedAgentDemo(chatModel, text(body, "text", "你好")));
	}

	/**
	 * untyped agent：读 {@code question} → 返回 {@code answer}，产物为 {@link UntypedAgent} 句柄。
	 *
	 * <p><b>使用场景</b>：只想要「一个提示词 + 一个输出键」的<b>轻量 agent</b>，或需要在
	 * 运行期（脚本 / 配置）动态拼装、不便预先定义接口时。代价是丢失编译期契约。</p>
	 *
	 * <p><b>请求体</b>：{@code {"question":"你好"}}（缺省 {@code "你好"}）。<br>
	 * <b>是否需模型</b>：是。<br>
	 * <b>响应</b>：{@code {"pattern":"untyped-agent","result":"..."}}</p>
	 */
	@PostMapping("/untyped-agent")
	public ResponseEntity<Map<String, Object>> untypedAgent(@RequestBody(required = false) Map<String, Object> body) {
		return reply("untyped-agent", AgenticWorkflowsDemo.runUntypedAgentDemo(chatModel, text(body, "question", "你好")));
	}

	// ------------------------------------------------------------------
	// 基础编排 sequence / loop / conditional / parallel —— 需要模型
	// ------------------------------------------------------------------

	/**
	 * sequence：抽取（extract）→ 回答（answer）两步顺序执行。
	 *
	 * <p><b>使用场景</b>：任务可拆成<b>固定的先后步骤</b>且顺序已知，不需要模型决定流程——
	 * 如「抽取 → 回答」「分类 → 检索 → 生成」。前一步的输出经 AgenticScope 按变量名流向下一步。</p>
	 *
	 * <p><b>请求体</b>：{@code {"request":"帮我查天气"}}（缺省 {@code "帮我查天气"}）。<br>
	 * <b>是否需模型</b>：是。<br>
	 * <b>响应</b>：{@code {"pattern":"sequence","result":"..."}}</p>
	 */
	@PostMapping("/sequence")
	public ResponseEntity<Map<String, Object>> sequence(@RequestBody(required = false) Map<String, Object> body) {
		return reply("sequence", AgenticWorkflowsDemo.runSequenceDemo(chatModel, text(body, "request", "帮我查天气")));
	}

	/**
	 * loop：反复执行回答 agent，直到结果以「。」结尾或达到 {@code maxIterations}。
	 *
	 * <p><b>使用场景</b>：需要对同一 agent <b>反复迭代直到满足退出条件</b>（内容合规 / 达到长度 /
	 * 逐轮修正），而<b>不是</b>固定步数时。退出条件基于 AgenticScope 的当前状态求值。</p>
	 *
	 * <p><b>请求体</b>：{@code {"query":"介绍杭州"}}（缺省 {@code "介绍杭州"}）。<br>
	 * <b>是否需模型</b>：是。<br>
	 * <b>响应</b>：{@code {"pattern":"loop","result":"..."}}</p>
	 */
	@PostMapping("/loop")
	public ResponseEntity<Map<String, Object>> loop(@RequestBody(required = false) Map<String, Object> body) {
		return reply("loop", AgenticWorkflowsDemo.runLoopDemo(chatModel, text(body, "query", "介绍杭州")));
	}

	/**
	 * conditional：按 {@code intent} 路由到回答分支（{@code query}）或回显分支（{@code echo}）。
	 *
	 * <p><b>使用场景</b>：执行哪条分支取决于<b>运行期状态</b>（意图、分类、标记）——如意图路由、
	 * 多策略择一。分支与条件可预先声明，但选哪条要等运行时才知道。此处用入参 {@code intent}
	 * 模拟运行期状态。</p>
	 *
	 * <p><b>请求体</b>：{@code {"intent":"query|echo","value":"介绍杭州"}}
	 * （缺省 {@code intent="query"}、{@code value="介绍杭州"}）。<br>
	 * <b>是否需模型</b>：是。<br>
	 * <b>响应</b>：{@code {"pattern":"conditional","result":"..."}}</p>
	 */
	@PostMapping("/conditional")
	public ResponseEntity<Map<String, Object>> conditional(@RequestBody(required = false) Map<String, Object> body) {
		String intent = text(body, "intent", "query");
		String value = text(body, "value", "介绍杭州");
		return reply("conditional", AgenticWorkflowsDemo.runConditionalDemo(chatModel, intent, value));
	}

	/**
	 * parallel：把「翻译」与「写宣传语」两个互不依赖的子任务并行执行，返回 {@code translation}。
	 *
	 * <p><b>使用场景</b>：多个<b>互不依赖</b>的子任务并行以缩短总耗时——如同时翻译 + 生成副标题、
	 * 多路检索后聚合。注意：有先后依赖的子任务应改用 sequence，不要放进 parallel。</p>
	 *
	 * <p><b>请求体</b>：{@code {"title":"杭州"}}（缺省 {@code "杭州"}）。<br>
	 * <b>是否需模型</b>：是。<br>
	 * <b>响应</b>：{@code {"pattern":"parallel","result":"..."}}</p>
	 */
	@PostMapping("/parallel")
	public ResponseEntity<Map<String, Object>> parallel(@RequestBody(required = false) Map<String, Object> body) {
		return reply("parallel", AgenticWorkflowsDemo.runParallelDemo(chatModel, text(body, "title", "杭州")));
	}

	// ------------------------------------------------------------------
	// planner / 声明式 createAgenticSystem —— 需要模型
	// ------------------------------------------------------------------

	/**
	 * planner：Planner LLM 依据 agent 拓扑<b>自主编排</b> agents 执行。
	 *
	 * <p><b>使用场景</b>：与 supervisor 同属「模型自主编排」，但更强调<b>显式规划</b>——适合
	 * 多步骤、步骤间有依赖的自主任务。supervisor 见 {@code POST /supervisor}，二者对比着看。</p>
	 *
	 * <p><b>请求体</b>：{@code {"request":"帮我查天气"}}（缺省 {@code "帮我查天气"}）。<br>
	 * <b>是否需模型</b>：是。<br>
	 * <b>响应</b>：{@code {"pattern":"planner","result":"..."}}</p>
	 */
	@PostMapping("/planner")
	public ResponseEntity<Map<String, Object>> planner(@RequestBody(required = false) Map<String, Object> body) {
		return reply("planner", AgenticWorkflowsDemo.runPlannerDemo(chatModel, text(body, "request", "帮我查天气")));
	}

	/**
	 * declarative：用注解（{@code @SequenceAgent} 等）静态声明的顺序系统，接口代理直接调用。
	 *
	 * <p><b>使用场景</b>：整条编排可在<b>编译期用注解写死</b>——希望「接口即系统」、少样板、
	 * 易读易维护，且不想在运行期手工 {@code .subAgents(...).build()} 时。</p>
	 *
	 * <p><b>请求体</b>：{@code {"request":"帮我查天气"}}（缺省 {@code "帮我查天气"}）。<br>
	 * <b>是否需模型</b>：是。<br>
	 * <b>响应</b>：{@code {"pattern":"declarative","result":"..."}}</p>
	 */
	@PostMapping("/declarative")
	public ResponseEntity<Map<String, Object>> declarative(@RequestBody(required = false) Map<String, Object> body) {
		return reply("declarative", AgenticWorkflowsDemo.runDeclarativeDemo(chatModel, text(body, "request", "帮我查天气")));
	}

	// ------------------------------------------------------------------
	// 离线可跑的模式（无需模型，任何环境可断言结果）
	// ------------------------------------------------------------------

	/**
	 * conditional 的<b>离线形态</b>：按 {@code mode} 路由到大写或小写映射，无需模型。
	 *
	 * <p><b>使用场景</b>：当<b>分支路由逻辑本身与模型无关</b>时，可先用确定性 agent 离线验证
	 * 编排骨架（分支选择、输出键汇聚），确认无误后再把分支换成真实模型 agent——
	 * 这是排查「是编排写错了还是模型没通」的最低成本手段。</p>
	 *
	 * <p><b>请求体</b>：{@code {"mode":"upper|lower","item":"hello"}}
	 * （缺省 {@code mode="upper"}、{@code item="hello"}）。<br>
	 * <b>是否需模型</b>：否。<br>
	 * <b>响应</b>：{@code {"pattern":"conditional-offline","result":"..."}}</p>
	 */
	@PostMapping("/conditional-offline")
	public ResponseEntity<Map<String, Object>> conditionalOffline(@RequestBody(required = false) Map<String, Object> body) {
		String mode = text(body, "mode", "upper");
		String item = text(body, "item", "hello");
		return reply("conditional-offline", AgenticWorkflowsDemo.runConditionalOfflineDemo(mode, item));
	}

	/**
	 * human-in-the-loop：把人工响应节点挂到 sequence 上，执行到该节点时取用人工响应后继续。
	 *
	 * <p><b>使用场景</b>：流程中间需要<b>人工审批 / 补充 / 确认</b>后才能继续——如审批流、
	 * 工单确认、敏感操作的二次确认。示例用固定响应模拟人工输入，真实场景由前端 / 回调注入。</p>
	 *
	 * <p><b>请求体</b>：无（示例返回固定人工响应）。<br>
	 * <b>是否需模型</b>：否。<br>
	 * <b>响应</b>：{@code {"pattern":"human-in-the-loop","result":"..."}}</p>
	 */
	@PostMapping("/human-in-the-loop")
	public ResponseEntity<Map<String, Object>> humanInTheLoop() {
		return reply("human-in-the-loop", AgenticWorkflowsDemo.runHitlDemo());
	}

	/**
	 * graph：用 langgraph4j 的 {@code StateGraph} 建图 → 编译 → 执行（节点为确定性函数）。
	 *
	 * <p><b>使用场景</b>：流程是<b>有向图</b>——含条件边、环、分支汇聚，需要显式图结构来表达；
	 * 或要把编译后的图挂为工具（{@code graphToolProvider}）交给模型调度时。本例是
	 * {@code START → upper → shout → END} 的最小链路。</p>
	 *
	 * <p><b>请求体</b>：无（入参 {@code text="hello"} 写死在示例内）。<br>
	 * <b>是否需模型</b>：否（节点为纯函数）。<br>
	 * <b>响应</b>：{@code {"pattern":"graph","result":"HELLO!"}}</p>
	 */
	@PostMapping("/graph")
	public ResponseEntity<Map<String, Object>> graph() {
		return reply("graph", AgenticWorkflowsDemo.runGraphDemo());
	}

	/**
	 * observability：给非 AI 编排挂 {@code AgentListener}，返回结果与监听到的 agent 调用次数。
	 *
	 * <p><b>使用场景</b>：需要<b>观测 / 审计编排执行</b>——每个 agent 的输入输出、耗时、token、
	 * 失败，用于监控、日志、计费或根因分析时。{@code AgentListener} 是切片，可挂到任意编排上，
	 * 与是否使用模型无关。</p>
	 *
	 * <p><b>请求体</b>：无（入参 {@code item="observe"} 写死在示例内）。<br>
	 * <b>是否需模型</b>：否。<br>
	 * <b>响应</b>：{@code {"pattern":"observability","result":"OBSERVE / agent 调用次数=1"}}</p>
	 */
	@PostMapping("/observability")
	public ResponseEntity<Map<String, Object>> observability() {
		return reply("observability", AgenticWorkflowsDemo.runObservabilityDemo());
	}

	/**
	 * agent-as-tool：注册中心注册编排 → 按 key 包装为 {@code ToolProvider} → 执行工具。
	 *
	 * <p><b>使用场景</b>：把<b>整条编排当作一个工具</b>交给上层模型（或脚本）自主决定何时调用——
	 * 例如把「列表映射」能力暴露为 {@code map_items} 工具。注册中心版按 key 取用编排；
	 * 不经注册中心的就地包装见 {@link AgenticWorkflowsDemo#directToolProviderDemo()}。</p>
	 *
	 * <p><b>请求体</b>：无（工具入参 {@code {"items":["x","y"]}} 写死在示例内）。<br>
	 * <b>是否需模型</b>：否（只演示工具包装与执行链路，未经过 LLM 调度）。<br>
	 * <b>响应</b>：{@code {"pattern":"tool-provider","result":"[X, Y]"}}</p>
	 */
	@PostMapping("/tool-provider")
	public ResponseEntity<Map<String, Object>> toolProvider() {
		return reply("tool-provider", AgenticWorkflowsDemo.runToolProviderExecutionDemo());
	}

	/**
	 * 统一响应封装：{@code {"pattern":"<模式名>","result":"<字符串结果>"}}。
	 */
	private static ResponseEntity<Map<String, Object>> reply(String pattern, String result) {
		return ResponseEntity.ok(Map.of("pattern", pattern, "result", result));
	}

	/**
	 * 从请求体取字符串字段，缺省时回落到默认值（{@code body} 可为 {@code null}）。
	 */
	private static String text(Map<String, Object> body, String key, String defaultValue) {
		if (body == null || body.get(key) == null) {
			return defaultValue;
		}
		return String.valueOf(body.get(key));
	}

	/**
	 * 把请求体 {@code items} 字段转为字符串列表；缺失或类型不符时抛
	 * {@link IllegalArgumentException}（由全局异常处理转 4xx）。
	 */
	private static List<String> asStringList(Object value) {
		if (!(value instanceof java.util.Collection)) {
			throw new IllegalArgumentException("items 必须是字符串数组");
		}
		return ((java.util.Collection<?>) value).stream()
				.map(String::valueOf)
				.toList();
	}
}
