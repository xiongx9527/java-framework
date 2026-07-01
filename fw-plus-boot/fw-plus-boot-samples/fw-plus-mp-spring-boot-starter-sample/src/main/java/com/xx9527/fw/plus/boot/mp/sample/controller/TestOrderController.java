package com.xx9527.fw.plus.boot.mp.sample.controller;

import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.excel.entity.FwPlusImportParams;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import cn.afterturn.easypoi.handler.inter.IExcelDataModel;
import cn.afterturn.easypoi.handler.inter.IExcelModel;
import com.xx9527.fw.plus.boot.mp.sample.service.TestOrderService;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;

@RestController("/test")
public class TestOrderController {

    @Resource
    private TestOrderService testOrderService;

    @GetMapping
    @PostMapping("/test01")
    public String test01(){
        testOrderService.test01();
        return "ok";
    }

    @PostMapping("/test02")
    public String test02(){
        testOrderService.test02();
        return "ok";
    }

    @PostMapping("/test03")
    public String test03(){
        testOrderService.test03();
        return "ok";
    }

    @PostMapping("/test04")
    public String test04(){
        testOrderService.test04();
        return "ok";
    }

    @PostMapping("/test05")
    public String test05() {
        testOrderService.test05();
        return "ok";
    }

    @PostMapping("/test06")
    public Long test06() {
        return testOrderService.test06();
    }

    @PostMapping("/importData")
    public Object importData(@RequestPart(value = "file", required = true) MultipartFile file,
                             @RequestParam(required=false,defaultValue = "0") int startSheetIndex
                             ) throws Exception {
        FwPlusImportParams params = new FwPlusImportParams();
        params.setNeedVerify(true);
        params.setNeedTrans(Boolean.TRUE);
        params.setStartSheetIndex(startSheetIndex);
        ExcelImportResult<ImportInfoForm> result = ExcelImportUtil.importExcelMore(file.getInputStream(), ImportInfoForm.class, params);
        if (CollectionUtils.isEmpty(result.getList())) {
            throw new RuntimeException("没有导入的数据");
        }
        testOrderService.test07(result,startSheetIndex);
        return null;
    }

    @Data
    public static class  ImportInfoForm implements IExcelModel, IExcelDataModel {
        /**
         * 行号字段
         * 用于记录数据处理过程中的行号信息，便于定位错误位置
         */
        private Integer rowNum;

        /**
         * 错误信息字段
         * 用于存储数据处理过程中产生的错误描述信息
         */
        private String errorMsg;

        @NotBlank
        @Excel(name = "分组、题干、选项ID（后8位为0为分组）")
        private String id;

        //@NotBlank
        @Excel(name = "分组、题干、选项编号")
        private String code;

        @NotBlank
        @Excel(name = "分组、题干、选项名称")
        private String name;

        @Excel(name = "题干-题型（1:选择题； 2:填空题）")
        private String type;

        @Excel(name = "题干-指标jsonPath")
        private String jsonPath ;

        @Excel(name = "题干-是否多选（1:是；0:否）")
        private String multiSelectFlag;

        @Excel(name = "选项是否只能单独被选")
        private String chooseOnlyFlag;

        @Excel(name = "分值")
        private String score;

        @Excel(name = "分组或者上级分组ID")
        private String groupId;

        @Excel(name = "题干ID或者上级题干ID")
        private String parentBodyId;

        @Excel(name = "题干、选项对应的下级题干ID")
        private String nextBodyId;

        @Excel(name = "排序值")
        private String orderIndex;

        @Excel(name = "题干、选项其他配置（JSON）")
        private String config;

    }


}
