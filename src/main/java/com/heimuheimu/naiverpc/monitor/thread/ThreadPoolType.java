package com.heimuheimu.naiverpc.monitor.thread;

/**
 * 线程池枚举类型
 */
public enum ThreadPoolType {

    /**
     * RPC 服务提供者使用的 Thread Pool
     */
    RPC_SERVER,

    /**
     * RPC 服务调用者使用的 Thread Pool
     */
    RPC_CLIENT

}
