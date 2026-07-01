package com.xiongx9527.fw.plus.nosql.redis.constant;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * data-redis增强模块常用常量
 *
 * @author Mr_wenpan@163.com 2021/08/25 15:57
 */
public interface DynamicRedisConstants {

    /**
     * 多数据源常量接口
     * 定义了不同数据源的标识符常量，用于在多数据源环境中区分和引用不同的数据源实现
     */
    interface MultiSource {

        /**
         * Redis模板数据源标识符
         * 用于标识基于RedisTemplate实现的数据源
         */
        String REDIS_TEMPLATE = "RedisTemplate";

        /**
         * Redis助手数据源标识符
         * 用于标识基于RedisHelper实现的数据源
         */
        String REDIS_HELPER = "RedisHelper";

        /**
         * 默认数据源标识符
         * 用于标识系统默认的数据源实现
         */
        String DEFAULT_SOURCE = "defaultSource";

    }


    /**
     * Redis助手名称常量接口
     * 该接口定义了Redis助手相关的默认名称常量，用于统一管理Redis组件的命名规范
     */
    interface DefaultRedisHelperName {

        /** Redis助手Bean名称常量 */
        String REDIS_HELPER = "redisHelper";

        /** 默认标识常量 */
        String DEFAULT = "my-default";

        /** 默认Redis助手名称常量 */
        String DEFAULT_REDIS_HELPER = "default-helper";
    }

    /**
     * Redis模板名称常量接口
     *
     * 该接口定义了Redis模板的默认名称常量，用于在Spring框架中引用RedisTemplate bean。
     *
     * @author [作者名]
     * @since [版本号]
     */
    interface DefaultRedisTemplateName {

        /**
         * Redis模板Bean名称
         *
         * Spring容器中RedisTemplate实例的默认Bean名称，用于通过依赖注入获取Redis操作模板。
         */
        String REDIS_TEMPLATE = "redisTemplate";

    }

    /**
     * redis script默认配置
     */
    interface RedisScript {
        /**
         * 通过传入的luaPath获取 DefaultRedisScript
         *
         * @param luaPath 脚本地址
         * @return DefaultRedisScript
         */
        static <T> DefaultRedisScript<T> getDefaultRedisScript(String luaPath) {
            ResourceScriptSource addQueueLua = new ResourceScriptSource(new ClassPathResource(luaPath));
            DefaultRedisScript<T> getRedisScript = new DefaultRedisScript<>();
            getRedisScript.setScriptSource(addQueueLua);
            return getRedisScript;
        }
    }

}
