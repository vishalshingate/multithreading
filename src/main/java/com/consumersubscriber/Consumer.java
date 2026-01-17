package com.consumersubscriber;

import java.util.Queue;

public class Consumer {
    public void consume(Queue<Integer> queue) {
        while(true) {
            synchronized(queue) {
                while(queue.isEmpty()) {
                    try {
                        System.out.println("queue is empty waiting for the producer to produce");
                        queue.wait();
                    }catch (InterruptedException e){
                        System.out.println("Consumer Interrupted");
                    }
                }
                System.out.println("Consumed item "+queue.poll());
                queue.notifyAll();

                try {
                    Thread.sleep(1000); // Slow down for visualization
                } catch (InterruptedException e) { }
            }
        }

    }


}
