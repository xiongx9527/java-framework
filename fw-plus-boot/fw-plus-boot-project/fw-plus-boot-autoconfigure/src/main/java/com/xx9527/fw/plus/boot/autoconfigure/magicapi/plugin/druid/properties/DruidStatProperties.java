package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lihengming [89921218@qq.com]
 */
@Data
@ConfigurationProperties("magic-api.plugin.druid")
public class DruidStatProperties {
    /**
     * AOP切点模式数组，用于定义需要进行监控的类或方法匹配规则
     */
    private String[] aopPatterns;

    /**
     * Druid监控页面Servlet实例，用于提供数据库连接池的Web监控界面
     */
    private StatViewServlet statViewServlet = new StatViewServlet();

    /**
     * Web统计过滤器实例，用于收集Web应用的访问统计信息
     */
    private WebStatFilter webStatFilter = new WebStatFilter();


    @Data
    public static class StatViewServlet {
        /**
         * 是否启用该功能
         */
        private boolean enabled;

        /**
         * URL匹配模式
         */
        private String urlPattern;

        /**
         * 允许访问的配置
         */
        private String allow;

        /**
         * 拒绝访问的配置
         */
        private String deny;

        /**
         * 登录用户名
         */
        private String loginUsername;

        /**
         * 登录密码
         */
        private String loginPassword;

        /**
         * 重置功能是否启用
         */
        private String resetEnable;
    }

    @Data
    public static class WebStatFilter {
        /**
         * 是否启用Web统计过滤器
         */
        private boolean enabled;
        
        /**
         * 过滤器拦截的URL模式
         */
        private String urlPattern;
        
        /**
         * 排除的URL列表，以逗号分隔
         */
        private String exclusions;
        
        /**
         * 会话统计的最大数量限制
         */
        private String sessionStatMaxCount;
        
        /**
         * 是否启用会话统计功能
         */
        private String sessionStatEnable;
        
        /**
         * 会话中用于标识用户的属性名
         */
        private String principalSessionName;
        
        /**
         * Cookie中用于标识用户的名称
         */
        private String principalCookieName;
        
        /**
         * 是否启用分析功能
         */
        private String profileEnable;
    }
}