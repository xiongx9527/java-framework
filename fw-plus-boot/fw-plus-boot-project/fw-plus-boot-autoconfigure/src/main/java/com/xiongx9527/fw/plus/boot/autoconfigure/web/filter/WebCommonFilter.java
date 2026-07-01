package com.xiongx9527.fw.plus.boot.autoconfigure.web.filter;

import com.xiongx9527.fw.plus.common.gray.GrayContext;
import com.xiongx9527.fw.plus.common.trace.AbstractTrace;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.WebUtils;
import com.xiongx9527.fw.plus.boot.common.web.ICommonFilterService;
import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptor;
import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptorProperties;
import com.xiongx9527.fw.plus.common.web.WebRequest;
import com.xiongx9527.fw.plus.common.web.WebResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

public interface WebCommonFilter {

    /**
     * 获取Web通用过滤服务
     *
     * 此方法旨在提供一个通用过滤服务实例，优先从Spring容器中获取实现了ICommonFilterService接口的Bean
     * 如果存在多个实现，任意返回一个如果不存在，则返回一个默认实现
     *
     * @return ICommonFilterService的实例，可以是Spring容器中的Bean或默认实现
     */
    default ICommonFilterService getWebCommonFilterService() {
        // 从Spring容器中获取所有类型为ICommonFilterService的Bean
        java.util.Map<String, ICommonFilterService> iWebCommonFilterServiceMap = SpringUtils.getBeansOfType(ICommonFilterService.class);

        // 检查获取到的Bean集合是否非空
        if (MapUtils.isNotEmpty(iWebCommonFilterServiceMap)) {
            // 如果集合非空，返回集合中的第一个元素
            return iWebCommonFilterServiceMap.values().iterator().next();
        }

        // 如果集合为空，返回一个默认的通用过滤服务实例
        return new DefaultCommonFilterService();
    }

    /**
     * 获取MyHttpEncryptor的实例
     * 此方法用于在Spring应用上下文中查找并返回一个MyHttpEncryptor实例
     * 如果存在多个MyHttpEncryptor实例，它将返回找到的第一个实例
     *
     * @return MyHttpEncryptor实例，如果没有找到则返回null
     */
    default MyHttpEncryptor getMyHttpEncryptor() {
        // 从Spring应用上下文中获取所有MyHttpEncryptor类型的Bean
        java.util.Map<String, MyHttpEncryptor> myHttpEncryptorMap = SpringUtils.getBeansOfType(MyHttpEncryptor.class);
        // 检查获取到的MyHttpEncryptor的Map是否不为空
        if (MapUtils.isNotEmpty(myHttpEncryptorMap)) {
            // 如果Map不为空，则获取Map中的第一个MyHttpEncryptor的实例
            // 这里的逻辑与IWebCommonFilterService相同，预期只有一个实现
            return myHttpEncryptorMap.values().iterator().next();
        }
        // 如果Map为空，表示没有找到MyHttpEncryptor的实现，返回null
        return null;
    }

    /**
     * 默认方法，用于获取MyHttpEncryptor的配置属性
     * 此方法旨在从Spring应用上下文中获取所有类型为MyHttpEncryptorProperties的Bean，
     * 并返回其中的一个配置实例如果不存在这样的Bean，则返回null
     *
     * @return MyHttpEncryptorProperties实例，如果找不到则返回null
     */
    default MyHttpEncryptorProperties getMyHttpEncryptorProperties() {
        // 从Spring上下文中获取所有MyHttpEncryptorProperties类型的Bean
        java.util.Map<String, MyHttpEncryptorProperties> myHttpEncryptorPropertiesMap = SpringUtils.getBeansOfType(MyHttpEncryptorProperties.class);
        // 检查获取到的MyHttpEncryptor的Map是否不为空
        if (MapUtils.isNotEmpty(myHttpEncryptorPropertiesMap)){
            // 如果不为空，返回Map中的第一个MyHttpEncryptorProperties实例
            return myHttpEncryptorPropertiesMap.values().iterator().next();
        }
        // 如果Map为空，表示没有找到MyHttpEncryptorProperties类型的Bean，返回null
        return null;
    }


    @Slf4j
    public static class DefaultCommonFilterService implements ICommonFilterService {
        /**
         * 重写resolveAndCacheLookupPath方法
         * 该方法尝试解析并缓存当前请求的查找路径
         * 如果解析过程中遇到任何错误，会记录错误信息并返回null
         *
         * @return 解析并缓存的查找路径，如果解析失败则返回null
         */
        @Override
        public String resolveAndCacheLookupPath() {
            try {
                // 尝试解析和缓存请求的查找路径
                return WebUtils.resolveAndCacheLookupPath(WebUtils.getRequest());
            } catch (Throwable e) {
                // 如果解析过程中出现任何错误，记录错误信息
                log.warn("resolveAndCacheLookupPath error", e);
            }
            // 解析失败时，返回null
            return null;
        }

        /**
         * 执行过滤器前置处理
         * 该方法负责初始化调用链追踪上下文和灰度发布上下文
         * 如果相应的上下文尚未初始化，则进行初始化操作
         */
        @Override
        public void doFilterBefore(WebRequest request, WebResponse response) {
            String traceId = AbstractTrace.get().getTraceId();
            if (StringUtils.isNotBlank(traceId)) {
                response.addHeader("Trace-Id", traceId);
            }
            try {
                // 检查并初始化灰度发布上下文
                if (!GrayContext.isGray()) {
                    GrayContext.init(request);
                }
            } catch (Throwable e) {
                // 如果初始化追踪上下文或灰度发布上下文时出现任何错误，记录错误信息
                log.warn("doFilterBefore error", e);
            }
        }

        /**
         * 执行过滤器后置处理逻辑
         * 清理当前线程的追踪上下文和灰度发布上下文，确保上下文不会在请求间传递
         */
        @Override
        public void doFilterAfter() {
            try {
                GrayContext.clear();
            } catch (Throwable e) {
                // 如果清理追踪上下文或灰度发布上下文时出现任何错误，记录错误信息
                log.warn("doFilterAfter error", e);
            }
        }

    }
}
