package com.xiongx9527.fw.plus.ai.askdata.publishing;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AskDataPublishingContractTest {

	@Test
	void 发布SPI必须将项目操作人上下文交给实现() {
		AskDataInterfacePublisher publisher = new AskDataInterfacePublisher() {
			@Override public AskDataPublishedInterface publish(AskDataProjectContext context, AskDataPublishRequest request) {
				return new AskDataPublishedInterface(context.getProjectId() + "-" + context.getOperatorId(), request.getName(), request.getDefinitionId());
			}
			@Override public void unpublish(AskDataProjectContext context, String interfaceId) { }
		};
		assertEquals("project-operator", publisher.publish(new AskDataProjectContext("project", "operator"),
				new AskDataPublishRequest("orders", "definition")).getId());
	}
}
