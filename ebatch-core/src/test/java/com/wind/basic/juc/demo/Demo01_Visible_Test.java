package com.wind.basic.juc.demo;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @ClassName Demo01_Visible_Test
 * @Description TODO
 * @Author xiaowj
 * @Date DATE{TIME}
 */
public class Demo01_Visible_Test {


    static volatile boolean isBreak = false;

    static void cb_wait(CyclicBarrier cb){
        try {
            cb.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    static class VisibleRunnable implements Runnable{
        private  boolean visible = false;
        CyclicBarrier cb = new CyclicBarrier(2);

        @Override
        public void run() {
            while (!visible){
                /*synchronized (cb){

                }*/
            }
            isBreak=true;
        }
        public void makeVisible(){
            this.visible = true;
        }
    }
    public static void main(String[] args) throws InterruptedException {
       // while (!isBreak){
            VisibleRunnable vr = new VisibleRunnable();
            Thread writer = new Thread(vr::makeVisible);
            Thread reader = new Thread(vr);

        reader.start();
        TimeUnit.MILLISECONDS.sleep(100);
        //reader.join();
        writer.start();
       // }
    }
}
