package com.consumersubscriber;

import java.util.LinkedList;
import java.util.Queue;

public class Main {

    public static void main(String[] args) {
        Queue<Integer> q = new LinkedList<Integer>();
         Producer p = new Producer();
         Consumer c = new Consumer();
        Thread t1 = new Thread(()->{p.produce(q);});
        Thread t2 = new Thread(()->{c.consume(q);});

        t1.start();
        t2.start();

    }
}
