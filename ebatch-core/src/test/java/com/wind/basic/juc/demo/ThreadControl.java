package com.wind.basic.juc.demo;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ClassName ThreadControl
 * @Description 控制ABC三个线程交替打印1,2,3
 * @Author xiaowj
 * @Date DATE{TIME}
 */
public class ThreadControl {

    // 线程数量
    final static int threadCount =3;
    // 执行数量
    final static int exeNumber = 10;

    // 控制是否打印
    final static AtomicInteger cas = new AtomicInteger(0);

    /**
    * @description 通过cas 操作控制
    * @date 2020/10/12 21:24
    * @params
    * @return
    **/

    static Runnable casRunnable = new Runnable() {

        @Override
        public void run() {
            int curNumber = 0;
            while (curNumber < exeNumber){
                synchronized (this){
                    if("A".equals(Thread.currentThread().getName())){
                        if(cas.compareAndSet(0,1)){
                            System.out.println(Thread.currentThread().getName() +"print " + (curNumber+1));
                            curNumber ++;
                        }
                    }

                    if("B".equals(Thread.currentThread().getName())){
                        if(cas.compareAndSet(1,2)){
                            System.out.println(Thread.currentThread().getName() +"print " + (curNumber+1));
                            curNumber ++;
                        }
                    }

                    if("C".equals(Thread.currentThread().getName())){
                        if(cas.compareAndSet(2,0)){
                            System.out.println(Thread.currentThread().getName() +"print " + (curNumber+1));
                            curNumber ++;
                        }
                    }
                }

                /*for(int index=0;index<threadCount;index++){
                    if(curNumber%threadCount!=cas.get())
                        continue;

                    if(cas.compareAndSet(index,(index+1)%threadCount)){
                        System.out.println(Thread.currentThread().getName() +"print " + (index+1));
                        curNumber ++;
                        break;
                    }
                }*/
            }
        }
    };

    static class SynchronizedRunnable implements Runnable{

        Object monitor;
        Object nextMonitor ;

        public SynchronizedRunnable(Object monitor, Object nextMonitor) {
            this.monitor = monitor;
            this.nextMonitor = nextMonitor;
        }

        @Override
        public void run() {
            int curNumber = 0;
            for (;;){

                synchronized (nextMonitor){
                    nextMonitor.notifyAll();
                }
                synchronized (monitor){
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName() +"print " + (curNumber+1));
                    curNumber++;
                    if(curNumber>=exeNumber)
                        break;
                }
            }
        }
    }

    public static void main(String[] args) {
        Thread ta = new Thread(casRunnable,"A");
        Thread tb = new Thread(casRunnable,"B");
        Thread tc = new Thread(casRunnable,"C");
        /*while(true){
            if(cas.get()==0){
                if(!ta.isAlive()){
                    ta.start();
                }
            }
            if(cas.get()==1){
                if(!tb.isAlive()){
                    tb.start();
                }
            }
            if(cas.get()==2){
                if(!tc.isAlive()){
                    tc.start();
                }
                break;
            }
        }*/


        Object ma = new Object();
        Object mb = new Object();
        Object mc = new Object();
        int threadCount=10;
        Object[] monitors = new Object[threadCount];

        monitors[0]=new Object();
        for(int i='A';i<'A'+threadCount;i++){
            System.out.println(Character.toString((char)i));
            if(i-'A'+1<monitors.length){
                monitors[i-'A'+1]=new Object();
            }
            new Thread(new SynchronizedRunnable(monitors[i-'A'],monitors[(i-'A'+1)%(threadCount)]), Character.toString((char)i)).start();
        }


        synchronized (monitors[0]){
            monitors[0].notifyAll();
        }
    }




}
