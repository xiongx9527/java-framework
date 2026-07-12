package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dify;

import com.xiongx9527.fw.plus.dify.DifyClientBuilder;
import com.xiongx9527.fw.plus.dify.app.IAppBaseClient;
import com.xiongx9527.fw.plus.dify.app.IAppChatClient;
import com.xiongx9527.fw.plus.dify.app.IAppCompletion;
import com.xiongx9527.fw.plus.dify.app.IAppFlowClient;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.web.IWebConsoleClient;
import org.ssssssss.magicapi.core.annotation.MagicModule;

@MagicModule("dify")
public class DifyModule {

    /**
     * 创建 Dify Web 控制台客户端
     * <p>
     * 通过服务器地址、用户名和密码进行认证连接，返回可用于操作 Dify Web 控制台的客户端实例。
     * </p>
     *
     * @param server Dify 服务器地址
     * @param userName 登录用户名
     * @param password 登录密码
     * @return IWebConsoleClient Web 控制台客户端实例
     */
    public IWebConsoleClient web(String server, String userName, String password) {
        return DifyClientBuilder.web(server, userName, password).connect();
    }

    /**
     * 创建 Dify 知识库客户端
     * <p>
     * 通过服务器地址和 API Key 构建知识库客户端，用于管理数据集、文档和向量检索等功能。
     * </p>
     *
     * @param server Dify 服务器地址
     * @param apiKey API 认证密钥
     * @return IDatasetClient 知识库客户端实例
     */
    public IDatasetClient dataset(String server, String apiKey) {
        return DifyClientBuilder.dataset(server, apiKey).build();
    }

    /**
     * 创建 Dify 基础应用客户端
     * <p>
     * 用于调用 Dify 应用的基础功能，如获取应用信息、执行简单任务等。
     * </p>
     *
     * @param server Dify 服务器地址
     * @param apiKey API 认证密钥
     * @return IAppBaseClient 基础应用客户端实例
     */
    public IAppBaseClient baseApp(String server, String apiKey) {
        return DifyClientBuilder.app(server, apiKey).base().build();
    }

    /**
     * 创建 Dify 对话应用客户端
     * <p>
     * 用于与 Dify 对话型应用进行交互，支持多轮对话、上下文记忆等功能。
     * </p>
     *
     * @param server Dify 服务器地址
     * @param apiKey API 认证密钥
     * @return IAppChatClient 对话应用客户端实例
     */
    public IAppChatClient chatApp(String server, String apiKey) {
        return DifyClientBuilder.app(server, apiKey).chat().build();
    }

    /**
     * 创建 Dify 工作流应用客户端
     * <p>
     * 用于调用 Dify 工作流类型的应用，支持复杂业务流程的编排和执行。
     * </p>
     *
     * @param server Dify 服务器地址
     * @param apiKey API 认证密钥
     * @return IAppFlowClient 工作流应用客户端实例
     */
    public IAppFlowClient flowApp(String server, String apiKey) {
        return DifyClientBuilder.app(server, apiKey).flow().build();
    }

    /**
     * 创建 Dify 文本生成应用客户端
     * <p>
     * 用于调用 Dify 文本补全类型的应用，适用于单次文本生成、内容创作等场景。
     * </p>
     *
     * @param server Dify 服务器地址
     * @param apiKey API 认证密钥
     * @return IAppCompletion 文本生成应用客户端实例
     */
    public IAppCompletion completionApp(String server, String apiKey) {
        return DifyClientBuilder.app(server, apiKey).completion().build();
    }

}
