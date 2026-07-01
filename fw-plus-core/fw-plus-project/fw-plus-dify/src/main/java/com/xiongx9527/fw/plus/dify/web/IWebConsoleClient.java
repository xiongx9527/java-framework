package com.xiongx9527.fw.plus.dify.web;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.dify.types.DifyPage;
import com.xiongx9527.fw.plus.dify.SimpleHttpClient;

public interface IWebConsoleClient {
    /**
     * 获取一个Simple http client对象
     */
    SimpleHttpClient httpClient();

    /**
     * 获取登录的Access token
     */
    String token();
    /**
     * 获取登录的Refresh token
     */
    String refreshToken();
    /**
     * 查询应用列表
     * @param page int
     * @param limit int
     * @param name 名称 (可选)
     */
    DifyPage<JSONObject> queryApps(int page, int limit, String name);

}
