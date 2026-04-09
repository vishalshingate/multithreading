package com.taskscheduler;

public class Task implements Runnable, Comparable<Task> {
    private static final java.util.concurrent.atomic.AtomicLong sequenceGenerator = new java.util.concurrent.atomic.AtomicLong(0);
    private final String name;
    private final int priority;
    private final long sequenceNumber;

    public Task(String name, int priority) {
        this.name = name;
        this.priority = priority;
        this.sequenceNumber = sequenceGenerator.getAndIncrement();
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public void run() {
        System.out.println("Processing Task: " + name + " with Priority: " + priority + " by " + Thread.currentThread().getName());
        try {
            Thread.sleep(1000); // Simulate work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public int compareTo(Task other) {
        // Higher priority value comes first (Descending order)
        int priorityDiff = Integer.compare(other.priority, this.priority);
        if (priorityDiff == 0) {
            // FIFO for same priority: smaller sequence number comes first
            return Long.compare(this.sequenceNumber, other.sequenceNumber);
        }
        return priorityDiff;
    }
    
    @Override
    public String toString() {
        return "Task{name='" + name + "', priority=" + priority + ", seq=" + sequenceNumber + "}";
    }
}




