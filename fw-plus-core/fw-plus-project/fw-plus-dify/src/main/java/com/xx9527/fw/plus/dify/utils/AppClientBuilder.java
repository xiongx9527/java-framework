package com.xx9527.fw.plus.dify.utils;

import com.xx9527.fw.plus.dify.app.IAppChatClient;
import com.xx9527.fw.plus.dify.app.impl.AppCompletionImpl;
import com.xx9527.fw.plus.dify.app.impl.AppFlowClientImpl;
import com.xx9527.fw.plus.dify.app.IAppBaseClient;
import com.xx9527.fw.plus.dify.app.IAppCompletion;
import com.xx9527.fw.plus.dify.app.IAppFlowClient;
import com.xx9527.fw.plus.dify.app.impl.AppBaseClientImpl;
import com.xx9527.fw.plus.dify.app.impl.AppChatClientImpl;
import com.xx9527.fw.plus.dify.types.ApiConfig;

/**
 * Dify Client builder
 */
public class AppClientBuilder {
    private String apiKey;
    private String baseUrl;

    /**
     * builder
     * @return DifyAppClientBuilder
     */
    public static AppClientBuilder builder() {
        return new AppClientBuilder();
    }

    /**
     * builder
     * @return DifyAppClientBuilder
     */
    public static AppClientBuilder builder(String server, String apiKey) {
        return new AppClientBuilder(server, apiKey);
    }

    /**
     * constructor
     */
    private AppClientBuilder() {

    }

    /**
     * constructor
     */
    private AppClientBuilder(String server, String apiKey) {
        this.baseUrl = server;
        this.apiKey = apiKey;
    }

    /**
     * create dify base client
     */
    public Builder<IAppBaseClient, AppBaseClientImpl> base() {
        return new Builder<>(AppBaseClientImpl.class);
    }

    /**
     * create dify chat client
     */
    public Builder<IAppChatClient, AppChatClientImpl> chat() {
        return new Builder<>(AppChatClientImpl.class);
    }

    /**
     * create dify flow client
     */
    public Builder<IAppFlowClient, AppFlowClientImpl> flow() {
        return new Builder<>(AppFlowClientImpl.class);
    }

    /**
     * create dify completion client
     */
    public Builder<IAppCompletion, AppCompletionImpl> completion() {
        return new Builder<>(AppCompletionImpl.class);
    }

    /**
     * Builder
     * @param <R>
     * @param <T>
     */
    public class Builder<R, T extends R> {
        private final Class<T> type;

        /**
         * constructor
         * @param type Class<T>
         */
        public Builder(Class<T> type) {
            this.type = type;
        }

        /**
         * config
         * @param config DifyConfig
         */
        public Builder<R, T> config(ApiConfig config) {
            baseUrl = config.getServer();
            apiKey = config.getApiKey();
            return this;
        }

        /**
         * dify server base url
         * @param baseUrl String
         */
        public Builder<R, T> baseUrl(String baseUrl) {
            AppClientBuilder.this.baseUrl = baseUrl;
            return this;
        }

        /**
         * dify app api key
         * @param apiKey String
         */
        public Builder<R, T> apiKey(String apiKey) {
            AppClientBuilder.this.apiKey = apiKey;
            return this;
        }

        /**
         * build
         */
        public R build() {
            if (baseUrl == null || apiKey == null) {
                throw new RuntimeException("Dify App Client Build Error: params is not defined");
            }

            try {
                return type.getConstructor(String.class, String.class).newInstance(baseUrl, apiKey);
            } catch (Exception e) {
                throw new RuntimeException("Dify App Client Build Error: class is not defined", e);
            }
        }
    }

}
