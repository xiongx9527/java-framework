package cn.afterturn.easypoi.word.parse;

import cn.afterturn.easypoi.cache.WordCache;
import cn.afterturn.easypoi.entity.ImageEntity;
import cn.afterturn.easypoi.util.PoiPublicUtil;
import cn.afterturn.easypoi.word.entity.MyXWPFDocument;
import cn.afterturn.easypoi.word.entity.params.ExcelListEntity;
import cn.afterturn.easypoi.word.parse.excel.ExcelEntityParse;
import cn.afterturn.easypoi.word.parse.excel.ExcelMapParse;
import cn.hutool.http.HttpUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.impl.CTDocument1Impl;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static cn.afterturn.easypoi.util.PoiElUtil.*;

/**
 * 解析07版的Word,替换文字,生成表格,生成图片
 *
 * @author JueYue
 * 2013-11-16
 * @version 1.0
 */
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public class ParseWord07 {
    /**
     * 根据条件改变段落中的文本值
     *
     * @param paragraph  当前处理的段落对象
     * @param currentRun 当前处理的文本运行对象
     * @param currentText 当前文本内容
     * @param runIndex   需要清空的文本运行索引列表
     * @param map        包含替换数据的Map
     * @throws Exception 处理过程中可能抛出的异常
     * @author JueYue
     * 2013-11-16
     */
    private void changeValues(XWPFParagraph paragraph, XWPFRun currentRun, String currentText,
                              List<Integer> runIndex, Map<String, Object> map) throws Exception {
        // 判断是不是迭代输出
        if (currentText.contains(FOREACH) && currentText.startsWith(START_STR)) {
            currentText = currentText.replace(FOREACH, EMPTY).replace(START_STR, EMPTY).replace(END_STR, EMPTY);
            String[] keys   = currentText.replaceAll("\\s{1,}", " ").trim().split(" ");
            List   list = (List)PoiPublicUtil.getParamsValue(keys[0], map);
            list.forEach(obj -> {
                if (obj instanceof ImageEntity) {
                    currentRun.setText("", 0);
                    ExcelMapParse.addAnImage((ImageEntity) obj, currentRun);
                } else {
                    try {
                        boolean isImage = false ;
                        ImageEntity imageEntity = null;
                        if (obj instanceof String) {
                            isImage = ImageUrlChecker.isImageUrl((String)obj);
                        }
                        if (isImage) {
                            currentRun.setText("", 0);
                            imageEntity = parseImageSizeFromUrl((String)obj);
                        }
                        if (imageEntity != null) {
                            ExcelMapParse.addAnImage(imageEntity, currentRun);
                        } else {
                            PoiPublicUtil.setWordText(currentRun, obj.toString());
                        }
                    } catch (Exception e) {
                        log.warn("图片地址错误:{}", obj);
                        PoiPublicUtil.setWordText(currentRun, obj.toString());
                    }
                    //PoiPublicUtil.setWordText(currentRun, obj.toString());
                }
            });
        } else {
            Object obj = PoiPublicUtil.getRealValue(currentText, map);
            // 如果是图片就设置为图片
            if (obj instanceof ImageEntity) {
                currentRun.setText("", 0);
                ExcelMapParse.addAnImage((ImageEntity) obj, currentRun);
            } else {
                try {
                    boolean isImage = false ;
                    ImageEntity imageEntity = null;
                    if (obj instanceof String) {
                        isImage = ImageUrlChecker.isImageUrl((String)obj);
                    }
                    if (isImage) {
                        currentRun.setText("", 0);
                        imageEntity = parseImageSizeFromUrl((String)obj);
                    }
                    if (imageEntity != null) {
                        ExcelMapParse.addAnImage(imageEntity, currentRun);
                    } else {
                        currentText = obj.toString();
                        PoiPublicUtil.setWordText(currentRun, currentText);
                    }
                } catch (Exception e) {
                    log.warn("图片地址错误:{}", obj);
                    currentText = obj.toString();
                    PoiPublicUtil.setWordText(currentRun, currentText);
                }
            }
        }

        for (int k = 0; k < runIndex.size(); k++) {
            paragraph.getRuns().get(runIndex.get(k)).setText("", 0);
        }
        runIndex.clear();
    }


    /**
     * 从URL查询参数中提取图片宽度和高度
     *
     * @param url 包含width和height参数的URL字符串
     * @return ImageEntity 包含宽高信息的ImageEntity对象，如果无法解析则返回null
     */
    private ImageEntity parseImageSizeFromUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        byte[] data = ImageDataCache.getImageData(url);
        if (data == null) {
            return null;
        }
        // 图片宽度，默认值为100像素
        int width = 100;
        // 图片高度，默认值为100像素
        int height = 100;
        // 图片位置类型，默认为嵌入式布局
        int locationType = ImageEntity.EMBED;
        // 图片跨行数，默认占1行
        int rowspan = 1;
        // 图片跨列数，默认占1列
        int colspan = 1;
        try {
            URL imageUrl = new URL(url);
            String query = imageUrl.getQuery();
            if (StringUtils.isNotBlank(query)) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        String key = pair.substring(0, idx);
                        String value = pair.substring(idx + 1);
                        if ("w".equalsIgnoreCase(key)) {
                            try {
                                width = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                log.warn("Invalid width value in URL: {}", value);
                            }
                        } else if ("h".equalsIgnoreCase(key)) {
                            try {
                                height = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                log.warn("Invalid height value in URL: {}", value);
                            }
                        } else if ("l".equalsIgnoreCase(key)) {
                            try {
                                locationType = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                log.warn("Invalid locationType value in URL: {}", value);
                            }
                        } else if ("r".equalsIgnoreCase(key)) {
                            try {
                                rowspan = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                log.warn("Invalid rowspan value in URL: {}", value);
                            }
                        } else if ("c".equalsIgnoreCase(key)) {
                            try {
                                colspan = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                log.warn("Invalid colspan value in URL: {}", value);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse image size from URL: {}", url, e);
        }
        ImageEntity imageEntity = new ImageEntity(data, width, height);
        imageEntity.setType(ImageEntity.Data);
        imageEntity.setLocationType(locationType);
        imageEntity.setRowspan(rowspan);
        imageEntity.setColspan(colspan);
        return imageEntity;
    }

    /**
     * 判断是不是迭代输出
     *
     * @return
     * @throws Exception
     * @author JueYue
     * 2013-11-18
     */
    private Object checkThisTableIsNeedIterator(XWPFTableCell cell,
                                                Map<String, Object> map) throws Exception {
        String text = cell.getText().trim();
        // 判断是不是迭代输出
        if (text != null && text.contains(FOREACH) && text.startsWith(START_STR)) {
            text = text.replace(FOREACH_NOT_CREATE, EMPTY).replace(FOREACH_AND_SHIFT, EMPTY)
                    .replace(FOREACH, EMPTY).replace(START_STR, EMPTY);
            String[] keys   = text.replaceAll("\\s{1,}", " ").trim().split(" ");
            Object   result = PoiPublicUtil.getParamsValue(keys[0], map);
            //添加list默认值，避免将{{$fe: list t.sn	t.hoby	t.remark}} 这类标签直接显示出来
            return Objects.nonNull(result) ? result : new ArrayList<Map<String, Object>>(0);
        }
        return null;
    }

    /**
     * 解析所有的文本
     *
     * @param paragraphs
     * @param map
     * @author JueYue
     * 2013-11-17
     */
    private void parseAllParagraph(List<XWPFParagraph> paragraphs,
                                   Map<String, Object> map) throws Exception {
        XWPFParagraph paragraph;
        for (int i = 0; i < paragraphs.size(); i++) {
            paragraph = paragraphs.get(i);
            if (paragraph.getText().indexOf(START_STR) != -1) {
                parseThisParagraph(paragraph, map);
            }
        }

    }

    /**
     * 解析这个段落
     *
     * @param paragraph
     * @param map
     * @author JueYue
     * 2013-11-16
     */
    private void parseThisParagraph(XWPFParagraph paragraph, Map<String, Object> map) throws Exception {
        XWPFRun run;
        // 拿到的第一个run,用来set值,可以保存格式
        XWPFRun currentRun = null;
        // 存放当前的text
        String currentText = "";
        String text;
        // 判断是不是已经遇到{{
        Boolean isfinde = false;
        // 存储遇到的run,把他们置空
        List<Integer> runIndex = new ArrayList<>();
        for (int i = 0; i < paragraph.getRuns().size(); i++) {
            run = paragraph.getRuns().get(i);
            text = run.getText(0);
            if (StringUtils.isEmpty(text)) {
                continue;
            } // 如果为空或者""这种这继续循环跳过
            if (isfinde) {
                currentText += text;
                if (currentText.indexOf(START_STR) == -1) {
                    isfinde = false;
                    runIndex.clear();
                } else {
                    runIndex.add(i);
                }
                if (currentText.indexOf(END_STR) != -1) {
                    changeValues(paragraph, currentRun, currentText, runIndex, map);
                    currentText = "";
                    isfinde = false;
                }
                // 判断是不是开始
            } else if (text.indexOf(START_STR) >= 0) {
                currentText = text;
                isfinde = true;
                currentRun = run;
            } else {
                currentText = "";
            }
            if (currentText.indexOf(END_STR) != -1) {
                changeValues(paragraph, currentRun, currentText, runIndex, map);
                isfinde = false;
            }
        }

    }

    /**
     * 解析表格中的一行，处理行内所有单元格的段落
     *
     * @param cells 表格行中的单元格列表
     * @param map   包含替换数据的Map
     * @throws Exception 解析过程中可能抛出的异常
     */
    private void parseThisRow(List<XWPFTableCell> cells, Map<String, Object> map) throws Exception {
        for (XWPFTableCell cell : cells) {
            parseAllParagraph(cell.getParagraphs(), map);
        }
    }


    /**
     * 解析这个表格
     *
     * @param table
     * @param map
     * @author JueYue
     * 2013-11-17
     */
    private void parseThisTable(XWPFTable table, Map<String, Object> map) throws Exception {
        XWPFTableRow        row;
        List<XWPFTableCell> cells;
        Object              listobj;
        for (int i = 0; i < table.getNumberOfRows(); i++) {
            row = table.getRow(i);
            cells = row.getTableCells();
            listobj = checkThisTableIsNeedIterator(cells.get(0), map);
            if (listobj == null) {
                parseThisRow(cells, map);
            } else if (listobj instanceof ExcelListEntity) {
                new ExcelEntityParse().parseNextRowAndAddRow(table, i, (ExcelListEntity) listobj);
                //删除之后要往上挪一行,然后加上跳过新建的行数
                i = i + ((ExcelListEntity) listobj).getList().size() - 1;
            } else if (listobj instanceof List) {
                ExcelMapParse.parseNextRowAndAddRow(table, i, (List) listobj);
                //删除之后要往上挪一行,然后加上跳过新建的行数
                i = i + ((List) listobj).size() - 1;
            } else {
                ExcelMapParse.parseNextRowAndAddRow(table, i, new ArrayList<>());
                //删除之后要往上挪一行,然后加上跳过新建的行数
                i = i - 1;
            }
        }
    }

    /**
     * 解析07版的Word并且进行赋值
     *
     * @return
     * @throws Exception
     * @author JueYue
     * 2013-11-16
     */
    public XWPFDocument parseWord(String url, Map<String, Object> map) throws Exception {
        MyXWPFDocument doc = WordCache.getXWPFDocument(url);
        parseWordSetValue(doc, map);
        return doc;
    }

    /**
     * 解析07版的Work并且进行赋值但是进行多页拼接
     *
     * @param url
     * @param list
     * @return
     */
    public XWPFDocument parseWord(String url, List<Map<String, Object>> list) throws Exception {
        if (list == null || list.size() == 0) {
            return null;
        } else if (list.size() == 1) {
            return parseWord(url, list.get(0));
        } else {
            MyXWPFDocument doc = WordCache.getXWPFDocument(url);
            parseWordSetValue(doc, list.get(0));
            //插入分页(推荐这种分页方式，每页上方不会产生留白)
            doc.createParagraph().createRun().addBreak(BreakType.PAGE);
            for (int i = 1; i < list.size(); i++) {
                MyXWPFDocument tempDoc = WordCache.getXWPFDocument(url);
                parseWordSetValue(tempDoc, list.get(i));
                //最后一页不插入分页
                if (i < list.size() - 1) {
                    tempDoc.createParagraph().createRun().addBreak(BreakType.PAGE);
                }
                doc.getDocument().addNewBody().set(tempDoc.getDocument().getBody());

            }
            return doc;
        }

    }

    /**
     * 解析07版的Word并且进行赋值
     *
     * @throws Exception
     */
    public void parseWord(XWPFDocument document, Map<String, Object> map) throws Exception {
        parseWordSetValue((MyXWPFDocument) document, map);
    }

    /**
     * 解析Word文档并设置值
     *
     * @param doc Word文档对象
     * @param map 包含替换数据的Map
     * @throws Exception 解析过程中可能抛出的异常
     */
    private void parseWordSetValue(MyXWPFDocument doc, Map<String, Object> map) throws Exception {
        // 第一步解析文档
        parseAllParagraph(doc.getParagraphs(), map);
        // 第二步解析页眉,页脚
        parseHeaderAndFoot(doc, map);
        // 第三步解析所有表格
        XWPFTable           table;
        Iterator<XWPFTable> itTable = doc.getTablesIterator();
        while (itTable.hasNext()) {
            table = itTable.next();
            if (table.getText().indexOf(START_STR) != -1) {
                parseThisTable(table, map);
            }
        }
        // 第四步解析所有文本框
        parseTextBoxes(doc, map);
    }

    /**
     * 解析页眉和页脚
     *
     * @param doc
     * @param map
     * @throws Exception
     */
    private void parseHeaderAndFoot(MyXWPFDocument doc, Map<String, Object> map) throws Exception {
        List<XWPFHeader> headerList = doc.getHeaderList();
        for (XWPFHeader xwpfHeader : headerList) {
            for (int i = 0; i < xwpfHeader.getListParagraph().size(); i++) {
                parseThisParagraph(xwpfHeader.getListParagraph().get(i), map);
            }
        }
        List<XWPFFooter> footerList = doc.getFooterList();
        for (XWPFFooter xwpfFooter : footerList) {
            for (int i = 0; i < xwpfFooter.getListParagraph().size(); i++) {
                parseThisParagraph(xwpfFooter.getListParagraph().get(i), map);
            }
        }
    }

    /**
     * 处理文本框内容的实用方法 - 通过重新构建文档实现
     */
    private void parseTextBoxes(MyXWPFDocument doc, Map<String, Object> map) throws Exception {
        try {
            // 获取文档的底层XML
            org.apache.xmlbeans.XmlObject documentXml = doc.getDocument();
            String xmlText = documentXml.xmlText();

            // 处理文本框中的模板变量
            String processedXml = processTextBoxTemplates(xmlText, map);

            // 如果XML有变化，重新构建文档
            if (!xmlText.equals(processedXml)) {
                CTDocument1Impl doc1 = (CTDocument1Impl) documentXml;
                doc1.set(CTDocument1.Factory.parse(processedXml));
                //documentXml.set(org.apache.xmlbeans.XmlObject.Factory.parse(processedXml));
            }

        } catch (Exception e) {
            log.warn("Error processing text boxes with map size: " + (map != null ? map.size() : 0), e);
        }
    }

    /**
     * 处理文本框XML中的模板变量
     */
    private String processTextBoxTemplates(String xml, Map<String, Object> map) {
        StringBuilder result = new StringBuilder(xml);

        try {
            // 查找所有文本框内容区域
            int startIndex = 0;
            while ((startIndex = result.indexOf("<w:txbxContent", startIndex)) != -1) {
                int endIndex = result.indexOf("</w:txbxContent>", startIndex);
                if (endIndex == -1) break;

                endIndex += "</w:txbxContent>".length();
                String txbxContent = result.substring(startIndex, endIndex);

                // 处理文本框内容中的模板
                String processedContent = processTemplateInXml(txbxContent, map);

                // 替换处理后的内容
                result.replace(startIndex, endIndex, processedContent);
                startIndex = startIndex + processedContent.length();
            }
        } catch (Exception e) {
            log.warn("Error processing text box templates", e);
        }

        return result.toString();
    }

    /**
     * 在XML片段中处理模板变量
     */
    private String processTemplateInXml(String xmlFragment, Map<String, Object> map) {
        String result = xmlFragment;

        try {
            // 查找所有文本节点 (w:t)
            int startIndex = 0;
            while ((startIndex = result.indexOf("<w:t", startIndex)) != -1) {
                int contentStart = result.indexOf(">", startIndex) + 1;
                int contentEnd = result.indexOf("</w:t>", contentStart);

                if (contentStart > 0 && contentEnd > contentStart) {
                    String textContent = result.substring(contentStart, contentEnd);
                    String processedText = processTextContent(textContent, map);

                    if (!textContent.equals(processedText)) {
                        // 替换处理后的文本
                        result = result.substring(0, contentStart) + processedText + result.substring(contentEnd);
                        contentEnd = contentStart + processedText.length();
                    }

                    startIndex = contentEnd + 6; // 移动到下一个位置
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Error processing template in XML", e);
        }

        return result;
    }

    /**
     * 处理文本内容中的模板变量
     */
    private String processTextContent(String text, Map<String, Object> map) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        String currentText = "";
        Boolean isFound = false;

        // 模仿 parseThisParagraph 的处理逻辑
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            currentText += ch;

            if (!isFound) {
                // 判断是不是开始
                if (currentText.indexOf(START_STR) >= 0) {
                    isFound = true;
                } else {
                    // 如果没有找到开始标记，将字符添加到结果中
                    if (currentText.length() > START_STR.length() ||
                            (currentText.length() == 1 && currentText.charAt(0) != START_STR.charAt(0))) {
                        result.append(currentText.charAt(0));
                        currentText = currentText.substring(1);
                    }
                }
            }

            if (isFound && currentText.indexOf(END_STR) != -1) {
                try {
                    // 处理找到的完整表达式
                    String processedValue = changeValues(currentText, map);
                    result.append(processedValue);
                    currentText = "";
                    isFound = false;
                } catch (Exception e) {
                    log.warn("Error processing text content: {}", currentText, e);
                    result.append(currentText);
                    currentText = "";
                    isFound = false;
                }
            }
        }

        // 添加剩余的文本
        result.append(currentText);

        return result.toString();
    }

    /**
     * 向StringBuilder中追加字符串，如果StringBuilder已有内容则先添加逗号分隔符
     * @param sb StringBuilder对象，用于构建最终的字符串结果
     * @param text 要追加的文本内容
     */
    private void appendString(StringBuilder sb, String text) {
        // 检查文本是否非空
        if (StringUtils.isNotEmpty(text)) {
            // 如果StringBuilder中已有内容，先添加逗号和空格作为分隔符
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(text);
        }
    }

    /**
     * 根据给定的文本和参数映射，处理文本中的占位符并替换为实际值。
     * 如果文本中包含迭代标记（如 FOREACH），则会对集合类型的参数进行遍历处理；
     * 否则直接获取对应参数值进行替换。
     * 对于图片实体或图片URL，会特殊处理为"[图片]"字符串。
     *
     * @param currentText 待处理的原始文本，可能包含占位符或迭代标记
     * @param map         参数映射，用于查找和替换文本中的占位符
     * @return 处理后的字符串结果
     * @throws Exception 在处理过程中如果发生异常，则抛出
     */
    private String changeValues(String currentText,Map<String, Object> map) throws Exception {
        StringBuilder sb = new StringBuilder();
        // 判断是不是迭代输出
        if (currentText.contains(FOREACH) && currentText.startsWith(START_STR)) {
            // 去除迭代标记，提取变量名
            currentText = currentText.replace(FOREACH, EMPTY).replace(START_STR, EMPTY).replace(END_STR, EMPTY);
            String[] keys   = currentText.replaceAll("\\s{1,}", " ").trim().split(" ");
            // 获取集合对象并遍历处理每个元素
            List  list = (List)PoiPublicUtil.getParamsValue(keys[0], map);
            list.forEach(obj -> {
                if (obj instanceof ImageEntity) {
                    appendString(sb, "[图片]");
                } else {
                    try {
                        boolean isImage = false ;
                        ImageEntity imageEntity = null;
                        // 检查是否是图片URL
                        if (obj instanceof String) {
                            isImage = ImageUrlChecker.isImageUrl((String)obj);
                        }
                        if (isImage) {
                            imageEntity = new ImageEntity();
                        }
                        // 根据是否是图片决定插入内容
                        if (imageEntity != null) {
                            appendString(sb, "[图片]");
                        } else {
                            appendString(sb, obj.toString());
                        }
                    } catch (Exception e) {
                        log.warn("图片地址错误:{}", obj);
                        appendString(sb, obj.toString());
                    }
                }
            });
        } else {
            // 非迭代情况：直接获取值
            Object obj = PoiPublicUtil.getRealValue(currentText, map);
            // 如果是图片就设置为图片
            if (obj instanceof ImageEntity) {
                appendString(sb, "[图片]");
            } else {
                try {
                    boolean isImage = false ;
                    ImageEntity imageEntity = null;
                    // 检查字符串是否为图片链接
                    if (obj instanceof String) {
                        isImage = ImageUrlChecker.isImageUrl((String)obj);
                    }
                    if (isImage) {
                        imageEntity = new ImageEntity();
                    }
                    // 插入图片标记或实际值
                    if (imageEntity != null) {
                        appendString(sb, "[图片]");
                    } else {
                        appendString(sb, obj.toString());
                    }
                } catch (Exception e) {
                    log.warn("图片地址错误:{}", obj);
                    appendString(sb, obj.toString());
                }
            }
        }

        return sb.toString();
    }

    static class ImageUrlChecker {

        /** URL 图片检查结果缓存，用于避免重复检查相同的 URL，提高性能 */
        private static final Cache<String, Boolean> CACHE;

        /** 缓存最大大小限制，防止内存溢出 */
        private static final int MAX_CACHE_SIZE = 1000;

        static {
            CACHE = CacheBuilder.newBuilder()
                    .maximumSize(MAX_CACHE_SIZE)
                    .expireAfterAccess(1, TimeUnit.HOURS)
                    .build();
        }

        /** 支持的图片文件扩展名集合，用于通过文件扩展名快速识别图片 */
        private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif"
        ));

        /** 支持的图片 Content-Type 集合，用于通过 HTTP 请求检查 Content-Type */
        private static final Set<String> IMAGE_CONTENT_TYPES = new HashSet<>(Arrays.asList(
                "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp",
                "image/svg+xml", "image/x-icon", "image/tiff"
        ));

        /**
         * 判断 URL 是否指向图片资源
         * 采用两级判断策略：首先通过 URL 扩展名快速判断，如果扩展名不明确则通过 HTTP 请求检查 Content-Type
         * 结果会被缓存以提高性能，避免重复检查相同 URL
         * 当缓存满时，会随机移除一个条目，再添加新的条目
         *
         * @param url 需要检查的 URL 字符串
         * @return boolean 如果 URL 指向图片资源则返回 true，否则返回 false
         */
        public static boolean isImageUrl(String url) {
            if (StringUtils.isEmpty(url) ||
                    (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://"))) {
                return false;
            }

            try {
                return CACHE.get(url, () -> {
                    boolean result;
                    if (hasImageExtension(url)) {
                        result = true;
                    } else {
                        result = checkContentType(url);
                    }
                    return result;
                });
            } catch (ExecutionException e) {
                log.warn("Failed to check image URL: {}", url, e);
                return false;
            }
        }

        /**
         * 检查 URL 是否具有图片扩展名
         *
         * @param url 需要检查的 URL 字符串
         * @return boolean 如果 URL 具有图片扩展名则返回 true，否则返回 false
         */
        private static boolean hasImageExtension(String url) {
            if (StringUtils.isEmpty(url)) {
                return false;
            }

            try {
                String path = new URL(url).getPath();
                int lastDot = path.lastIndexOf('.');
                if (lastDot != -1) {
                    String extension = path.substring(lastDot + 1).toLowerCase();
                    return IMAGE_EXTENSIONS.contains(extension);
                }
            } catch (Exception e) {
                log.warn("Failed to parse URL: {}", url, e);
            }
            return false;
        }

        /**
         * 通过检查 URL 资源的 Content-Type 来判断是否为图片
         *
         * @param urlString 需要检查的 URL 字符串
         * @return boolean 如果 URL 资源的 Content-Type 为图片类型则返回 true，否则返回 false
         */
        private static boolean checkContentType(String urlString) {
            if (StringUtils.isEmpty(urlString)) {
                return false;
            }

            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

                // 检查 HTTP 响应状态码，只有 200 才继续处理
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return false;
                }

                // 获取 Content-Type 并检查是否为图片类型
                String contentType = connection.getContentType();
                if (contentType != null) {
                    return IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase());
                }
            } catch (Exception e) {
                log.warn("Error checking content type for URL: {}", urlString, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return false;
        }

    }

    /**
     * 图片数据缓存管理器
     */
    static class ImageDataCache {
        /** 图片数据缓存，避免重复下载相同 URL 的图片 */
        private static final Cache<String, byte[]> CACHE;
        /** 缓存最大大小限制，防止内存溢出 */
        private static final int MAX_CACHE_SIZE = 500;

        static {
            CACHE = CacheBuilder.newBuilder()
                    .maximumSize(MAX_CACHE_SIZE)
                    .expireAfterAccess(1, TimeUnit.HOURS)
                    .build();
        }

        /**
         * 获取图片数据，优先从缓存获取
         *
         * @param url 图片 URL
         * @return 图片字节数组
         */
        public static byte[] getImageData(String url) {
            try {
                return CACHE.get(url, () -> {
                    byte[] data = HttpUtil.downloadBytes(url);
                    if (data == null || data.length == 0) {
                        throw new RuntimeException("Failed to download image");
                    }
                    return data;
                });
            } catch (ExecutionException e) {
                log.warn("Failed to download image from URL: {}", url, e);
                return null;
            }
        }

        /**
         * 清除缓存
         */
        public static void clearCache() {
            CACHE.invalidateAll();
        }

        /**
         * 获取缓存大小
         *
         * @return 缓存条目数量
         */
        public static int getCacheSize() {
            return (int) CACHE.size();
        }
    }

}
