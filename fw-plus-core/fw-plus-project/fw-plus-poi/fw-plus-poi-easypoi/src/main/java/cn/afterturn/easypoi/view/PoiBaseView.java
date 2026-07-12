package cn.afterturn.easypoi.view;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.view.AbstractView;

/**
 * @author EDY
 */
@Slf4j
public abstract class PoiBaseView extends AbstractView {

    /**
     * PoiBaseView构造函数
     *
     * 无参构造函数，用于创建PoiBaseView对象实例
     */
    public PoiBaseView() {
    }

    /**
     * 检查请求是否来自Internet Explorer (IE)浏览器
     * 此方法通过检查HTTP请求的User-Agent头来确定客户端是否使用IE浏览器
     * 它搜索User-Agent头中特定的字符串，以识别不同版本的IE浏览器
     *
     * @param request HTTP请求对象，用于获取User-Agent头
     * @return 如果请求来自IE浏览器，则返回true；否则返回false
     */
    protected static boolean isIE(HttpServletRequest request) {
        // 获取User-Agent头并转换为小写，以实现不区分大小写的比较
        String userAgent = request.getHeader("USER-AGENT").toLowerCase();

        // 检查User-Agent头中是否包含“msie”字符串，这是旧版本IE浏览器的标识
        // 检查是否包含“rv:11.0”字符串，这是IE 11版本的标识
        // 检查是否包含“edge”字符串，这是Edge浏览器的标识，它也是基于IE的
        return userAgent.indexOf("msie") > 0 || userAgent.indexOf("rv:11.0") > 0 || userAgent.indexOf("edge") > 0;
    }

    /**
     * 根据视图名称渲染Excel视图
     *
     * @param model 包含要渲染数据的模型
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param viewName 视图名称，决定使用哪种Excel视图进行渲染
     */
    public static void render(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response, String viewName) {
        // 初始化视图对象，根据不同的视图名称选择不同的视图类
        PoiBaseView view = null;
        if ("easypoiBigExcelView".equals(viewName)) {
            view = new EasypoiBigExcelExportView();
        } else if ("easypoiMapExcelView".equals(viewName)) {
            view = new EasypoiMapExcelView();
        } else if ("easypoiExcelView".equals(viewName)) {
            view = new EasypoiSingleExcelView();
        } else if ("easypoiTemplateExcelView".equals(viewName)) {
            view = new EasypoiTemplateExcelView();
        } else if ("MapGraphExcelView".equals(viewName)) {
            view = new MapGraphExcelView();
        }

        // 尝试渲染视图，若发生异常则记录错误日志
        try {
            view.renderMergedOutputModel(model, request, response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
