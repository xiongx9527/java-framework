package com.xiongx9527.fw.plus.common.exception;

import com.xiongx9527.fw.plus.common.enums.ErrorCategory;
import com.xiongx9527.fw.plus.common.enums.ErrorSource;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;

import java.util.Locale;

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
 *   throw new BizException(ErrorSource.CLIENT, ErrorCategory.REQUEST, 101, "重复提交");
 *   // → code = 40,001,101
 *
 *   // 带附加数据
 *   throw new BizException(ErrorSource.CLIENT, ErrorCategory.BUSINESS, 101, "库存不足", Map.of("sku", "A001"));
 *   // → code = 40,005,101
 *
 *   // 带原始异常
 *   throw new BizException(ErrorSource.SERVER, ErrorCategory.DATABASE, 101, "连接超时", e);
 *   // → code = 50,009,101
 *
 *   // 快捷方法
 *   throw BizException.client(ErrorCategory.REQUEST, 101, "重复提交");    // code = 40,001,101
 *   throw BizException.server(ErrorCategory.DATABASE, 101, "连接超时");    // code = 50,009,101
 *   throw BizException.param(101, "参数缺失");                              // code = 40,001,101
 *   throw BizException.biz(101, "库存不足");                               // code = 40,005,101
 *   throw BizException.internal(101, "系统繁忙");                           // code = 50,010,101
 * </pre>
 *
 * <h3>状态码段分配</h3>
 * <ul>
 *   <li><b>框架内置异常类</b>：各子类以 {@code public static final int CODE} 常量自声明固定状态码，
 *       占用各分类下 specific 的 <b>001-099</b> 段（分配表见各异常类 CODE 注释）。</li>
 *   <li><b>业务代码</b>：建议 specific 从 <b>100</b> 起分配，避免与框架内置码冲突。</li>
 * </ul>
 *
 * @see ErrorSource      错误来源
 * @see ErrorCategory    错误分类
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_CODE = 50_999_000;

    /** 8 位统一状态码 */
    private final int code;
    /** 附加数据，可为 null */
    private final Object data;
    /**
     * 国际化文案参数，可为 null。
     *
     * <p>当异常以「状态码 + 参数」方式构造（不传文案）时，参数在此保存，
     * 由 {@link #getMessage()} 在读取时按当前请求语言从资源文件解析文案。</p>
     */
    private final Object[] args;

    /** 获取 8 位统一状态码 */
    public int getCode() {
        return code;
    }

    /** 获取附加数据，可能为 null */
    public Object getData() {
        return data;
    }

    /** 通用异常 */
    public BizException() {
        super();
        this.code = DEFAULT_CODE;
        this.data = null;
        this.args = null;
    }

    /** 通用异常 + 消息 */
    public BizException(String message) {
        super(message);
        this.code = DEFAULT_CODE;
        this.data = null;
        this.args = null;
    }

    /** 通用异常 + 消息 + 原始异常 */
    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = DEFAULT_CODE;
        this.data = null;
        this.args = null;
    }

    /** 通用异常 + 格式化消息 */
    public BizException(String msgFormat, Object... args) {
        super(String.format(msgFormat, args));
        this.code = DEFAULT_CODE;
        this.data = null;
        this.args = null;
    }

    /** 通用异常 + 原始异常 */
    public BizException(Throwable cause) {
        super(cause);
        this.code = DEFAULT_CODE;
        this.data = null;
        this.args = null;
    }

    /**
     * 固定状态码 + 消息：供框架内置异常子类以 {@code CODE} 常量自声明错误码
     * @param code    8 位统一状态码（40/50 开头）
     * @param message 错误消息
     */
    public BizException(int code, String message) {
        super(message);
        this.code = checkCode(code);
        this.data = null;
        this.args = null;
    }

    /**
     * 固定状态码 + 国际化参数（内部构造器）：文案不写死，读取时按当前语言从资源文件解析。
     *
     * <p>参数类型为 {@code Object[]} 数组，与 {@link #BizException(int, String)} 无重载歧义，
     * 供子类以「具体错误码 + 参数」构造使用。</p>
     *
     * @param code 8 位统一状态码
     * @param args 文案占位参数（可为 null 或空数组，表示无占位符）
     */
    protected BizException(int code, Object[] args) {
        super((String) null);
        this.code = checkCode(code);
        this.data = null;
        this.args = args;
    }

    /** 固定状态码 + 消息 + 原始异常 */
    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = checkCode(code);
        this.data = null;
        this.args = null;
    }

    /** 固定状态码 + 原始异常（消息取 {@code cause.toString()}，与 {@link #BizException(Throwable)} 一致） */
    public BizException(int code, Throwable cause) {
        super(cause);
        this.code = checkCode(code);
        this.data = null;
        this.args = null;
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
        this.args = null;
    }

    /**
     * 固定状态码 + 国际化参数：文案由资源文件按状态码解析。
     *
     * <p>工厂方法形式，避免与 {@link #BizException(int, String)} 重载歧义。</p>
     *
     * @param code 8 位统一状态码
     * @param args 文案占位参数
     */
    public static BizException i18n(int code, Object... args) {
        return new BizException(code, args);
    }

    /**
     * 自由组合错误码 + 国际化参数。
     *
     * @param source   错误来源
     * @param category 错误分类
     * @param specific 具体错误编号
     * @param args     文案占位参数
     */
    public static BizException i18n(ErrorSource source, ErrorCategory category, int specific, Object... args) {
        return new BizException(source, category, specific, args);
    }

    /** 自由组合 + 附加数据 */
    public BizException(ErrorSource source, ErrorCategory category, int specific, String message, Object data) {
        super(message);
        this.code = buildCode(source, category, specific);
        this.data = data;
        this.args = null;
    }

    /**
     * 自由组合 + 附加数据 + 国际化参数。
     *
     * <p>工厂方法形式，避免重载歧义。</p>
     *
     * @param source   错误来源
     * @param category 错误分类
     * @param specific 具体错误编号
     * @param data     附加数据
     * @param args     文案占位参数
     */
    public static BizException i18nWithData(ErrorSource source, ErrorCategory category, int specific,
                                            Object data, Object... args) {
        return new BizException(source, category, specific, data, args);
    }

    /**
     * 自由组合 + 附加数据 + 国际化参数（内部构造器）。
     */
    protected BizException(ErrorSource source, ErrorCategory category, int specific, Object data, Object[] args) {
        super((String) null);
        this.code = buildCode(source, category, specific);
        this.data = data;
        this.args = args;
    }

    /**
     * 自由组合错误码 + 国际化参数（内部构造器）。
     */
    protected BizException(ErrorSource source, ErrorCategory category, int specific, Object[] args) {
        super((String) null);
        this.code = buildCode(source, category, specific);
        this.data = null;
        this.args = args;
    }

    /** 自由组合 + 原始异常 */
    public BizException(ErrorSource source, ErrorCategory category, int specific, String message, Throwable cause) {
        super(message, cause);
        this.code = buildCode(source, category, specific);
        this.data = null;
        this.args = null;
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

    // ═══════════════════ 国际化快捷方法（文案由资源文件按状态码提供） ═══════════════════

    /**
     * 客户端错误（文案走 i18n）。
     */
    public static BizException i18nClient(ErrorCategory category, int specific, Object... args) {
        return new BizException(ErrorSource.CLIENT, category, specific, args);
    }

    /**
     * 服务端错误（文案走 i18n）。
     */
    public static BizException i18nServer(ErrorCategory category, int specific, Object... args) {
        return new BizException(ErrorSource.SERVER, category, specific, args);
    }

    /**
     * 参数错误（CLIENT + REQUEST，文案走 i18n）。
     */
    public static BizException i18nParam(int specific, Object... args) {
        return new BizException(ErrorSource.CLIENT, ErrorCategory.REQUEST, specific, args);
    }

    /**
     * 业务错误（CLIENT + BUSINESS，文案走 i18n）。
     */
    public static BizException i18nBiz(int specific, Object... args) {
        return new BizException(ErrorSource.CLIENT, ErrorCategory.BUSINESS, specific, args);
    }

    /**
     * 服务端内部错误（SERVER + INFRASTRUCTURE，文案走 i18n）。
     */
    public static BizException i18nInternal(int specific, Object... args) {
        return new BizException(ErrorSource.SERVER, ErrorCategory.INFRASTRUCTURE, specific, args);
    }

    // ═══════════════════ 国际化 ═══════════════════

    /**
     * 返回当前语言下的错误文案。
     *
     * <p>当异常以「状态码 + 参数」构造（未传字面量文案）时，这里按当前请求语言
     * 从资源文件（{@code i18n/messages*.properties}）动态解析文案；否则返回构造时传入的文案。</p>
     *
     * @return 当前语言下的错误文案；资源缺失时返回兜底文案（状态码描述）
     */
    @Override
    public String getMessage() {
        if (args != null) {
            String resolved = I18nMessageResolver.resolve(code, args);
            if (resolved != null) {
                return resolved;
            }
            // 资源缺失：返回状态码占位，避免调用方拿到 null
            return I18nMessageResolver.resolve(code);
        }
        String message = super.getMessage();
        if (message == null) {
            // 未提供文案且无参数：按状态码取资源文案
            String resolved = I18nMessageResolver.resolve(code);
            if (resolved != null) {
                return resolved;
            }
        }
        return message;
    }

    /**
     * 按指定语言返回错误文案（用于显式构造多语言响应，如批量导出）。
     *
     * @param locale 目标语言
     * @return 指定语言下的文案
     */
    public String getMessage(Locale locale) {
        if (args != null) {
            String resolved = I18nMessageResolver.resolve(code, locale, args);
            if (resolved != null) {
                return resolved;
            }
        }
        String message = super.getMessage();
        if (message != null) {
            return message;
        }
        String resolved = I18nMessageResolver.resolve(code, locale);
        return resolved != null ? resolved : String.valueOf(code);
    }

    /**
     * 获取构造时传入的原始文案（未经国际化解析），可能为 null。
     */
    public String getRawMessage() {
        return super.getMessage();
    }

    /**
     * 获取国际化占位参数，可能为 null。
     */
    public Object[] getArgs() {
        return args;
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
        return checkCode(code);
    }

    private static int checkCode(int code) {
        if (code < 10_000_000 || code > 99_999_999) {
            throw new IllegalStateException("状态码非 8 位: " + code);
        }
        return code;
    }
}
