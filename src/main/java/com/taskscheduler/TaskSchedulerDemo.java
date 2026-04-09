package com.taskscheduler;

import java.util.stream.IntStream;

public class TaskSchedulerDemo {

    public static void main(String[] args) {
        // Scheduler with a single thread to strictly see the priority order in execution
        PriorityTaskScheduler scheduler = new PriorityTaskScheduler(1);

        System.out.println("Submitting tasks...");

        // Submitting tasks in random order of priority to demonstrate sorting
        scheduler.schedule(new Task("LowPriorityTask", 1));
        scheduler.schedule(new Task("MediumPriorityTask", 5));
        scheduler.schedule(new Task("HighPriorityTask", 10));
        scheduler.schedule(new Task("CriticalTask", 20));
        scheduler.schedule(new Task("AnotherLowPriority", 2));

        // Submit more tasks
        IntStream.range(0, 5).forEach(i -> {
            int priority = (int) (Math.random() * 20);
            scheduler.schedule(new Task("RandomTask-" + i, priority));
        });

        System.out.println("Wait for execution to finish...");
        scheduler.shutdown();
    }
}

