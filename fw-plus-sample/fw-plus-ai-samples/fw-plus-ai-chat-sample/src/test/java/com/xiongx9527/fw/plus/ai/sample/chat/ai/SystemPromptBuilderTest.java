package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SystemPromptBuilder}：人设常在、RAG 段与 profile 段按有无拼装。
 */
class SystemPromptBuilderTest {

	private final UserProfileProvider profiles = new UserProfileProvider();
	private final SystemPromptBuilder builder = new SystemPromptBuilder(profiles);

	@Test
 void containsConventionsWithoutRagOrProfile() {
		String text = builder.build("ghost");

		assertTrue(text.contains("fw-plus 内置智能助手"), text);
		assertTrue(!text.contains("【可参考的资料】"), "检索资料不走系统提示：" + text);
		assertTrue(!text.contains("当前用户："), text);
	}


	@Test
	void appendsProfileWhenRegistered() {
		profiles.save("u-1", new UserProfileProvider.UserProfile("小王", "Asia/Shanghai", "简洁回答"));

		String text = builder.build("u-1");

		assertTrue(text.contains("小王"), text);
		assertTrue(text.contains("Asia/Shanghai"), text);
		assertTrue(text.indexOf("小王") > 0, text);
		assertTrue(!text.contains("【可参考的资料】"), "无资料时不应出现资料标题");
	}


}
