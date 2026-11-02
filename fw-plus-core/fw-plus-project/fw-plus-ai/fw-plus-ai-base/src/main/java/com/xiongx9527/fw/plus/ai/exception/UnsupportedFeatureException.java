package com.xiongx9527.fw.plus.ai.exception;

/**
 * 「能力未实现」异常。
 *
 * <p><b>作用</b>：端口接口用默认方法给出统一签名，但并非每个实现都支持全部能力
 * （例如某向量库不支持按过滤条件删除）。此时抛本异常，语义上明确表示
 * 「契约合法、当前实现不提供」，而不是参数错误或系统故障。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.exception.UnsupportedFeatureException}。</p>
 *
 * @author xiongxiao
 */
public class UnsupportedFeatureException extends AiException {

    /**
     * 构造异常。
     *
     * @param message 消息
     */
    public UnsupportedFeatureException(String message) {
        super(message);
    }
}
