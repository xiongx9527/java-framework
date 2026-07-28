package com.xiongx9527.fw.plus.boot.mate.sample.dto.form;

import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.anno.UnTrans;
import com.fhs.core.trans.constant.TransType;
import com.xiongx9527.fw.plus.boot.mate.sample.dto.vo.UserInfoVo;
import com.xiongx9527.fw.plus.common.validator.CommonsValidator;
import com.xiongx9527.fw.plus.common.validator.CommonsValidatorAnn;
import com.xiongx9527.fw.plus.easytrans.common.MateEasyTransDTO;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
public class SaveUserForm extends MateEasyTransDTO {

    @CommonsValidatorAnn(message = "id不能为空", dataValidType = CommonsValidatorAnn.DataValidType.OBJECT_IS_NOT_EMPTY)
    private Long id;

    @Trans(type = TransType.ENUM , target = UserInfoVo.UserSexEnum.class,fields = {"type","desc"},ref="sexShow")
    private Integer sex;

    private String sexShow;

    @CommonsValidatorAnn(message = "sexValidetor不能为空",
            conditionExpressions = "#formData.sex == 1",
            dataValidType = CommonsValidatorAnn.DataValidType.OBJECT_IS_NOT_EMPTY)
    private String sexValidetor;

    @NotNull
    private Long userExtId;

    @CommonsValidatorAnn(message = "开始时间不能大于结束时间",
            conditionExpressions = "#formData.endDate != null ",
            dataValidExpressions = "#formData.startDate.getTime() < #formData.endDate.getTime()",
            dataValidType = CommonsValidatorAnn.DataValidType.VALID_EXPRESSIONS,
            description = "")
    @NotNull
    private Date startDate;

    @NotNull
    private Date endDate;

    @Valid
    private List<SaveUserItemForm> list;




    @Data
    public static class SaveUserItemForm {

        @CommonsValidatorAnn(message = "第{itemIndex}行的id不能为空",
                conditionExpressions = " 1==1 " ,
                dataValidType = CommonsValidatorAnn.DataValidType.OBJECT_IS_NOT_EMPTY)
        private Integer a;

        private Integer b;
    }


}
