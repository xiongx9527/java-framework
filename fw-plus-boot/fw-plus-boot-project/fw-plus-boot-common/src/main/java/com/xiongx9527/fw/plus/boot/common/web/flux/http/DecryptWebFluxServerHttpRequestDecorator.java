package com.xiongx9527.fw.plus.boot.common.web.flux.http;

import com.xiongx9527.fw.plus.boot.common.web.BaseRequest;
import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptor;
import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class DecryptWebFluxServerHttpRequestDecorator extends MyWebFluxServerHttpRequestDecorator implements BaseRequest {

    private ServerHttpRequest delegate;

    private MyHttpEncryptor encryptor = null;

    private MyHttpEncryptorProperties encryptorProperties = null;

    private Map<String, String[]> parameterMap = null;

    private String requestBodyStr = null;

    public DecryptWebFluxServerHttpRequestDecorator(ServerHttpRequest delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    public DecryptWebFluxServerHttpRequestDecorator(ServerHttpRequest delegate,
                                                  MyHttpEncryptor encryptor,
                                                  MyHttpEncryptorProperties encryptorProperties) {
        this(delegate);
        this.encryptor = encryptor;
        this.encryptorProperties = encryptorProperties;
        if (isNeedDecrypt()) {
            decryptParameter();
            decryptRequestBody();
        }
    }


    /**
     * 判断是否需要解密
     */
    public Boolean isNeedDecrypt() {
        if (null != encryptorProperties ) {
            if (MyHttpEncryptorProperties.MyHttpEncryptorMode.ALL.equals(encryptorProperties.getEncryptionMode())
                    || MyHttpEncryptorProperties.MyHttpEncryptorMode.Decryption.equals(encryptorProperties.getEncryptionMode())) {
                return Boolean.TRUE;
            }
            try {
                return Boolean.getBoolean(delegate.getHeaders().getFirst(encryptorProperties.getDecryptionFlagHeadName()));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return Boolean.FALSE;
    }

    /**
     * 判断是否需要加密
     *
     * 此方法尝试从请求头中获取加密标志，以决定是否需要对数据进行加密
     * 加密标志的头信息名称由加密器属性配置指定
     *
     * @return Boolean.FALSE 如果不需要加密或获取加密标志时出现异常，则默认返回false
     */
    public Boolean isNeedEncrypt() {
        if (null != encryptorProperties) {
            if (MyHttpEncryptorProperties.MyHttpEncryptorMode.ALL.equals(encryptorProperties.getEncryptionMode())
                    || MyHttpEncryptorProperties.MyHttpEncryptorMode.Encryption.equals(encryptorProperties.getEncryptionMode())) {
                return Boolean.TRUE;
            }
            try {
                // 从请求头中获取加密标志，并将其解析为Boolean值
                return Boolean.getBoolean(delegate.getHeaders().getFirst(encryptorProperties.getEncryptionFlagHeadName()));
            } catch (Exception e) {
                // 如果出现异常，记录错误日志
                log.error(e.getMessage(), e);
            }
        }
        // 默认返回false，表示不需要加密
        return Boolean.FALSE;
    }

    /**
     * 解密参数方法
     */
    private void decryptParameter() {
        if (isNeedDecrypt()) {
            String pathInfo = delegate.getURI().getRawQuery()
                    .substring(delegate.getPath().contextPath().value().length());
            if (StringUtils.isNotEmpty(pathInfo)){
                try {
                    this.parameterMap = parseDecryptedParams(decrypt(pathInfo));
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public Flux<DataBuffer> getBody() {
        if (isNeedDecrypt()) {
            byte[] bytes = requestBodyStr.getBytes(StandardCharsets.UTF_8);
            DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
            DataBuffer buffer = factory.wrap(bytes);
            return Flux.just(buffer);
        }
        return getDelegate().getBody();
    }

    /**
     * 解密请求体
     */
    private void decryptRequestBody() {
        if (isNeedDecrypt()) {
            getDelegate().getBody().collectList().flatMapMany(dataBuffers -> {
                try {
                    String requestBody = dataBuffers.stream()
                            .map(dataBuffer -> {
                                String content = dataBuffer.toString(StandardCharsets.UTF_8);
                                DataBufferUtils.release(dataBuffer);
                                return content;
                            })
                            .reduce("", String::concat);
                    String decryptedBody = decrypt(requestBody);
                    this.requestBodyStr = decryptedBody;
                } catch (Exception e) {
                    log.error("Error while decrypting request body", e);
                } finally {
                    dataBuffers.forEach(DataBufferUtils::release);
                }
                return null;
            });
        }
    }

    @Override
    public MultiValueMap<String, String> getQueryParams() {
        parameterMap = MapUtils.emptyIfNull(parameterMap);
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            for (String value : values) {
                queryParams.add(name, value);
            }
        }
        return queryParams;
    }


    @Override
    public MyHttpEncryptor encryptor() {
        return encryptor;
    }

    @Override
    public MyHttpEncryptorProperties encryptorProperties() {
        return encryptorProperties;
    }
}
