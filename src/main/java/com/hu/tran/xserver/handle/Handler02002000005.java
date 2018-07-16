package com.hu.tran.xserver.handle;

import java.util.Map;

/**
 * @author hutiantian
 * @create 2018/6/15 11:27
 * @since 1.0.0
 */
public class Handler02002000005 implements Handler{

    public void handler(Map<String,Object> request, Map<String,Object> response){
        response.put("code","哈哈哈");
    }
}
