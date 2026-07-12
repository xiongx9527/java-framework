package com.xiongx9527.fw.plus.boot.common.web;

import com.xiongx9527.fw.plus.common.web.WebRequest;
import com.xiongx9527.fw.plus.common.web.WebResponse;

public interface ICommonFilterService {

    String resolveAndCacheLookupPath() ;

    void doFilterBefore(WebRequest request, WebResponse response);

    void doFilterAfter();

}
