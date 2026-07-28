package com.xiongx9527.fw.plus.common.util;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.common.exception.BaseRuntimeException;
import com.xiongx9527.fw.plus.common.web.WebRequest;
import com.xiongx9527.fw.plus.common.web.WebRequestContextHolder;
import com.xiongx9527.fw.plus.common.web.WebResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public abstract class WebUtils extends org.springframework.web.util.WebUtils {
    /**
     * 默认路径匹配器，使用Ant风格的路径匹配
     */
    private static final PathMatcher defaultPathMatcher = new AntPathMatcher();

    /**
     * 默认URL路径助手，用于处理URL路径相关操作
     */
    private static final UrlPathHelper defaultUrlPathHelper = new UrlPathHelper();

    /**
     * 标识是否支持WebFlux框架的标志位
     */
    private static boolean isWebFlux = false;

    /**
     * 标识是否支持WebMvc框架的标志位
     */
    private static boolean isWebMvc = false;

    private static final int WEB_APPLICATION_TYPE_NONE = 0;

    private static final int WEB_APPLICATION_TYPE_SERVLET = 1;

    private static final int WEB_APPLICATION_TYPE_REACTIVE = 2;

    private static volatile int webApplicationType = WEB_APPLICATION_TYPE_NONE;

    /**
     * 静态初始化块，用于检测当前环境中的Web框架类型
     * 通过尝试加载WebFluxConfigurer和WebMvcConfigurer类来判断是否支持相应的框架
     */
    static {
        try {
            Class.forName("org.springframework.web.reactive.config.WebFluxConfigurer");
            isWebFlux = true;
            log.debug("WebFlux framework detected");
        } catch (ClassNotFoundException e) {
            log.debug("WebFlux framework not detected");
        }

        try {
            Class.forName("org.springframework.web.servlet.config.annotation.WebMvcConfigurer");
            isWebMvc = true;
            log.debug("WebMvc framework detected");
        } catch (ClassNotFoundException e) {
            log.debug("WebMvc framework not detected");
        }
    }



    /**
     * 获取当前线程的Web请求对象
     *
     * 此方法使用WebRequestContextHolder来获取当前线程绑定的Web请求对象WebRequest
     * 主要用于在跨层调用或异步处理中获取请求相关的信息，如请求URL、请求参数等
     *
     * @return 当前线程的Web请求对象如果当前线程没有绑定Web请求，则返回null
     */
    public static WebRequest getRequest() {
        return WebRequestContextHolder.getRequest();
    }


    /**
     * 获取当前线程的Web响应对象
     * 此方法用于从WebRequestContextHolder中获取当前线程的Web响应对象WebResponse
     * 主要用于在跨不同层次的调用中保持响应对象的可访问性
     *
     * @return WebResponse 当前线程的Web响应对象
     */
    public static WebResponse getResponse() {
        return WebRequestContextHolder.getResponse();
    }


    /**
     * 解析并缓存请求的查找路径
     *
     * 该方法使用DefaultUrlPathHelper类的实例来解析HTTP请求中的查找路径，
     * 并将该路径缓存以供后续使用这有助于提高性能，因为它避免了对相同请求路径的重复解析
     *
     * @param request HttpServletRequest对象，包含当前请求的所有信息
     * @return 解析并缓存的查找路径作为字符串返回
     */
    public static String resolveAndCacheLookupPath(WebRequest request) {
        return WebRequestContextHolder.resolveAndCacheLookupPath(request);
    }

    /**
     * 获取请求的查找路径
     * 查找路径用于确定请求的目标资源，例如处理请求映射时
     * 此方法利用Spring的UrlPathHelper工具类来解析请求，提取出查找路径
     *
     * @param request HttpServletRequest对象，包含请求相关的信息
     * @return 返回解析后的查找路径字符串
     */
    public static String getLookupPath(WebRequest request) {
        return WebRequestContextHolder.getLookupPath(request);
    }

    /**
     * 根据给定的模式匹配路径
     * 此方法用于检查给定的路径是否与指定的模式匹配它使用系统的默认路径匹配器来执行实际的匹配
     * 主要用于在处理文件或路径时需要进行模式匹配的场景
     *
     * @param pattern 模式字符串，用于匹配路径
     * @param path    路径字符串，需要与模式进行匹配
     * @return 如果路径与模式匹配，则返回true；否则返回false
     */
    public static boolean matchPath(String pattern, String path) {
        return defaultPathMatcher.match(pattern, path);
    }

    /**
     * 获取请求的IP地址
     * 该方法尝试从HTTP请求头中获取真实的IP地址，如果所有尝试均失败，则回退到服务器提供的远程地址
     *
     * @param request HTTP请求对象，用于获取请求头信息
     * @return 请求的IP地址如果无法确定IP地址，则返回"unknown"
     */
    public static String getRequestIp(WebRequest request) {
        // 尝试从请求头中获取原始转发的IP地址
        String ip = request.getHeader("x-original-forwarded-for");
        if (StringUtils.isEmpty(ip)) {
            // 如果获取失败，则尝试获取转发的IP地址
            ip = request.getHeader("x-forwarded-for");
        }
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 处理IP地址中可能存在的逗号分隔情况，取第一个IP地址
            int idx = ip.indexOf(",");
            if (idx != -1) {
                ip = ip.substring(0, idx);
            }
            // 有时候会带有端口，把端口去掉
            idx = ip.indexOf(':');
            if (idx > 0) {
                ip = ip.substring(0, idx);
            }
            return ip;
        }

        // 尝试从其他请求头中获取IP地址
        ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 所有尝试均失败，回退到服务器提供的远程地址
        return request.getRemoteAddr();
    }

    /**
     * 从HttpServletRequest中获取请求体
     *
     * @param request HttpServletRequest对象，用于获取请求体内容
     * @return byte[] 请求体的字节内容
     * @throws BaseRuntimeException 当读取请求体失败时抛出内部错误异常
     */
    public static byte[] getRequestBody(WebRequest request) {
        try {
            byte[] body;
            // 获取请求内容的长度
            int len = request.getContentLength();
            if (len > 0) {
                // 如果内容长度大于0，根据长度初始化字节数组
                body = new byte[len];
            }
            else {
                // 如果内容长度为0，初始化一个默认大小的字节数组
                body = new byte[512];
            }

            // 初始化实际读取到的请求体长度
            int bodyLen = 0;
            // 使用try-with-resources确保ServletInputStream在使用后能被正确关闭
            try(InputStream is = request.getInputStream()) {
                int n;
                do {
                    // 从输入流中读取数据到字节数组中
                    n = is.read(body, bodyLen, body.length - bodyLen);
                    if (n < 0) {
                        // 如果读取到的数据长度小于0，表示已经读取完毕
                        break;
                    }
                    // 累加实际读取到的数据长度
                    bodyLen += n;
                    // 如果当前缓冲区已满，扩大缓冲区大小
                    if (bodyLen == body.length) {
                        body = Arrays.copyOf(body, body.length + 1024);
                    }
                } while (true);
            }
            // 根据实际读取到的数据长度，返回请求体字节数组
            if (bodyLen == body.length) {
                return body;
            } else {
                return Arrays.copyOf(body, bodyLen);
            }
        } catch (IOException e) {
            // 当读取请求体过程中发生IO异常时，抛出自定义内部错误异常
            throw new BaseRuntimeException("获取RequestBody失败", e);
        }
    }

    /**
     * 将HTTP请求的主体内容转换为字符串形式
     * 此方法主要用于从HTTP请求中获取请求体，并将其编码为UTF-8格式的字符串
     * 适用于需要以字符串形式处理请求体的场景
     *
     * @param request HttpServletRequest对象，包含客户端的请求信息
     * @return 请求体的字符串表示，若请求体为空，则返回空字符串
     */
    public static String getRequestBodyAsString(WebRequest request) {
        // 获取请求体的字节内容
        byte[] body = getRequestBody(request);
        // 将字节内容转换为UTF-8编码的字符串
        return new String(body,0, body.length, StandardCharsets.UTF_8);
    }

    /**
     * 从HttpServletRequest中获取请求体，并将其解析为指定类型的对象
     * 此方法主要用于处理HTTP请求中的请求体，将其转换为方便操作的对象形式
     *
     * @param request HttpServletRequest对象，包含客户端发送的请求信息
     * @param clazz 指定的类类型，用于解析请求体字符串到该类型的对象
     * @return 解析后的对象，类型为clazz指定的类型
     */
    public static <T> T getRequestBody(WebRequest request, Class<T> clazz) {
        // 获取请求体字节数组
        byte[] body = getRequestBody(request);
        // 将字节数组转换为字符串
        String str = new String(body,0, body.length, StandardCharsets.UTF_8);
        // 使用JSON工具类将字符串解析为指定类型的对象
        return JSON.parseObject(str, clazz);
    }


    /**
     * 判断HTTP请求是否为WebSocket升级请求
     *
     * @param request HTTP请求对象，用于获取请求头信息
     * @return 如果请求希望升级到WebSocket，则返回true；否则返回false
     */
    public static boolean isWebSocketRequest(WebRequest request) {
        String connectionHeader = request.getHeader("Connection");
        String upgradeHeader = request.getHeader("Upgrade");
        return "upgrade".equalsIgnoreCase(connectionHeader) && "websocket".equalsIgnoreCase(upgradeHeader);
    }

    /**
     * 判断是否为JSON请求
     * 通过检查请求的Content-Type和Accept头，以及X-Requested-With头来判断请求是否为JSON请求
     *
     * @param request HttpServletRequest对象，用于获取请求头信息
     * @return 如果请求为JSON请求，则返回true；否则返回false
     */
    public static boolean isJsonRequest(WebRequest request) {
        // 获取请求的Content-Type
        String contentType = request.getContentType();
        // 检查Content-Type是否为application/json、application/xml或text/json
        // 如果是，则认为是JSON请求，返回true
        // 这里检查Content-Type是否包含多种JSON相关的类型，以适应不同的情况
        return contentType != null && (contentType.contains("application/json")
                || contentType.contains("application/xml") || contentType.contains("text/json") );
    }

    /**
     * 将HTTP请求参数绑定到指定的数据对象上
     *
     * @param request HTTP请求对象，包含请求参数
     * @param data 需要绑定请求参数的目标数据对象
     * @return 返回绑定结果，包含数据对象及可能的验证错误信息
     */
    public static BindingResult bindRequestParameters(jakarta.servlet.http.HttpServletRequest request, Object data) {
        // 创建ServletRequestDataBinder实例，用于绑定请求参数到数据对象
        ServletRequestDataBinder binder = new ServletRequestDataBinder(data);

        // 执行参数绑定操作
        binder.bind(request);

        // 返回绑定结果，可以用于后续的错误检查或数据验证
        return binder.getBindingResult();
    }

    /**
     * 将Web请求参数绑定到指定的数据对象上，兼容Servlet和WebFlux请求。
     *
     * @param request Web请求对象，包含请求参数
     * @param data 需要绑定请求参数的目标数据对象
     * @return 返回绑定结果，包含数据对象及可能的验证错误信息
     */
    public static BindingResult bindRequestParameters(WebRequest request, Object data) {
        Object nativeRequest = request == null ? null : request.getRequest();
        if (nativeRequest instanceof jakarta.servlet.http.HttpServletRequest) {
            return bindRequestParameters((jakarta.servlet.http.HttpServletRequest) nativeRequest, data);
        }
        DataBinder binder = new DataBinder(data);
        binder.bind(new MutablePropertyValues(getParameterMap(request)));
        return binder.getBindingResult();
    }

    private static Map<String, String[]> getParameterMap(WebRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }
        Map<String, String[]> parameters = new LinkedHashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            parameters.put(name, request.getParameterValues(name));
        }
        return parameters;
    }

    public static void resultErrorResponse(WebRequest request, WebResponse response, int errorCode,Object result,String errPage) throws Throwable {
        // 判断是否为JSON请求
        if (WebUtils.isJsonRequest(request)) {
            // 是JSON请求，调用restResponse方法处理
            restResponse(response, result);
        } else {
            // 处理其他类型的请求
            // 此处可根据需要添加处理其他请求类型的代码
        }
    }

    /**
     * 根据请求类型返回相应的结果
     * 此方法旨在处理不同的请求类型，并提供适当的响应
     * 主要用于判断请求是否为JSON类型，从而调用不同的处理方法
     *
     * @param request  HttpServletRequest对象，用于获取请求信息
     * @param response HttpServletResponse对象，用于发送响应信息
     * @param result    要返回给客户端的结果对象
     * @throws IOException 如果在处理响应过程中发生I/O错误
     */
    public static void resultResponse(WebRequest request, WebResponse response, Object result) throws Throwable {
        // 判断是否为JSON请求
        if (WebUtils.isJsonRequest(request)) {
            // 是JSON请求，调用restResponse方法处理
            restResponse(response, result);
        } else {
            // 处理其他类型的请求
            // 此处可根据需要添加处理其他请求类型的代码
        }
    }

    /**
     * 准备HTTP响应内容，以RESTful风格返回结果
     * 此方法主要用于设置HTTP响应的头部信息，以及将结果对象序列化为JSON格式写入响应
     *
     * @param response HTTP响应对象，用于设置响应的各类信息
     * @param result 结果对象，可以是任意类型，如果是指定的RestResult类型，将额外设置追踪ID
     * @throws IOException 当写入响应流时发生错误
     */
    public static void restResponse(WebResponse response, Object result) throws Throwable {
        // 设置响应的内容类型为JSON格式
        response.setContentType("application/json");
        // 设置字符编码为UTF-8，确保兼容性并防止中文乱码
        response.setCharacterEncoding("UTF-8");
        // 设置响应的过期时间为0，表示不缓存
        response.setDateHeader("Expires", 0);
        // 通过设置响应头，禁止浏览器和代理服务器缓存响应内容
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        // 进一步设置缓存控制，明确检查机制
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        // 设置Pragma头，作为旧HTTP版本的缓存控制
        response.setHeader("Pragma", "no-cache");
        // 将结果对象序列化为JSON字符串，并写入响应流
        response.writeWith(JSON.toJSONString(result));
    }

    /**
     * 获取默认的路径匹配器
     *
     * @return PathMatcher类型的默认路径匹配器实例
     */
    public static PathMatcher defaultPathMatcher() {
        return defaultPathMatcher;
    }

    /**
     * 获取默认的URL路径帮助器
     *
     * @return UrlPathHelper类型的默认URL路径帮助器实例
     */
    public static UrlPathHelper defaultUrlPathHelper() {
        return defaultUrlPathHelper;
    }

    /**
     * 判断当前环境是否为WebFlux模式
     *
     * @return boolean类型，true表示当前为WebFlux环境，false表示不是WebFlux环境
     */
    public static boolean isWebFlux() {
        if (webApplicationType == WEB_APPLICATION_TYPE_REACTIVE) {
            return true;
        }
        if (webApplicationType == WEB_APPLICATION_TYPE_SERVLET) {
            return false;
        }
        return isWebFlux && !isWebMvc;
    }

    /**
     * 判断当前环境是否为WebMvc模式
     *
     * @return boolean类型，true表示当前为WebMvc环境，false表示不是WebMvc环境
     */
    public static boolean isWebMvc() {
        if (webApplicationType == WEB_APPLICATION_TYPE_SERVLET) {
            return true;
        }
        if (webApplicationType == WEB_APPLICATION_TYPE_REACTIVE) {
            return false;
        }
        return isWebMvc && !isWebFlux;
    }

    /**
     * 标记当前应用为WebFlux运行环境。
     */
    public static void useWebFlux() {
        webApplicationType = WEB_APPLICATION_TYPE_REACTIVE;
    }

    /**
     * 标记当前应用为WebMvc运行环境。
     */
    public static void useWebMvc() {
        webApplicationType = WEB_APPLICATION_TYPE_SERVLET;
    }

}
