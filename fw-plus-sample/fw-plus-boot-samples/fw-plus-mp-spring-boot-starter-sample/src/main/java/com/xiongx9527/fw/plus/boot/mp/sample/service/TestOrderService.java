package com.xiongx9527.fw.plus.boot.mp.sample.service;

import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import com.xiongx9527.fw.plus.boot.mp.sample.controller.TestOrderController;

public interface TestOrderService {

    void test01();

    void test02();

    void test03();

    void test04();

    void test05();

    Long test06();

    void test07(ExcelImportResult<TestOrderController.ImportInfoForm> result,int startSheetIndex);

}
