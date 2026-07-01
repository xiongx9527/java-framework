package com.xx9527.fw.plus.boot.common.web.mvc.filter;

import com.xx9527.fw.plus.boot.common.web.BaseFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.List;

/**
 * @author liufeng
 * @date 2021/8/24 15:22
 */
public abstract class WebMvcBaseRequestFilter extends OncePerRequestFilter implements BaseFilter {
    private final List<String> excludeUrlPatterns;

    public WebMvcBaseRequestFilter() {
        this.excludeUrlPatterns = null;
    }


    public WebMvcBaseRequestFilter(List<String> excludeUrlPatterns) {
        this.excludeUrlPatterns = excludeUrlPatterns;
    }
}
