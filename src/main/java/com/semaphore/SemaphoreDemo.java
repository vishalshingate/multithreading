package com.semaphore;

import java.util.concurrent.Semaphore;

public class SemaphoreDemo {

    // A Semaphore with 3 permits.
    // This implies only 3 threads can access the resource simultaneously.
    private static final Semaphore semaphore = new Semaphore(3);

    static class User implements Runnable {
        private String name;

        public User(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                System.out.println(name + " : trying to access resource...");

                // 1. Acquire a permit
                // If count > 0, count decreases and thread proceeds.
                // If count == 0, thread BLOCKS until a permit is released.
                semaphore.acquire();

                System.out.println(name + " : GOT ACCESS! (Permits left: " + semaphore.availablePermits() + ")");

                // Simulating work (e.g., using a database connection)
                Thread.sleep(2000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println(name + " : releasing resource.");

                // 2. Release the permit
                // Increases the count, potentially waking up a waiting thread.
                semaphore.release();
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Semaphore Demo (Limit: 3 concurrent users) ---");

        // We spawn 6 threads, but only 3 allowed at a time.
        for (int i = 1; i <= 6; i++) {
            new Thread(new User("User-" + i)).start();
        }
    }
}

