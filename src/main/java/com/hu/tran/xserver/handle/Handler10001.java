package com.hu.tran.xserver.handle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hutiantian
 * @create 2018/6/15 11:27
 * @since 1.0.0
 */
public class Handler10001 implements Handler{

    public void handler(Map<String,Object> request, Map<String,Object> response){
        response.put("ReturnCode","00000000000000");
        ArrayList<Map<String,String>> list = new ArrayList();
        Map<String,String> map1 = new HashMap<String, String>();
        map1.put("OtptFile","3000501506240286ad09080063e7_300050.html");
        Map<String,String> map2 = new HashMap<String, String>();
        map2.put("OtptFile","3000501506240286ad09080063e7_300050.xml");
        list.add(map1);
        list.add(map2);
        response.put("list",list);
    }
}
