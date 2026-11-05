package com.xiongx9527.fw.plus.auth.social;

/**
 * 身份提供方类型。
 */
public enum IdpType {
    OIDC(IdpCategory.GENERAL),
    CAS(IdpCategory.GENERAL),
    SAML2(IdpCategory.GENERAL),
    DINGTALK(IdpCategory.ENTERPRISE),
    FEISHU(IdpCategory.ENTERPRISE),
    LARK(IdpCategory.ENTERPRISE),
    LDAP(IdpCategory.ENTERPRISE),
    WECHAT_WORK(IdpCategory.ENTERPRISE),
    WECHAT_SCAN(IdpCategory.PERSONAL);

    private final IdpCategory category;

    IdpType(IdpCategory category) {
        this.category = category;
    }

    public IdpCategory category() {
        return category;
    }
}
