package com.xiongx9527.fw.plus.dify.web.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.xiongx9527.fw.plus.dify.types.ApiConfig;
import com.xiongx9527.fw.plus.dify.types.DifyPage;
import com.xiongx9527.fw.plus.dify.web.IWebConsoleClient;
import com.xiongx9527.fw.plus.dify.SimpleHttpClient;
import com.xiongx9527.fw.plus.dify.routes.ConsoleRoutes;
import com.xiongx9527.fw.plus.dify.web.entity.TokenList;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
public class WebClientImpl implements IWebConsoleClient {
    private final ApiConfig config;

    /**
     * constructor
     *
     * @param server Dify 服务地址
     * @param token  Dify access token
     */
    public WebClientImpl(String server, TokenList token) {
        config = ApiConfig.builder().server(server).apiKey(token.getAccessToken()).refreshToken(token.getRefreshToken()).build();
    }

    @Override
    public SimpleHttpClient httpClient() {
        return SimpleHttpClient.get(config);
    }

    @Override
    public String token() {
        return config.getApiKey();
    }

    @Override
    public String refreshToken() {
        return config.getRefreshToken();
    }

    @Override
    public DifyPage<JSONObject> queryApps(int page, int limit, String name) {
        String result = SimpleHttpClient.get(config).requestJson(ConsoleRoutes.APPS, new HashMap<String, Object>() {{
            put("page", page);
            put("limit", limit);
            put("name", name);
        }});

        return JSON.parseObject(result, new TypeReference<DifyPage<JSONObject>>() {});
    }

}
