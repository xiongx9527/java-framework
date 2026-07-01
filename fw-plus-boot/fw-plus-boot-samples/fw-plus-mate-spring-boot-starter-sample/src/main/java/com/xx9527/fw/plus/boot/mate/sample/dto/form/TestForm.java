package com.xx9527.fw.plus.boot.mate.sample.dto.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
public class TestForm {

    @NotBlank(message = "名字不能为空" , groups = {TestFormGroup.class})
    private String name;



    public interface TestFormGroup {
    }
}
