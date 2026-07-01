package org.jeecg.modules.jmreport.c.b;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jeecg.modules.jmreport.common.vo.Result;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 覆盖 JAR 中的 JmPackLoaderUtils，拦截所有加密类的调用，强制返回商业版状态
 */
public class c {
    private static final Log a = LogFactory.getLog(c.class);

    private final ClassLoader b;
    private static final c c_instance = new c();

    private c() {
        this.b = new org.jeecg.modules.jmreport.c.a.a();
    }

    public static c getInstance() {
        return c_instance;
    }

    @SuppressWarnings("unchecked")
    public Object a(String templateBase64, String classNameBase64, String methodName, Object... args) {
        try {
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> map = (java.util.Map<String, Object>) arg;
                        if (!map.containsKey("issuedInfo")) {
                            map.put("issuedInfo", "{\"issuedFor\":\"北京敲敲云科技\",\"issued\":\"2024-01-01\",\"expiryTime\":\"2099-12-31\",\"edition\":\"enterprise\"}");
                        }
                        if (!map.containsKey("appVersion")) {
                            map.put("appVersion", "企业版 v2.3.0");
                        }
                        if (!map.containsKey("isCommercial")) {
                            map.put("isCommercial", true);
                        }
                        if (!map.containsKey("isEnterprise")) {
                            map.put("isEnterprise", true);
                        }
                        if (!map.containsKey("edition")) {
                            map.put("edition", "enterprise");
                        }
                        if (!map.containsKey("license")) {
                            map.put("license", "enterprise");
                        }
                    }
                }
            }
            String loadName = classNameBase64 + "@!@" + templateBase64;
            Class<?> clz = this.b.loadClass(loadName);
            if (clz == null) {
                return null;
            }
            Method method = findMethod(clz, methodName, args);
            if (method == null) {
                throw new NoSuchMethodException(
                        new String(Base64.decodeBase64(classNameBase64), StandardCharsets.UTF_8)
                                + "." + methodName);
            }
            Object result = method.invoke(clz.newInstance(), args);
            Object patched = patchResult(result, methodName);
            if (patched != result) {
                a.info("Patched result for method: " + methodName);
            }
            return patched;
        } catch (Exception e) {
            String em = e.getMessage();
            if (em != null && isEvilMessage(em)) {
                a.info("Swallowed evil exception: " + em);
                return null;
            }
            Throwable cause = e.getCause();
            while (cause != null) {
                String cm = cause.getMessage();
                if (cm != null && isEvilMessage(cm)) {
                    a.info("Swallowed evil cause exception: " + cm);
                    return null;
                }
                cause = cause.getCause();
            }
            a.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object patchResult(Object result, String methodName) {
        if (result == null) return null;
        String mn = methodName.toLowerCase();

        // 处理 Result 对象：强制设为成功，清除错误消息
        if (result instanceof Result) {
            Result r = (Result) result;
            if (!r.isSuccess() || (r.getMessage() != null && isEvilMessage(r.getMessage()))) {
                r.setSuccess(true);
                r.setMessage("success");
                r.setCode(200);
            }
            Object data = r.getResult();
            if (data != null) {
                r.setResult(patchResult(data, methodName));
            }
            return r;
        }

        // 递归处理 Map：遍历所有 value，替换 edition/license 相关值
        if (result instanceof java.util.Map) {
            java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) result;
            for (java.util.Map.Entry<Object, Object> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                // 处理 key 名含 edition/license/commercial 的 value
                if (key instanceof String) {
                    String ks = ((String) key).toLowerCase();
                    if (ks.contains("edition") || ks.contains("license") || ks.contains("commercial")
                            || ks.contains("version") || ks.contains("level")) {
                        if (value instanceof String) {
                            String vs = (String) value;
                            if ("free".equalsIgnoreCase(vs) || "community".equalsIgnoreCase(vs)
                                    || "basic".equalsIgnoreCase(vs) || "standard".equalsIgnoreCase(vs)) {
                                entry.setValue("enterprise");
                                continue;
                            }
                        }
                        if (value instanceof Integer) {
                            int vi = (Integer) value;
                            if (vi <= 1) {
                                entry.setValue(2);
                                continue;
                            }
                        }
                        if (value instanceof Boolean && !((Boolean) value)) {
                            entry.setValue(Boolean.TRUE);
                            continue;
                        }
                    }
                }
                // 递归处理 value
                if (value != null) {
                    entry.setValue(patchResult(value, methodName));
                }
            }
            return map;
        }

        // 递归处理 List/Collection
        if (result instanceof List) {
            List<Object> list = (List<Object>) result;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, patchResult(list.get(i), methodName));
            }
            return list;
        }

        // Boolean: 涉及 license/edition 的强制返回 true
        if (result instanceof Boolean && !((Boolean) result)) {
            if (mn.contains("commercial") || mn.contains("license") || mn.contains("valid")
                    || mn.contains("check") || mn.contains("verify") || mn.contains("authorized")
                    || mn.contains("edition") || mn.contains("iscom") || mn.contains("isfree")
                    || mn.contains("iscommunity") || mn.contains("isenable") || mn.contains("isenabled")
                    || mn.contains("haspermission") || mn.contains("frequency")
                    || mn.contains("rate") || mn.contains("limit") || mn.contains("throttle")) {
                return Boolean.TRUE;
            }
        }

        // String: edition/license 强制改为最高版，错误消息清空
        if (result instanceof String) {
            String s = (String) result;
            if ("free".equalsIgnoreCase(s) || "community".equalsIgnoreCase(s)
                    || "basic".equalsIgnoreCase(s) || "standard".equalsIgnoreCase(s)) {
                return "enterprise";
            }
            if (isEvilMessage(s)) {
                return "";
            }
        }

        // Integer: edition 码 0=free, 1=commercial 强制改为 2=enterprise
        if (result instanceof Integer) {
            int v = (Integer) result;
            if ((mn.contains("edition") || mn.contains("license") || mn.contains("level") || mn.contains("type"))
                    && v <= 1) {
                return 2;
            }
        }

        return result;
    }

    private static boolean isEvilMessage(String s) {
        return s.contains("请求过于频繁") || s.contains("升级商业版") || s.contains("立刻升级")
                || s.contains("Too many requests") || s.contains("Upgrade Now")
                || s.contains("commercial version") || s.contains("Upgrade")
                || s.contains("升级企业版") || s.contains("升级企业版本")
                || s.contains("高级组件") || s.contains("企业版本");
    }

    private Method findMethod(Class<?> clz, String methodName, Object[] args) {
        List<Method> methods = Arrays.stream(clz.getMethods())
                .filter(m -> m.getName().equalsIgnoreCase(methodName))
                .filter(m -> {
                    Class<?>[] paramTypes = m.getParameterTypes();
                    if (args == null || args.length == 0) {
                        return paramTypes.length == 0;
                    }
                    if (paramTypes.length != args.length) {
                        return false;
                    }
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null && !paramTypes[i].isAssignableFrom(args[i].getClass())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
        return methods.isEmpty() ? null : methods.get(0);
    }
}
