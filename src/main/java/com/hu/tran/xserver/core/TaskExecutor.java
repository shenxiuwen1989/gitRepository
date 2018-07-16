package com.hu.tran.xserver.core;

import com.hu.tran.xserver.common.Constant;
import com.hu.tran.xserver.pack.Field;
import com.hu.tran.xserver.pack.Pack;
import com.hu.tran.xserver.pack.PackMapper;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务处理类
 * @author hutiantian
 * @create 2018/6/12 10:05
 * @since 1.0.0
 */
public class TaskExecutor {
    private static final Logger log = Logger.getLogger(TaskExecutor.class);

    private static final String charset = "UTF-8";				//请求消息编码
    private static final String charset1 = "GBK";				//请求消息编码
    private static final int lengthInfo = 8;				    //请求报文中长度字节的位数
    private static final int lengthInfo1 = 111;				    //电子账户
    private static TaskExecutor taskHandler = new TaskExecutor();

    private TaskExecutor(){}

    public static  TaskExecutor getInstance(){
        return taskHandler;
    }

    /**
     * 消息处理类
     */
    public void execute(ByteArrayOutputStream baos) throws Exception{
        byte[] origin = baos.toByteArray();                     //原始报文长度
        baos.reset();                                           //重置baos
        byte[] copy = new byte[origin.length-lengthInfo];       //截取字段后的长度
        //截取除长度字段外的xml报文
        System.arraycopy(origin, lengthInfo, copy, 0, origin.length - lengthInfo);
        Document reqDoc = null;
        byte[] copy2 = new byte[10];       //截取前言，用于匹配serviceId
        try{
            reqDoc =  DocumentHelper.parseText(new String(copy,charset));
        }catch (Exception e){
            //电子账户报文处理
            byte[] copy1 = new byte[origin.length-lengthInfo1];       //截取字段后的长度
            //截取除长度字段外的xml报文
            System.arraycopy(origin, lengthInfo1, copy1, 0, origin.length - lengthInfo1);
            reqDoc =  DocumentHelper.parseText(new String(copy1,charset1));
            System.arraycopy(origin, 21, copy2, 0, 10);
        }
        Document resDoc =  DocumentHelper.createDocument();
        String serviceCode = null;
        try {
            serviceCode = reqDoc.getRootElement().element("SYS_HEAD").element("ServiceCode").getText();
        }catch (Exception e){
            serviceCode = new String(copy2,charset1).toString();
        }
        if(serviceCode==null){
            //可以返回一个表示错误的报文
            return;
        }
        Map<String,Object> request = new HashMap<String,Object>();		//传给具体handler类的request
        Map<String,Object> response = new HashMap<String,Object>();		//传给具体handler类的response

        //获取请求ServiceCode对应的pack对象
        Pack pack = PackMapper.getInstance().getPack(serviceCode);
        if(pack==null){             //没有serviceId的情况下，直接返回错误报文，不记录日志
            log.error("未找到请求服务编码"+serviceCode+"对应的服务！");
            response.put("ReturnCode","");
            response.put("ReturnMsg","未找到请求服务编码"+serviceCode+"对应的服务！");
            return;
        }
        //将请求报文按xml标签转换后赋值给request对象
        try{
            unpackRequest(reqDoc.getRootElement(),pack,request);
        }catch(Exception e){
            log.error("解析请求报文"+serviceCode+"异常！",e);
            if(e.getMessage().startsWith("NULLABLE")){			//必送字段未上从报错
                response.put("ReturnCode","");
                response.put("ReturnMsg",e.getMessage().split(",")[1]);
            }else{
                response.put("ReturnCode","");
                response.put("ReturnMsg","解析请求报文异常！");
            }
            addResponse(resDoc,pack,response,baos);
            return;
        }
        //调用xml中配置的serviceId对应的处理类handler方法
        try{
            //处理结果为失败，直接返回错误信息错误码，不需要添加响应字段
            pack.getHandler().handler(request, response);
        }catch(Exception e){
            log.error("解析请求报文"+serviceCode+"异常！",e);
            return;
        }

        //组装响应报文
        addResponse(resDoc,pack,response,baos);
    }

    /**
     * 将请求报文中的字段按xml配置赋值给request
     * @param root 请求报文的根元素
     * @param pack 请求serviceId对应的MQPack对象
     * @param request 请求报文转换成map后的指针
     */
    private void unpackRequest(Element root,Pack pack,Map<String,Object> request) throws Exception{
        //遍历请求字段，
        for(int i=0;i<pack.getRequestList().size();i++){
            Element tempRoot = root;
            Field field = pack.getRequestList().get(i);
            String loop = field.getLoop();
            if(loop.equals("")){							//非循环域字段
                String value = getValueFromXml(tempRoot,field.getTag());
                //目前只对上送报文的非循环域标签的字段做非空判断，其它非空校验有时间在优化
                if(field.getNullable().equals("true")&&value.equals("")){
                    throw new Exception("NULLABLE异常,必传字段："+field.getDesc()+"未上送或值为空！");
                }
                request.put(field.getName(), value);
            }else{											//循环域字段一次性处理完
                ArrayList<Field> fieldList = new ArrayList<Field>();
                List<Map<String,String>> mapList = new ArrayList<Map<String,String>>();
                for(Field field1:pack.getRequestList()){
                    if(loop.equals(field1.getLoop())){
                        fieldList.add(field1);
                    }
                }
                getLoopFromXml(fieldList,mapList,tempRoot);
                if(mapList.size()==0){					//xml循环域标签不符合规范，循环内标签默认给空
                    Map<String,String> map = new HashMap<String,String>();
                    for(Field field2:fieldList){
                        map.put(field2.getName(), "");
                    }
                    mapList.add(map);
                }
                request.put(loop, mapList);
                i = i+fieldList.size()-1;
            }
        }
    }

    /**
     * 解析请求xml的普通标签
     * @param elem xml的root
     * @param tag 请求字段对应的tag
     * @return 该tag在xml中的value，未找到至赋空字符串
     */
    private String getValueFromXml(Element elem,String tag){
        String value = "";
        String[] strAarry = tag.split("/");
        for(int i=1;i<strAarry.length;i++){
            if((elem = elem.element(strAarry[i]))==null){		//xml中没有此标签，默认空
                break;
            }
            if(i==strAarry.length-1){
                value = elem.getTextTrim();
            }
        }
        return value;
    }

    /**
     * 解析请求xml的循环标签
     * @param list 同一loop的标签集合
     * @param mapList 解析后的结果list
     * @param elem xml的root
     */
    private void getLoopFromXml(ArrayList<Field> list,List<Map<String,String>> mapList,Element elem){
        String[] strAarry = list.get(0).getTag().split("/");
        //这里默认tag标签的倒数第二层为循环标签，且标签长度大于3
        for(int i=1;i<strAarry.length-2;i++){				//遍历到循环域标签的倒数第三位
            if((elem = elem.element(strAarry[i]))==null){
                break;
            }
            if(i==strAarry.length-3){
                List<Element> elemList = elem.elements(strAarry[i+1]); //拿到循环域
                if(elemList==null){
                    break;
                }
                for(Element ele:elemList){								//遍历循环域标签
                    Map<String,String> map = new HashMap<String,String>();
                    for(Field field:list){							//遍历循环域字段并赋值
                        Element e = ele;
                        String[] temp = field.getTag().split("/");
                        if((e = e.element(temp[temp.length-1]))==null){
                            map.put(field.getName(), "");
                        }else{
                            map.put(field.getName(), e.getTextTrim());
                        }
                    }
                    mapList.add(map);
                }
            }
        }
    }

    /**
     * 根据response和pack配置生成响应报文
     * @param doc 返回的document文档
     * @param pack 请求serviceId对应的Pack对象
     * @param response 具体服务方接口handler处理后的结果map
     */
    private void addResponse(Document doc,Pack pack,Map<String,Object> response,ByteArrayOutputStream baos) throws Exception{
        Element root = doc.addElement(pack.getRoot());              //添加根节点
        //遍历响应字段，
        for(int i=0;i<pack.getResponseList().size();i++){
            Element tempRoot = root;
            Field field = pack.getResponseList().get(i);
            String loop = field.getLoop();
            if(loop.equals("")){							//非循环域字段
                if(response.get(field.getName())!=null){
                    putValueToXml(tempRoot,field.getTag(),response.get(field.getName()).toString());
                }else{
                    putValueToXml(tempRoot,field.getTag(),"");
                }
            }else{											//循环域字段一次性处理完
                ArrayList<Field> fieldList = new ArrayList<Field>();
                for(Field field1:pack.getResponseList()){
                    if(field1.getLoop().equals(loop)){
                        fieldList.add(field1);
                    }
                }
                List<Map<String,String>> mapList = null;
                try{
                    mapList = (List<Map<String,String>>)response.get(loop);
                }catch(Exception e){
                    mapList = new ArrayList<Map<String,String>>();
                    log.info("拼接处理结果异常,循环域结果为空！",e);
                }
                if(mapList!=null&&mapList.size()>0){
                    putLoopToXml(tempRoot,mapList,fieldList);
                }
                i = i+fieldList.size()-1;
            }
        }
        //格式化一下，便于阅读
        OutputFormat format = new OutputFormat();
        format.setEncoding(pack.getEncoding());						//设置编码
        format.setNewlines(true);									//是否换行
        format.setIndent(true);										//缩进
        format.setIndent("    ");									//用4个空格缩进
        StringWriter sw = new StringWriter();
        XMLWriter xmlWriter = new XMLWriter(sw,format);
        xmlWriter.write(doc);
        baos.write(sw.toString().getBytes(pack.getEncoding()));
        sw.close();
        xmlWriter.close();
    }

    /**
     * 将response的非循环域结果放入xml
     * @param elem 根元素
     * @param tag 标签集合
     * @param value 最后一个标签的值
     */
    private void putValueToXml(Element elem,String tag,String value){
        String[] strAarry = tag.split("/");
        for(int i=1;i<strAarry.length;i++){
            if(elem.element(strAarry[i])==null){
                elem = elem.addElement(strAarry[i]);
            }else{
                elem = elem.element(strAarry[i]);
            }
            if(i==strAarry.length-1){
                elem.setText(value);
            }
        }
    }

    /**
     * 将response的循环域结果放入xml
     * @param elem 根元素
     * @param mapList 响应结果map集合
     * @param fieldList 同一loop的标签集合
     */
    private void putLoopToXml(Element elem,List<Map<String,String>> mapList,List<Field> fieldList){
        String[] strAarry = fieldList.get(0).getTag().split("/");
        //这里tag标签的倒数第二层为循环标签，且标签长度大于3
        for(int i=1;i<strAarry.length-2;i++){
            if(elem.element(strAarry[i])==null){
                elem = elem.addElement(strAarry[i]);
            }else{
                elem = elem.element(strAarry[i]);
            }
            if(i==strAarry.length-3){
                Element e = elem;
                for(Map<String,String> map:mapList){
                    Element eArray = e.addElement(strAarry[i+1]);
                    for(Field field:fieldList){
                        String temp = map.get(field.getName());
                        //tag最后一层为循环内标签，且只支持一层
                        String[] sArray = field.getTag().split("/");
                        if(temp!=null){
                            eArray.addElement(sArray[sArray.length-1]).setText(temp);
                        }else{
                            eArray.addElement(sArray[sArray.length-1]);
                        }
                    }
                }
            }
        }
    }
}
