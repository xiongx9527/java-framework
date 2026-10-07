package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

/**
 * memoryId 构建与解析工具：约定 memoryId 格式为 {@code {bizType}:{conversationId}}，
 * 冒号前缀即业务类型（用于存储层 {@code biz_type} 维度），冒号后为会话标识。
 *
 * <p>调用方通过 {@link #build(String, String)} 构造 memoryId，存储层自动解析出业务类型；
 * memoryId 不含冒号时业务类型退化为默认值 {@link #DEFAULT_BIZ_TYPE}，原始值整体作为会话标识。</p>
 */
public final class ChatMemoryIds {

    /** 业务类型与会话标识的分隔符 */
    public static final String SEPARATOR = ":";

    /** memoryId 不含业务类型前缀时的默认业务类型 */
    public static final String DEFAULT_BIZ_TYPE = "default";

    /** 业务类型合法字符：字母、数字、下划线、中划线 */
    private static final String BIZ_TYPE_PATTERN = "^[A-Za-z0-9_-]+$";

    private ChatMemoryIds() {
    }

    /**
     * 构建包含业务类型的 memoryId：{@code {bizType}:{conversationId}}。
     *
     * @throws IllegalArgumentException bizType 为空或包含非法字符 / conversationId 为空
     */
    public static String build(String bizType, String conversationId) {
        if (bizType == null || bizType.trim().isEmpty()) {
            throw new IllegalArgumentException("ChatMemoryIds.build: bizType 不能为空");
        }
        if (!bizType.matches(BIZ_TYPE_PATTERN)) {
            throw new IllegalArgumentException(
                    "ChatMemoryIds.build: 非法业务类型 '" + bizType + "'（仅允许字母、数字、下划线、中划线）");
        }
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("ChatMemoryIds.build: conversationId 不能为空");
        }
        return bizType + SEPARATOR + conversationId;
    }

    /**
     * 从 memoryId 中解析业务类型（冒号前缀）；无冒号时返回 {@code defaultBizType}。
     */
    public static String parseBizType(String memoryId, String defaultBizType) {
        if (memoryId == null) {
            return defaultBizType;
        }
        int index = memoryId.indexOf(SEPARATOR);
        if (index <= 0) {
            return defaultBizType;
        }
        String bizType = memoryId.substring(0, index);
        return bizType.trim().isEmpty() ? defaultBizType : bizType;
    }

    /**
     * 从 memoryId 中解析会话标识（冒号后段）；无冒号时返回原始 memoryId。
     */
    public static String parseConversationId(String memoryId) {
        if (memoryId == null) {
            return null;
        }
        int index = memoryId.indexOf(SEPARATOR);
        return index <= 0 ? memoryId : memoryId.substring(index + 1);
    }
}
