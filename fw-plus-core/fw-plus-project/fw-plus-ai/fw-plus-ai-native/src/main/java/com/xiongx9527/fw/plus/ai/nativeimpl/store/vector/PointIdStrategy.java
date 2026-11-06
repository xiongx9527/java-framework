package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

/**
 * Qdrant point id 生成策略。
 *
 * <p>{@code UUID}（默认）：已是合法 UUID 则透传，否则用 {@code nameUUIDFromBytes} 把字符串 id 哈希成稳定 UUID，
 * 适合大多数场景；{@code RAW}：直接以原始字符串 id 作为 point id，保留数字 / 语义化命名空间
 * （如产品用 long 主键 {@code String.valueOf(rowId)}），无需重建集合即可按 point id 直接操作。</p>
 */
public enum PointIdStrategy {
    UUID,
    RAW
}
