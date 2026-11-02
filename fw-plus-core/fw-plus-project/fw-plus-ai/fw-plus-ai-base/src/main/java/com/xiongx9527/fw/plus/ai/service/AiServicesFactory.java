package com.xiongx9527.fw.plus.ai.service;

/** AiServices 构建 SPI，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.spi.services.AiServicesFactory}；由各后端 adapter 经 META-INF/services 注册。 */
public interface AiServicesFactory {
    <T> AiServices<T> create(AiServiceContext context);
}
