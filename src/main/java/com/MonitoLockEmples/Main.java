package com.MonitoLockEmples;

public class Main {
    public static void main(String[] args) {
        MonitorLock monitorLock = new MonitorLock();
        Thread  thread1 = new Thread(()->{monitorLock.task1();});
        Thread  thread2 = new Thread(()->{monitorLock.task2();});
        Thread  thread3 = new Thread(()-> {monitorLock.task3();});

        thread1.start();
        thread2.start();
        thread3.start();
    }
}
