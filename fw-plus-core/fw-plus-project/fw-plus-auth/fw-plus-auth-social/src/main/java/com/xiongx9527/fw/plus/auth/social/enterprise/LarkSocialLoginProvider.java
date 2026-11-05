package com.xiongx9527.fw.plus.auth.social.enterprise;
import com.xiongx9527.fw.plus.auth.social.*;

/**
 * LarkSuite（LARK）登录授权：与飞书协议同构，仅 base 指向 open.larksuite.com。
 */
public class LarkSocialLoginProvider extends FeishuSocialLoginProvider {

    @Override
    public IdpType type() {
        return IdpType.LARK;
    }

    @Override
    protected String defaultBase() {
        return "https://open.larksuite.com";
    }
}
