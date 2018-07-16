package com.hu.tran.xserver.handle;

import java.util.Map;

/**
 * 请求报文处理的handler接口
 */
public interface Handler {

    /**
     * 处理请求的方法
     * @param request 请求map
     * @param response 结果返回map
     */
    void handler(Map<String,Object> request, Map<String,Object> response);
}
