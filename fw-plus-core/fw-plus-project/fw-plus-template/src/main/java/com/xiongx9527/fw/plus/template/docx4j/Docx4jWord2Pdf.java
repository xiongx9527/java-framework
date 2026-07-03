package com.xiongx9527.fw.plus.template.docx4j;

import com.xiongx9527.fw.plus.template.filechange.AbstractWord2Pdf;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.fonts.PhysicalFont;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.convert.out.FOSettings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;


@Slf4j
public class Docx4jWord2Pdf extends AbstractWord2Pdf {

    /**
     * 加载系统字体
     */
    private void loadSystemFonts(IdentityPlusMapper fontMapper) {
        if (fontMapper == null || fontMapper.getFontMappings() == null) {
            System.err.println("✗ fontMapper 或其映射表为空");
            return;
        }

        String osName = System.getProperty("os.name").toLowerCase();
        OsType osType = getOsType(osName);

        List<FontInfo> fontsToTry = getFontListForOs(osType);

        for (FontInfo fontInfo : fontsToTry) {
            String fontName = fontInfo.getFontName();
            String displayName = fontInfo.getDisplayName();
            String fontPath = fontInfo.getFontPath();

            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    PhysicalFont font = PhysicalFonts.get(fontPath);
                    fontMapper.getFontMappings().put(fontName, font);
                    log.info("✓ 加载字体: " + displayName);
                } catch (Exception e) {
                    log.warn("✗ 加载字体失败: " + displayName, e);
                }
            }
        }
    }

    // 枚举表示操作系统类型
    enum OsType {
        WINDOWS, MAC, LINUX
    }

    // 获取操作系统类型
    private OsType getOsType(String osName) {
        if (osName.contains("windows")) {
            return OsType.WINDOWS;
        } else if (osName.contains("mac")) {
            return OsType.MAC;
        } else {
            return OsType.LINUX;
        }
    }

    /**
     * 字体信息类，用于封装字体的相关信息
     */
    public class FontInfo {
        /**
         * 字体名称
         */
        private final String fontName;

        /**
         * 显示名称
         */
        private final String displayName;

        /**
         * 字体文件路径
         */
        private final String fontPath;

        /**
         * 构造函数，初始化字体信息对象
         *
         * @param fontName 字体名称
         * @param displayName 显示名称
         * @param fontPath 字体文件路径
         */
        public FontInfo(String fontName, String displayName, String fontPath) {
            this.fontName = fontName;
            this.displayName = displayName;
            this.fontPath = fontPath;
        }

        /**
         * 获取字体名称
         *
         * @return 字体名称
         */
        public String getFontName() { return fontName; }

        /**
         * 获取显示名称
         *
         * @return 显示名称
         */
        public String getDisplayName() { return displayName; }

        /**
         * 获取字体文件路径
         *
         * @return 字体文件路径
         */
        public String getFontPath() { return fontPath; }
    }

    // 根据操作系统获取字体列表
    private List<FontInfo> getFontListForOs(OsType osType) {
        switch (osType) {
            case WINDOWS:
                return Arrays.asList(
                        new FontInfo("SimSun", "宋体", "C:/Windows/Fonts/simsun.ttc"),
                        new FontInfo("SimHei", "黑体", "C:/Windows/Fonts/simhei.ttf"),
                        new FontInfo("Microsoft YaHei", "微软雅黑", "C:/Windows/Fonts/msyh.ttc"),
                        new FontInfo("KaiTi", "楷体", "C:/Windows/Fonts/simkai.ttf"),
                        new FontInfo("FangSong", "仿宋", "C:/Windows/Fonts/simfang.ttf"),
                        new FontInfo("Arial", "Arial", "C:/Windows/Fonts/arial.ttf"),
                        new FontInfo("Times New Roman", "Times New Roman", "C:/Windows/Fonts/times.ttf")
                );
            case MAC:
                return Arrays.asList(
                        new FontInfo("SimSun", "宋体", "/System/Library/Fonts/Songti.ttc"),
                        new FontInfo("SimHei", "黑体", "/System/Library/Fonts/Heiti.ttc"),
                        new FontInfo("Microsoft YaHei", "微软雅黑", "/System/Library/Fonts/PingFang.ttc"),
                        new FontInfo("KaiTi", "楷体", "/System/Library/Fonts/Kaiti.ttc"),
                        new FontInfo("Arial", "Arial", "/System/Library/Fonts/Arial.ttf"),
                        new FontInfo("Times New Roman", "Times New Roman", "/System/Library/Fonts/Times.ttc")
                );
            case LINUX:
            default:
                return Arrays.asList(
                        new FontInfo("SimSun", "宋体", "/usr/share/fonts/simsun.ttc"),
                        new FontInfo("SimHei", "黑体", "/usr/share/fonts/simhei.ttf"),
                        new FontInfo("Microsoft YaHei", "微软雅黑", "/usr/share/fonts/msyh.ttc"),
                        new FontInfo("KaiTi", "楷体", "/usr/share/fonts/simkai.ttf"),
                        new FontInfo("FangSong", "仿宋", "/usr/share/fonts/simfang.ttf"),
                        new FontInfo("Arial", "Arial", "/usr/share/fonts/truetype/freefont/FreeSans.ttf"),
                        new FontInfo("Times New Roman", "Times New Roman", "/usr/share/fonts/truetype/freefont/FreeSerif.ttf")
                );
        }
    }

    /**
     * 配置Word文档字体设置，确保PDF转换时字体正确显示
     *
     * @param wordPackage WordprocessingMLPackage对象，不能为空
     * @throws Exception 字体配置过程中可能抛出的异常
     */
    private void configureFonts(WordprocessingMLPackage wordPackage) throws Exception {
        if (wordPackage == null) {
            throw new IllegalArgumentException("wordPackage cannot be null");
        }
        // 获取字体映射器
        IdentityPlusMapper fontMapper = new IdentityPlusMapper();
        loadSystemFonts(fontMapper);
        // 设置字体映射器
        wordPackage.setFontMapper(fontMapper);
    }


    /**
     * 将Word文档转换为PDF格式
     *
     * @param srcData 原始Word文档数据字节数组，不能为空
     * @return 转换后的PDF文档数据字节数组
     * @throws Exception 转换过程中可能抛出的异常
     */
    @Override
    public byte[] change(byte[] srcData) throws Exception {
        // 输入验证
        if (srcData == null) {
            throw new IllegalArgumentException("srcData cannot be null");
        }

        // 1. 加载 Word 文档
        WordprocessingMLPackage wordPackage = Docx4J.load(new ByteArrayInputStream(srcData));
        // 2. 配置字体（关键步骤！）
        configureFonts(wordPackage);
        // 3. 创建 PDF 设置
        FOSettings foSettings = Docx4J.createFOSettings();
        foSettings.setWmlPackage(wordPackage);
        foSettings.setApacheFopMime("application/pdf");

        // 4. 执行转换
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Docx4J.toFO(foSettings, outputStream, Docx4J.FLAG_EXPORT_PREFER_XSL);
            return outputStream.toByteArray();
        }
    }

}
