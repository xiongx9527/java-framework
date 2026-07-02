package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

import java.util.Date;

@Data
public class VueInfo extends PathMagicEntity {


    /**
     * JavaScript依赖资源字符串
     * 用于存储页面所需的JavaScript文件路径或内容
     */
    private String jsDependences;

    /**
     * CSS依赖资源字符串
     * 用于存储页面所需的CSS文件路径或内容
     */
    private String cssDependences;


    /**
     * 编译后的CSS样式字符串
     * 用于存储经过编译处理的CSS代码内容
     */
    private String compileCss;

    /**
     * 编译后的JavaScript脚本字符串
     * 用于存储经过编译处理的JS代码内容
     */
    private String compileJs;

    /**
     * 描述信息字符串
     * 用于存储相关的描述说明文本
     */
    private String description;

     /**
     * 参数信息字符串
     * 用于存储Vue3组件的参数信息
     */
    private String params;


    /**
     * 历史记录列表，用于存储Vue3组件信息的历史数据
     * 该字段为私有成员变量，只能在当前类内部访问
     */
    private String history;


    /**
     * 创建并返回一个包含Vue信息的实体对象
     *
     * @return MagicEntity Vue信息实体对象
     */
    @Override
    public MagicEntity simple() {
        // 创建Vue信息对象
        VueInfo info = new VueInfo();
        // 调用父类的simple方法进行基础信息设置
        super.simple(info);
        // 设置Vue编译相关的属性信息
        info.setCompileCss(this.compileCss);
        info.setCompileJs(this.compileJs);
        info.setDescription(this.description);
        info.setParams(this.params);
        info.setJsDependences(this.jsDependences);
        info.setCssDependences(this.cssDependences);
        info.setHistory(this.history);
        return info;
    }


    /**
     * 创建并返回当前VueInfo对象的深拷贝副本
     *
     * @return VueInfo 返回一个新的VueInfo对象，包含与当前对象相同的数据
     */
    @Override
    public VueInfo copy() {
        // 创建新的VueInfo实例
        VueInfo info = new VueInfo();
        // 复制父类属性
        super.copyTo(info);
        // 复制VueInfo特有属性
        info.setCompileCss(this.compileCss);
        info.setCompileJs(this.compileJs);
        info.setDescription(this.description);
        info.setParams(this.params);
        info.setJsDependences(this.jsDependences);
        info.setCssDependences(this.cssDependences);
        info.setHistory(this.history);
        return info;
    }

    @Data
    public static class Vue3ComponentHisInfo {
        private String id;
        private String sourceCode;
        private String jsDependences;
        private String cssDependences;
        private Long createTime;
        private Long updateTime;
        private String createBy;
        private String updateBy;
        public Date getCreateDate() {
            if (createTime == null) {
                return null;
            }
            return new Date(createTime);
        }

    }


}
