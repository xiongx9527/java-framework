package eureka.gateway.sample;

import com.alibaba.fastjson.JSON;
import com.xx9527.fw.plus.boot.autoconfigure.trace.plumelog.annotation.PlumelogTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/magicapiSample")
public class MagicapiSampleController {

    @PlumelogTrace
    @GetMapping("/trace")
    public String trace() throws InterruptedException {
        log.info("trace 开始拉");
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        return JSON.toJSONString(map);
    }
}
