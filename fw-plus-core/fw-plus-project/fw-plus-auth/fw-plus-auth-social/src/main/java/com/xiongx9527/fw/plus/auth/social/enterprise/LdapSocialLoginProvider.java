package com.xiongx9527.fw.plus.auth.social.enterprise;
import com.xiongx9527.fw.plus.auth.social.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * LDAP（LDAP）账密登录授权（JDK 内置 JNDI，无额外依赖）。
 *
 * <p>流程：① 用服务账号 bind → ② 在 base_dn 下 SUBTREE 搜索匹配用户得到用户 DN →
 * ③ 用用户 DN + 口令 bind 校验 → ④ 读取属性映射为 {@link ExternalIdentity}。</p>
 *
 * <p>配置（存于 IdpConfig.extra）：</p>
 * <ul>
 *   <li>{@code ldap_urls}：LDAP 服务器地址，逗号分隔</li>
 *   <li>{@code base_dn}：搜索基准</li>
 *   <li>{@code bind_dn}/{@code bind_password}：服务账号（匿名绑定可留空）</li>
 *   <li>{@code user_filter}：搜索过滤，{@code {0}} 占位用户名，默认 {@code (|(uid={0})(mail={0}))}</li>
 *   <li>{@code mapping}：JSON，{@code account/name/email} 各自对应的属性名</li>
 * </ul>
 */
public class LdapSocialLoginProvider extends AbstractSocialLoginProvider implements SocialOrgTreeProvider {

    @Override
    public IdpType type() {
        return IdpType.LDAP;
    }

    @Override
    public boolean supportsPasswordLogin() {
        return true;
    }

    @Override
    public String authorizationUrl(IdpConfig config, AuthorizationUrlRequest request) {
        throw new UnsupportedOperationException("LDAP uses password login, not authorization URL");
    }

    @Override
    public ExternalIdentity resolveIdentity(IdpConfig config, IdpCallback callback) {
        throw new UnsupportedOperationException("LDAP uses loginWithPassword, not resolveIdentity");
    }

    @Override
    public ExternalIdentity loginWithPassword(IdpConfig config, String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("username/password must not be empty");
        }
        String urls = config.extra("ldap_urls");
        String baseDn = config.extra("base_dn");
        String userFilter = first(config.extra("user_filter"), "(|(uid={0})(mail={0}))");
        String bindDn = config.extra("bind_dn");
        String bindPassword = config.extra("bind_password");

        DirContext serviceCtx = null;
        try {
            serviceCtx = new InitialDirContext(buildEnv(urls, bindDn, bindPassword));
            String userDn = findUserDn(serviceCtx, baseDn, userFilter, username);
            DirContext userCtx = null;
            try {
                userCtx = new InitialDirContext(buildEnv(urls, userDn, password));
            } finally {
                closeQuietly(userCtx);
            }
            return readIdentity(serviceCtx, userDn, config);
        } catch (NamingException e) {
            throw new IllegalStateException("LDAP login failed for user " + username + ": " + e.getMessage(), e);
        } finally {
            closeQuietly(serviceCtx);
        }
    }

    private Hashtable<String, String> buildEnv(String urls, String principal, String credentials) {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, urls);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        if (principal != null) {
            env.put(Context.SECURITY_PRINCIPAL, principal);
        }
        if (credentials != null) {
            env.put(Context.SECURITY_CREDENTIALS, credentials);
        }
        return env;
    }

    private String findUserDn(DirContext ctx, String baseDn, String filter, String username) throws NamingException {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String prepared = filter.replace("{0}", escape(username));
        NamingEnumeration<SearchResult> results = ctx.search(baseDn, prepared, controls);
        try {
            if (results.hasMore()) {
                SearchResult result = results.next();
                return result.getNameInNamespace();
            }
        } finally {
            results.close();
        }
        throw new IllegalStateException("LDAP user not found: " + username);
    }

    private ExternalIdentity readIdentity(DirContext ctx, String userDn, IdpConfig config) throws NamingException {
        Attributes attributes = ctx.getAttributes(userDn);
        Mapping mapping = parseMapping(config.extra("mapping"));
        String account = readAttr(attributes, mapping.account, "uid", "cn", "sAMAccountName", "userPrincipalName");
        String name = readAttr(attributes, mapping.name, "cn", "displayName", "name");
        String email = readAttr(attributes, mapping.email, "mail", "email", "userPrincipalName");
        return new ExternalIdentity(account, first(name, account), email, account, null);
    }

    private static String readAttr(Attributes attributes, String mapped, String... candidates) {
        if (mapped != null && !mapped.isEmpty()) {
            Attribute a = attributes.get(mapped);
            if (a != null) {
                try {
                    Object v = a.get();
                    if (v != null) {
                        return String.valueOf(v);
                    }
                } catch (NamingException ignored) {
                    // fall through
                }
            }
        }
        for (String c : candidates) {
            Attribute a = attributes.get(c);
            if (a != null) {
                try {
                    Object v = a.get();
                    if (v != null) {
                        return String.valueOf(v);
                    }
                } catch (NamingException ignored) {
                    // try next
                }
            }
        }
        return null;
    }

    private static Mapping parseMapping(String mappingJson) {
        Mapping m = new Mapping();
        if (mappingJson == null) {
            return m;
        }
        JSONObject obj = JSONUtil.parseObj(mappingJson);
        m.account = obj.getStr("account");
        m.name = obj.getStr("name");
        m.email = obj.getStr("email");
        return m;
    }

    /** 转义 LDAP 搜索过滤中的特殊字符（RFC 4515）。 */
    private static String escape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 0x5C) {
                sb.append("\\");
            } else if (c == '*') {
                sb.append("\2a");
            } else if (c == '(') {
                sb.append("\28");
            } else if (c == ')') {
                sb.append("\29");
            } else if (c == 0) {
                sb.append("\00");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void closeQuietly(DirContext ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException ignored) {
                // ignore
            }
        }
    }

    @Override
    public SocialOrgNode getNode(IdpConfig config, String nodeId) {
        String urls = config.extra("ldap_urls");
        String bindDn = config.extra("bind_dn");
        String bindPassword = config.extra("bind_password");
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(buildEnv(urls, bindDn, bindPassword));
            return buildLdapNode(ctx, nodeId, config, false);
        } catch (NamingException e) {
            throw new IllegalStateException("LDAP getNode failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(ctx);
        }
    }

    private static class Mapping {
        String account;
        String name;
        String email;
    }

    @Override
    public boolean supports(IdpType type) {
        return type == IdpType.LDAP;
    }

    @Override
    public List<SocialOrgNode> getUserTree(IdpConfig config) {
        String urls = config.extra("ldap_urls");
        String baseDn = config.extra("base_dn");
        String bindDn = config.extra("bind_dn");
        String bindPassword = config.extra("bind_password");
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(buildEnv(urls, bindDn, bindPassword));
            SocialOrgNode root = buildLdapNode(ctx, baseDn, config, true);
            List<SocialOrgNode> roots = new ArrayList<SocialOrgNode>();
            roots.add(root);
            return roots;
        } catch (NamingException e) {
            throw new IllegalStateException("LDAP getUserTree failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(ctx);
        }
    }

    private SocialOrgNode buildLdapNode(DirContext ctx, String dn, IdpConfig config, boolean recursive) throws NamingException {
        Mapping mapping = parseMapping(config.extra("mapping"));
        Attributes attrs = ctx.getAttributes(dn);
        SocialOrgNode node = new SocialOrgNode(dn, readAttr(attrs, mapping.name, "ou", "cn", "o", "dc"), null);

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        controls.setReturningAttributes(new String[]{"ou", "cn", "uid", "displayName", "name",
                "mail", "email", "sAMAccountName", "userPrincipalName"});

        NamingEnumeration<SearchResult> subs = ctx.search(dn,
                "(|(objectClass=organizationalUnit)(objectClass=container))", controls);
        try {
            while (subs.hasMore()) {
                SearchResult sr = subs.next();
                String childDn = sr.getNameInNamespace();
                if (recursive) {
                    node.addChild(buildLdapNode(ctx, childDn, config, true));
                } else {
                    Attributes ca = ctx.getAttributes(childDn);
                    node.addChild(new SocialOrgNode(childDn, readAttr(ca, mapping.name, "ou", "cn", "o", "dc"), node.getId()));
                }
            }
        } finally {
            subs.close();
        }

        NamingEnumeration<SearchResult> users = ctx.search(dn,
                "(|(objectClass=inetOrgPerson)(objectClass=user))", controls);
        try {
            while (users.hasMore()) {
                SearchResult sr = users.next();
                String userDn = sr.getNameInNamespace();
                Attributes ua = ctx.getAttributes(userDn);
                String account = readAttr(ua, mapping.account, "uid", "cn", "sAMAccountName", "userPrincipalName");
                String name = readAttr(ua, mapping.name, "cn", "displayName", "name");
                String email = readAttr(ua, mapping.email, "mail", "email");
                node.addUser(new SocialOrgUser(first(account, name), first(name, account), email));
            }
        } finally {
            users.close();
        }
        return node;
    }
}
