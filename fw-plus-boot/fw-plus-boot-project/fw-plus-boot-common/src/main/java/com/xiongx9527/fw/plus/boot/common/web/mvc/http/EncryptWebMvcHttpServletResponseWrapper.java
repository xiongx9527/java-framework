package com.xiongx9527.fw.plus.boot.common.web.mvc.http;

import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptor;
import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptorProperties;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class EncryptWebMvcHttpServletResponseWrapper extends HttpServletResponseWrapper {
    private HttpServletResponse response = null;
    private MyHttpEncryptor encryptor = null;
    private MyHttpEncryptorProperties encryptorProperties = null;
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private PrintWriter writer = new PrintWriter(outputStream);

    public EncryptWebMvcHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
        this.response = response;
    }

    public EncryptWebMvcHttpServletResponseWrapper(HttpServletResponse response,
                                                   MyHttpEncryptor encryptor,
                                                   MyHttpEncryptorProperties encryptorProperties) {
        this(response);
        this.encryptor = encryptor;
        this.encryptorProperties = encryptorProperties;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }
            @Override
            public void setWriteListener(WriteListener writeListener) {
                // Not implemented
            }
            @Override
            public void write(int b) throws IOException {
                outputStream.write(b);
            }
        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return writer;
    }

    /**
     * 获取字节数组
     *
     * 此方法用于获取当前输出流中的字节数组表示形式它将输出流中写入的所有字节转换为一个字节数组
     * 这在需要将输出流的内容序列化或进行字节级操作时非常有用
     *
     * @return 字节数组，包含输出流中的所有字节
     */
    public byte[] getByteArray() {
        return outputStream.toByteArray();
        /***
        try {
            // 获取原始字节数组
            byte[] originalBytes = outputStream.toByteArray();
            // 将原始字节数组转换为字符串，使用响应的字符编码
            String originalContent = new String(originalBytes, response.getCharacterEncoding());
            // 使用加密器对原始内容进行加密
            String encryptedContent = encryptor.encrypt(encryptorProperties.getAlgorithm(), encryptorProperties.getPublicKey(),
                    encryptorProperties.getIv(), encryptorProperties.getSalt(), originalContent);
            // 将加密后的内容写入响应流
            return encryptedContent.getBytes(response.getCharacterEncoding());
        } catch (Throwable e) {
            // 如果在加密或写入过程中发生异常，将其包装为运行时异常并重新抛出
            throw new RuntimeException(e);
        }
         ***/
    }

    /**
     * 加密并写入数据
     * 本方法用于获取原始数据，对其进行加密，并将加密后的数据写入响应流中
     * 主要步骤包括：
     * 1. 获取原始字节数组并转换为字符串
     * 2. 使用加密器对原始内容进行加密
     * 3. 将加密后的内容写入响应流
     *
     * @throws Exception 如果在加密或写入过程中发生错误，则抛出异常
     */

    public Object encryptAndWrite() {
        try {
            byte[] originalBytes = getByteArray();
            // 将原始字节数组转换为字符串，使用响应的字符编码
            String originalContent = new String(originalBytes, response.getCharacterEncoding());
            // 使用加密器对原始内容进行加密
            String encryptedContent = encryptor.encrypt(encryptorProperties.getAlgorithm(), encryptorProperties.getPublicKey(),
                    encryptorProperties.getIv(), encryptorProperties.getSalt(), originalContent);
            response.getOutputStream().write(encryptedContent.getBytes(response.getCharacterEncoding()));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

}
