package com.hu.tran.xserver.core;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;

/**
 * 任务类
 * @author hutiantian
 * @create 2018/6/12 10:08
 * @since 1.0.0
 */
public class Task implements Runnable{
    private static final Logger log = Logger.getLogger(Task.class);

    private static final Long timeout = 10000L;             //未收到消息得超时时间，单位毫秒
    private static final Long count = 60L;                  //已收到报文的时间计数器，单位毫秒
    private Socket socket;

    public Task(Socket socket){
        this.socket = socket;
    }

    public void run(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();       //返回字节流
        try{
            InputStream is = socket.getInputStream();
            //尝试读取返回消息报文
            long startTime = System.nanoTime();				                //开始接收时间
            boolean readingFlag = false;
            long lastReadTime = System.nanoTime();		                    //上次读取报文内容时间
            while(true) {
                // available()这个方法可以在读写操作前先得知数据流里有多少个字节可以读取
                if(is.available() > 0) {
                    byte[] tmpBytes = new byte[1024];
                    //read() 返回值为实际读取的字节数;如果没有可用的字节,因为已经到达流的末尾, -1返回的值 ;如果tmpBytes.length==0,则返回0
                    int ret = is.read(tmpBytes);
                    if(ret == -1) {
                        break;
                    } else {
                        if(ret > 0) {
                            //读取到有效字节
                            lastReadTime = System.nanoTime();
                            readingFlag = true;
                            baos.write(tmpBytes, 0, ret);
                            continue;
                        }
                    }
                }
                long nowTime = System.nanoTime();
                //判断是否已超时
                if(readingFlag) {
                    //已开始接收报文
                    if((nowTime - lastReadTime) / 1000000l > count) {
                        //指定时间内没有再读取到内容，认为接收完毕
                        break;
                    }
                } else {
                    //未开始接收报文
                    if((nowTime - startTime) > (1000000l * timeout)) {
                        log.debug("接受报文超时");
                        break;
                    }
                }
            }
        }catch (Exception e){
            log.error("接受请求消息异常",e);
            return;
        }
        if(baos.size()==0){
            log.error("请求消息内容为空！");
            return;
        }
        //将任务交给任务处理类
        try{
            TaskExecutor.getInstance().execute(baos);
        }catch (Exception e){
            log.error("处理消息失败！",e);
            return;
        }
        if(baos.size()==0){
            log.error("返回消息内容为空！");
            return;
        }
        //将结果返回
        try {
            //BufferedOutputStream输出，在极端情况下可提升一些效率
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            bos.write(baos.toByteArray());
            bos.flush();
            baos.reset();
        } catch(Exception e) {
            log.error("返回结果消息失败！",e);
            return;
        }finally {
            try {
                socket.close();
            }catch (Exception e){
                log.error("关闭socket异常",e);
            }
        }

    }
}
