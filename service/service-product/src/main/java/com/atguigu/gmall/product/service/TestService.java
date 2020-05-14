package com.atguigu.gmall.product.service;


public interface TestService {

    void testLock();

    //写锁
    String readLock();

    //读锁
    String writeLock();
}
