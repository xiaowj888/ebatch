package com.wind.ebatch.core;


import java.util.List;
import java.util.concurrent.Callable;

/**
 * @program: ebatch
 * @description: 批量执行器
 * @author: xiaowj
 * @created: 2020-08-08
 **/
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
