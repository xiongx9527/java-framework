package com.xiongx9527.fw.plus.ai.interactive.confirm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * 人工确认服务（对应文档 §6.33.4 / §6.33.5）。
 *
 * <p>{@link #issue(String, String, long, long)} 针对"动作类型 + 参数快照"生成一次性、有有效期的
 * 确认令牌（令牌内携带动作摘要 {@code actionDigest}）；{@link #verify(ConfirmToken, String, String, long)}
 * 在业务真正提交前校验令牌未被复用、未过期、归属用户一致、且本次参数与签发时完全一致，
 * 防止"确认 A、提交 B"。确认令牌与幂等键职责不同（见 §6.33.5）。</p>
 */
public class ConfirmationService {

    private final ConfirmTokenStore tokenStore;
    private final long defaultTtlMillis;

    public ConfirmationService() {
        this(new InMemoryConfirmTokenStore(), TimeUnit.MINUTES.toMillis(5L));
    }

    public ConfirmationService(ConfirmTokenStore tokenStore, long defaultTtlMillis) {
        if (tokenStore == null) {
            throw new IllegalArgumentException("tokenStore is required");
        }
        this.tokenStore = tokenStore;
        this.defaultTtlMillis = defaultTtlMillis > 0 ? defaultTtlMillis : TimeUnit.MINUTES.toMillis(5L);
    }

    /** 签发确认令牌：digest = sha256(actionType + "|" + paramsCanonical)。 */
    public ConfirmToken issue(String actionType, String paramsCanonical, long uid, long ttlMillis) {
        long now = System.currentTimeMillis();
        String actionDigest = digest((actionType == null ? "" : actionType) + "|"
                + (paramsCanonical == null ? "" : paramsCanonical));
        long ttl = ttlMillis > 0 ? ttlMillis : defaultTtlMillis;
        return ConfirmToken.builder()
                .actionDigest(actionDigest)
                .uid(uid)
                .expiresAt(now + ttl)
                .build();
    }

    /** 提交前校验；全部通过后标记令牌已用（一次性）。失败抛 {@link ConfirmationException}。 */
    public void verify(ConfirmToken token, String actionType, String paramsCanonical, long uid) {
        if (token == null) {
            throw new ConfirmationException("confirm token is missing");
        }
        String tokenId = token.getToken();
        if (tokenStore.isUsed(tokenId)) {
            throw new ConfirmationException("confirm token already used");
        }
        if (token.isExpired()) {
            throw new ConfirmationException("confirm token expired");
        }
        if (token.getUid() != uid) {
            throw new ConfirmationException("confirm token uid mismatch");
        }
        String expected = digest((actionType == null ? "" : actionType) + "|"
                + (paramsCanonical == null ? "" : paramsCanonical));
        if (!expected.equals(token.getActionDigest())) {
            throw new ConfirmationException("confirm token action digest mismatch");
        }
        tokenStore.markUsed(tokenId);
    }

    /** SHA-256 十六进制摘要（Java 8 兼容，不用 HexFormat）。 */
    public static String digest(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
