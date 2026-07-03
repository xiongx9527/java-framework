package com.xiongx9527.fw.plus.boot.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.annotation.PlumelogTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
@Slf4j
public class MagicapiSampleService {

    @Resource
    private MagicapiSampleBService magicapiSampleBService;

    @PlumelogTrace
    public void trace() throws InterruptedException {
        Thread.sleep(6000);
        magicapiSampleBService.trace2();
    }



}
