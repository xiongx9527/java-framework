package com.xiongx9527.fw.plus.template.docx4j;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.docx4j.Docx4J;
import com.xiongx9527.fw.plus.template.filechange.AbstractWord2Html;
import org.docx4j.convert.out.HTMLSettings;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.docx4j.relationships.Relationship;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class Docx4jWord2Html extends AbstractWord2Html {

    /**
     * 图片类型映射表，用于存储文件扩展名与MIME类型的对应关系
     * key: 文件扩展名（不包含点号），如 "png", "jpg"
     * value: 对应的MIME类型，如 "image/png", "image/jpeg"
     */
    private static final Map<String, String> IMAGE_TYPE_MAP = new HashMap<>();

    static {
        // 初始化常见图片格式的MIME类型映射
        IMAGE_TYPE_MAP.put("png", "image/png");
        IMAGE_TYPE_MAP.put("jpeg", "image/jpeg");
        IMAGE_TYPE_MAP.put("jpg", "image/jpeg");
        IMAGE_TYPE_MAP.put("gif", "image/gif");
        IMAGE_TYPE_MAP.put("bmp", "image/bmp");
        IMAGE_TYPE_MAP.put("webp", "image/webp");
        IMAGE_TYPE_MAP.put("tiff", "image/tiff");
        IMAGE_TYPE_MAP.put("tif", "image/tiff");
        IMAGE_TYPE_MAP.put("svg", "image/svg+xml");
        IMAGE_TYPE_MAP.put("ico", "image/x-icon");
    }

    /**
     * 获取图片类型的MIME类型字符串
     *
     * @param imagePart 图片部分对象，用于判断其具体类型
     * @return 返回对应的MIME类型字符串，如果无法识别则返回通用二进制流类型
     */
    private String getImageType(BinaryPartAbstractImage imagePart) {
        if (Objects.isNull(imagePart)) {
            return null;
        }
        // 通过类名判断图片类型
        String name = imagePart.getClass().getSimpleName().toLowerCase();

        // 遍历预定义的图片类型映射表进行匹配
        for (Map.Entry<String, String> entry : IMAGE_TYPE_MAP.entrySet()) {
            if (name.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 默认返回通用二进制流类型
        return "application/octet-stream"; // 返回通用二进制流类型而不是假设为PNG
    }

    /**
     * 将BinaryPartAbstractImage转换为Base64编码的数据URL
     *
     * @param part 图片二进制部分
     * @return Base64编码的数据URL字符串
     */
    private String convertImagePartToBase64(Part part) {
        try {
            BinaryPartAbstractImage imagePart = (BinaryPartAbstractImage) part;
            byte[] imageData = imagePart.getBytes();
            String base64 = Base64.encodeBase64String(imageData);
            String mimeType = getImageType(imagePart);
            return "data:" + mimeType + ";base64," + base64;
        } catch (Exception e) {
            log.warn("Failed to convert image to base64: " + part.getPartName(), e);
            return null;
        }
    }


    /**
     * 从Word文档包中提取所有图片并转换为Base64编码的数据URL
     *
     * @param wordPackage Word文档处理包，包含文档的所有部分
     * @return Map集合，键为图片的关系ID，值为图片的Base64数据URL字符串
     */
    private Map<String, String> extractImages2Base64(WordprocessingMLPackage wordPackage) {
        Map<String, String> images = new HashMap<>();
        // 遍历Word文档中的所有部分，查找图片部分进行处理
        wordPackage.getParts().getParts().values().forEach(part -> {
            if (part instanceof BinaryPartAbstractImage) {
                try {
                    String dataUrl = convertImagePartToBase64(part);
                    // 获取图片关联关系，用于建立图片与文档内容的映射
                    RelationshipsPart relationshipsPart = part.getRelationshipsPart();
                    if (relationshipsPart != null && relationshipsPart.getRelationships() != null) {
                        List<Relationship> relationshipList = relationshipsPart.getRelationships().getRelationship();
                        if (CollectionUtils.isNotEmpty(relationshipList)) {
                            String relationId = relationshipList.get(0).getId();
                            images.put(relationId, dataUrl);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract image: " + part.getPartName(), e);
                }
            }
        });
        return images;
    }

    /**
     * 将传入的 DOCX 格式字节数组转换为 HTML 格式的字节数组。
     * <p>
     * 此方法使用 Docx4J 库加载 Word 文档，并提取其中的图片为 Base64 编码，
     * 然后配置 HTML 转换设置，将文档中的图片引用替换为内联的 Base64 图片数据，
     * 最终输出完整的 HTML 内容。
     * </p>
     *
     * @param srcData 原始 DOCX 文档的字节数据，不能为 null
     * @return 转换后的 HTML 字节数据
     * @throws Exception 如果在转换过程中发生错误（如 IO 异常或 Docx4J 处理异常），则抛出该异常
     */
    @Override
    public byte[] change(byte[] srcData) throws Exception {
        // 使用 try-with-resources 自动关闭输入流
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(srcData)) {
            // 1. 加载 Word 文档
            WordprocessingMLPackage wordPackage = Docx4J.load(inputStream);

            // 2. 提取所有图片为 base64
            Map<String, String> images = extractImages2Base64(wordPackage);

            // 3. 配置 HTML 设置
            HTMLSettings htmlSettings = new HTMLSettings();
            htmlSettings.setWmlPackage(wordPackage);

            // 4. 设置图片处理器（将图片替换为 base64）
            htmlSettings.setImageHandler((picture, relationship, part) -> {
                String base64Image = images.get(relationship.getId());
                if (base64Image == null) {
                    synchronized (images) {
                        base64Image = images.get(relationship.getId());
                        if (base64Image == null) {
                            base64Image = convertImagePartToBase64(part);
                            if (base64Image != null) {
                                images.put(relationship.getId(), base64Image);
                            }
                        }
                    }
                }
                return base64Image;
            });

            // 5. 执行转换并返回结果
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                Docx4J.toHTML(htmlSettings, outputStream, Docx4J.FLAG_EXPORT_PREFER_XSL);
                return outputStream.toByteArray();
            } finally {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    log.error("Failed to close output stream", e);
                    throw e; // 抛出异常以便上层处理
                }
            }
        } catch (Docx4JException | IOException e) {
            throw new Exception("Error processing DOCX to HTML conversion", e);
        }
    }




}
