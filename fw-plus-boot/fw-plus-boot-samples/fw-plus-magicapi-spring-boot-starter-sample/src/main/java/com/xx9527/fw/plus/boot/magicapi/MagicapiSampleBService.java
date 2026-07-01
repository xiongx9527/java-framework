package com.xx9527.fw.plus.boot.magicapi;

import com.xx9527.fw.plus.boot.autoconfigure.trace.plumelog.annotation.PlumelogTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MagicapiSampleBService {

    @PlumelogTrace
    public void trace2() throws InterruptedException {
        Thread.sleep(700);

    }

}
