package com.xiongx9527.fw.plus.auth.satoken.provider;

import cn.dev33.satoken.sso.model.SaCheckTicketResult;
import cn.dev33.satoken.sso.processor.SaSsoClientProcessor;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.AuthHeaderExtractor;

import java.util.Collections;

/**
 * Sa-Token SSO 客户端认证：以 {@code SaSsoClientProcessor.checkTicket} 把请求携带的 ticket
 * 交给外部 SSO 服务端（模式三 http）或共享存储（模式二）校验，MCP/WS/Knowledge 侧不实现 SSO 服务端、
 * 不签发令牌。
 * <p>
 * 凭据读取：{@code ticket} header，其次 {@code Authorization: Bearer}。
 * ticket 校验通过后主体取 loginId；ticket 不携带 scopes，认证主体 scopes 为空集。
 * </p>
 */
public class SaTokenSsoAuthService implements AuthService {

    @Override
    public AuthPrincipal authenticate(AuthRequest request) {
        try {
            String ticket = ticket(request);
            if (ticket == null) {
                return AuthPrincipal.ANONYMOUS;
            }
            // currUri 传 null：不依赖 Servlet/Reactive 请求上下文，也不注册单点注销回调
            SaCheckTicketResult result = SaSsoClientProcessor.instance.checkTicket(ticket, null);
            if (result == null || result.loginId == null) {
                return AuthPrincipal.ANONYMOUS;
            }
            String subject = String.valueOf(result.loginId);
            if (subject.trim().isEmpty()) {
                return AuthPrincipal.ANONYMOUS;
            }
            return new AuthPrincipal(subject, Collections.<String>emptySet());
        } catch (RuntimeException e) {
            // 无效 ticket、SSO 客户端未配置、与认证中心通信失败等一律 fail-closed
            return AuthPrincipal.ANONYMOUS;
        }
    }

    private String ticket(AuthRequest request) {
        String ticket = AuthHeaderExtractor.header(request, "ticket");
        if (ticket == null) {
            ticket = AuthHeaderExtractor.bearerToken(request);
        }
        return ticket;
    }
}