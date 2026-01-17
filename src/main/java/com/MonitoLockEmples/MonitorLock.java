package com.MonitoLockEmples;

public class MonitorLock {
    public synchronized void task1()  {

        try {


            System.out.println("task1");

            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.out.println("task1 Interrupted" + e.getMessage());
        }
        catch (Exception e) {
            System.out.println("task1 Exception" + e.getMessage());
        }
    }

    public synchronized void task2(){
        synchronized (MonitorLock.class) {
            System.out.println("task2");
        }

    }

    public synchronized void task3(){
        System.out.println("task3");
    }

    public void task4() {
        synchronized (this) {
            try {
                System.out.println("task4 (synchronized block this) - started");
                Thread.sleep(3000);
                System.out.println("task4 - ended");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void task5() {
        synchronized (MonitorLock.class) {
            try {
                System.out.println("task5 (synchronized class) - started");
                Thread.sleep(3000);
                System.out.println("task5 - ended");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
