package com.workstealingpoolExecutor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.FutureTask;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        ForkJoinTask<Integer> futureTask = pool.submit((new ComputeSumTask(0,100)));
        System.out.println(futureTask.get());
    }
}
