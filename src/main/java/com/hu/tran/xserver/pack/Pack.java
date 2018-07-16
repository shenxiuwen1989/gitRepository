package com.hu.tran.xserver.pack;

import com.hu.tran.xserver.handle.Handler;
import lombok.Data;

import java.util.ArrayList;

/**
 * xml报文对象
 * @author hutiantian
 * @create 2018/6/8 19:30
 * @since 1.0.0
 */
@Data
public class Pack {
    private String packCode;				    //通讯报文交易码
    private String desc;					    //通讯报文描述
    private String logFlag;                     //是否记录日志标识
    private String encoding;				    //通讯报文编码格式
    private Handler handler;                    //报文处理类
    private String root;                        //返回的根节点
    private ArrayList<Field> requestList;       //请求字段集合
    private ArrayList<Field> responseList;      //响应字段集合
}
