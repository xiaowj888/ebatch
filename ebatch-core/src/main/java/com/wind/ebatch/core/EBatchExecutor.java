package com.wind.ebatch.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wind.ebatch.tools.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

@Component
public class EBatchExecutor implements IBatchExecutor{

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private ThreadPoolTaskExecutor eBatchThreadPool;


    /**
     * Adaptor for Runnables.
     */
    static final class AdaptedRunnable
            implements IBatchCallable<Void,Void> {
        final Runnable runnable;
        Integer index;
        AdaptedRunnable(Runnable runnable,Integer index) {
            if (runnable == null) throw new NullPointerException();
            this.runnable = runnable;
            this.index = index;
        }
        @Override
        public Void param() {
            return null;
        }
        @Override
        public Void call() {
            runnable.run();
            return null;
        }
        @Override
        public Integer index() {
            return index;
        }
    }

    /**
     * Adaptor for Callable
     */
    static final class AdaptedCallable<R>
            implements IBatchCallable<Void,R> {
        final Callable<? extends R> callable;
        Integer index;
        AdaptedCallable(Callable<? extends R> callable,Integer index) {
            if (callable == null) throw new NullPointerException();
            this.callable = callable;
            this.index = index;
        }

        @Override
        public Void param() {
            return null;
        }

        @Override
        public R call() throws Exception {
            return callable.call();
        }
        @Override
        public Integer index() {
            return index;
        }
    }



    /**
     * Adaptor for busiService
     */
    static final class AdaptedBusiService<E,R>
            implements IBatchCallable<E,R> {
        final IBusiService<E,? extends R> busiService;
        E param;
        Integer index;
        AdaptedBusiService(IBusiService<E,? extends R> busiService,E param,Integer index) {
            if (busiService == null) throw new NullPointerException();
            this.busiService = busiService;
            this.param = param;
            this.index = index;
        }
        @Override
        public E param() {
            return param;
        }
        @Override
        public R call() throws Exception {
            return busiService.doService(param);
        }

        @Override
        public Integer index() {
            return index;
        }
    }


    /**
    * @description //同步处理分区 返回分区执行结果
    * @date 2020/8/9 13:32
    * @params
    * @return
    **/
    private class SyncBatchCallable<E,R> implements Callable<Map<E,R>>{
        private EBatchRes<E,R> res;
        private List<IBatchCallable<E,R>> batchCallables;

        SyncBatchCallable(EBatchRes<E, R> res, List<IBatchCallable<E,R>> batchCallables) {
            this.res = res;
            this.batchCallables = batchCallables;
        }
        /**
        * 遇到单个任务的异常之后，取消同分区任务的后续所有任务
        * 发现异常后其它分区任务都取消
        **/
        @Override
        public Map<E,R> call() throws Exception {
            Map<E, R> resMap = Maps.newHashMapWithExpectedSize(batchCallables.size());
            for (IBatchCallable<E,R> call : batchCallables) {
                if (null == res.getException()) {
                    R exeRes = call.call();
                    resMap.put(call.param(), exeRes);
                    res.getResMap().put(call.index(), Pair.of(call.param(), exeRes));
                } else {
                    if(logger.isDebugEnabled()){
                        logger.debug("current batch has exception,stop next task");
                    }
                }
            }
            return resMap;
        }
    }



    /**
     * @description //异步处理分区 返回分区执行结果
     * @date 2020/8/9 13:32
     * @params
     * @return
     **/
    private class AsyncBatchCallable<E,R> implements Callable<Map<E,R>>{
        private List<IBatchCallable<E,R>> batchCallables;

        AsyncBatchCallable( List<IBatchCallable<E,R>> batchCallables) {
            this.batchCallables = batchCallables;
        }
        /**
         * 遇到单个任务的异常之后，同分区任务的后续继续执行
         * 这里需要对异常分类处理，简单只处理RuntimeException
         *
         * 应该保留回调方法
         **/
        @Override
        public Map<E,R> call() throws Exception {
            for (IBatchCallable<E,R> call : batchCallables) {
                R exeRes = call.call();
                System.out.println("执行回调开始:" + call.index() + "   " + call.param() +"  "+ exeRes);
            }
            return null;
        }
    }




    @Override
    public EBatchRes<Void,Void> syncBatchRunnable(List<Runnable> batchList) throws InterruptedException {
        List<List<Runnable>> partitions = partition(batchList);

        //transfer partitions to batch callable
        List<List<IBatchCallable<Void, Void>>> partitionCall = Transfers.transferRunnable(partitions);

        //submit partition
        return submit(partitionCall);
    }

    @Override
    public <V> EBatchRes<Void, V> syncBatchCallable(List<Callable<V>> batchList) throws InterruptedException {
        //partition
        List<List<Callable<V>>> partitions = partition(batchList);

        //transfer partitions to batch callable
        List<List<IBatchCallable<Void, V>>> partitionCall = Transfers.transferCallable(partitions);

        //submit partition
        return submit(partitionCall);
    }



    @Override
    public <E, R> EBatchRes<E, R> syncBatch(List<E> batchList, IBusiService<E, R> singleService) throws InterruptedException {
        //partition
        List<List<E>> partitions = partition(batchList);

        //transfer partitions to batch callable
        List<List<IBatchCallable<E, R>>> partitionCall = Transfers.transferBusiService(partitions,singleService);

        //submit partition
        return submit(partitionCall);
    }



    /**
     * @description // 默认使用线程池核心线程数的一半作为分区数量
     * @date 2020/8/9 13:10
     * @params [batchList]
     * @return java.util.List<java.util.List<E>> 分区后的结果:返回每个线程需要执行的任务数
     **/
    private <E> List<List<E>> partition(List<E> batchList) {
        // partition for the batchList
        int coreSize = eBatchThreadPool.getCorePoolSize();
        int perSize = (batchList.size() / coreSize) << 1;
        perSize = (perSize <= 0 ? batchList.size() : perSize);
        List<List<E>> partition = Lists.partition(batchList, perSize);
        logger.info("batch start info : batchListSize【{}】,coreSize【{}】,threadSize【{}】,perThreadSize【{}】",
                batchList.size(),coreSize,partition.size(),perSize);
        return partition;
    }


    /**
     * @param partition 按分区提交任务到线程池执行
     * @param <E> 参数
     * @param <R> 返回结果
     * @return 返回统一处理实体
     * @throws InterruptedException 外部线程中断
     */
    private <E, R> EBatchRes<E, R> submit(List<List<IBatchCallable<E, R>>> partition) throws InterruptedException {
        final EBatchRes<E, R> res = new EBatchRes<>();
        res.setResMap(Maps.newConcurrentMap());
        List<ListenableFuture<?>> futures = Lists.newArrayList();
        for (final List<IBatchCallable<E, R>> list : partition) {
            futures.add(eBatchThreadPool.submitListenable(new SyncBatchCallable<E,R>(
                    res,list
            )));
        }
        //handle callback
        handleCallback(res, futures);
        return res;
    }




    /**
    * @description //处理futures回调
    * @date 2020/8/9 16:17
    * @params [res, futures]
    * @return void
    **/
    private void handleCallback(EBatchRes res, List<ListenableFuture<?>> futures) throws InterruptedException {
        // use to control all the task finish
        final CountDownLatch countDownLatch = new CountDownLatch(futures.size());
        // each partition thread execute itself callback
        for (ListenableFuture<?> f : futures) {
            f.addCallback(new ListenableFutureCallback<Object>() {
                @Override
                public void onFailure(Throwable ex) {
                    if(logger.isDebugEnabled()){
                        logger.debug("batch task execute with exception",ex);
                    }
                    if (null == res.getException()) {
                        res.setException(ex);
                    }
                    countDownLatch.countDown();
                }
                @Override
                public void onSuccess(Object result) {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
    }



    @Override
    public <E, R> void asyncBatch(List<E> batchList, IBusiService<E, R> singleService) throws InterruptedException {
        //partition
        List<List<E>> partitions = partition(batchList);

        //transfer partitions to batch callable
        List<List<IBatchCallable<E, R>>> partitionCall = Transfers.transferBusiService(partitions,singleService);

        execute(partitionCall);
    }


    @Override
    public void asyncBatchRunnable(List<Runnable> batchList) throws InterruptedException {
        //partition
        List<List<Runnable>> partitions = partition(batchList);

        //transfer partitions to batch callable
        List<List<IBatchCallable<Void, Void>>> partitionCall = Transfers.transferRunnable(partitions);

        execute(partitionCall);
    }

    @Override
    public <R> void asyncBatchCallable(List<Callable<R>> batchList) throws InterruptedException {
        //partition
        List<List<Callable<R>>> partitions = partition(batchList);

        //transfer partitions to batch callable
        List<List<IBatchCallable<Void,R>>> partitionCall = Transfers.transferCallable(partitions);

        execute(partitionCall);
    }


    /**
     * @param partition 按分区通过线程池执行任务
     * @param <E>  参数
     * @param <R>  返回结果
     * @throws InterruptedException 外抛中断异常
     */
    private <E, R> void execute(List<List<IBatchCallable<E, R>>> partition) throws InterruptedException {
        List<ListenableFuture<?>> futures = Lists.newArrayList();
        for (final List<IBatchCallable<E, R>> list : partition) {
            futures.add(eBatchThreadPool.submitListenable(new AsyncBatchCallable<>(list)));
        }
        //handle callback
        for (ListenableFuture<?> f : futures) {
            f.addCallback(new ListenableFutureCallback<Object>() {
                @Override
                public void onFailure(Throwable ex) {
                    if(logger.isDebugEnabled()){
                        logger.debug("batch task execute with exception",ex);
                    }
                }
                @Override
                public void onSuccess(Object result) {
                }
            });
        }
    }

}
