package com.wind.ebatch.core;

/**
 * @program: ebatch
 * @description: 每个分区统一处理的接口
 * @author: xiaowj
 * @created: 2020-08-12 14:19
 **/
public interface IBatchCallable<E,R> {
    Integer index();
    E param();
    R call() throws Exception;
}
