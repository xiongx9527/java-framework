/**
 * Copyright 2013-2015 JueYue (qrb.jueyue@gmail.com)
 *   
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.afterturn.easypoi.view;

import java.util.Map;

import cn.afterturn.easypoi.util.WebFilenameUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Controller;

import cn.afterturn.easypoi.entity.vo.TemplateWordConstants;
import cn.afterturn.easypoi.word.WordExportUtil;

/**
 * Word模板视图
 * 
 * @author JueYue
 *  2014年6月30日 下午9:15:49
 */
@SuppressWarnings("unchecked")
@Controller(TemplateWordConstants.EASYPOI_TEMPLATE_WORD_VIEW)
public class EasypoiTemplateWordView extends PoiBaseView {

    /**
     * 定义Microsoft Word文档的内容类型常量
     * 该常量用于标识HTTP响应或请求中的内容格式为Microsoft Word文档
     */
    private static final String CONTENT_TYPE = "application/msword";


    /**
     * 构造函数
     *
     * 初始化EasypoiTemplateWordView实例，设置内容类型为Microsoft Word文档格式
     */
    public EasypoiTemplateWordView() {
        setContentType(CONTENT_TYPE);
    }


    /**
     * 渲染合并输出模型，生成Word文档并输出到响应流
     *
     * @param model 包含模板数据的模型Map，应包含URL、MAP_DATA等键值对
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        String codedFileName = "临时文件.docx";
        if (model.containsKey(TemplateWordConstants.FILE_NAME)) {
            codedFileName = (String) model.get(TemplateWordConstants.FILE_NAME) + ".docx";
        }
        // 设置符合RFC 5987标准的文件名响应头
        response.setHeader("content-disposition", WebFilenameUtils.disposition(codedFileName));
        // 导出Word文档
        XWPFDocument document = WordExportUtil.exportWord07(
            (String) model.get(TemplateWordConstants.URL),
            (Map<String, Object>) model.get(TemplateWordConstants.MAP_DATA));
        // 将文档写入响应输出流
        ServletOutputStream out = response.getOutputStream();
        document.write(out);
        out.flush();
    }

}
