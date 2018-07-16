package com.hu.tran.xserver.pack;

import com.hu.tran.xserver.core.Application;
import com.hu.tran.xserver.handle.Handler;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 报文缓存对象
 * @author hutiantian
 * @create 2018/6/9 9:21
 * @since 1.0.0
 */
public class PackMapper {
    private static final Logger log = Logger.getLogger(PackMapper.class);

    private static PackMapper packMapper;
    private static Map<String,Pack> packMap = new HashMap();
    private static final String handlerPath = "com.hu.tran.xserver.handle.";

    //私有无参构造方法
    private PackMapper(){}

    private PackMapper(String configFilePath) throws Exception{
        String pathTemp = configFilePath;
        //中文转乱
        configFilePath = URLDecoder.decode(this.getClass().getResource(configFilePath).getPath(),"utf-8");
        //打成可执行jar文件后不能用file来读取document,需要转url
        if(configFilePath.contains("jar!")){
            //this.class.getProtectionDomain().getCodeSource().getLocation()如果直接执行.class文件那么会得到当前class的绝对路径。
            //如果封装在jar包里面执行jar包那么会得到当前jar包的绝对路径
            String jarPath = Application.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            boolean ret = true;
            boolean ret1 = false;
            Enumeration<JarEntry> entry = new JarFile(jarPath).entries();
            while(entry.hasMoreElements()) {
                JarEntry jar = entry.nextElement();
                String jarName = jar.getName();
                if(jarName.startsWith(pathTemp.replace("/","")+"/")&&jarName.length()>5){
                    ret1 = true;
                    Document doc = null;
                    try{
                        SAXReader sax = new SAXReader();
                        doc = sax.read(new URL("jar:file:"+jarPath+"!/"+jarName));
                    }catch (Exception e){
                        ret = false;
                        log.error("读取服务方报文"+jarName+"异常！",e);
                        break;
                    }
                    Pack pack = getPack(doc.getRootElement());
                    if(pack==null){
                        ret = false;
                        log.error("组装服务方报文:"+jarName+"异常！");
                        break;
                    }
                    packMap.put(pack.getPackCode(), pack);
                }
            }
            if(ret&&ret1){
                packMapper = this;
            }
        }else{
            File[] fileList = new File(configFilePath).listFiles();
            if(fileList!=null&&fileList.length>0){
                boolean ret = true;
                for (File file:fileList) {
                    Document doc = null;
                    try{
                        SAXReader sax = new SAXReader();
                        doc = sax.read(file);
                    }catch (Exception e){
                        ret = false;
                        log.error("读取服务方报文"+file.getName()+"异常！",e);
                        break;
                    }
                    Pack pack = getPack(doc.getRootElement());
                    if(pack==null){
                        ret = false;
                        log.error("组装服务方报文:"+file.getName().split("\\.")[0]+"异常！");
                        break;
                    }
                    packMap.put(pack.getPackCode(), pack);
                }
                if(ret){
                    packMapper = this;
                }
            }
        }
    }

    /**
     * 解析报文对象
     */
    private Pack getPack(Element root){
        ArrayList<Field> requestList = getField(root,"Request");
        ArrayList<Field> responseList = getField(root,"Response");
        if(requestList==null||requestList.size()==0||responseList==null||responseList.size()==0){
            log.error("服务方报文的XML字段集合异常！");
            return null;
        }
        Element packCode = root.element("PackCode");
        if(packCode==null){
            log.error("未配置PackCode字段！");
            return null;
        }
        Element desc = root.element("Desc");
        if(desc==null){
            log.error("未配置Desc字段！");
            return null;
        }
        Element logFlag = root.element("LogFlag");
        if(logFlag==null){
            log.error("未配置LogFlag字段！");
            return null;
        }
        String flag = logFlag.getText();
        if(!flag.equals("true")&&!flag.equals("false")){
            log.error("LogFlag字段的值只能为true或者false！");
            return null;
        }
        Element encoding = root.element("Encoding");
        if(encoding==null){
            log.error("未配置Encoding字段！");
            return null;
        }
        Element className = root.element("Handler");
        if(className==null){
            log.error("未配置Handler字段！");
            return null;
        }
        Element root1 = root.element("Root");
        if(root1==null){
            log.error("未配置Root字段！");
            return null;
        }

        //通过反射找到具体Handler
        String clazz = handlerPath+className.getText();
        Handler handler = null;
        try{
            handler = (Handler)Class.forName(clazz).newInstance();
        }catch (Exception e){
            log.error("反射生产handler对象失败，请检查Handler配置是否正确！",e);
            return null;
        }
        Pack pack = new Pack();
        pack.setPackCode(packCode.getText());
        pack.setDesc(desc.getText());
        pack.setLogFlag(flag);
        pack.setEncoding(encoding.getText());
        pack.setHandler(handler);
        pack.setRequestList(requestList);
        pack.setResponseList(responseList);
        pack.setRoot(root1.getText());
        return pack;
    }

    /**
     *  解析请求、响应描述字段
     */
    private ArrayList<Field> getField(Element root,String listName){
        ArrayList<Field> list = new ArrayList<Field>();
        Element request = root.element(listName);
        if(request==null){
            log.error("服务方报文未配置"+listName+"标签！");
            return null;
        }
        List<Element> elist = request.elements("Field");
        if(elist==null||elist.size()==0){
            log.error("获取服务方报文"+listName+"的Field标签失败！");
            return null;
        }
        for(Element element:elist){
            Field field = new Field();
            String name = element.attributeValue("name");
            if(name==null||name.equals("")){
                log.error(listName+"未配置[name]字段或名称为空！");
                return null;
            }
            field.setName(name);
            String desc = element.attributeValue("desc");
            if(desc==null||desc.equals("")){
                log.error(name+"标签未配置[desc]字段或描述为空！");
                return null;
            }
            field.setDesc(desc);
            String length = element.attributeValue("len");
            if(length==null||length.equals("")){
                log.error(name+"标签未配置[len]字段！");
                return null;
            }
            int len = 0;
            try{
                len = Integer.parseInt(length);
            }catch(Exception e){
                log.error(name+"标签的[len]值设置错误！"+e);
                return null;
            }
            field.setLen(len);
            String loop = element.attributeValue("loop");
            if(loop==null){
                log.error(name+"标签未配置[loop]字段！");
                return null;
            }
            field.setLoop(loop);
            String nullable = element.attributeValue("nullable");
            if(nullable==null){
                log.error(name+"标签未配置[loop]字段！");
                return null;
            }
            field.setNullable(nullable);
            String tag = element.attributeValue("tag");
            if(tag==null||tag.equals("")){
                log.error(name+"标签未配置[tag]字段或值为空！");
                return null;
            }
            field.setTag(tag);
            list.add(field);
        }
        return list;
    }

    /**
     * 初始化实例对象
     */
    public static PackMapper init(String configFilePath){
        if(packMapper==null){
            try {
                new PackMapper(configFilePath);
            }catch (Exception e){
                log.error("初始化报文缓存对象异常！",e);
            }
        }
        if(packMapper!=null){
            log.debug("初始化报文缓存对象成功！");
        }
        return packMapper;
    }

    public static PackMapper getInstance(){
        return packMapper;
    }

    public Pack getPack(String packCode){
        if(null != packCode && !"".equals(packCode) && packMap.containsKey(packCode)) {
            return packMap.get(packCode);
        } else {
            return null;
        }
    }
}
