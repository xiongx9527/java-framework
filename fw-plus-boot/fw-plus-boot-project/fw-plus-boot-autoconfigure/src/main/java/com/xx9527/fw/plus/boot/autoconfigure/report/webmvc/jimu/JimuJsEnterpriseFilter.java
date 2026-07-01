package com.xx9527.fw.plus.boot.autoconfigure.report.webmvc.jimu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@WebFilter(urlPatterns = {
        "/jmreport/*",
        "/drag/lib/js/*",
        "/jmreport/desreport_/js/*",
        "/jmreport/desreport_/corelib/*"
})
public class JimuJsEnterpriseFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(JimuJsEnterpriseFilter.class);

    private static final String ENTERPRISE_CLASSPATH = "/jimu/enterprise";

    private static final Map<byte[], byte[]> REPLACEMENTS = new LinkedHashMap<>();

    static {
        // 关闭 isLimitUse 标志：让报表设计器的 barcode/qrcode/高级功能可用
        add("isLimitUse=!0", "isLimitUse=!1");
        // 关闭 drag BI 设计器的 isLimit 标志（VUE_APP_isLimit + isLimit:JSON.parse）
        add("VUE_APP_isLimit:\"true\"", "VUE_APP_isLimit:\"false\"");
        // 关闭所有 JSON.parse("true") 硬编码限制（按钮禁用+点击拦截）
        add("JSON.parse(\"true\")", "JSON.parse(\"false\")");
        // 直接禁用所有 isLimitUse 读检查，防止 Vi 下拉面板将 barcode/qrcode
        // 渲染为 disabled + 升级提示，而不是正常的操作 handler
        add("x.spreadsheet.isLimitUse&&", "!1&&");
        add("&&x.spreadsheet.isLimitUse", "&&!1");
        // 修复 Yc dispatch：barcode/qrcode 调用 addBarcode/addQrcode 方法
        add("else if(\"barcode\"===t);else if(\"qrcode\"===t);",
            "else if(\"barcode\"===t)this.addBarcode();else if(\"qrcode\"===t)\"barcode\"===e?this.addBarcode():this.addQrcode();");
        // 注入 addBarcode 方法体：使用免费版的 Oo (barcode layer) + eu (注册) + au (聚焦)
        add("{key:\"addBarcode\",value:function(){}}",
            "{key:\"addBarcode\",value:function(){var t=this.selector.cellRect,e=t.left,n=t.top,i=t.ri,r=t.ci,a=new Oo({top:n,left:e}).initRCPosition(i,r);a.focusLayer(),eu.call(this,a,!1),au.call(this,a.id),a.resetLayerSize()}}");
        // 注入 addQrcode 方法体：使用免费版的 Mo (qrcode layer) + eu (注册) + au (聚焦)
        add("{key:\"addQrcode\",value:function(){}}",
            "{key:\"addQrcode\",value:function(){var t=this.selector.cellRect,e=t.left,n=t.top,i=t.ri,r=t.ci,a=new Mo({top:n,left:e}).initRCPosition(i,r);a.focusLayer(),eu.call(this,a,!1),au.call(this,a.id),a.resetLayerSize()}}");
        // 修复 view-setting/view 按钮：替换 Yc dispatch 中的空语句为 onSettingEvent 调用
        add("else if(\"view-setting\"===t);else if(\"view\"===t);else if(\"redo\"===t)",
            "else if(\"v\"===t[0])this.onSettingEvent(t);else if(\"redo\"===t)      ");
        // 清除URL-encoded 升级提示
        add("decodeURIComponent(\"%E6%AD%A4%E5%8A%9F%E8%83%BD%EF%BC%8C%E9%9C%80%E8%A6%81%E5%8D%87%E7%BA%A7%E4%BC%81%E4%B8%9A%E7%89%88%E6%9C%AC%EF%BC%81\")", "decodeURIComponent(\"\")");
        add("decodeURIComponent(\"%E6%AD%A4%E5%8A%9F%E8%83%BD%EF%BC%8C%E9%9C%80%E8%A6%81%E5%8D%87%E7%BA%A7%E4%BC%81%E4%B8%9A%E7%89%88%E6%9C%AC\")", "decodeURIComponent(\"\")");
    }

    private static void add(String from, String to) {
        REPLACEMENTS.put(from.getBytes(), to.getBytes());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String uri = ((HttpServletRequest) request).getRequestURI();
        boolean isJs = uri.endsWith(".js");
        boolean isDrag = uri.contains("/drag/lib/");

        byte[] content;

        if (isDrag) {
            content = loadEnterpriseResource(uri);
            if (content == null) {
                content = getLocalContent(request, response, chain);
            }
        } else {
            content = getLocalContent(request, response, chain);
        }

        if (content == null || content.length == 0) {
            return;
        }

        if (content.length > 5 * 1024 * 1024) {
            response.setContentLength(content.length);
            response.getOutputStream().write(content);
            return;
        }

        int count = 0;
        for (Map.Entry<byte[], byte[]> entry : REPLACEMENTS.entrySet()) {
            byte[] from = entry.getKey();
            byte[] to = entry.getValue();
            byte[] replaced = replace(content, from, to);
            if (!Arrays.equals(replaced, content)) {
                int occurrences = (content.length - replaced.length) / Math.max(1, from.length - to.length);
                if (occurrences == 0 && from.length == to.length) {
                    occurrences = countOccurrences(content, from);
                }
                count += occurrences;
                content = replaced;
            }
        }

        if (isJs) {
            content = appendPatch(content);
        }

        log.warn("JS filter [{}]: {} bytes, {} replacement(s), enterprise: {}", uri, content.length, count, isDrag);

        response.setContentLength(content.length);
        response.getOutputStream().write(content);
    }

    private static byte[] loadEnterpriseResource(String uri) {
        String path = ENTERPRISE_CLASSPATH + uri;
        try (InputStream is = JimuJsEnterpriseFilter.class.getResourceAsStream(path)) {
            if (is == null) {
                log.debug("Enterprise resource not found: {}", path);
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            byte[] data = out.toByteArray();
            log.info("Enterprise resource [{}]: {} bytes", uri, data.length);
            return data;
        } catch (IOException e) {
            log.warn("Failed to load enterprise resource [{}]: {}", uri, e.getMessage());
            return null;
        }
    }

    private static byte[] getLocalContent(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ByteResponseWrapper wrapper = new ByteResponseWrapper((HttpServletResponse) response);
        chain.doFilter(request, wrapper);
        return wrapper.getBytes();
    }

    private static class ByteResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        ByteResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
                @Override
                public void write(int b) { output.write(b); }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setWriteListener(WriteListener listener) {}
            };
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(output);
        }

        byte[] getBytes() {
            return output.toByteArray();
        }
    }

    private static byte[] appendPatch(byte[] content) {
        String patch = ";(function(){" +
                "function q(i){" +
                "var t=i.toolbar;if(!t||t.__p)return;t.__p=1;" +
                "var c=t.change;" +
                "t.change=function(e,n){" +
                "if(e==='view-setting'||e==='view'){i.onSettingEvent(e);return}" +
                "return c.apply(this,arguments)}" +
                "}" +
                "var id=setInterval(function(){" +
                "var x=window.x;if(!x)return;" +
                "if(x.sheet)q(x.sheet);" +
                "var s=x.spreadsheet;" +
                "if(s&&!s.__h){" +
                "s.__h=1;" +
                "x.spreadsheet=function(){" +
                "var i=s.apply(this,arguments);" +
                "setTimeout(function(){q(i)},100);" +
                "return i};" +
                "for(var k in s)x.spreadsheet[k]=s[k]" +
                "}" +
                "if(x.sheet&&x.sheet.toolbar&&x.sheet.toolbar.__p)clearInterval(id)" +
                "},50);" +
                "})();";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(content);
            out.write(patch.getBytes());
        } catch (IOException e) {
            log.error("Failed to append patch", e);
            return content;
        }
        return out.toByteArray();
    }

    private static int countOccurrences(byte[] data, byte[] pattern) {
        int count = 0;
        int fromLen = pattern.length;
        for (int i = 0; i <= data.length - fromLen; i++) {
            boolean match = true;
            for (int j = 0; j < fromLen; j++) {
                if (data[i + j] != pattern[j]) { match = false; break; }
            }
            if (match) { count++; i += fromLen - 1; }
        }
        return count;
    }

    private static byte[] replace(byte[] data, byte[] from, byte[] to) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int dataLen = data.length;
        int fromLen = from.length;
        int i = 0;
        while (i < dataLen) {
            boolean match = dataLen - i >= fromLen;
            for (int j = 0; j < fromLen && match; j++) {
                if (data[i + j] != from[j]) { match = false; break; }
            }
            if (match) {
                result.write(to, 0, to.length);
                i += fromLen;
            } else {
                result.write(data[i]);
                i++;
            }
        }
        return result.toByteArray();
    }
}
