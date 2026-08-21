package com.xiongx9527.fw.plus.dify.utils;

import com.xiongx9527.fw.plus.dify.types.DifyException;
import com.xiongx9527.fw.plus.dify.web.WebAuthService;
import com.xiongx9527.fw.plus.dify.types.DifyClientException;
import com.xiongx9527.fw.plus.dify.types.WebConfig;
import com.xiongx9527.fw.plus.dify.web.IWebConsoleClient;
import com.xiongx9527.fw.plus.dify.web.entity.TokenList;
import com.xiongx9527.fw.plus.dify.web.impl.WebClientImpl;

/**
 * Dify Web Client Builder
 */
public class WebClientBuilder {
    private String server;
    private String userName;
    private String password;

    /**
     * builder
     */
    public static WebClientBuilder builder() {
        return new WebClientBuilder();
    }

    /**
     * builder
     * @param server Dify 服务地址
     * @param userName 用户名
     * @param password 账号密码
     */
    public static WebClientBuilder builder(String server, String userName, String password) {
        return new WebClientBuilder(server, userName, password);
    }

    /**
     * constructor
     */
    private WebClientBuilder() {

    }

    /**
     * constructor
     * @param server Dify 服务地址
     * @param userName 用户名
     * @param password 账号密码
     */
    private WebClientBuilder(String server, String userName, String password) {
        this.server = server;
        this.userName = userName;
        this.password = password;
    }

    /**
     * dify server base url
     * @param baseUrl String
     */
    public WebClientBuilder baseUrl(String baseUrl) {
        this.server = baseUrl;
        return this;
    }

    /**
     * dify login user name
     * @param userName String
     */
    public WebClientBuilder user(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * dify login password
     * @param password String
     */
    public WebClientBuilder password(String password) {
        this.password = password;
        return this;
    }

    /**
     * build.
     * 创建IWebClient对象时，会默认执行登录
     */
    public IWebConsoleClient connect() {
        String normalizedServer = DifyValidationUtils.normalizeServer(server, "web", "connect");
        String requiredUserName = DifyValidationUtils.requireText(userName, "userName", "web", "connect");
        String requiredPassword = DifyValidationUtils.requireText(password, "password", "web", "connect");

        try {
            WebConfig config = WebConfig.builder()
                    .server(normalizedServer)
                    .userName(requiredUserName)
                    .password(requiredPassword)
                    .build();
            TokenList tokenList = WebAuthService.login(config);
            return new WebClientImpl(normalizedServer, tokenList);
        } catch (DifyClientException | DifyException e) {
            throw e;
        } catch (Exception e) {
            throw new DifyClientException("Dify web client connect error", e);
        }
    }


}
