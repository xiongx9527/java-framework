package com.xx9527.fw.plus.boot.magicapi;

import com.xx9527.fw.plus.boot.autoconfigure.trace.plumelog.annotation.PlumelogTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
@RequestMapping("/magicapiSample")
public class MagicapiSampleController {

    @Resource
    MagicapiSampleService magicapiSampleService;

    @PlumelogTrace
    @GetMapping("/trace")
    public String trace() throws InterruptedException {
        log.info("trace 开始拉");
        //PlumelogUtil.traceLog(log,"String com.xx9527.fw.plus.boot.magicapi.MagicapiSampleController.trace");
        Thread.sleep(5000);
        magicapiSampleService.trace();
        return "ok";
    }
}
