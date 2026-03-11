package com.learnkafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@SpringBootApplication
@EnableAsync
public class AsyncSelfInvocationDemo implements CommandLineRunner {

    @Autowired
    private ReportService reportService;

    // @Autowired
    // private EmailAsyncService emailAsyncService; // Unused in main runner

    public static void main(String[] args) {
        SpringApplication.run(AsyncSelfInvocationDemo.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- Starting Async Demo ---");

        System.out.println("\n[1] Testing SAME CLASS Self-Invocation (Will FAIL to be Async)");
        reportService.generateReportInternal();

        System.out.println("\n[2] Testing DIFFERENT CLASS Call (Will SUCCEED being Async)");
        reportService.generateReportExternal();

        System.out.println("\n[3] Testing SAME CLASS with Self-Injection (Will SUCCEED being Async)");
        reportService.generateReportSelfInjected();
    }
}

@Service
class ReportService {

    @Autowired
    private EmailAsyncService emailAsyncService;

    @Autowired
    @Lazy // Required to break circular dependency
    private ReportService self;

    // -------------------------------------------------------------
    // SCENARIO 1: Internal Call (FAILS)
    // -------------------------------------------------------------
    public void generateReportInternal() {
        System.out.println("[Main] Generating Report (Internal)... Thread: " + Thread.currentThread().getName());

        // PROBLEM: Calling 'this.sendEmail()' bypasses the proxy!
        sendEmail();

        System.out.println("[Main] Report Done (Internal).");
    }

    // -------------------------------------------------------------
    // SCENARIO 2: External Call (WORKS)
    // -------------------------------------------------------------
    public void generateReportExternal() {
        System.out.println("[Main] Generating Report (External)... Thread: " + Thread.currentThread().getName());

        // CORRECT: Calling a method on a DIFFERENT bean (which is a proxy)
        emailAsyncService.sendEmailExternal();

        System.out.println("[Main] Report Done (External).");
    }

    // -------------------------------------------------------------
    // SCENARIO 3: Self-Injection Call (WORKS) - The "Hack"
    // -------------------------------------------------------------
    public void generateReportSelfInjected() {
        System.out.println("[Main] Generating Report (Self-Inject)... Thread: " + Thread.currentThread().getName());

        // CORRECT: Calling the method on the PROXY of this class
        self.sendEmail();

        System.out.println("[Main] Report Done (Self-Inject).");
    }

    @Async
    public void sendEmail() {
        System.out.println("   --> [Async?] Sending Email... Thread: " + Thread.currentThread().getName());
        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
        System.out.println("   --> [Async?] Email Sent!");
    }
}

@Service
class EmailAsyncService {
    @Async
    public void sendEmailExternal() {
        System.out.println("   --> [Async-Ext] Sending External Email... Thread: " + Thread.currentThread().getName());
        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
        System.out.println("   --> [Async-Ext] External Email Sent!");
    }
}

