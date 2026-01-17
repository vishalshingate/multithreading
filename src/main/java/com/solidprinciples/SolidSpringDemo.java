package com.solidprinciples;

// ---------------------------------------------------------
// 1. INTERFACE SEGREGATION & OPEN/CLOSED (The Contracts)
// ---------------------------------------------------------
interface NotificationService {
    void send(String message);
}

// ---------------------------------------------------------
// 2. SINGLE RESPONSIBILITY (Implementations)
// ---------------------------------------------------------

// Specific implementation for Email
class EmailService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("Sending EMAIL: " + message);
    }
}

// Specific implementation for SMS (Extension without modifying EmailService - OCP)
class SmsService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("Sending SMS: " + message);
    }
}

// ---------------------------------------------------------
// 3. DEPENDENCY INVERSION (High Level Module)
// ---------------------------------------------------------
class NotificationController {

    // Depends on Abstraction (Interface), NOT Concrete class (EmailService)
    private final NotificationService notificationService;

    // Constructor Injection (Spring does this automatically via @Autowired)
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void triggerNotification(String msg) {
        // Liskov Substitution: We don't care if it's Email or SMS. It just works.
        notificationService.send(msg);
    }
}

// ---------------------------------------------------------
// MAIN (Simulating Spring Container)
// ---------------------------------------------------------
public class SolidSpringDemo {
    public static void main(String[] args) {
        System.out.println("--- Spring Boot SOLID Principles Demo ---");

        // Scenario 1: Injecting Email Service
        NotificationService emailService = new EmailService();
        NotificationController controller1 = new NotificationController(emailService);
        controller1.triggerNotification("Hello via Email");

        // Scenario 2: Injecting SMS Service
        // We changed behavior without touching the Controller code! (OCP & DIP)
        NotificationService smsService = new SmsService();
        NotificationController controller2 = new NotificationController(smsService);
        controller2.triggerNotification("Hello via SMS");
    }
}

