package com.xiongx9527.fw.plus.ai.mcp.server.feature;

import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.server.feature.PackageAnnotationScanner;
import com.ajaxjs.mcp.server.feature.annotation.CompletePrompt;
import com.ajaxjs.mcp.server.feature.annotation.CompleteResourceTemplate;
import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Prompt;
import com.ajaxjs.mcp.server.feature.annotation.Resource;
import com.ajaxjs.mcp.server.feature.annotation.ResourceTemplate;
import com.ajaxjs.mcp.server.feature.annotation.Tool;
import com.xiongx9527.fw.plus.ai.mcp.server.annotation.McpServer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 扫描 {@code @McpService} 能力并按 {@link McpServer} 注解归属到对应 key 的 {@link FeatureMgr}。
 *
 * <p>{@link FeatureMgr#init(String)} 只能整包注册，无法把单个方法注册到指定 FeatureMgr
 * （{@code addTool} 等均为私有方法），因此这里走反射注册，语义与内核 {@code FeatureMgr#init}
 * 的注册顺序保持一致。</p>
 */
public class McpFeatureScanner {

    /**
     * 默认 runtime 的 key，未标注 {@link McpServer} 的能力归属到此。
     */
    public static final String DEFAULT_KEY = "default";

    private static final String COMPLETION_PROMPT_KIND = "ref/prompt";
    private static final String COMPLETION_RESOURCE_KIND = "ref/resource";

    private static final Method ADD_TOOL = resolveMethod("addTool");
    private static final Method ADD_PROMPT = resolveMethod("addPrompt");
    private static final Method ADD_RESOURCE = resolveMethod("addResource");
    private static final Method ADD_RESOURCE_TEMPLATE = resolveMethod("addResourceTemplate");
    private static final Method ADD_COMPLETION = resolveMethod("addCompletion");

    /**
     * 逐个包扫描 {@code @McpService} 类，将能力按注解归属注册到对应 key 的 FeatureMgr。
     *
     * @param basePackages 待扫描的包
     * @param featureMgrs  key → FeatureMgr 映射，必须包含 {@link #DEFAULT_KEY}
     * @throws IOException 扫描目录失败
     * @throws ClassNotFoundException 扫描到的类无法加载
     */
    public void register(List<String> basePackages, Map<String, FeatureMgr> featureMgrs)
            throws IOException, ClassNotFoundException {
        if (!featureMgrs.containsKey(DEFAULT_KEY)) {
            throw new IllegalArgumentException("featureMgrs must contain key '" + DEFAULT_KEY + "'");
        }
        for (String basePackage : basePackages) {
            for (Class<?> clazz : PackageAnnotationScanner.findClassesWithAnnotation(basePackage, McpService.class)) {
                registerClass(clazz, featureMgrs);
            }
        }
    }

    private void registerClass(Class<?> clazz, Map<String, FeatureMgr> featureMgrs) {
        Object instance = newInstance(clazz);
        String classKey = serverKey(clazz.getAnnotation(McpServer.class));
        if (classKey == null) {
            classKey = DEFAULT_KEY;
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String key = serverKey(method.getAnnotation(McpServer.class));
            if (key == null) {
                key = classKey;
            }
            FeatureMgr featureMgr = featureMgrs.get(key);
            if (featureMgr == null) {
                throw new IllegalStateException("MCP capability '" + method.getName() + "' is bound to "
                        + "unresolved server key '" + key + "'. Declare it under 'fw-plus.ai.mcp.servers'.");
            }
            registerMethod(featureMgr, method, instance);
        }
    }

    private void registerMethod(FeatureMgr featureMgr, Method method, Object instance) {
        Prompt prompt = method.getAnnotation(Prompt.class);
        if (prompt != null) {
            invoke(ADD_PROMPT, featureMgr, prompt, method, instance);
            return;
        }
        ResourceTemplate resourceTemplate = method.getAnnotation(ResourceTemplate.class);
        if (resourceTemplate != null) {
            invoke(ADD_RESOURCE_TEMPLATE, featureMgr, resourceTemplate, method, instance);
            return;
        }
        CompletePrompt completePrompt = method.getAnnotation(CompletePrompt.class);
        if (completePrompt != null) {
            invoke(ADD_COMPLETION, featureMgr, COMPLETION_PROMPT_KIND, completePrompt.value(), method, instance);
            return;
        }
        CompleteResourceTemplate completeResourceTemplate = method.getAnnotation(CompleteResourceTemplate.class);
        if (completeResourceTemplate != null) {
            invoke(ADD_COMPLETION, featureMgr, COMPLETION_RESOURCE_KIND, completeResourceTemplate.value(), method, instance);
            return;
        }
        Resource resource = method.getAnnotation(Resource.class);
        if (resource != null) {
            invoke(ADD_RESOURCE, featureMgr, resource, method, instance);
            return;
        }
        Tool tool = method.getAnnotation(Tool.class);
        if (tool != null) {
            invoke(ADD_TOOL, featureMgr, tool, method, instance);
        }
    }

    private String serverKey(McpServer annotation) {
        if (annotation == null) {
            return null;
        }
        String value = annotation.value();
        return value == null || value.isEmpty() ? DEFAULT_KEY : value;
    }

    private Object newInstance(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to instantiate @McpService class '" + clazz.getName() + "'", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Unable to instantiate @McpService class '" + clazz.getName() + "'", e.getCause());
        }
    }

    private static void invoke(Method method, FeatureMgr featureMgr, Object... args) {
        try {
            method.invoke(featureMgr, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access " + method, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException(method.getName() + " failed", cause);
        }
    }

    private static Method resolveMethod(String name) {
        for (Method declared : FeatureMgr.class.getDeclaredMethods()) {
            if (declared.getName().equals(name)) {
                declared.setAccessible(true);
                return declared;
            }
        }
        throw new IllegalStateException("FeatureMgr#" + name + " not found");
    }
}