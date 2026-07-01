package com.xx9527.fw.plus.boot.common.web.mvc.http;

import com.xx9527.fw.plus.boot.common.web.BaseRequest;
import com.xx9527.fw.plus.boot.common.web.MyHttpEncryptor;
import com.xx9527.fw.plus.boot.common.web.MyHttpEncryptorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

@Slf4j
public class DecryptWebMvcHttpServletRequestWrapper extends MyWebMvcHttpServletRequestWrapper implements BaseRequest {

    private HttpServletRequest request = null;

    private MyHttpEncryptor encryptor = null;

    private MyHttpEncryptorProperties encryptorProperties = null;

    private Map<String, String[]> parameterMap = null;

    private String requestBodyStr = null;


    public DecryptWebMvcHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
        this.request = request;
    }

    public DecryptWebMvcHttpServletRequestWrapper(HttpServletRequest request ,
                                                  MyHttpEncryptor encryptor,
                                                  MyHttpEncryptorProperties encryptorProperties) {
        this(request);
        this.encryptor = encryptor;
        this.encryptorProperties = encryptorProperties;
        if (isNeedDecrypt()) {
            decryptParameter();
            decryptRequestBody();
        }
    }

    @Override
    public MyHttpEncryptor encryptor() {
        return this.encryptor;
    }

    @Override
    public MyHttpEncryptorProperties encryptorProperties() {
        return this.encryptorProperties;
    }


    /**
     * 判断是否需要解密
     *
     * 此方法尝试从请求头中获取一个特定的加密标志，以决定是否对数据进行解密
     * 它根据预定义的请求头名称来获取值，并将其解析为布尔值如果请求头中没有设置标志，
     * 或者出现了任何异常（如读取请求头失败），方法将默认返回false，表示不需要解密
     *
     * @return Boolean 表示是否需要解密如果不需要解密，则返回Boolean.FALSE
     */
    public Boolean isNeedDecrypt() {
        if (null != encryptorProperties ) {
            if (MyHttpEncryptorProperties.MyHttpEncryptorMode.ALL.equals(encryptorProperties.getEncryptionMode())
              || MyHttpEncryptorProperties.MyHttpEncryptorMode.Decryption.equals(encryptorProperties.getEncryptionMode())) {
                return Boolean.TRUE;
            }
            try {
                return Boolean.getBoolean(request.getHeader(encryptorProperties.getDecryptionFlagHeadName()));
            } catch (Exception e) {
                // 如果出现异常，记录错误日志
                log.error(e.getMessage(), e);
            }
        }
        // 默认返回false，表示不需要加密
        return Boolean.FALSE;
    }

    /**
     * 判断是否需要加密
     *
     * 此方法尝试从请求头中获取加密标志，以决定是否需要对数据进行加密
     * 加密标志的头信息名称由加密器属性配置指定
     *
     * @return Boolean.FALSE 如果不需要加密或获取加密标志时出现异常，则默认返回false
     */
    public Boolean isNeedEncrypt() {
        if (null != encryptorProperties) {
            if (MyHttpEncryptorProperties.MyHttpEncryptorMode.ALL.equals(encryptorProperties.getEncryptionMode())
                    || MyHttpEncryptorProperties.MyHttpEncryptorMode.Encryption.equals(encryptorProperties.getEncryptionMode())) {
                return Boolean.TRUE;
            }
            try {
                // 从请求头中获取加密标志，并将其解析为Boolean值
                return Boolean.getBoolean(request.getHeader(encryptorProperties.getEncryptionFlagHeadName()));
            } catch (Exception e) {
                // 如果出现异常，记录错误日志
                log.error(e.getMessage(), e);
            }
        }
        // 默认返回false，表示不需要加密
        return Boolean.FALSE;
    }

    /**
     * 解密参数方法
     * 该方法旨在解密包含在请求路径中的参数信息
     * 它首先获取请求的路径信息，然后尝试解密并解析这些信息
     * 注意：此方法依赖于全局变量request和parameterMap，未在代码片段中定义
     */
    private void decryptParameter() {
        if (isNeedDecrypt()) {
            // 获取请求路径信息，去除上下文路径长度
            String pathInfo = request.getRequestURI().substring(request.getContextPath().length());
            // 检查路径信息是否非空
            if (StringUtils.isNotEmpty(pathInfo)){
                try {
                    // 尝试解密路径信息，并解析为参数映射
                    this.parameterMap = parseDecryptedParams(decrypt(pathInfo));
                } catch (Throwable e) {
                    // 捕获并记录解密或解析过程中可能发生的任何异常
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 解密请求体
     * 该方法用于读取并解密请求中的原始加密数据
     */
    private void decryptRequestBody() {
        if (isNeedDecrypt()) {
            // 读取原始请求体
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = null;
            try {
                // 获取请求的输入流
                InputStream inputStream = request.getInputStream();
                if (inputStream != null) {
                    // 使用BufferedReader读取输入流中的数据
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    char[] charBuffer = new char[128];
                    int bytesRead;
                    // 循环读取数据直到结束
                    while ((bytesRead = bufferedReader.read(charBuffer)) != -1) {
                        // 将读取的数据追加到StringBuilder中
                        stringBuilder.append(charBuffer, 0, bytesRead);
                    }
                }
            } catch (IOException ex) {
                // 异常处理：关闭资源
            } finally {
                // 确保BufferedReader被关闭
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException ex) {
                        // 异常处理：忽略IO异常
                    }
                    // 对读取到的原始数据进行解密处理
                    requestBodyStr = decrypt(stringBuilder.toString());
                }
            }
        }
    }

    /**
     * 重写getParameter方法，以支持从自定义参数映射中获取参数值
     * 当参数映射为空时，委托给原始请求对象处理参数获取
     *
     * @param name 参数名称
     * @return 参数值，如果参数不存在或者映射中没有对应值，则返回null
     */
    @Override
    public String getParameter(String name) {
        // 当参数映射为空时，调用原始请求的getParameter方法
        if (MapUtils.isEmpty(parameterMap)){
            return request.getParameter(name);
        }
        // 从参数映射中获取指定名称的参数值数组
        String[] values = parameterMap.get(name);
        // 如果数组为空或者长度为0，则返回null；否则返回数组的第一个元素
        return values == null || values.length == 0 ? null : values[0];
    }

    /**
     * 重写getParameterMap方法以获取请求参数的映射
     * 此方法首先检查parameterMap是否为空如果为空，则返回原始请求的参数映射；
     * 否则，返回一个不可修改的parameterMap副本这样做的目的是为了
     * 提供一个安全的参数映射视图，防止外部修改
     *
     * @return 如果parameterMap为空，则返回原始请求的参数映射；
     *         否则返回一个不可修改的parameterMap副本
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        // 检查parameterMap是否为空如果为空，则返回原始请求的参数映射
        if (MapUtils.isEmpty(parameterMap)){
            return request.getParameterMap();
        }
        // 返回一个不可修改的parameterMap副本，以提供安全的参数映射视图
        return Collections.unmodifiableMap(parameterMap);
    }

    /**
     * 重写getParameterNames方法
     *
     * 当parameterMap为空时，调用原始请求的getParameterNames方法返回参数名称的Enumeration
     * 否则，将parameterMap的键集转换为Enumeration并返回
     * 这样做是为了根据parameterMap的状态决定返回值，从而实现对请求参数的动态处理
     *
     * @return 一个包含所有参数名称的Enumeration
     */
    @Override
    public Enumeration<String> getParameterNames() {
        // 检查parameterMap是否为空
        if (MapUtils.isEmpty(parameterMap)){
            // 如果为空，则调用原始请求的getParameterNames方法
            return request.getParameterNames();
        }
        // 如果不为空，则将parameterMap的键集转换为Enumeration
        return Collections.enumeration(parameterMap.keySet());
    }

    /**
     * 重写getParameterValues方法以处理参数获取逻辑
     * 此方法首先检查parameterMap是否为空，如果为空，则调用原始的request.getParameterValues方法
     * 否则，从parameterMap中获取对应的参数值数组
     * 这种做法的目的是在parameterMap存在时优先使用其中的参数值，以确保参数值的正确性和一致性
     *
     * @param name 参数名，用于获取对应的参数值数组
     * @return String[] 参数值数组，如果parameterMap中没有对应的参数，则返回null或空数组
     */
    @Override
    public String[] getParameterValues(String name) {
        // 检查parameterMap是否为空，如果为空则调用原始的getParameterValues方法
        if (MapUtils.isEmpty(parameterMap)){
            return request.getParameterValues(name);
        }
        // 从parameterMap中获取参数值数组并返回
        return parameterMap.get(name);
    }

    /**
     * 重写getInputStream方法以提供自定义的ServletInputStream
     * 这个方法根据requestBodyStr是否为空来决定返回原始的ServletInputStream还是一个基于requestBodyStr的自定义ServletInputStream
     *
     * @return ServletInputStream对象，用于读取HTTP请求的正文
     * @throws IOException 如果读取过程中发生I/O错误
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 如果requestBodyStr为空，则返回原始的ServletInputStream
        if (StringUtils.isEmpty(requestBodyStr)){
            return request.getInputStream();
        }

        // 创建一个基于requestBodyStr的ByteArrayInputStream，使用UTF-8编码
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(requestBodyStr.getBytes(StandardCharsets.UTF_8));

        // 返回一个匿名内部类实例，实现ServletInputStream接口
        return new ServletInputStream() {
            // isFinished方法返回false，表示输入流尚未结束
            @Override
            public boolean isFinished() {
                return false;
            }

            // isReady方法返回true，表示输入流已准备好
            @Override
            public boolean isReady() {
                return true;
            }

            // setReadListener方法未实现，因为当前上下文不需要异步读取功能
            @Override
            public void setReadListener(ReadListener readListener) {
                // Not implemented
            }

            // read方法从ByteArrayInputStream中读取下一个字节
            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
    }

    /**
     * 重写getReader方法，提供一个用于读取输入流的BufferedReader对象
     * 这个方法主要用于需要以字符流方式处理输入的情况，比如解析或显示输入数据
     *
     * @return BufferedReader对象，封装了当前实体的输入流，用于高效地读取字符数据
     * @throws IOException 如果无法创建用于读取输入流的BufferedReader对象
     */
    @Override
    public BufferedReader getReader() throws IOException {
        // 创建并返回一个BufferedReader对象，它将当前实体的输入流包装为一个字符流
        // 使用InputStreamReader将输入字节流转换为字符流，并指定字符集
        // BufferedReader进一步包装InputStreamReader，以提供高效的读取操作
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }
}
