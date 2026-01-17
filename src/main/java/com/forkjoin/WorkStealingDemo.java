package com.forkjoin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class WorkStealingDemo {
 private int test =10;
    public static void main(String[] args) {
        // 1. Create a Work Stealing Pool
        // Ideally, parallelism = number of CPU cores.
        ExecutorService executor = Executors.newWorkStealingPool();

        // Note: newWorkStealingPool() returns a ForkJoinPool
        ForkJoinPool forkJoinPool = (ForkJoinPool) executor;

        System.out.println("Pool Size: " + forkJoinPool.getParallelism());

        long start = System.currentTimeMillis();

        // 2. Submit a large recursive task
        FibonacciTask task = new FibonacciTask(40);
        Long result = forkJoinPool.invoke(task);

        long end = System.currentTimeMillis();
        System.out.println("Result: " + result);
        System.out.println("Time taken: " + (end - start) + "ms");

        System.out.println("Steal Count: " + forkJoinPool.getStealCount());
    }
}

// RecursiveTask: A task that returns a result
class FibonacciTask extends RecursiveTask<Long> {
    private final int n;

    public FibonacciTask(int n) {
        this.n = n;
    }

    @Override
    protected Long compute() {
        // Base case: if task is small enough, compute directly
        if (n <= 1) {
            return (long) n;
        }

        // Recursive case: Split task
        FibonacciTask f1 = new FibonacciTask(n - 1);

        // Fork: Push task f1 to the deque of the current worker thread
        f1.fork();

        FibonacciTask f2 = new FibonacciTask(n - 2);

        // Compute f2 directly on this thread (optimization)
        Long result2 = f2.compute();

        // Join: Wait for the result of f1 (or steal it back if not started)
        Long result1 = f1.join();

        return result1 + result2;
    }
}

