package com.xiongx9527.fw.plus.boot.sample.centrifuge;

import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.service.CentrifugoConnectionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.service.CentrifugoPublishService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/sample/centrifuge")
public class DemoCentrifugoController {

    @Resource
    private CentrifugoPublishService publishService;

    @Resource
    private CentrifugoConnectionService connectionService;

    @PostMapping("/send-default")
    public String sendDefault(@RequestParam String channel, @RequestParam String message) {
        publishService.publish(channel, message.getBytes());
        return "ok";
    }

    @PostMapping("/send-instance")
    public String sendInstance(@RequestParam String instanceName, @RequestParam String channel, @RequestParam String message) {
        publishService.publish(instanceName, channel, message.getBytes());
        return "ok";
    }

    @PostMapping("/send-all")
    public String sendAll(@RequestParam String channel, @RequestParam String message) {
        publishService.publishToAll(channel, message.getBytes());
        return "ok";
    }

    @PostMapping("/direct-send")
    public String directSend(@RequestParam String instanceName, @RequestParam String channel, @RequestParam String message) {
        connectionService.send(instanceName, channel, message.getBytes());
        return "ok";
    }

    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String instanceName, @RequestParam String channel) {
        publishService.subscribe(instanceName, channel);
        return "ok";
    }

    @PostMapping("/unsubscribe")
    public String unsubscribe(@RequestParam String instanceName, @RequestParam String channel) {
        publishService.unsubscribe(instanceName, channel);
        return "ok";
    }

    @GetMapping("/instances")
    public String instances() {
        return String.join(",", connectionService.getInstanceNames());
    }
}
