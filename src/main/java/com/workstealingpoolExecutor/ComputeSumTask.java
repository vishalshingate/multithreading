package com.workstealingpoolExecutor;

import java.util.concurrent.RecursiveTask;

public class ComputeSumTask extends RecursiveTask<Integer> {
    private int start;
    private int end;
    public ComputeSumTask(int start, int end) {
        this.start = start;
        this.end = end;
    }
    @Override
    protected Integer compute() {
        int sum = 0;
        if (end - start <= 4) {
            for (int i = start; i <= end; i++) {
                sum += i;
            }
            return sum;
        }
        else {
            //split the task
            int mid = (start + end) / 2;

            ComputeSumTask leftTask = new ComputeSumTask(start, mid);
            ComputeSumTask rightTask = new ComputeSumTask(mid+1, end);

            // fork the subtasks for parallel execution

            leftTask.fork();
            rightTask.fork();

            // combine the result


            int leftResult = leftTask.join();

            int rightResult = rightTask.join();


            return leftResult + rightResult;
        }


    }

}
