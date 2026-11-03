package com.xiongx9527.fw.plus.ai.base.guard;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * 默认出站守卫：只允许 http / https，且目标不能是环回、内网、链路本地或云元数据地址。
 *
 * <p>校验发生在 DNS 解析之后（用解析出的 IP 判断），避免「域名指向内网」绕过。
 * 解析失败时按拒绝处理——宁可挡住，也不要放行一个无法确认的地址。</p>
 */
public class DefaultOutboundUrlGuard implements OutboundUrlGuard {

    /** 云元数据地址（AWS / GCP / Azure 通用）。 */
    private static final String METADATA_HOST = "169.254.169.254";

    @Override
    public void check(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new OutboundUrlBlockedException("url is required");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new OutboundUrlBlockedException("invalid url: " + url);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new OutboundUrlBlockedException("scheme not allowed: " + scheme);
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new OutboundUrlBlockedException("host is required");
        }
        if (METADATA_HOST.equals(host)) {
            throw new OutboundUrlBlockedException("metadata address is not allowed: " + host);
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new OutboundUrlBlockedException("cannot resolve host: " + host);
        }
        for (InetAddress address : addresses) {
            if (isBlocked(address)) {
                throw new OutboundUrlBlockedException("address not allowed: " + address.getHostAddress());
            }
        }
    }

    private boolean isBlocked(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
            return true;
        }
        byte[] raw = address.getAddress();
        if (raw.length == 4) {
            int first = raw[0] & 0xFF;
            int second = raw[1] & 0xFF;
            // 100.64.0.0/10 运营商级 NAT
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
            // 0.0.0.0/8
            return first == 0;
        }
        // IPv6 唯一本地地址 fc00::/7
        return raw.length == 16 && ((raw[0] & 0xFE) == 0xFC);
    }

    /** 便于日志：把主机名转小写。 */
    static String normalize(String host) {
        return host == null ? null : host.toLowerCase(Locale.ROOT);
    }
}
