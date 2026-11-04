 # fw-plus-ai-interactive

交互编排场景层（与 fw-plus-ai-askdata 平级的框架模块），封装 反问 / 选项 / 智能问卷 / 智能预约 的通用原语与场景封装。

## 定位与边界

- 与 fw-plus-ai-askdata 平级：二者都是 场景 / 领域层，不是 fw-plus-ai-support（support = 把 base 组合）。
- 只依赖 fw-plus-ai-base 与 fw-plus-ai-support，不依赖任何 MCP / skill 实现。MCP 暴露、从 MCP / DB 取数、产品接线都是业务侧职责。
- 内部多轮收集由引擎自身状态机完成，不需要 skill 编排；skill 只在 agent 外层做调哪个场景的可选路由。

## 通用原语（包 com.xiongx9527.fw.plus.ai.interactive）

- Form / Question / ClarifyOption / ClarifyFrame：问卷 / 题目的结构；文案走 titleKey（i18n），选项必须可校验。
- FormCollector：确定性参数收集状态机。缺参由代码判定（不交模型）；落实五条硬约束：一次最多问 3 个、同一 slot 不连续问两次、文案走 i18n、选项可校验、最大轮次降级（返回 DEGRADED）。
- AnswerExtractor（策略接口）：DefaultAnswerExtractor 为规则版，extract(question, utterance) 返回 ExtractionResult（certain=false 时调用方继续反问）。模型版可替换，接口不变。
- QuestionValidator（策略接口）：格式 / 范围 / 必填校验，返回 ValidationResult。
- confirm 包：ConfirmationService（一次性、有有效期、防确认A提交B 的确认令牌）、ConfirmGate（四级确认 L0-L3 判定）、ConfirmTokenStore（默认内存实现 InMemoryConfirmTokenStore）。

## 场景封装

### 智能预约（appointment 包）

- AppointmentItem：预约项目（携带可预约窗口 slots）。
- AppointmentFlow：模板不定——业务传入 Form 模板（不同项目不同字段），时段选项来自 item 的 slots；create(item, template) 开启，toRequest() 返回带 itemId 的 AppointmentRequest。
- 预约项目由业务 / 上游选定后传入，流程内部不做 列出项目供用户选；选预约项目是上游菜单（可另作 MCP 工具）。

### 智能问卷（questionnaire 包）

- Paper：卷子（卷 id + 有序题目）。
- QuestionnaireInstance：通过卷 id 开启实例；current() / previous() / next() 均返回带用户答案的 AnsweredQuestion；submitAnswer(...) 提交当前题；finish() 交卷返回全量 题干名 → 答案。
- QuestionnaireService：按卷 id 登记并开启实例。

## 工具端口（tool 包，MCP / skill 无关）

框架只定义契约与 DTO，业务据此类实现具体工具（内部组合引擎、从 MCP / DB 取数、管会话）：

- ClarifyTool：反问 / 选项的一次性收集对话。
- QuestionnaireTool：卷 id 开实例、上 / 当前 / 下一题、提交、交卷。
- AppointmentTool：预约项目 id 开实例、提交、交卷（返回 AppointmentRequest）。

会话 DTO：ClarifySession / QuestionnaireSession / AppointmentSession。输出复用 ClarifyFrame / AnsweredQuestion / CollectionResult / AppointmentRequest，均为 JSON 友好 POJO，可直接序列化给模型或前端。

## 约定

- 异常：InteractiveException（50_008_005）、ConfirmationException（50_008_006），均为 BizException 子类。
- Java 8 兼容（不使用 text blocks / Map.of / records）。
- 主代码零硬编码中文文案（中文 Javadoc 允许）；文案统一走 titleKey。

## 快速示例

FormCollector 反问 / 参数收集:

    Form form = Form.builder().id("demo").titleKey("demo.title")
        .questions(Arrays.asList(
            Question.builder().name("date").kind(Question.Kind.TEXT).required(true)
                .pattern("(\d{4}-\d{2}-\d{2})").build()))
        .build();
    FormCollector c = new FormCollector(form);
    CollectionResult r = c.submitUtterance("日期是2026-11-10", new DefaultAnswerExtractor());
    // r.getStatus() == COMPLETE, c.getAnswers().get("date") == "2026-11-10"

AppointmentFlow 智能预约:

    List<ClarifyOption> slots = Arrays.asList(
        ClarifyOption.builder().id("s1").label("11-10 09:00").build());
    AppointmentItem item = AppointmentItem.builder().id("room-a").name("会议室A").slots(slots).build();
    Form tpl = Form.builder().id("ap").titleKey("t")
        .questions(Arrays.asList(AppointmentFlow.timeSlotQuestion(item))).build();
    AppointmentFlow flow = AppointmentFlow.create(item, tpl);
    flow.submitAnswer(AppointmentFlow.Q_TIME_SLOT, "s1");
    AppointmentRequest req = flow.toRequest(); // req.getItemId() == "room-a"

左问卷 QuestionnaireInstance:

    QuestionnaireInstance inst = QuestionnaireInstance.open(paper);
    AnsweredQuestion q = inst.current();        // 当前题（含答案）
    inst.submitAnswer("张三");
    AnsweredQuestion n = inst.next();           // 下一题（含答案）
    Map<String, Object> answers = inst.finish(); // 交卷，返回全量 题干名 → 答案
