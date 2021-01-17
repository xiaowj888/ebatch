package com.wind.basic.juc.demo;

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

public class Demo04_Reorder_Test {
    static volatile boolean isBreak = false;
    static class ReorderRunnable {
        private  long x = 0;
        private  long y = 0;
        private  long i = 0;
        private  long j = 0;

        private Long other = 0L;


        public void write(){


            x=1;
            i=y;



            other = 1L;
        }
        public void read(){
            y=1;
            j=x;



            other = 1L;
        }





    }
    public static void main(String[] args) throws InterruptedException {
       while (!isBreak) {
           ReorderRunnable vr = new ReorderRunnable();
           Thread writer = new Thread(vr::write);
           Thread reader = new Thread(vr::read);
           writer.start();
           reader.start();

           writer.join();
           reader.join();
           if(vr.i==0 && vr.j==0){
               System.out.println("reorder i="+vr.i +" j="+vr.j);
               isBreak = true;
           }
       }

    }
}
