package com.xiongx9527.fw.plus.dify.routes;

import com.xiongx9527.fw.plus.dify.types.DifyRoute;
import com.xiongx9527.fw.plus.dify.types.HttpMethod;

public class ConsoleRoutes {
    // 登录
    public static final DifyRoute LOGIN = new DifyRoute(
            "/console/api/login",
            HttpMethod.POST
    );

    // 应用列表
    public static final DifyRoute APPS = new DifyRoute(
            "/console/api/apps",
            HttpMethod.GET
    );


}
