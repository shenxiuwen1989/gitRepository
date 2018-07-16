package com.hu.tran.xserver.handle;

import java.util.Map;

/**
 * @author hutiantian
 * @date: 2018/7/13 16:42
 * @since 1.0.0
 */
public class Handler200001 implements Handler {

    public void handler(Map<String,Object> request, Map<String,Object> response){
        response.put("status","S");
        response.put("mediid","6231460599000008705");
    }
}
