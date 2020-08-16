package com.wind.ebatch.core;

/**
 * @program: ebatch
 * @description: 业务执行Service
 * @author: xiaowj
 * @created: 2020-08-08
 **/
interface IBusiService<E,R>{
    /**
    *@description 执行doService方法,调用批处理时需要实现 {@link IBusiService}
    *@param e 业务执行的入参
    *@return R 执行返回结果
    **/
    R doService(E e);
}
