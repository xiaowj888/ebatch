package com.wind.ebatch.core;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: ebatch
 * @description: 转换工具 先冗余代码
 * @author: xiaowj
 * @created: 2020-08-12 16:48
 **/
public class Transfers {


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

    public static List<List<IBatchCallable<Void, Void>>> transferRunnable(List<List<Runnable>> partitions) {
        List<List<IBatchCallable<Void,Void>>> partitionCall = Lists.newArrayListWithCapacity(partitions.size());
        AtomicInteger index = new AtomicInteger();
        for (List<Runnable> partition : partitions) {
            List<IBatchCallable<Void,Void>> calls = Lists.newArrayListWithCapacity(partition.size());
            partition.forEach((e) -> {
                calls.add(new EBatchExecutor.AdaptedRunnable(e,index.getAndIncrement()));
            });
            partitionCall.add(calls);
        }
        return partitionCall;
    }

    public static <V> List<List<IBatchCallable<Void,V>>> transferCallable(List<List<Callable<V>>> partitions) {
        List<List<IBatchCallable<Void,V>>> partitionCall = Lists.newArrayListWithCapacity(partitions.size());
        AtomicInteger index = new AtomicInteger();
        for (List<Callable<V>> partition : partitions) {
            List<IBatchCallable<Void,V>> calls = Lists.newArrayListWithCapacity(partition.size());
            partition.forEach((e) -> {
                calls.add(new EBatchExecutor.AdaptedCallable<>(e,index.getAndIncrement()));
            });
            partitionCall.add(calls);
        }
        return partitionCall;
    }
}
