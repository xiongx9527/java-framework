package com.xiongx9527.fw.plus.common.exception;

import com.xiongx9527.fw.plus.common.enums.ErrorCategory;
import com.xiongx9527.fw.plus.common.enums.ErrorSource;
import lombok.Getter;

/**
 * 业务异常，携带 8 位统一状态码、错误消息和可选的附加数据。
 *
 * <h3>状态码结构（8 位数字，分 3 段）</h3>
 * <pre>
 *   40001001
 *   ││ │││ │││
 *   ││ │││ └── 第 6-8 位：具体错误（001-999），由 specific 参数指定
 *   ││ └─── 第 3-5 位：错误分类（1-999），对应 {@link ErrorCategory}
 *   └───── 第 1-2 位：错误来源（40/50），对应 {@link ErrorSource}
 *
 *   40 = 客户端错误  50 = 服务端错误
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>
 *   // 基础用法：来源 + 分类 + 编号 + 消息
 *   throw new BizException(ErrorSource.CLIENT, ErrorCategory.REQUEST, 1, "重复提交");
 *   // → code = 40,001,001
 *
 *   // 带附加数据
 *   throw new BizException(ErrorSource.CLIENT, ErrorCategory.BUSINESS, 1, "库存不足", Map.of("sku", "A001"));
 *   // → code = 40,005,001
 *
 *   // 带原始异常
 *   throw new BizException(ErrorSource.SERVER, ErrorCategory.DATABASE, 1, "连接超时", e);
 *   // → code = 50,009,001
 *
 *   // 快捷方法
 *   throw BizException.client(ErrorCategory.REQUEST, 1, "重复提交");    // code = 40,001,001
 *   throw BizException.server(ErrorCategory.DATABASE, 1, "连接超时");    // code = 50,009,001
 *   throw BizException.param(1, "参数缺失");                              // code = 40,001,001
 *   throw BizException.biz(1, "库存不足");                               // code = 40,005,001
 *   throw BizException.internal(1, "系统繁忙");                           // code = 50,010,001
 * </pre>
 *
 * @see ErrorSource      错误来源
 * @see ErrorCategory    错误分类
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_CODE = 50_999_000;

    /** 8 位统一状态码 */
    private final int code;
    /** 附加数据，可为 null */
    private final Object data;

    /** 通用异常 */
    public BizException() {
        super();
        this.code = DEFAULT_CODE;
        this.data = null;
    }

    /** 通用异常 + 消息 */
    public BizException(String message) {
        super(message);
        this.code = DEFAULT_CODE;
        this.data = null;
    }

    /** 通用异常 + 消息 + 原始异常 */
    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = DEFAULT_CODE;
        this.data = null;
    }

    /** 通用异常 + 格式化消息 */
    public BizException(String msgFormat, Object... args) {
        super(String.format(msgFormat, args));
        this.code = DEFAULT_CODE;
        this.data = null;
    }

    /** 通用异常 + 原始异常 */
    public BizException(Throwable cause) {
        super(cause);
        this.code = DEFAULT_CODE;
        this.data = null;
    }

    /**
     * 自由组合错误码
     * @param source   错误来源，决定第 1-2 位
     * @param category 错误分类，决定第 3-5 位
     * @param specific 具体错误编号（0-999），自动补零到 3 位
     * @param message  错误消息
     */
    public BizException(ErrorSource source, ErrorCategory category, int specific, String message) {
        super(message);
        this.code = buildCode(source, category, specific);
        this.data = null;
    }

    /** 自由组合 + 附加数据 */
    public BizException(ErrorSource source, ErrorCategory category, int specific, String message, Object data) {
        super(message);
        this.code = buildCode(source, category, specific);
        this.data = data;
    }

    /** 自由组合 + 原始异常 */
    public BizException(ErrorSource source, ErrorCategory category, int specific, String message, Throwable cause) {
        super(message, cause);
        this.code = buildCode(source, category, specific);
        this.data = null;
    }

    // ═══════════════════ 快捷静态方法 ═══════════════════

    /** 客户端错误 */
    public static BizException client(ErrorCategory category, int specific, String message) {
        return new BizException(ErrorSource.CLIENT, category, specific, message);
    }
    public static BizException client(ErrorCategory category, int specific, String message, Object data) {
        return new BizException(ErrorSource.CLIENT, category, specific, message, data);
    }

    /** 服务端错误 */
    public static BizException server(ErrorCategory category, int specific, String message) {
        return new BizException(ErrorSource.SERVER, category, specific, message);
    }
    public static BizException server(ErrorCategory category, int specific, String message, Object data) {
        return new BizException(ErrorSource.SERVER, category, specific, message, data);
    }

    /** 参数错误（CLIENT + REQUEST + specific） */
    public static BizException param(int specific, String message) {
        return new BizException(ErrorSource.CLIENT, ErrorCategory.REQUEST, specific, message);
    }
    public static BizException param(int specific, String message, Object data) {
        return new BizException(ErrorSource.CLIENT, ErrorCategory.REQUEST, specific, message, data);
    }

    /** 业务错误（CLIENT + BUSINESS + specific） */
    public static BizException biz(int specific, String message) {
        return new BizException(ErrorSource.CLIENT, ErrorCategory.BUSINESS, specific, message);
    }
    public static BizException biz(int specific, String message, Object data) {
        return new BizException(ErrorSource.CLIENT, ErrorCategory.BUSINESS, specific, message, data);
    }

    /** 服务端内部错误（SERVER + INFRASTRUCTURE + specific） */
    public static BizException internal(int specific, String message) {
        return new BizException(ErrorSource.SERVER, ErrorCategory.INFRASTRUCTURE, specific, message);
    }
    public static BizException internal(int specific, String message, Object data) {
        return new BizException(ErrorSource.SERVER, ErrorCategory.INFRASTRUCTURE, specific, message, data);
    }

    // ═══════════════════ 内部 ═══════════════════

    /**
     * 组装 8 位状态码：source(2位) + category(3位) + specific(3位)
     *
     * <pre>
     *   buildCode(CLIENT, REQUEST, 1)
     *   = 40 × 1,000,000 + 1 × 1,000 + 1
     *   = 40,000,000 + 1,000 + 1
     *   = 40,001,001
     *   // 解读：40(客户端) + 001(请求/参数) + 001(重复提交)
     * </pre>
     */
    private static int buildCode(ErrorSource source, ErrorCategory category, int specific) {
        if (specific < 0 || specific > 999) {
            throw new IllegalArgumentException("specific 必须在 0-999 之间: " + specific);
        }
        int code = source.getCode() * 1_000_000 + category.getCode() * 1_000 + specific;
        if (code < 10_000_000 || code > 99_999_999) {
            throw new IllegalStateException("状态码非 8 位: " + code);
        }
        return code;
    }
}
