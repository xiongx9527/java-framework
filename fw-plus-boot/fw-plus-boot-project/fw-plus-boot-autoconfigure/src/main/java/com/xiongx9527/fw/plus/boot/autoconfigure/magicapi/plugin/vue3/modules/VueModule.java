package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.modules;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.model.VueInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.service.VueInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.service.VueMagicDynamicRegistry;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@MagicModule("vue3")
public class VueModule implements BaseService {

    /**
     * Vue模块类
     *
     * @param properties MagicAPI配置属性对象
     */
    private final MagicAPIProperties properties;

    /**
     * 构造函数，初始化Vue模块
     *
     * @param properties MagicAPI配置属性对象，用于配置Vue模块的相关参数
     */
    public VueModule(MagicAPIProperties properties) {
        this.properties = properties;
    }


    // 构建 Composition API 示例
    private String buildCompositionApiExample(String jsUrl) {
        return "// 远程组件使用示例 - Composition API\n" +
                "<template>\n" +
                "  <div>\n" +
                "    <h1>远程组件演示</h1>\n" +
                "    <component :is=\"remoteComponent\" v-if=\"remoteComponent\" />\n" +
                "    <div v-else>等待加载组件...</div>\n" +
                "  </div>\n" +
                "</template>\n" +
                "<script setup>\n" +
                "import { ref, onMounted, defineAsyncComponent } from 'vue';\n" +
                "const remoteComponent = ref(null);\n" +
                "onMounted(async () => {\n" +
                "  try {\n" +
                "    const module = await import(/* @vite-ignore */ '" + jsUrl + "');\n" +
                "    remoteComponent.value = defineAsyncComponent(() => Promise.resolve(module.default));\n" +
                "  } catch (error) {\n" +
                "    console.error('加载远程组件失败:', error);\n" +
                "    // 可在此处添加 UI 提示逻辑\n" +
                "  }\n" +
                "});\n" +
                "</script>";
    }

    // 构建 Options API 示例
    private String buildOptionsApiExample(String jsUrl) {
        return "// 远程组件使用示例 - Options API\n" +
                "<template>\n" +
                "  <div>\n" +
                "    <h1>远程组件演示</h1>\n" +
                "    <component :is=\"remoteComponent\" v-if=\"remoteComponent\" />\n" +
                "    <div v-else>等待加载组件...</div>\n" +
                "  </div>\n" +
                "</template>\n" +
                "<script>\n" +
                "import { ref, onMounted, defineAsyncComponent } from 'vue';\n" +
                "export default {\n" +
                "  name: 'RemoteDemo',\n" +
                "  setup() {\n" +
                "    const remoteComponent = ref(null);\n" +
                "    onMounted(async () => {\n" +
                "      try {\n" +
                "        const module = await import(/* @vite-ignore */ '" + jsUrl + "');\n" +
                "        remoteComponent.value = defineAsyncComponent(() => Promise.resolve(module.default));\n" +
                "      } catch (error) {\n" +
                "        console.error('加载远程组件失败:', error);\n" +
                "        // 可在此处添加 UI 提示逻辑\n" +
                "      }\n" +
                "    });\n" +
                "    return {\n" +
                "      remoteComponent\n" +
                "    };\n" +
                "  }\n" +
                "};\n" +
                "</script>";
    }

    /**
     * 创建组件的使用示例代码字符串，包含 Composition API 和 Options API 两种方式。
     * 示例中展示如何通过动态导入的方式加载远程 Vue 组件。
     *
     * @param componentId 组件唯一标识符，用于构建远程组件的请求地址
     * @return 包含两种使用方式（Composition API 和 Options API）的完整代码示例字符串
     */
    private String createUsageExample(String componentId) {
        // 构建远程组件 JS 文件的访问 URL
        String jsUrl = properties.getWeb() + "/vue3/component/export.js?id=" + componentId;

        // 拼接并返回完整的使用示例代码，包括 Composition API 和 Options API 的实现方式
        return "// 使用方法 - Composition API (推荐)\n" +
                "<template>\n" +
                "  <div>\n" +
                "    <h1>远程组件演示</h1>\n" +
                "    <component :is=\"remoteComponent\" v-if=\"remoteComponent\" />\n" +
                "    <div v-else>等待加载组件...</div>\n" +
                "  </div>\n" +
                "</template>\n" +
                "<script setup>\n" +
                "import { ref, onMounted, defineAsyncComponent } from 'vue';\n" +
                "const remoteComponent = ref(null);\n" +
                "onMounted(async () => {\n" +
                "  try {\n" +
                "    const module = await import(/* @vite-ignore */ '" + jsUrl + "');\n" +
                "    remoteComponent.value = defineAsyncComponent(() => Promise.resolve(module.default));\n" +
                "  } catch (error) {\n" +
                "    console.error('加载远程组件失败:', error);\n" +
                "  }\n" +
                "});\n" +
                "</script>\n\n" +

                "// 使用方法 - Options API\n" +
                "<template>\n" +
                "  <div>\n" +
                "    <h1>远程组件演示</h1>\n" +
                "    <component :is=\"remoteComponent\" v-if=\"remoteComponent\" />\n" +
                "    <div v-else>等待加载组件...</div>\n" +
                "  </div>\n" +
                "</template>\n" +
                "<script>\n" +
                "import { ref, onMounted, defineAsyncComponent } from 'vue';\n" +
                "export default {\n" +
                "  name: 'RemoteDemo',\n" +
                "  setup() {\n" +
                "    const remoteComponent = ref(null);\n" +
                "    onMounted(async () => {\n" +
                "      try {\n" +
                "        const module = await import(/* @vite-ignore */ '" + jsUrl + "');\n" +
                "        remoteComponent.value = defineAsyncComponent(() => Promise.resolve(module.default));\n" +
                "      } catch (error) {\n" +
                "        console.error('加载远程组件失败:', error);\n" +
                "      }\n" +
                "    });\n" +
                "    return {\n" +
                "      remoteComponent\n" +
                "    };\n" +
                "  }\n" +
                "};\n" +
                "</script>";
    }

    /**
     * 创建远程使用示例
     * @param jsUrl JavaScript文件的URL地址
     * @return 返回组合API示例和选项API示例拼接而成的字符串，两个示例之间用两个换行符分隔
     */
    private String createRemoteUsageExample(String jsUrl) {
        // 构建组合API使用示例
        String compositionApiExample = buildCompositionApiExample(jsUrl);
        // 构建选项API使用示例
        String optionsApiExample = buildOptionsApiExample(jsUrl);
        // 将两种API示例拼接返回
        return compositionApiExample + "\n\n" + optionsApiExample;
    }



    /**
     * 获取远程组件信息
     *
     * @param vue3Id Vue3组件ID
     * @return 包含组件信息的Map对象，包含以下键值对：
     *         - id: 组件ID
     *         - name: 组件名称
     *         - jsUrl: 组件JS文件的访问URL
     *         - exportUrl: 组件导出为URL的访问地址
     *         - createdTime: 组件创建时间
     *         - usage: 远程使用示例
     * @throws RuntimeException 当获取组件信息失败时抛出
     */
    public Map<String,Object> getRemoteComponentInfo(String vue3Id) {
        try {
            // 获取Vue组件信息
            VueInfo entity = MagicConfiguration.getMagicResourceService().file(vue3Id);
            if (Objects.isNull(entity)) {
                entity = SpringUtils.getBean(VueMagicDynamicRegistry.class).getMapping(vue3Id);
            }
            if (Objects.isNull(entity)) {
                throw new IllegalArgumentException("Vue component not found for id: " + vue3Id);
            }

            // 检查组件是否已编译
            if (StringUtils.isBlank(entity.getCompileJs())) {
                throw new IllegalArgumentException("Vue component is not compiled for id: " + vue3Id);
            }

            // 构建远程访问URL相关信息
            String baseUrl = properties.getWeb();
            String jsUrl = baseUrl + "/vue3/component/export.js?id=" + vue3Id;
            Map<String, Object> result = new HashMap<>();
            result.put("id", vue3Id);
            result.put("name", entity.getName());
            result.put("jsUrl", SpringUtils.getBean(VueInfoMagicResourceStorage.class).buildMappingKey(entity));
            //result.put("exportUrl", baseUrl + "/vue3/component/exportAsUrl?id=" + vue3Id);
            result.put("createdTime", entity.getCreateTime());
            result.put("usage", createRemoteUsageExample(jsUrl));

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create exportable component for vue3Id: " + vue3Id, e);
        }
    }

    /**
     * 创建可导出的Vue组件
     *
     * @param vue3Id Vue组件的唯一标识符
     * @return 返回可导出的组件字符串表示
     * @throws RuntimeException 当组件创建失败时抛出运行时异常
     * @throws IllegalArgumentException 当指定ID的Vue组件不存在或未编译时抛出非法参数异常
     */
    public String createExportableComponent(String vue3Id) {
        try {
            // 获取Vue组件信息
            VueInfo entity = MagicConfiguration.getMagicResourceService().file(vue3Id);
            if (Objects.isNull(entity)) {
                entity = SpringUtils.getBean(VueMagicDynamicRegistry.class).getMapping(vue3Id);
            }

            // 验证Vue组件是否存在
            if (Objects.isNull(entity)) {
                throw new IllegalArgumentException("Vue component not found for id: " + vue3Id);
            }

            // 检查组件是否已编译
            if (StringUtils.isBlank(entity.getCompileJs())) {
                throw new IllegalArgumentException("Vue component is not compiled for id: " + vue3Id);
            }

            // 创建可导出的组件
            return createExportableComponent(entity.getCompileJs(), entity.getCompileCss(), entity.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create exportable component for vue3Id: " + vue3Id, e);
        }
    }

    /**
     * 创建可导出的组件代码 - 返回有效的 ES 模块
     */
    private String createExportableComponent(String compiledJs, String compiledCss, String componentId) {
        final String STYLE_ID_PREFIX = "magic-vue-";

        // 安全地替换 Vue 动态导入语法（注意：当前正则较宽松）
        String processedJs = compiledJs
                .replaceAll("await import\\('vue'\\)\\.then\\(m\\s*=>\\s*m\\.(\\w+)\\)", "Vue.$1")
                .replace("export default", "const component =")
                .trim();

        // 转义 CSS 内容中的反引号防止模板字符串注入
        String escapedCss = (compiledCss != null) ? compiledCss.replace("`", "\\`") : "";

        // 构建样式注入逻辑
        StringBuilder styleInjectionCode = new StringBuilder();
        styleInjectionCode.append("// MagicVue 远程组件 - ").append(componentId).append("\n");
        styleInjectionCode.append("// 自动注入样式\n");
        styleInjectionCode.append("if (typeof document !== 'undefined' && document.head) {\n");
        styleInjectionCode.append("    const styleId = '").append(STYLE_ID_PREFIX).append(componentId).append("';\n");
        styleInjectionCode.append("    if (!document.getElementById(styleId)) {\n");
        styleInjectionCode.append("        try {\n");
        styleInjectionCode.append("            const style = document.createElement('style');\n");
        styleInjectionCode.append("            style.id = styleId;\n");
        styleInjectionCode.append("            style.textContent = `").append(escapedCss).append("`;\n");
        styleInjectionCode.append("            document.head.appendChild(style);\n");
        styleInjectionCode.append("        } catch (e) {\n");
        styleInjectionCode.append("            console.warn('样式注入失败:', e);\n");
        styleInjectionCode.append("        }\n");
        styleInjectionCode.append("    }\n");
        styleInjectionCode.append("}\n\n");

        // 组件定义及导出
        styleInjectionCode.append("// 组件定义\n");
        styleInjectionCode.append(processedJs).append(";\n\n");
        styleInjectionCode.append("// 导出组件\n");
        styleInjectionCode.append("export default component;");

        return styleInjectionCode.toString();
    }

    /**
     * 替换所有 Vue 导入语句为全局 Vue 引用
     */
    private String replaceVueImports(String code) {
        // 匹配 await import('vue').then(m => m.xxx) 模式
        Pattern pattern = Pattern.compile("await import\\('vue'\\)\\.then\\(m\\s*=>\\s*m\\.(\\w+)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(code);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String methodName = matcher.group(1);
            matcher.appendReplacement(result, "Vue." + methodName);
        }
        matcher.appendTail(result);
        return result.toString();
    }



    /**
     * 根据提供的组件名称、编译后的 JavaScript 和 CSS 代码、依赖资源以及参数，
     * 动态生成一个完整的 HTML 页面字符串，用于在浏览器中运行 Vue 组件。
     *
     * @param name              页面标题及组件名称
     * @param compiledJs        编译后的 JavaScript 组件代码
     * @param compiledCss       编译后的 CSS 样式代码
     * @param jsDependences     JavaScript 依赖库的 URL 数组
     * @param cssDependences    CSS 依赖库的 URL 数组
     * @param params            传递给 Vue 组件的初始化参数
     * @return                  完整的 HTML 页面字符串
     */
    public String createHtml(String name, String compiledJs, String compiledCss, String[] jsDependences,String[] cssDependences, Map<String, Object> params) {
        // 初始化参数映射，如果为空则创建新的HashMap
        params = params == null ? new HashMap<>() : params;
        // 在生成的 JavaScript 代码中注入参数
        String paramsJson = JSON.toJSONString(params);
        // 移除 JavaScript 代码中的 export default 语句，使其可在浏览器环境中直接执行
        String executableJs = compiledJs.replace("export default", "");
        // 动态替换所有 vue 导入为全局 Vue 引用
        executableJs = replaceVueImports(executableJs);
        // 构建依赖库脚本标签
        StringBuilder jsDependencyScripts = new StringBuilder();
        if (jsDependences != null && jsDependences.length > 0) {
            for (String dependency : jsDependences) {
                if (StringUtils.isNotBlank(dependency)) {
                    jsDependencyScripts.append("    <script src=\"").append(dependency).append("\"></script>\n");
                }
            }
        }
        // 构建CSS依赖库链接标签
        StringBuilder cssDependencyLinks = new StringBuilder();
        if (cssDependences != null && cssDependences.length > 0) {
            for (String dependency : cssDependences) {
                if (StringUtils.isNotBlank(dependency)) {
                    cssDependencyLinks.append("    <link rel=\"stylesheet\" href=\"").append(dependency).append("\">\n");
                }
            }
        }

        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + name + "</title>\n" +
                jsDependencyScripts.toString() +
                cssDependencyLinks.toString() +  // 将CSS依赖链接添加到HTML头部
                "    <style>\n" +
                compiledCss + "\n" +    // 插入组件样式
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"app\"></div>\n" +
                "    <script>\n" +
                "        const componentParams = " + paramsJson + ";\n" +  // 注入参数
                "        async function initVueApp() {\n" +
                "            try {\n" +
                "                const component = await " + executableJs + ";\n" +
                "                // 处理 <script setup> 组件的参数传递\n" +
                "                if (typeof component === 'object') {\n" +
                "                    // 对于使用 defineProps 的组件，直接传递 props\n" +
                "                    const { createApp } = Vue;\n" +
                "                    const app = createApp(component, componentParams);\n" +  // 通过 createApp 的第二个参数传递 props
                "                    app.mount('#app');\n" +
                "                } else {\n" +
                "                    const { createApp } = Vue;\n" +
                "                    createApp(component).mount('#app');\n" +
                "                }\n" +
                "            } catch (error) {\n" +
                "                console.error('Vue app initialization failed:', error);\n" +
                "                document.getElementById('app').innerHTML = \n" +
                "                    '<div style=\"color: red; padding: 20px;\">组件初始化失败: ' + error.message + '</div>';\n" +
                "            }\n" +
                "        }\n" +
                "        initVueApp();\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * 根据组件ID和参数生成Vue组件的HTML视图页面
     *
     * @param id 组件唯一标识符，用于查找对应的Vue组件信息
     * @param params 页面渲染所需的参数集合，可包含组件初始化数据等
     * @return ResponseEntity<String> 包含完整HTML页面的响应实体，设置Content-Type为text/html
     * @throws IllegalArgumentException 当未找到对应组件或源码未编译时抛出异常
     */
    public ResponseEntity<String> view(String id, Map<String, Object> params) {
        // 获取Vue组件文件信息
        VueInfo entity = MagicConfiguration.getMagicResourceService().file(id);
        // 如果从MagicResourceService中未获取到，则尝试从VueMagicDynamicRegistry中获取映射
        if (Objects.isNull(entity)) {
            entity = SpringUtils.getBean(VueMagicDynamicRegistry.class).getMapping(id);;
        }
        // 验证组件是否存在
        if (Objects.isNull(entity)) {
            throw new IllegalArgumentException("未找到对应的组件");
        }
        // 验证组件源码是否已编译
        if (StringUtils.isBlank(entity.getCompileJs())) {
            throw new IllegalArgumentException("源码还未编译");
        }

        // 获取组件名称和依赖信息
        String name = entity.getName();
        String jsDependences = entity.getJsDependences();
        // 如果实体中没有设置JS依赖，则使用默认的JS依赖配置
        if (StringUtils.isBlank(jsDependences)) {
            jsDependences = getJsDependences();
        }

        String cssDependences = entity.getCssDependences();
        // 如果实体中没有设置CSS依赖，则使用默认的CSS依赖配置
        if (StringUtils.isBlank(cssDependences)) {
            cssDependences = getCssDependences();
        }

        // 创建最终的HTML页面
        String html = createHtml(name, entity.getCompileJs(), entity.getCompileCss(),
                jsDependences.split(","), cssDependences.split(",") ,params);
        return ResponseEntity.ok()
                .header("Content-Type", "text/html;charset=utf-8")
                .body(html);

    }


}
