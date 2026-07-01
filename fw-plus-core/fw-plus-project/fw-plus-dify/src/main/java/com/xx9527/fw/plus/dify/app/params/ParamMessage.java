package com.xx9527.fw.plus.dify.app.params;

import com.xx9527.fw.plus.common.trace.AbstractTrace;
import lombok.Data;
import org.apache.commons.lang.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ParamMessage {
    /**
     * 允许传入 App 定义的各变量值
     * 如果变量是文件类型，请指定一个包含以下 files 中所述键的对象
     */
    private Map<String, Object> inputs;

    /**
     * 响应模式
     */
    private String responseMode = "streaming"; //blocking, streaming

    /**
     * 用户标识
     */
    private String user = "magic-api";

    /**
     * 跟踪 ID
     */
    private String traceId;


    /**
     * 文件列表，适用于传入文件结合文本理解并回答问题，仅当模型支持 Vision 能力时可用
     * 具体请看官方文档
     */
    private List<ParamFile> files;

    /**
     * ToMap
     *
     * @return Map<String, Object>
     */
    public Map<String, Object> toMap() {
        return new HashMap<String, Object>() {{
            put("inputs", inputs == null ? new HashMap<>() : inputs);
            put("response_mode", responseMode);
            put("user", user);
            put("files", files);
            put("trace_id", ObjectUtils.defaultIfNull(traceId, AbstractTrace.get().getTraceId()));
        }};
    }

    /**
     * 构建 ParamMessage 对象
     * 
     * <p>该静态工厂方法用于快速创建并初始化 ParamMessage 实例，
     * 适用于需要一次性设置所有参数的场景。</p>
     *
     * @param inputs 输入变量映射，包含 App 定义的各变量值，可为 null
     * @param responseMode 响应模式，可选值：blocking（阻塞模式）、streaming（流式模式）
     * @param user 用户标识，用于标识请求的用户身份
     * @param traceId 跟踪 ID，用于链路追踪和日志关联，可为 null
     * @param files 文件列表，用于传入文件结合文本理解并回答问题，仅当模型支持 Vision 能力时可用，可为 null
     * @return 初始化完成的 ParamMessage 对象
     */
    public static ParamMessage build(Map<String, Object> inputs, String responseMode, String user, String traceId, List<ParamFile> files) {
        ParamMessage paramMessage = new ParamMessage();
        paramMessage.setInputs(inputs);
        paramMessage.setResponseMode(responseMode);
        paramMessage.setUser(user);
        paramMessage.setTraceId(traceId);
        paramMessage.setFiles(files);
        return paramMessage;
    }


    /**
     * 构建 ParamMessage 对象（简化版）
     * 
     * <p>该静态工厂方法用于快速创建 ParamMessage 实例，仅设置输入变量映射，
     * 其他参数使用默认值。</p>
     *
     * @param inputs 输入变量映射，包含 App 定义的各变量值，可为 null
     * @return 初始化完成的 ParamMessage 对象，其中 inputs 为传入值，其他字段使用默认值
     */
    public static ParamMessage build(Map<String, Object> inputs) {
        ParamMessage paramMessage = new ParamMessage();
        paramMessage.setInputs(inputs);
        return paramMessage;
    }

    /**
     * 构建 ParamMessage 对象（简化版）
     * 
     * <p>该静态工厂方法用于快速创建 ParamMessage 实例，设置输入变量映射和用户标识，
     * 其他参数使用默认值。</p>
     *
     * @param inputs 输入变量映射，包含 App 定义的各变量值，可为 null
     * @param user 用户标识，用于标识请求的用户身份
     * @return 初始化完成的 ParamMessage 对象，其中 inputs 和 user 为传入值，其他字段使用默认值
     */
    public static ParamMessage build(Map<String, Object> inputs, String user) {
        ParamMessage paramMessage = new ParamMessage();
        paramMessage.setUser(user);
        paramMessage.setInputs(inputs);
        return paramMessage;
    }


}
