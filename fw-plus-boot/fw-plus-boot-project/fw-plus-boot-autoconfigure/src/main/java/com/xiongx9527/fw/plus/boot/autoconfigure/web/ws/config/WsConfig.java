package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 通用配置，绑定前缀 {@code fw.plus.ws}。
 *
 * <p>该配置类不引用任何 magic-api 类型，位于 {@code com.xiongx9527.fw.plus.boot.autoconfigure.web.ws}
 * 包内，任何应用都可以直接配置 {@code fw.plus.ws.*}。</p>
 *
 * @author zzy
 */
@RefreshScope
@ConfigurationProperties("fw.plus.ws")
public class WsConfig extends HashMap<String, Object> {

    /**
     * 把嵌套配置展开为点号扁平键，例如 {@code {token={secret=x}}} 展开为 {@code {"token.secret": x}}。
     * <p>内部统一按点号扁平键读取配置（如 {@code get("token.secret")}），而 Spring Boot 会把
     * {@code fw.plus.ws.token.secret} 这类嵌套配置绑定成嵌套 Map，因此读取前先展开一层。
     * 本身已是扁平键的配置项原样保留。</p>
     *
     * @return 点号扁平键的配置映射
     */
    public Map<String, Object> flatten() {
        Map<String, Object> flat = new HashMap<>(this.size() * 2);
        flattenInto("", this, flat);
        return flat;
    }

    /**
     * 递归展开嵌套 Map，非 Map 的叶子值写入扁平键。
     *
     * @param prefix 当前层级前缀，顶层传空字符串
     * @param source 待展开的配置源
     * @param target 展开结果
     */
    private static void flattenInto(String prefix, Map<?, ?> source, Map<String, Object> target) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flattenInto(key, (Map<?, ?>) value, target);
            } else if (value != null) {
                target.put(key, value);
            }
        }
    }

    /**
     * 按点号扁平键读取字符串配置。
     *
     * @param key 扁平键
     * @return 配置值，不存在时返回 null
     */
    public String getString(String key) {
        Object value = flatten().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
