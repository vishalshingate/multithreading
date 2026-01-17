package com.CreateThread;


public class NewThread extends Thread {

    @Override
    public void run() {
        System.out.println("New Thread"+ Thread.currentThread().getName()+" group Name" + Thread.currentThread().getThreadGroup().getName());
    }


    public static void main(String[] args) {
        System.out.println("Thread "+ Thread.currentThread().getName());

        Thread t1 = new NewThread();
        t1.start();
    }


}
