package com.wind.ebatch.core;

import com.wind.ebatch.tools.Pair;

import java.util.Map;
/**
 * @program: ebatch
 * @description: 批量执行器返回结果
 * @author: xiaowj
 * @created: 2020-08-08
 **/
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

    public Map<Integer, Pair<E, R>> getResMap() {
        return resMap;
    }

    public void setResMap(Map<Integer, Pair<E, R>> resMap) {
        this.resMap = resMap;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}