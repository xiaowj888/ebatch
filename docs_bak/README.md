# 通用的批处理场景
> 业务开发中,经常会遇到需要处理批次任务的场景,如果单个任务处理耗时比较久(文件处理,网络处理,耗时查询等),为了更加快速的
完成批处理任务,我们可以使用多线程来跑批,通常为了保证批次完成后处理后续的流程,会有如下代码结构：

```java
class Driver2 { // ...
  void main() throws InterruptedException {
    CountDownLatch doneSignal = new CountDownLatch(N);
    Executor e = ...
    for (int i = 0; i < N; ++i) // create and start threads
      e.execute(new WorkerRunnable(doneSignal, i));
    doneSignal.await();           // wait for all to finish
  }
}

class WorkerRunnable implements Runnable {
  private final CountDownLatch doneSignal;
  private final int i;
  WorkerRunnable(CountDownLatch doneSignal, int i) 
    this.doneSignal = doneSignal;
    this.i = i;
  }
  public void run() {
    try {
      doWork(i);
      doneSignal.countDown();
    } catch (InterruptedException ex) {} // return;
  }
  void doWork() { ... }
}}
```

> 老爷子为CountDownLatch写的通用使用demo,这种结构相信大部分后端批处理都有使用到,为了避免在多个业务系统总
使用批处理的时候都需要开发一套上面CountDownLatch代码结构,使用spring的ThreadPoolTaskExecutor配合CountDownLatch
开发了一套通用的批处理框架。


> 首先定义抽象接口，批处理的入口参数为List<E> 类型,返回参数EBatchRes<E, R>封装了一个Map<Integer, Pair<E,R>> resMap
key为原来List<E>参数在List中的位置,Pair<E,R>返回批处理IBusiService.doService的入参和返回参数

```java
public interface IBatchExecutor {

    /**
    * @description 同步批次处理，所有批次同步执行完成后返回执行结果
    * @Date 2020/8/8 12:49
    * @param batchList
    * @param singleService
    * @return java.util.Map<E,R>
    **/
    <E,R> EBatchRes<E, R> syncBatch(List<E> batchList,IBusiService<E,R> singleService) throws InterruptedException;
    /**
     * @description 同步批次处理，所有批次同步执行完成后返回执行结果
     * @Date 2020/8/8 12:49
     **/
    EBatchRes<Void,Void> syncBatchRunnable(List<Runnable> batchList) throws InterruptedException;

    /**
     * @description 同步批次处理，所有批次同步执行完成后返回执行结果
     * @Date 2020/8/8 12:49
     **/
    <R> EBatchRes<Void,R> syncBatchCallable(List<Callable<R>> batchList) throws InterruptedException;

    /**
    * @description 异步执行批次处理，异常不中断，所以任务执行完
    * @Date 2020/8/8 12:53
    * @param batchList
    * @param singleService
    * @return
    **/
    <E,R> void asyncBatch(List<E> batchList,IBusiService<E,R> singleService) throws InterruptedException;

    /**
     * @description 异步执行批次处理，异常不中断，所以任务执行完
     * @Date 2020/8/8 12:49
     **/
    void asyncBatchRunnable(List<Runnable> batchList) throws InterruptedException;

    /**
     * @description 异步执行批次处理，异常不中断，所以任务执行完
     * @Date 2020/8/8 12:49
     **/
    <R> void asyncBatchCallable(List<Callable<R>> batchList) throws InterruptedException;
}
```
> 批处理的业务处理接口
```java
interface IBusiService<E,R>{
    /**
    *@description 执行doService方法,调用批处理时需要实现 {@link IBusiService}
    *@param e 业务执行的入参
    *@return R 执行返回结果
    **/
    R doService(E e);
}
```

> 批处理的返回对象

```java

class EBatchRes<E, R> {
    //返回执行结果 
    private Map<Integer, Pair<E,R>> resMap;
    //保存执行异常 中断剩余任务
    private volatile Throwable exception;

    public EBatchRes() {
    }

    public EBatchRes(Map<Integer, Pair<E,R>> resMap, Throwable exception) {
        this.resMap = resMap;
        this.exception = exception;
    }
}
```

> EBatchExecutor对核心方法IBatchExecutor#syncBatch的实现

```java
 @Override
    public <E, R> EBatchRes<E, R> syncBatch(List<E> batchList, IBusiService<E, R> singleService) throws InterruptedException {
        //partition
        List<List<E>> partitions = partition(batchList);

        //transfer partitions to batch callable
        List<List<IBatchCallable<E, R>>> partitionCall = Transfers.transferBusiService(partitions,singleService);

        //submit partition
        return submit(partitionCall);
    }
```

首先根据线程池核心线程数量来对任务分区,使用核心线程数的一半来分区,不让一个批次就占满CPU也让批次任务更具伸缩性
当然实际任务分区应该根据业务场景来决定,这里只提供一种默认思路

```java
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
```

然后将分区后的参数根据不同的类型转换成批处理接口IBatchCallable为任务提交作准备

```java

public static <R, E> List<List<IBatchCallable<E,R>>> transferBusiService(List<List<E>> partitions, IBusiService<E,R> singleService) {
        List<List<IBatchCallable<E,R>>> partitionCall = Lists.newArrayListWithCapacity(partitions.size());
        AtomicInteger index = new AtomicInteger();
        for (List<E> partition : partitions) {
            List<IBatchCallable<E,R>> calls = Lists.newArrayListWithCapacity(partition.size());
            partition.forEach((e) -> {
                calls.add(new EBatchExecutor.AdaptedBusiService<>(singleService,e, index.getAndIncrement()));
            });
            partitionCall.add(calls);
        }
        return partitionCall;
    }


    //具体接口的实现
    
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
```

看核心的提交逻辑,使用ThreadPoolTaskExecutor#submitListenable来提交任务,该任务返回一个可监听的Future对象能够
更加方便对线程池提交任务的结果作处理

```java

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
    
    
    // 核心的同步分区处理接口
    
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
```

对线程池处理结果的回调处理
```java

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
```


