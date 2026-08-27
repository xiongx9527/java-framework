package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

public interface MagicTinyflowKnowledgePermissionEvaluator {
	void check(String tenantId, String action, String resourceId, MagicHttpServletRequest request);
}
