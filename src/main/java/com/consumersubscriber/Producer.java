package com.consumersubscriber;

import java.util.Queue;

public class Producer {

    public void produce(Queue<Integer> queue){
        while(true){
            synchronized(queue) {
                while(queue.size() >= 10){
                    System.out.println("Producer is over (queue full), waiting...");
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("producing the item " + (queue.size() + 1));
                queue.offer(10);
                queue.notifyAll();

                try {
                    Thread.sleep(500); // Slow down for visualization
                } catch (InterruptedException e) { }
            }
        }
    }

}
