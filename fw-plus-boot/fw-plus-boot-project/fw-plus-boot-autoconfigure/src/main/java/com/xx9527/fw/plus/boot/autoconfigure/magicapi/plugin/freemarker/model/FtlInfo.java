package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

@Data
public class FtlInfo extends PathMagicEntity {
    private String description;
    private String params;

    /**
     * 重写simple方法以创建并初始化一个FtlInfo对象
     *
     * 该方法首先创建一个FtlInfo实例，然后调用父类的simple方法进行初始化
     * 最后，将初始化后的FtlInfo对象返回
     *
     * @return MagicEntity 返回初始化后的FtlInfo对象
     */
    @Override
    public FtlInfo simple() {
        // 创建一个新的FtlInfo对象
        FtlInfo info = new FtlInfo();
        // 调用父类的simple方法对info进行初始化
        super.simple(info);
        // 返回初始化后的info对象
        info.setParams(this.params);
        info.setDescription(this.description);
        return info;
    }

    /**
     * 创建当前对象的深拷贝
     *
     * 通过实例化一个新的FtlInfo对象，并调用super.copyTo方法将当前对象的属性值复制到新对象中
     * 此方法确保了新对象与当前对象具有相同的属性值，但不共享内存空间
     *
     * @return FtlInfo 返回一个新的FtlInfo对象，其属性值与当前对象相同
     */
    @Override
    public FtlInfo copy() {
        FtlInfo info = new FtlInfo();
        super.copyTo(info);
        info.setParams(this.params);
        info.setDescription(this.description);
        return info;
    }

}
