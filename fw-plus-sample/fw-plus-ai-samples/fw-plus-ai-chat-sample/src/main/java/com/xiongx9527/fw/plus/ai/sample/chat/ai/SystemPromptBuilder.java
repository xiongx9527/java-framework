package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import org.springframework.stereotype.Component;

/**
 * 拼装系统提示：固定人设 + 个人资料 + 本轮检索资料。
 *
 * <p><b>为什么不用 {@code AiServices.systemMessageProvider}</b>：它的入参只有 memoryId
 * （{@code Function<Object, String>}，0.35.0 到 1.x 都没变），拿不到用户消息，而检索必须有 query。
 * profile 按 memoryId 查得到，仍走 provider；检索资料靠 query，改走 retrievalAugmentor。</p>
 *
 * <p><b>为什么不用框架的 {@code retrievalAugmentor}</b>：它把检索结果拼进 user message，
 * 随后 {@code DefaultAiServices.appendOutputFormatInstructions} 用
 * {@code UserMessage.from(text)} 无条件重建消息（源码 :323-332），自定义消息类型全部丢失；
 * 更要紧的是那段内容会随 user message 永久沉积进会话记忆，与后续问题无关却每轮重复计费。</p>
 *
 * <p>写进 memory 的是 {@code SystemMessage}，{@code MessageWindowChatMemory.add} 对它是
 * 「内容相同则跳过、不同则替换」语义，所以每轮只有一条、不会堆积。</p>
 */
@Component
public class SystemPromptBuilder {

	private static final String CONVENTIONS =
			"你是 fw-plus 内置智能助手，遵守以下约定：\n"
			+ "1. 全程使用简体中文，回答简洁专业，不做无关寒暄；\n"
			+ "2. 涉及天气、数据、时间的问题只依据工具返回的结果作答，禁止编造或推测；\n"
			+ "3. 以下资料仅供参考，与问题无关时不要引用，无法确认时明确说明不知道；\n"
			+ "4. 遇到超出能力的问题，说明限制并给出可行的替代建议；\n"
			+ "5. 没有用户资料时不要猜测对方身份，按匿名用户回答。";

	private final UserProfileProvider userProfileProvider;

	public SystemPromptBuilder(UserProfileProvider userProfileProvider) {
		this.userProfileProvider = userProfileProvider;
	}

	/**
	 * 拼装完整系统提示：固定人设 + 个人资料。
	 *
	 * <p>检索资料不在这里——它由 {@code retrievalAugmentor} 注入 user message，见
	 * {@code RagAutoConfig}。</p>
	 *
	 * @param memoryId 会话标识，用于查个人资料
	 * @return 系统提示全文，可直接包成 {@code SystemMessage} 写进 memory
	 */
	public String build(Object memoryId) {
		StringBuilder text = new StringBuilder(CONVENTIONS);

		UserProfileProvider.UserProfile profile = userProfileProvider.find(memoryId);
		if (profile != null) {
			text.append('\n').append(profile.render());
		}
		return text.toString();
	}
}
