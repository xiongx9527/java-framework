package com.xiongx9527.fw.plus.boot.mate.sample.dto.form;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.handler.inter.IExcelDataModel;
import cn.afterturn.easypoi.handler.inter.IExcelModel;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.xiongx9527.fw.plus.boot.mate.sample.dto.vo.UserInfoVo;
import com.xiongx9527.fw.plus.easytrans.common.MateEasyTransDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ImportUserForm extends MateEasyTransDTO implements IExcelModel , IExcelDataModel {

    @NotBlank
    @Excel(name = "邮箱")
    private String email;
    
    @Excel(name = "姓名")
    private String userName;

    @Trans(type = TransType.ENUM , target = UserInfoVo.UserSexEnum.class,fields = {"desc","type"},ref="sex")
    @Excel(name = "性别")
    private String sexName;
    
    private Integer sex;

    private Integer rowNum;

    private String errorMsg;
}
