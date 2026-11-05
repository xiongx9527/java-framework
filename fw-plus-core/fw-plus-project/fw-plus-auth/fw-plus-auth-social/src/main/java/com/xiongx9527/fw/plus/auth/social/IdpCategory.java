package com.xiongx9527.fw.plus.auth.social;

/**
 * 身份提供方分类。
 * <ul>
 *     <li>{@link #GENERAL} 通用：个人与企业均可接入（OAuth2/OIDC、CAS、SAML2）</li>
 *     <li>{@link #ENTERPRISE} 企业：企业组织身份（钉钉、飞书、Lark、LDAP、企业微信）</li>
 *     <li>{@link #PERSONAL} 个人：个人身份（个人微信扫码）</li>
 * </ul>
 */
public enum IdpCategory {
    GENERAL,
    ENTERPRISE,
    PERSONAL
}
