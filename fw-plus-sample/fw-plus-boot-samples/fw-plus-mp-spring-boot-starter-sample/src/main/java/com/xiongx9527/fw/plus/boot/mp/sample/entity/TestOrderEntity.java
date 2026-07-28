package com.xiongx9527.fw.plus.boot.mp.sample.entity;

import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.common.mate.annotation.Algorithm;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author xiongxiao
 * @since 2025-01-10
 */
@SuppressWarnings("serial")
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("test_order")
public class  TestOrderEntity  extends MpBaseEntity {
    @FieldEncrypt(algorithm=Algorithm.PBEWithMD5AndDES)
    // @FieldEncrypt(algorithm=Algorithm.MD5_16,executeDecrypt=false)
    private String phone;

    private String name;

    private Integer status;
}
