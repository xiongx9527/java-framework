package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelAttempt;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelUsage;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataCheckpointStore;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 将旧 Flow 检查点单向写入 Core 会话与检查点 SPI。
 *
 * <p>完整恢复快照以 JSON 保存到 Core 会话存储，Core 检查点存储负责保存严格递增的阶段版本和 SQL 重放状态。
 * 恢复 token 使用同一隔离上下文建立索引。适配方向固定为旧 Flow 到 Core，Core SPI 不依赖自动配置层 DTO。</p>
 */
public final class CoreAskDataConversationStoreAdapter implements AskDataConversationStore {

	private static final String RESUME_PREFIX = "resume:";

	private final com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore conversationStore;
	private final AskDataCheckpointStore checkpointStore;
	private final ObjectMapper objectMapper;

	public CoreAskDataConversationStoreAdapter(
			com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore conversationStore,
			AskDataCheckpointStore checkpointStore) {
		if (conversationStore == null || checkpointStore == null) {
			throw new IllegalArgumentException("Core conversationStore 和 checkpointStore 不能为空");
		}
		this.conversationStore = conversationStore;
		this.checkpointStore = checkpointStore;
		this.objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("askdata-checkpoint-contracts");
		module.addDeserializer(AskDataModelUsage.class, new ModelUsageDeserializer());
		module.addDeserializer(AskDataModelAttempt.class, new ModelAttemptDeserializer());
		this.objectMapper.registerModule(module);
	}

	@Override
	public AskDataCheckpoint save(AskDataCheckpoint checkpoint) {
		if (checkpoint == null) {
			throw new IllegalArgumentException("checkpoint 不能为空");
		}
		AskDataProjectContext context = new AskDataProjectContext(checkpoint.getProjectId(), checkpoint.getOperatorId());
		return save(context, checkpoint);
	}

	@Override
	public AskDataCheckpoint save(AskDataProjectContext context, AskDataCheckpoint checkpoint) {
		if (!AskDataConversationStore.isBoundTo(context, checkpoint)) {
			throw new IllegalArgumentException("Checkpoint does not match the requested project and operator scope");
		}
		try {
			String payload = objectMapper.writeValueAsString(checkpoint);
			com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore.Conversation conversation =
					new com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore.Conversation(
							checkpoint.getRequestSummary(), payload);
			conversationStore.save(context, checkpoint.getConversationId(), conversation);
			if (notBlank(checkpoint.getResumeToken())) {
				conversationStore.save(context, RESUME_PREFIX + checkpoint.getResumeToken(), conversation);
			}
			String checkpointKey = checkpoint.getConversationId();
			AskDataCheckpointStore.Checkpoint previous = checkpointStore.load(context, checkpointKey);
			long version = Math.max(System.currentTimeMillis(), previous == null ? 1L : previous.getVersion() + 1L);
			checkpointStore.save(context, checkpointKey, new AskDataCheckpointStore.Checkpoint(version,
					checkpoint.getCurrentStep(), sqlExecutionState(checkpoint)));
			return snapshot(payload);
		} catch (RuntimeException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IllegalStateException("AskData Core 检查点序列化失败", exception);
		}
	}

	@Override
	@Deprecated
	public AskDataCheckpoint loadLatest(String conversationId) {
		return null;
	}

	@Override
	public AskDataCheckpoint loadLatest(AskDataProjectContext context, String conversationId) {
		return load(context, conversationId);
	}

	@Override
	@Deprecated
	public AskDataCheckpoint loadByResumeToken(String resumeToken) {
		return null;
	}

	@Override
	public AskDataCheckpoint loadByResumeToken(AskDataProjectContext context, String resumeToken) {
		return load(context, RESUME_PREFIX + resumeToken);
	}

	private AskDataCheckpoint load(AskDataProjectContext context, String key) {
		if (context == null || !notBlank(key)) {
			return null;
		}
		com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore.Conversation conversation =
				conversationStore.load(context, key);
		if (conversation == null || !notBlank(conversation.getAnswer())) {
			return null;
		}
		AskDataCheckpoint checkpoint = snapshot(conversation.getAnswer());
		return AskDataConversationStore.isBoundTo(context, checkpoint) ? checkpoint : null;
	}

	private AskDataCheckpoint snapshot(String payload) {
		try {
			return objectMapper.readValue(payload, AskDataCheckpoint.class);
		} catch (Exception exception) {
			throw new IllegalStateException("AskData Core 检查点反序列化失败", exception);
		}
	}

	private static AskDataCheckpointStore.SqlExecutionState sqlExecutionState(AskDataCheckpoint checkpoint) {
		if ("EXECUTING".equals(checkpoint.getExecutionState())) {
			return AskDataCheckpointStore.SqlExecutionState.UNKNOWN;
		}
		if (Boolean.TRUE.equals(checkpoint.getSqlExecuted())) {
			return AskDataCheckpointStore.SqlExecutionState.SUCCEEDED;
		}
		if (Boolean.FALSE.equals(checkpoint.getSqlExecuted())) {
			return AskDataCheckpointStore.SqlExecutionState.NOT_EXECUTED;
		}
		return AskDataCheckpointStore.SqlExecutionState.NOT_EXECUTED;
	}

	private static boolean notBlank(String value) {
		return value != null && value.trim().length() > 0;
	}

	private static final class ModelUsageDeserializer extends JsonDeserializer<AskDataModelUsage> {
		@Override
		public AskDataModelUsage deserialize(JsonParser parser, DeserializationContext context) throws IOException {
			JsonNode node = parser.getCodec().readTree(parser);
			JsonNode input = node.get("inputTokens");
			if (input == null || input.isNull()) {
				return AskDataModelUsage.unknown();
			}
			JsonNode output = node.get("outputTokens");
			JsonNode total = node.get("totalTokens");
			if (output == null || output.isNull() || total == null || total.isNull()) {
				return context.reportInputMismatch(AskDataModelUsage.class,
						"AskData 模型用量必须同时包含 input/output/total tokens");
			}
			JsonNode cost = node.get("cost");
			return new AskDataModelUsage(input.longValue(), output.longValue(), total.longValue(),
					cost == null || cost.isNull() ? null : new BigDecimal(cost.asText()));
		}
	}

	private static final class ModelAttemptDeserializer extends JsonDeserializer<AskDataModelAttempt> {
		@Override
		public AskDataModelAttempt deserialize(JsonParser parser, DeserializationContext context) throws IOException {
			JsonNode node = parser.getCodec().readTree(parser);
			JsonNode usageNode = node.get("usage");
			AskDataModelUsage usage = usageNode == null || usageNode.isNull()
					? AskDataModelUsage.unknown()
					: parser.getCodec().treeToValue(usageNode, AskDataModelUsage.class);
			return new AskDataModelAttempt(
					AskDataModelAttempt.ModelType.valueOf(requiredText(node, "modelType", context)),
					requiredText(node, "modelKey", context),
					AskDataModelRoutePolicy.valueOf(requiredText(node, "routePolicy", context)),
					requiredText(node, "routeReason", context),
					AskDataModelAttempt.State.valueOf(requiredText(node, "state", context)),
					optionalText(node, "failureCode"), optionalText(node, "switchReason"),
					optionalText(node, "actualModelKey"), usage);
		}

		private static String requiredText(JsonNode node, String field, DeserializationContext context)
				throws IOException {
			String value = optionalText(node, field);
			if (!notBlank(value)) {
				return context.reportInputMismatch(AskDataModelAttempt.class,
						"AskData 模型尝试缺少字段: %s", field);
			}
			return value;
		}

		private static String optionalText(JsonNode node, String field) {
			JsonNode value = node.get(field);
			return value == null || value.isNull() ? null : value.asText();
		}
	}
}
