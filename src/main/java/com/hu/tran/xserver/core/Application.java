package com.hu.tran.xserver.core;

import com.hu.tran.xserver.pack.PackMapper;
import org.apache.log4j.Logger;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author hutiantian
 * @create 2018/6/15 11:35
 * @since 1.0.0
 */
public class Application {
    private static final Logger log = Logger.getLogger(Application.class);

    private static final int port = 60000;
    private static final String path = "/pack";         //配置文件存放路径

    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(port);
            if(PackMapper.init(path)==null){
                log.error("初始化失败！");
                System.exit(0);
            }
            //一直循环监听，不退出main
            while (true) {
                Socket socket = server.accept();
                Task task = new Task(socket);
                TaskThreadPool pool = new TaskThreadPool();
                pool.execute(task);
            }
        }catch (Exception e){
            log.error(e);
        }
    }
}
