package com.xiongx9527.fw.plus.boot.common.web;

import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;

public class MyHttpEncryptor extends DefaultEncryptor {
    /**
     * 是否全局加密
     * @return
     */
    public boolean isGolabal() {
        return true;
    }
}
