package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model.FtlInfo;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

public class FtlInfoMagicResourceStorage extends AbstractPathMagicResourceStorage<FtlInfo> implements MagicBaseService {

    /**
     * 重写父类的folder方法，用于指定FreeMarker模板所在的文件夹.
     *
     * 此方法没有输入参数.
     *
     * @return 返回字符串"freemarker"，表示模板文件夹的路径.
     */
    @Override
    public String folder() {
        return "freemarker";
    }

    /**
     * 重写magicClass方法
     * 该方法用于返回一个Class对象，该Class对象指示了方法关注的类类型
     * 在这个案例中，它总是返回FtlInfo.class，这意味着该方法关注的是FtlInfo类
     *
     * @return Class<FtlInfo> 返回FtlInfo类的Class对象
     */
    @Override
    public Class<FtlInfo> magicClass() {
        return FtlInfo.class;
    }

    /**
     * 用于验证给定的FtlInfo实体对象是否有效
     * 该方法重写了父类或接口中的方法，旨在进行特定的验证逻辑
     *
     * @param entity FtlInfo对象，需要验证的实体
     */
    @Override
    public void validate(FtlInfo entity) {
        validateJSON(entity.getParams(),"参数");
    }

    /**
     * 生成映射键
     * 此方法重写自父类，旨在根据FTL信息和魔法资源服务提供的组路径来构建一个唯一的映射键
     * 主要用于在给定FTL信息时，确定其在特定资源组中的唯一位置或标识
     *
     * @param ftlInfo FTL信息对象，包含生成映射键所需的信息
     * @return 生成的映射键字符串
     */
    @Override
    public String buildMappingKey(FtlInfo ftlInfo) {
        return buildMappingKey(ftlInfo, magicResourceService.getGroupPath(ftlInfo.getGroupId()));
    }

}
