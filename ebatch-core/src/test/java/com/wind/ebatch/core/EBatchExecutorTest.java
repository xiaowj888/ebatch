package com.wind.ebatch.core;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.Callable;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EBatchExecutorTest.class)
@SpringBootApplication(scanBasePackages = "com.wind")
class EBatchExecutorTest {

    @Resource
    IBatchExecutor batchExecutor;

    @Test
    void syncBatch() {

        ThreadLocal<Integer> threadLocal = new InheritableThreadLocal<>();
        threadLocal.set(0);

        final List<String> param = Lists.newArrayList();

        for(int i= 1;i<=20000;i++){
            param.add(""+i);
        }

        try {
            EBatchRes<String, Object> map = batchExecutor.syncBatch(param, new IBusiService<String, Object>() {
                @Override
                public Object doService(String s) {
                    System.out.println(Thread.currentThread().getName()
                            +"   " +Thread.currentThread().getId()
                            +"   " +threadLocal.get()
                            + " 执行参数 " + s);
                    int j = 0;
                    int  exeNumber = 2000000000;
                    for (int i = 1; i <= exeNumber; i++) {
                        if(!Thread.currentThread().isInterrupted()){
                            if((i == (exeNumber >> 2) && threadLocal.get()>5) ){
                                if(Thread.currentThread().getId() >= 20){
                                    System.out.println("外抛异常");
                                    throw new RuntimeException("error");
                                }
                            }
                            j++;
                        }
                    }
                    threadLocal.set(threadLocal.get() + 1);
                    return j;
                }
            });
            System.out.println("批次执行结果:"+ JSONObject.toJSONString(map.getResMap()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    void syncBatchRunnable() {


        ThreadLocal<Integer> threadLocal = new InheritableThreadLocal<>();
        threadLocal.set(0);

        final List<Runnable> param = Lists.newArrayList();

        for(int k= 1;k<=200;k++){
            param.add(() -> {
                System.out.println(Thread.currentThread().getName()
                        +"   " +Thread.currentThread().getId()
                        +"   " +threadLocal.get()
                        );
                int j = 0;
                int  exeNumber = 2000000000;
                for (int i = 1; i <= exeNumber; i++) {
                    if(!Thread.currentThread().isInterrupted()){
                        if((i == (exeNumber >> 2) && threadLocal.get()>5) ){
                            if(Thread.currentThread().getId() >= 20){
                                System.out.println("外抛异常");
                                throw new RuntimeException("error");
                            }
                        }
                        j++;
                    }
                }
                threadLocal.set(threadLocal.get() + 1);
            });
        }

        try {
            EBatchRes<Void, Void> map = batchExecutor.syncBatchRunnable(param);
            System.out.println("批次执行结果:"+ JSONObject.toJSONString(map.getResMap()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }



    @Test
    void syncBatchCallable() {


        ThreadLocal<Integer> threadLocal = new InheritableThreadLocal<>();
        threadLocal.set(0);

        final List<Callable<Long>> param = Lists.newArrayList();

        for(int k= 1;k<=200;k++){
            param.add(() -> {
                System.out.println(Thread.currentThread().getName()
                        +"   " +Thread.currentThread().getId()
                        +"   " +threadLocal.get()
                );
                int j = 0;
                int  exeNumber = 2000000000;
                for (int i = 1; i <= exeNumber; i++) {
                    if(!Thread.currentThread().isInterrupted()){
                        if((i == (exeNumber >> 2) && threadLocal.get()>5) ){
                            if(Thread.currentThread().getId() >= 20){
                                System.out.println("外抛异常");
                                throw new RuntimeException("error");
                            }
                        }
                        j++;
                    }
                }
                threadLocal.set(threadLocal.get() + 1);
                return (long) j;
            });
        }

        try {
            EBatchRes<Void, Long> map = batchExecutor.syncBatchCallable(param);
            System.out.println("批次执行结果:"+ JSONObject.toJSONString(map.getResMap()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    @Test
    void asyncBatch() {

        ThreadLocal<Integer> threadLocal = new InheritableThreadLocal<>();
        threadLocal.set(0);

        final List<String> param = Lists.newArrayList();

        for(int i= 1;i<=100;i++){
            param.add(""+i);
        }

        try {
          batchExecutor.asyncBatch(param, new IBusiService<String, Object>() {
                @Override
                public Object doService(String s) {
                    System.out.println(Thread.currentThread().getName()
                            +"   " +Thread.currentThread().getId()
                            +"   " +threadLocal.get()
                            + " 执行参数 " + s);
                    int j = 0;
                    int  exeNumber = 2000000000;
                    for (int i = 1; i <= exeNumber; i++) {
                        if(!Thread.currentThread().isInterrupted()){
                            if((i == (exeNumber >> 2) && threadLocal.get()>5) ){
                                if(Thread.currentThread().getId() >= 20){
                                    System.out.println("外抛异常");
                                    throw new RuntimeException("error");
                                }
                            }
                            j++;
                        }
                    }
                    threadLocal.set(threadLocal.get() + 1);
                    return j;
                }
            });

          Thread.sleep(1000 * 60 *5);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}