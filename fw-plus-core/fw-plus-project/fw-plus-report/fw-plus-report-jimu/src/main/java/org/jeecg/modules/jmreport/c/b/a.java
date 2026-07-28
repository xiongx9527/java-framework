package org.jeecg.modules.jmreport.c.b;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 覆盖 JAR 中的 JmPackDecUtils，跳过 d() 中的 license RSA 签名校验，避免 Runtime.halt(0) 杀死 JVM
 */
public class a {
    private static final Log c = LogFactory.getLog(a.class);

    // /META-INF/.templates/
    public static String a = new String(Base64.decodeBase64("L01FVEEtSU5GLy50ZW1wbGF0ZXMv"), StandardCharsets.UTF_8);
    // jmreport.desreport.pass_pem
    public static String b = new String(Base64.decodeBase64("am1yZXBvcnQuZGVzcmVwb3J0LnBhc3NfcGVt"), StandardCharsets.UTF_8);
    // /cert/publicCerts.keystore
    private static String d = new String(Base64.decodeBase64("L2NlcnQvcHVibGljQ2VydHMua2V5c3RvcmU="), StandardCharsets.UTF_8);
    // jmreport.desreport.cer_pem_sig
    private static String e = new String(Base64.decodeBase64("am1yZXBvcnQuZGVzcmVwb3J0LmNlcl9wZW1fc2ln"), StandardCharsets.UTF_8);
    private static long f = 0L;
    private static final int g = 16;

    // 覆盖原版的许可证校验，直接返回不校验，防止 Runtime.halt(0) 杀 JVM
    private static void d(byte[] bArr) {
        // 原版会做 RSA 签名校验 + Runtime.halt(0)，这里直接跳过
    }

    // ========== 以下方法保持原版逻辑，确保解密功能正常 ==========

    public static String a(String templatePath) {
        byte[] passBytes = b(b);
        d(passBytes);
        String relativePath = templatePath.substring(0, templatePath.lastIndexOf("."))
                .replace("/", ".");
        String content = new String(a(relativePath, passBytes), StandardCharsets.UTF_8);
        if (content.isEmpty()) {
            return content;
        }
        String folder = templatePath.substring(0, templatePath.lastIndexOf("/") + 1);
        Pattern p = Pattern.compile("<#include\\s+\"(.*)\"\\s*/*>");
        Matcher m = p.matcher(content);
        while (m.find()) {
            String included = m.group(1);
            if (included != null && !included.isEmpty()) {
                if (included.startsWith("./")) {
                    included = folder + included.substring(2);
                } else if (!included.startsWith("/")) {
                    included = folder + included;
                }
                String incContent = a(included);
                content = m.replaceFirst(Matcher.quoteReplacement(incContent));
                m = p.matcher(content);
            }
        }
        // 解密后注入：强制让模板中的 license/商业版 判断走商业版逻辑
        content = patchTemplate(content);
        return content;
    }

    private static String patchTemplate(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String[] evilMessages = {
            "请求过于频繁，请升级商业版！立刻升级",
            "请求过于频繁，请升级商业版! 立刻升级",
            "请求过于频繁,请升级商业版！立刻升级",
            "请求过于频繁,请升级商业版! 立刻升级",
            "请求过于频繁，请升级商业版",
            "请求过于频繁,请升级商业版",
            "请求过于频繁",
            "升级商业版",
            "立刻升级",
            "此功能，需要升级企业版本",
            "此功能,需要升级企业版本",
            "此功能，需要升级企业版本！",
            "此功能,需要升级企业版本!",
            "需要升级企业版本",
            "高级组件，请升级企业版使用！",
            "高级组件,请升级企业版使用！",
            "高级组件，请升级企业版使用!",
            "高级组件,请升级企业版使用!",
            "请升级企业版",
            "升级企业版本",
            "升级企业版",
            "Too many requests，Please upgrade the commercial version！ Upgrade Now",
            "Too many requests,Please upgrade the commercial version! Upgrade Now",
            "Too many requests, Please upgrade the commercial version! Upgrade Now",
            "Too many requests. Please upgrade the commercial version! Upgrade Now",
            "Too many requests",
            "Please upgrade the commercial version",
            "Upgrade Now",
        };
        int replaced = 0;
        for (String msg : evilMessages) {
            if (content.contains(msg)) {
                content = content.replace(msg, "");
                replaced++;
            }
        }
        if (replaced > 0) {
            c.info("patchTemplate removed " + replaced + " evil message(s)");
        }
        // 强制所有版本标识为企业版
        content = content
                .replace("\"开源版\"", "\"企业版\"")
                .replace("'开源版'", "'企业版'")
                .replace("\"免费版\"", "\"企业版\"")
                .replace("'免费版'", "'企业版'");
        // 干掉所有商业版/许可证相关的条件判断，让系统认为已是企业版
        content = content
                .replaceAll("(?i)<#if\\s+!\\s*(isCommercial|iscommercial|is_commercial|commercial|license|isEnterprise|isenterprise|is_enterprise|enterprise)\\s*>", "<#if false>")
                .replaceAll("(?i)<#if\\s+(isCommercial|iscommercial|is_commercial|commercial|license|isEnterprise|isenterprise|is_enterprise|enterprise)\\s*==\\s*false\\s*>", "<#if false>")
                .replaceAll("(?i)<#if\\s+(isCommercial|iscommercial|is_commercial|commercial|license|isEnterprise|isenterprise|is_enterprise|enterprise)\\s*>", "<#if true>")
                .replaceAll("(?i)<#if\\s+(isCommercial|iscommercial|is_commercial|commercial|license|isEnterprise|isenterprise|is_enterprise|enterprise)\\s*==\\s*true\\s*>", "<#if true>")
                .replaceAll("license\\s*!=\\s*'commercial'", "false")
                .replaceAll("license\\s*!=\\s*\"commercial\"", "false")
                .replaceAll("license\\s*!=\\s*'enterprise'", "false")
                .replaceAll("license\\s*!=\\s*\"enterprise\"", "false")
                .replaceAll("edition\\s*!=\\s*'commercial'", "false")
                .replaceAll("edition\\s*!=\\s*\"commercial\"", "false")
                .replaceAll("edition\\s*==\\s*'free'", "false")
                .replaceAll("edition\\s*==\\s*\"free\"", "false")
                .replaceAll("(?i)license\\.valid\\s*==\\s*false", "false")
                .replaceAll("(?i)!\\s*license\\.valid", "false");
        return content;
    }

    public static byte[] a(String name, byte[] keyBytes) {
        byte[] passKey = b(name);
        if (passKey == null || passKey.length == 0) {
            return "".getBytes();
        }
        try {
            byte[] result = b(passKey, keyBytes);
            // 如果解密结果是有效的 UTF-8 文本，应用模板修补（处理直接调用此方法而不经过 a(String templatePath) 的情况）
            try {
                String text = new String(result, StandardCharsets.UTF_8);
                String patched = patchTemplate(text);
                if (!patched.equals(text)) {
                    c.info("patched template in a(String,byte[]): " + name);
                    return patched.getBytes(StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {
                // 不是文本，跳过
            }
            return result;
        } catch (Exception ex) {
            c.error(ex.getMessage(), ex);
            return "".getBytes();
        }
    }

    public static byte[] b(String name) {
        return a(a, name);
    }

    public static byte[] a(String basePath, String name) {
        byte[] data = null;
        if (name == null) {
            name = "";
        }
        URL url = a.class.getResource(new StringBuilder(basePath).append(name).toString());
        if (url == null) {
            return null;
        }
        String path = Objects.requireNonNull(url).getPath();
        try {
            if (path.contains("jar!")) {
                if (path.trim().startsWith("file:") || path.trim().startsWith("nested:")) {
                    InputStream is = a.class.getResourceAsStream(new StringBuilder(basePath).append(name).toString());
                    if (is != null) {
                        try {
                            data = org.jeecg.modules.jmreport.c.b.b.a(is);
                        } catch (IOException ex) {
                            c.error(ex.getMessage(), ex);
                        }
                    }
                }
            } else {
                data = org.jeecg.modules.jmreport.c.b.b.a(new File(path));
            }
        } catch (Exception ex) {
            c.error(ex.getMessage(), ex);
        }
        return data;
    }

    public static byte[] a(byte[] data, char[] password) {
        if (password == null) {
            throw new IllegalArgumentException("key is null");
        }
        int mod = password.length % 16;
        if (mod > 0) {
            int padding = 16 - mod;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < padding; i++) {
                sb.append("0");
            }
            password = a(new char[][]{password, sb.toString().toCharArray()});
        }
        byte[] result = null;
        try {
            byte[] raw = org.jeecg.modules.jmreport.c.b.b.a(password);
            SecretKeySpec keySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            result = cipher.doFinal(data);
        } catch (Exception ex) {
            // ignore
        }
        return result;
    }

    public static char[] a(char[]... arrays) {
        int totalLen = 0;
        for (char[] arr : arrays) {
            totalLen += arr.length;
        }
        char[] result = new char[totalLen];
        int pos = 0;
        for (char[] arr : arrays) {
            System.arraycopy(arr, 0, result, pos, arr.length);
            pos += arr.length;
        }
        return result;
    }

    public static byte[] a(byte[] data, byte[] keyBytes) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(spec);
        return a(data, keyFactory, privateKey);
    }

    public static byte[] b(byte[] data, byte[] keyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        PublicKey publicKey = keyFactory.generatePublic(spec);
        return a(data, keyFactory, publicKey);
    }

    private static byte[] a(byte[] data, KeyFactory keyFactory, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, key);
        int dataLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        int count = 0;
        while (dataLen - offset > 0) {
            byte[] cache;
            if (dataLen - offset > 128) {
                cache = cipher.doFinal(data, offset, 128);
            } else {
                cache = cipher.doFinal(data, offset, dataLen - offset);
            }
            out.write(cache);
            count++;
            offset = count * 128;
        }
        byte[] result = out.toByteArray();
        out.close();
        return result;
    }

    public static byte[] a(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        return md.digest();
    }

    public static String b(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return null;
        }
        return c(a(data));
    }

    public static String c(byte[] data) {
        return java.util.Base64.getEncoder().encodeToString(data);
    }
}
