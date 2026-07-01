package com.xx9527.fw.plus.boot.mate.sample.dto.vo;

import com.fhs.core.trans.anno.TransDefaultSett;
import com.xx9527.fw.plus.easytrans.common.MateEasyTransDTO;
import lombok.Data;

@Data
@TransDefaultSett(isUseCache = true,cacheSeconds = 1000,maxCache = 1000,isAccess = false)
public class UserExtInfo extends MateEasyTransDTO {

    private Long id;

    private String desc;
}
