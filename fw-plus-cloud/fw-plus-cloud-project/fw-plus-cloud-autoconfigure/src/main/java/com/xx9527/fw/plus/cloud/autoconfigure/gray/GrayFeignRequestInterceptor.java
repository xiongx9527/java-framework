package com.xx9527.fw.plus.cloud.autoconfigure.gray;

import com.xx9527.fw.plus.common.gray.GrayContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;

public class GrayFeignRequestInterceptor implements RequestInterceptor {
    /**
     * 应用灰度发布相关的请求头信息到请求模板中
     *
     * @param template 请求模板对象，用于设置HTTP请求头信息
     */
    @Override
    public void apply(RequestTemplate template) {
        // 如果当前处于灰度发布状态，则添加灰度版本和标签请求头
        if(GrayContext.isGray()) {
            template.header(GrayContext.GRAY_VERSION_HEADER, GrayContext.getVersion());
            template.header(GrayContext.GRAY_TAG_HEADER, GrayContext.getTag());
        }
    }
}
