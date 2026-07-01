package org.ssssssss.magicapi.modules.http;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.nio.charset.StandardCharsets;

@MagicModule("http")
public class BaseHttpModule extends org.ssssssss.magicapi.modules.http.HttpModule {

    public BaseHttpModule(RestTemplate template) {
        super(template);
    }

    public static RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        configureRestTemplate(restTemplate);
        return restTemplate;
    }

    /**
     * 为已有的 RestTemplate 添加智能 JSON 回退转换器。
     * 适用于通过 RestTemplateBuilder 等方式创建的 RestTemplate。
     */
    private static void configureRestTemplate(RestTemplate restTemplate) {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        restTemplate.getMessageConverters().add(0,
                new AbstractHttpMessageConverter<Object>(MediaType.ALL) {
                    @Override
                    protected boolean supports(Class<?> clazz) {
                        return true;
                    }
                    @Override
                    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
                        return false;
                    }
                    @Override
                    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws java.io.IOException {
                        String body = StreamUtils.copyToString(inputMessage.getBody(), StandardCharsets.UTF_8);
                        try {
                            return objectMapper.readValue(body, Object.class);
                        } catch (Exception e) {
                            return body;
                        }
                    }
                    @Override
                    protected void writeInternal(Object object, HttpOutputMessage outputMessage) {
                        throw new UnsupportedOperationException();
                    }
                }
        );
    }

}
