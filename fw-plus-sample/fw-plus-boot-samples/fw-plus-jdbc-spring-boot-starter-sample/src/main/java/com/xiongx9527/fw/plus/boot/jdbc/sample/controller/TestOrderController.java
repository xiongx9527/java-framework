package com.xiongx9527.fw.plus.boot.jdbc.sample.controller;

import com.xiongx9527.fw.plus.boot.jdbc.sample.service.TestOrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("/test")
public class TestOrderController {

    @Resource
    private TestOrderService testOrderService;

    @PostMapping("/test01")
    public String test01(){
        testOrderService.test01();
        return "ok";
    }


}
