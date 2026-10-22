package com.xiongx9527.fw.plus.boot.mate.sample.dto.vo;

import cn.afterturn.easypoi.excel.annotation.Excel;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dromara.core.trans.anno.Trans;
import com.xiongx9527.fw.plus.easytrans.annotation.TransType;
import com.xiongx9527.fw.plus.common.strategy.SensitiveType;
import com.xiongx9527.fw.plus.easytrans.common.MateEasyTransDTO;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldSensitive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class UserInfoVo extends MateEasyTransDTO {

    private Long id = 2077547956433113088L;

    private BigDecimal amount = new BigDecimal(2077547956433113088L);

    @Excel(name = "mobile", width = 25, orderNum = "1")
    @FieldSensitive(SensitiveType.mobile)
    private String mobile;

    @Excel(name = "address", width = 25, orderNum = "2")
    @FieldSensitive(SensitiveType.address)
    private String address;

    @Excel(name = "email", width = 25, orderNum = "3")
    @FieldSensitive(SensitiveType.email)
    private String email;

    @Excel(name = "custom", width = 25, orderNum = "4")
    @FieldSensitive("custom")
    private String custom;

    @Trans(type = TransType.SIMPLE , target = UserExtInfo.class,fields = {"desc"},refs = {"userExtDesc"})
    private Long userExtId;

    @Excel(name = "userExtDesc", width = 25, orderNum = "5")
    private String userExtDesc;

    @Trans(type = TransType.ENUM , targetClassName = "com.xiongx9527.fw.plus.boot.mate.sample.dto.vo.UserInfoVo$UserSexEnum",fields = {"type","desc"},ref="sexShow")
    private Integer sex;

    @Excel(name = "sexShow", width = 25, orderNum = "6")
    private String sexShow;


    private Date date;


    @Trans(type = TransType.BOOLEAN , ref="delFlagShow")
    private Boolean delFlag;

    private String delFlagShow;

    @Override
    @JsonIgnore
    @JSONField(serialize = false)
    protected Object getUniqueValue() {
        return mobile;
    }

    public static enum UserSexEnum {
        男(1,"男"),
        女(0,"女");

        private Integer type;
        private String desc;

        public String getDesc() {
            return desc;
        }

        UserSexEnum(Integer type, String desc) {
            this.type = type;
            this.desc = desc;
        }

        public static UserSexEnum getEnum(Integer type) {
            UserSexEnum value = null;
            for(UserSexEnum temp : UserSexEnum.values()) {
                if(type.equals(temp.type)) {
                    value = temp;
                    break;
                }
            }
            return value;
        }

        @Override
        public String toString() {
            return this.getDesc();
        }
    }

}
