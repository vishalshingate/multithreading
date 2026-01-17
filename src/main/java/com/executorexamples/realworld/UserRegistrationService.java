package com.executorexamples.realworld;
/*
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class UserRegistrationService {

    // Main Method called by Controller
    public void registerUser(String username) {
        long startTime = System.currentTimeMillis();

        // 1. Critical Path (Must be synchronous)
        saveToDatabase(username);

        // 2. Non-Critical Path (Fire and Forget)
        // If these fail or take 10 seconds, the user shouldn't wait.
        sendWelcomeEmail(username);
        notifyAuditService(username);

        long endTime = System.currentTimeMillis();
        System.out.println("User " + username + " registered in " + (endTime - startTime) + "ms");
    }

    private void saveToDatabase(String username) {
        // Simulate DB call
        try { Thread.sleep(50); } catch (Exception e) {}
        System.out.println("1. [Sync] Saved user to DB.");
    }

    @Async // Runs in "Prod-Worker-X"
    public void sendWelcomeEmail(String username) {
        System.out.println("2. [Async] Sending email to " + username + " (Thread: " + Thread.currentThread().getName() + ")");
        try {
            Thread.sleep(2000); // Simulate slow SMTP

            // Allow random failure to test Error Handler
            if (new Random().nextBoolean()) {
                throw new RuntimeException("SMTP Server Timeout!");
            }

        } catch (InterruptedException e) {}
        System.out.println("   [Async] Email sent.");
    }

    @Async // Runs in "Prod-Worker-X"
    public void notifyAuditService(String username) {
        System.out.println("3. [Async] Sending audit log for " + username);
        try { Thread.sleep(100); } catch (Exception e) {}
    }
}

*/
