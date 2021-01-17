package com.wind.basic.juc.demo;

import org.springframework.cglib.core.ReflectUtils;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @ClassName ThreadLocalTest
 * @Description 测试Java ThreadLocal的使用
 * @Author xiaowj
 * @Date DATE{TIME}
 */
public class ThreadLocalTest {

    static class BusiService{
        private ThreadLocal<String> sessionLocal = ThreadLocal.withInitial(()->{
            return "defaultSessionId";
        });

        public void logIn(){
            String currentSession = "session" + new Random().nextInt(5);
            System.out.println(Thread.currentThread().getName() + "login with session :"+currentSession);
            sessionLocal.set(currentSession);
        }

        public void doBusi(){
            System.out.println(Thread.currentThread().getName() + "get session :"+sessionLocal.get());
        }
    }

    static BusiService  busiService = new BusiService();
    static class BusiThread extends Thread{


        @Override
        public void run() {
            for(int i=0;i<5;i++){
                //BusiService busiService = new BusiService() ;
                busiService.logIn();
                busiService.doBusi();
            }
            // 如果不是静态变量 这里回收后 threadLocals.table.entry 中key为null value 不为null
            System.gc();
            // do other things
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    public static void main(String[] args) {
        BusiService busiService = new BusiService();

        BusiThread t1 = new BusiThread();
        BusiThread t2 = new BusiThread();

        t1.start();
        t2.start();

        System.out.println("do something ...");

    }

    static void printThreadLocal(Thread thread){


    }






}
