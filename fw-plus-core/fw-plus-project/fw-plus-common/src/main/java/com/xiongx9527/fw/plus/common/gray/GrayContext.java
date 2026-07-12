package com.xiongx9527.fw.plus.common.gray;


import com.xiongx9527.fw.plus.common.web.WebRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * 灰度上下文管理类，用于在线程间传递灰度版本和标签信息
 * 支持阿里巴巴的TransmittableThreadLocal以实现线程池环境下的上下文传递
 */
public abstract class GrayContext {

    /**
     * 灰度发布版本标识请求头名称常量
     * 用于在HTTP请求头中传递灰度版本信息
     */
    public static final String GRAY_VERSION_HEADER = "grayVersion";

    /**
     * 灰度发布标签标识请求头名称常量
     * 用于在HTTP请求头中传递灰度标签信息
     */
    public static final String GRAY_TAG_HEADER = "grayTag";

    /**
     * 线程本地变量，存储当前线程的灰度版本信息
     * 使用ThreadLocal确保线程安全，每个线程独立存储自己的版本信息
     */
    private static final ThreadLocal<String> VERSION;

    /**
     * 线程本地变量，存储当前线程的灰度标签信息
     * 使用ThreadLocal确保线程安全，每个线程独立存储自己的标签信息
     */
    private static final ThreadLocal<String> TAG;

    /**
     * 设置当前线程的灰度版本信息
     * @param version 灰度版本字符串
     */
    public static void setVersion(String version) { VERSION.set(version); }

    /**
     * 获取当前线程的灰度版本信息
     * @return 当前设置的灰度版本字符串
     */
    public static String getVersion() { return VERSION.get(); }

    /**
     * 设置当前线程的灰度标签信息
     * @param tag 灰度标签字符串
     */
    public static void setTag(String tag) { TAG.set(tag); }

    /**
     * 获取当前线程的灰度标签信息
     * @return 当前设置的灰度标签字符串
     */
    public static String getTag() { return TAG.get(); }

    /**
     * 清除当前线程的灰度上下文信息，避免内存泄漏
     */
    public static void clear() {
        VERSION.remove();
        TAG.remove();
    }

    /**
     * 判断当前请求是否为灰度环境
     *
     * @return boolean 返回true表示当前处于灰度环境，false表示非灰度环境
     */
    public static boolean isGray() {
        return StringUtils.isNotBlank(getVersion()) && StringUtils.isNotBlank(getTag());
    }

    /**
     * 初始化灰度配置信息
     *
     * @param request WebRequest对象，用于获取请求头中的灰度版本和标签信息
     */
    public static void init(WebRequest request) {
        String grayVersion = request.getHeader(GRAY_VERSION_HEADER);
        String grayTag = request.getHeader(GRAY_TAG_HEADER);

        if (StringUtils.isNotBlank(grayVersion) && StringUtils.isNotBlank(grayTag)) {
            setVersion(grayVersion);
            setTag(grayTag);
        }
    }

    static {
        ThreadLocal<String> version;
        ThreadLocal<String> tag;
        try {
            // 尝试创建阿里巴巴的TransmittableThreadLocal实例，用于支持线程池上下文传递
            version = (ThreadLocal)Class.forName("com.alibaba.ttl.TransmittableThreadLocal").newInstance();
            tag = (ThreadLocal)Class.forName("com.alibaba.ttl.TransmittableThreadLocal").newInstance();
        } catch (Throwable e) {
            // 如果TransmittableThreadLocal不可用，则降级使用标准的InheritableThreadLocal
            version = new InheritableThreadLocal();
            tag = new InheritableThreadLocal();
        }

        VERSION = version;
        TAG = tag;
    }
}
