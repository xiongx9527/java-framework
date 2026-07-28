package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.modules;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.model.VueInfo;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

@MagicModule("vue3")
public class VueModule implements BaseService {

    private static final Pattern VUE_IMPORT_PATTERN = Pattern.compile("await import\\('vue'\\)\\.then\\(m\\s*=>\\s*m\\.(\\w+)\\)");

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


    /**
     * 创建组件的使用示例代码字符串，包含 Composition API 和 Options API 两种方式。
     * 示例中展示如何通过动态导入的方式加载远程 Vue 组件。
     *
     * @param path Vue3组件ID或者路径
     * @return 包含两种使用方式（Composition API 和 Options API）的完整代码示例字符串
     */
    private String createUsageExample(String path) {
        String jsUrl = getWebBaseUrl() + getVue3ComponentPath() + normalizePath(path);

        return "// Composition API\n" +
                "<script setup>\n" +
                "import { ref, onMounted, defineAsyncComponent } from 'vue';\n" +
                "const remoteComponent = ref(null);\n" +
                "onMounted(async () => {\n" +
                "  try {\n" +
                "    const m = await import(/* @vite-ignore */ '" + jsUrl + "');\n" +
                "    remoteComponent.value = defineAsyncComponent(() => Promise.resolve(m.default));\n" +
                "  } catch (e) {\n" +
                "    console.error('加载失败:', e);\n" +
                "  }\n" +
                "});\n" +
                "</script>\n\n" +

                "// Options API\n" +
                "<script>\n" +
                "import { defineAsyncComponent } from 'vue';\n" +
                "export default {\n" +
                "  data() { return { remoteComponent: null }; },\n" +
                "  async mounted() {\n" +
                "    try {\n" +
                "      const m = await import(/* @vite-ignore */ '" + jsUrl + "');\n" +
                "      this.remoteComponent = defineAsyncComponent(() => Promise.resolve(m.default));\n" +
                "    } catch (e) {\n" +
                "      console.error('加载失败:', e);\n" +
                "    }\n" +
                "  }\n" +
                "};\n" +
                "</script>";
    }

    /**
     *
     * @param path Vue3组件ID或者路径
     * @return 包含组件信息的Map对象，包含以下键值对：
     *         - id: 组件ID
     *         - name: 组件名称
     *         - jsUrl: 组件JS文件的访问URL
     *         - exportUrl: 组件导出为URL的访问地址
     *         - createdTime: 组件创建时间
     *         - usage: 远程使用示例
     * @throws RuntimeException 当获取组件信息失败时抛出
     */
    public Map<String,Object> getRemoteComponentInfo(String path) {
        try {
            // 按路径从动态注册表中获取，路径不匹配时回退到 id 查找
            VueInfo entity = SpringUtils.getBean(VueMagicDynamicRegistry.class).getMapping(path);
            if (Objects.isNull(entity)) {
                entity = MagicConfiguration.getMagicResourceService().file(path);
            }
            if (Objects.isNull(entity)) {
                throw new IllegalArgumentException("Vue component not found for path: " + path);
            }

            // 检查组件是否已编译
            if (StringUtils.isBlank(entity.getCompileJs())) {
                throw new IllegalArgumentException("Vue component is not compiled for path: " + path);
            }

            // 构建远程访问URL相关信息
            String baseUrl = getWebBaseUrl();
            String jsUrl = baseUrl + getVue3ComponentPath() + normalizePath(path);
            Map<String, Object> result = new HashMap<>();
            result.put("id", entity.getId());
            result.put("path", normalizePath(path));
            result.put("name", entity.getName());
            result.put("jsUrl", jsUrl);
            result.put("createdTime", entity.getCreateTime());
            result.put("usage", createUsageExample(path));

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create exportable component for path: " + path, e);
        }
    }

    /**
     * 创建可导出的Vue组件
     *
     * @param path Vue3组件ID或者路径
     * @return 返回可导出的组件字符串表示
     * @throws RuntimeException 当组件创建失败时抛出运行时异常
     * @throws IllegalArgumentException 当指定ID的Vue组件不存在或未编译时抛出非法参数异常
     */
    public String createExportableComponent(String path) {
        try {
            // 按路径从动态注册表中获取，路径不匹配时回退到 id 查找
            VueInfo entity = SpringUtils.getBean(VueMagicDynamicRegistry.class).getMapping(path);
            if (Objects.isNull(entity)) {
                entity = MagicConfiguration.getMagicResourceService().file(path);
            }

            // 验证Vue组件是否存在
            if (Objects.isNull(entity)) {
                throw new IllegalArgumentException("Vue component not found for path: " + path);
            }

            // 检查组件是否已编译
            if (StringUtils.isBlank(entity.getCompileJs())) {
                throw new IllegalArgumentException("Vue component is not compiled for path: " + path);
            }

            // 解析依赖，实体未配置时使用默认值
            String jsDependences = entity.getJsDependences();
            if (StringUtils.isBlank(jsDependences)) {
                jsDependences = getJsDependences();
            }
            String cssDependences = entity.getCssDependences();
            if (StringUtils.isBlank(cssDependences)) {
                cssDependences = getCssDependences();
            }

            // 创建可导出的组件
            return createExportableComponent(entity.getCompileJs(), entity.getCompileCss(),
                    jsDependences.split(","), cssDependences.split(","), path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create exportable component for path: " + path, e);
        }
    }

    /**
     * 创建可导出的组件代码 - 返回有效的 ES 模块
     */
    private String createExportableComponent(String compiledJs, String compiledCss,
                                              String[] jsDependences, String[] cssDependences, String path) {
        final String STYLE_ID_PREFIX = "magic-vue-";

        // CSS ID 不能包含路径分隔符，替换为 '-'，先规范化路径
        String safeId = normalizePath(path).replace('/', '-');

        // 安全地替换 Vue 动态导入语法
        String processedJs = replaceVueImports(compiledJs)
                .replace("export default", "const component =")
                .trim();

        // 转义 CSS 内容中的反引号防止模板字符串注入
        String escapedCss = (compiledCss != null) ? compiledCss.replaceAll("`", "\\\\`") : "";

        // 构建依赖加载 + 样式注入 + 组件导出
        StringBuilder code = new StringBuilder();
        code.append("// MagicVue 远程组件 - ").append(normalizePath(path)).append("\n");

        // 依赖加载
        boolean hasJsDeps = loadDependencies(code, jsDependences, cssDependences);
        if (hasJsDeps) {
            code.append("await Promise.all(_deps);\n\n");
        } else {
            code.append("\n");
        }

        // 样式注入
        code.append("if (typeof document !== 'undefined' && document.head) {\n");
        code.append("    const styleId = '").append(STYLE_ID_PREFIX).append(safeId).append("';\n");
        code.append("    if (!document.getElementById(styleId)) {\n");
        code.append("        try {\n");
        code.append("            const style = document.createElement('style');\n");
        code.append("            style.id = styleId;\n");
        code.append("            style.textContent = `").append(escapedCss).append("`;\n");
        code.append("            document.head.appendChild(style);\n");
        code.append("        } catch (e) {\n");
        code.append("            console.warn('样式注入失败:', e);\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("}\n\n");

        // useVue3Component 工具函数，供递归加载子组件使用
        code.append(createUseVue3ComponentJs()).append("\n\n");

        // 组件定义及导出
        code.append("// 组件定义\n");
        code.append(processedJs).append(";\n\n");
        code.append("// 导出组件\n");
        code.append("export default component;");

        return code.toString();
    }

    /**
     * 生成依赖资源的动态加载代码（JS 和 CSS），返回 true 表示有 JS 依赖需要等待
     */
    private boolean loadDependencies(StringBuilder code, String[] jsDependences, String[] cssDependences) {
        boolean hasJsDeps = false;
        if (jsDependences != null && jsDependences.length > 0) {
            for (String dependency : jsDependences) {
                if (StringUtils.isBlank(dependency)) {
                    continue;
                }
                String url = resolveUrl(dependency.trim());
                if (!hasJsDeps) {
                    code.append("var _deps=[];\n");
                    code.append("function _loadScript(u){return new Promise(function(r,e){if(document.querySelector('script[src=\"'+u+'\"]')){r();return;}var s=document.createElement('script');s.src=u;s.onload=r;s.onerror=e;document.head.appendChild(s);});}\n");
                    hasJsDeps = true;
                }
                code.append("_deps.push(_loadScript('").append(url).append("'));\n");
            }
        }
        if (cssDependences != null && cssDependences.length > 0) {
            for (String dependency : cssDependences) {
                if (StringUtils.isBlank(dependency)) {
                    continue;
                }
                String url = resolveUrl(dependency.trim());
                code.append("if (typeof document !== 'undefined' && !document.querySelector('link[href=\"").append(url).append("\"]')) {\n");
                code.append("    var link = document.createElement('link');\n");
                code.append("    link.rel = 'stylesheet';\n");
                code.append("    link.href = '").append(url).append("';\n");
                code.append("    document.head.appendChild(link);\n");
                code.append("}\n");
            }
        }
        return hasJsDeps;
    }

    /**
     * 获取 Web 基础 URL，确保不以 "/" 结尾
     */
    private String getWebBaseUrl() {
        String web = properties.getWeb();
        return web.endsWith("/") ? web.substring(0, web.length() - 1) : web;
    }

    /**
     * 规范化路径，确保不以 "/" 开头
     */
    private static String normalizePath(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    /**
     * 生成 useVue3Component 工具函数的 JS 代码
     */
    private String createUseVue3ComponentJs() {
        return "const  useVue3Component=async(p)=>{const m=await import('" + getWebBaseUrl() + getVue3ComponentPath() + "'+p.replace(/^\\//,''));return m.default;};";
    }

    /**
     * 解析依赖资源的完整URL地址
     * <p>
     * 如果传入的URL是相对路径（不以http://或https://开头），则将其拼接Web基础路径转换为绝对URL；
     * 如果已经是完整的HTTP/HTTPS URL，则直接返回原值。
     * </p>
     *
     * @param dependency 依赖资源的URL地址，可以是相对路径或完整的HTTP/HTTPS URL
     * @return 解析后的完整URL地址
     */
    private String resolveUrl(String dependency) {
        if (!dependency.startsWith("http://") && !dependency.startsWith("https://")) {
            String baseUrl = properties.getWeb();
            if (baseUrl.endsWith("/") && dependency.startsWith("/")) {
                return baseUrl + dependency.substring(1);
            } else if (baseUrl.endsWith("/") || dependency.startsWith("/")) {
                return baseUrl + dependency;
            } else {
                return baseUrl + "/" + dependency;
            }
        }
        return dependency;
    }

    /**
     * 替换所有 Vue 导入语句为全局 Vue 引用
     */
    private String replaceVueImports(String code) {
        Matcher matcher = VUE_IMPORT_PATTERN.matcher(code);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, "Vue." + matcher.group(1));
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

        // 处理 compiledCss 为 null 的情况
        String safeCss = compiledCss != null ? compiledCss : "";

        // 构建CSS依赖库链接标签（CSS 在 <head> 中正常加载）
        StringBuilder cssDependencyLinks = new StringBuilder();
        if (cssDependences != null && cssDependences.length > 0) {
            for (String dependency : cssDependences) {
                if (StringUtils.isNotBlank(dependency)) {
                    cssDependencyLinks.append("    <link rel=\"stylesheet\" href=\"").append(resolveUrl(dependency)).append("\">\n");
                }
            }
        }

        // 构建 JS 依赖动态加载脚本（Blob URL iframe 中 <script src> 可能失败，改用动态注入）
        StringBuilder jsLoader = new StringBuilder();
        if (jsDependences != null && jsDependences.length > 0) {
            jsLoader.append("        function _loadScript(u){return new Promise(function(r,e){if(document.querySelector('script[data-magic-deps=\"'+u+'\"]')){r();return;}var s=document.createElement('script');s.src=u;s.setAttribute('data-magic-deps',u);s.onload=r;s.onerror=function(){e(new Error('Failed to load: '+u));};document.head.appendChild(s);});}\n");
            jsLoader.append("        var _deps=[];\n");
            for (String dependency : jsDependences) {
                if (StringUtils.isNotBlank(dependency)) {
                    jsLoader.append("        _deps.push(_loadScript('").append(resolveUrl(dependency.trim())).append("'));\n");
                }
            }
        }

        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + escapeHtml4(name) + "</title>\n" +
                cssDependencyLinks.toString() +
                "    <style>\n" +
                safeCss + "\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"app\"></div>\n" +
                "    <script>\n" +
                jsLoader.toString() +
                "        " + createUseVue3ComponentJs() + "\n" +
                "        const componentParams = " + paramsJson + ";\n" +
                "        async function initVueApp() {\n" +
                "            try {\n" +
                "                if (typeof _deps !== 'undefined') await Promise.all(_deps);\n" +
                "                const component = await " + executableJs + ";\n" +
                "                if (typeof component === 'object') {\n" +
                "                    const { createApp } = Vue;\n" +
                "                    const app = createApp(component, componentParams);\n" +
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
     * @param path Vue3组件ID或者路径
     * @param params 页面渲染所需的参数集合，可包含组件初始化数据等
     * @return ResponseEntity<String> 包含完整HTML页面的响应实体，设置Content-Type为text/html
     * @throws IllegalArgumentException 当未找到对应组件或源码未编译时抛出异常
     */
    public ResponseEntity<String> view(String path, Map<String, Object> params) {
        // 按路径从动态注册表中获取，路径不匹配时回退到 id 查找
        VueInfo entity = SpringUtils.getBean(VueMagicDynamicRegistry.class).getMapping(path);
        if (Objects.isNull(entity)) {
            entity = MagicConfiguration.getMagicResourceService().file(path);
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
