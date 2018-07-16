package com.hu.tran.xserver.core;

import java.util.concurrent.*;

/**
 * 请求处理线程池
 * @author hutiantian
 * @create 2018/6/12 10:00
 * @since 1.0.0
 */
public class TaskThreadPool {
    private ThreadPoolExecutor pool;

    private static final int corePoolSize = 8;					//核心池大小
    private static final int maximunPoolSize = 15;				//最大线程数
    private static final long keepAliveTime = 20;				//最大空闲时间，默认20分钟
    //阻塞队列，存储待执行的任务,最多存储100个待处理任务
    private static final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(100);
    //拒绝处理任务策略：丢弃任务，不抛异常
    private static final RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardPolicy();

    public TaskThreadPool(){
        pool = new ThreadPoolExecutor(corePoolSize,maximunPoolSize,keepAliveTime,TimeUnit.MINUTES,workQueue,handler);
        pool.prestartCoreThread();								//预启动一个处理线程
        //pool.prestartAllCoreThreads();						//预启动所有处理线程
    }

    public void execute(Runnable command){
        pool.execute(command);
    }
}
