package com.xiongx9527.fw.plus.boot.mate.sample.dto.form;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Data
public class TestForm {

    @NotBlank(message = "名字不能为空" , groups = {TestFormGroup.class})
    private String name;



    public interface TestFormGroup {
    }
}
