package com.xiongxiao.fw.plus.ai.support.guard;

/**
 * 出站地址校验：模型 / 数据源地址由用户配置时，防止 SSRF。
 *
 * <p><b>作用</b>：baseUrl 是用户可填的配置项，若不校验，攻击者可让它指向内网地址
 * （{@code 169.254.169.254}、{@code 127.0.0.1}、内网网段），借服务端发起请求。校验需覆盖
 * 协议白名单、主机名解析结果（防 DNS 重绑定）、内网网段与重定向跳转。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer/ai-port-layer.md} §6.29.3 OutboundUrlGuard
 * 与增强清单「出站地址校验（SSRF）」。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>{@link #isAllowed(String)} 只做静态判定，不发起网络请求以外的副作用（允许解析 DNS，但不得建立业务连接）。</li>
 *   <li>{@link #assertAllowed(String)} 拒绝时抛 {@link com.xiongx9527.fw.plus.ai.exception.IllegalConfigurationException}。</li>
 *   <li>实现需对 {@code localhost}、回环、链路本地、私有网段、元数据地址一律拒绝，白名单可配置放行。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface OutboundUrlGuard {

    /**
     * 判断地址是否允许访问。
     *
     * @param url 待校验地址
     * @return 允许返回 {@code true}
     */
    boolean isAllowed(String url);

    /**
     * 校验地址，不允许时抛出配置异常。
     *
     * @param url 待校验地址
     */
    void assertAllowed(String url);
}
