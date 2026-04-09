package com.taskscheduler;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * PriorityTaskScheduler uses a ThreadPoolExecutor with a PriorityBlockingQueue.
 * The queue requires tasks to be Comparable or a Comparator to be provided.
 * Since we are submitting Runnable tasks, the Runnable implementation (Task) must implement Comparable.
 * Note: If you use submit() instead of execute(), the tasks are wrapped in FutureTask, which is not Comparable,
 * and will cause a ClassCastException at runtime unless the queue is configured otherwise.
 */
public class PriorityTaskScheduler {

    private final ThreadPoolExecutor executorService;

    public PriorityTaskScheduler(int poolSize) {
        // Initializing ThreadPoolExecutor with a PriorityBlockingQueue
        // The queue will order tasks based on their natural ordering (compareTo method of Task)
        this.executorService = new ThreadPoolExecutor(
                poolSize,       // Core pool size
                poolSize,       // Maximum pool size
                0L,             // Keep alive time (0 for fixed size)
                TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>() // The priority queue
        );
    }

    /**
     * Schedules a task for execution.
     * Note: We use execute() instead of submit() because submit() wraps the task in a FutureTask
     * which does not implement Comparable, causing ClassCastException in PriorityBlockingQueue.
     * @param task The task to schedule
     */
    public void schedule(Task task) {
        System.out.println("Scheduling: " + task);
        executorService.submit(task);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
