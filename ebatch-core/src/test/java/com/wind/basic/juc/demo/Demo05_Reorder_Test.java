package com.wind.basic.juc.demo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @ClassName Demo01_Visible_Test
 * @Description TODO
 * @Author xiaowj
 * @Date DATE{TIME}
 * reorder v1=1v2=2v3=3vn=4
 * reorder v1=0v2=0v3=3vn=4
 * reorder v1=0v2=0v3=0vn=4
 * reorder v1=0v2=0v3=3vn=4
 * reorder v1=0v2=0v3=0vn=4
 * reorder v1=1v2=2v3=3vn=4
 */

public class Demo05_Reorder_Test {
    static volatile boolean isBreak = false;
    static class ReorderRunnable {
        private  long x = 0;
        private  long y = 0;
        private  long i = 0;
        private  long j = 0;

        private  long k = 0;
        private Long other = 0L;


        public void write(){




            x=1;

            i=y;

            other = 1L;
        }
        public void read(){


            y=1;
            j=x;
            k=2;


            other = 1L;
        }





    }
    public static void main(String[] args) throws InterruptedException, ExecutionException {

        ExecutorService executorService =
                Executors.newFixedThreadPool(6);

        while (!isBreak) {
           ReorderRunnable vr = new ReorderRunnable();
            Future<?> write = executorService.submit(vr::write);
            Future<?> read = executorService.submit(vr::read);
            write.get();
            read.get();
           if(vr.i==2 || vr.j == 2){
               System.out.println("reorder i="+vr.i +" j="+vr.j);
               isBreak = true;
           }
       }

        executorService.shutdown();

    }
}
