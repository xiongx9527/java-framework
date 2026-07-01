package com.xx9527.fw.plus.boot.magicapi;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/baidu")
    public ResponseEntity<Void> test() {
        return ResponseEntity.status(302)
                .header("Location", "http://jbptest.oss-cn-beijing.aliyuncs.com/dev/6a3a3000a876e8d47c5f6eff.html?Expires=1782201894&OSSAccessKeyId=LTAI5tDQFDkdcZQfmzy5CwDT&Signature=fZgARbsB9xgbSdsQJKHAAZdHSZM%3D")
                .build();
    }

    @GetMapping(value = "/baidu1", produces = "text/html;charset=UTF-8")
    public String test1() {
        // fetch() 不受 OSS Content-Disposition: attachment 影响，
        // 拉取后转 blob URL 放到 iframe 里，绕过浏览器的下载行为
        String ossUrl = "http://jbptest.oss-cn-beijing.aliyuncs.com/dev/6a3a46179354e699c59019b6.html?Expires=1782207527&OSSAccessKeyId=LTAI5tDQFDkdcZQfmzy5CwDT&Signature=qjKPFhtfTPlBvAvS3LpLYS4SGPk%3D";
        return "<!DOCTYPE html>\n"
                + "<html><body style=\"margin:0\">\n"
                + "<iframe id=\"f\" style=\"width:100%;height:100vh;border:none\"></iframe>\n"
                + "<script>\n"
                + "fetch('" + ossUrl + "')\n"
                + "  .then(r => r.blob())\n"
                + "  .then(b => document.getElementById('f').src = URL.createObjectURL(b));\n"
                + "</script>\n"
                + "</body></html>";
    }

}
