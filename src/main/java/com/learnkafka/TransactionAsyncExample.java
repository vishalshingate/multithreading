package com.learnkafka;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;

/**
 * This class demonstrates two ways to handle post-transaction async tasks:
 * 1. Using {@link TransactionSynchronizationManager} (Programmatic)
 * 2. Using {@link TransactionalEventListener} (Declarative/Clean)
 */
@SpringBootApplication
@EnableAsync
public class TransactionAsyncExample implements CommandLineRunner {

    @Autowired
    private OrderService orderService;

    public static void main(String[] args) {
        SpringApplication.run(TransactionAsyncExample.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- Starting Transaction Demo ---");

        // 1. programmatic synchronization
        System.out.println("\n[1] Testing Programmatic Synchronization (Manual)");
        orderService.placeOrderManualSync("Order-001");

        // 2. event listener (cleaner)
        System.out.println("\n[2] Testing Declarative Event Listener (Spring Way)");
        orderService.placeOrderEventBased("Order-002");
    }
}

// --- Domain ---
@Entity
@Table(name = "orders")
class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String orderNumber;

    public Order() {}
    public Order(String orderNumber) { this.orderNumber = orderNumber; }

    @Override
    public String toString() { return "Order{id=" + id + ", orderNumber='" + orderNumber + "'}"; }
}

interface OrderRepository extends JpaRepository<Order, Long> {}

// --- Service ---
@Service
class OrderService {
    @Autowired private OrderRepository orderRepository;
    @Autowired private EmailService emailService;
    @Autowired private ApplicationEventPublisher eventPublisher;

    /**
     * Approach 1: Programmatic Synchronization
     * Registers a callback to run explicitly AFTER the transaction commits.
     */
    @Transactional
    public void placeOrderManualSync(String orderNumber) {
        System.out.println("[Tx] Transaction started for: " + orderNumber);

        Order order = new Order(orderNumber);
        orderRepository.save(order);
        System.out.println("[Tx] Saved to DB (pending commit): " + order);

        // This block registers the action to happen ONLY on success
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                System.out.println("[Sync] Transaction committed successfully. Triggering async email...");
                emailService.sendOrderConfirmation(order.orderNumber);
            }
        });

        System.out.println("[Tx] Method ending (commit happens next)");
    }

    /**
     * Approach 2: Event-Based (Recommended)
     * Publish an event. The listener handles the "after commit" logic.
     */
    @Transactional
    public void placeOrderEventBased(String orderNumber) {
        System.out.println("[Tx] Transaction started for: " + orderNumber);

        Order order = new Order(orderNumber);
        orderRepository.save(order);

        // Publish event immediately. The listener decides WHEN to react.
        System.out.println("[Tx] Publishing event...");
        eventPublisher.publishEvent(new OrderCreatedEvent(order));

        System.out.println("[Tx] Method ending (commit happens next)");
    }
}

// --- Email Service (Async) ---
@Service
class EmailService {
    @Async
    public void sendOrderConfirmation(String orderId) {
        System.out.println("   --> [Async Email] Sending email for " + orderId + " on thread: " + Thread.currentThread().getName());
        try { Thread.sleep(1000); } catch (InterruptedException e) {} // Simulate work
        System.out.println("   --> [Async Email] Email SENT for " + orderId);
    }
}

// --- Events ---
record OrderCreatedEvent(Order order) {}

@Component
class OrderEventListener {

    @Autowired private EmailService emailService;

    /**
     * @TransactionalEventListener listens for events published within a transaction.
     * phase = AFTER_COMMIT ensures this runs only after the DB transaction is successful.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        System.out.println("[Listener] Received event AFTER_COMMIT for: " + event.order());
        emailService.sendOrderConfirmation(event.order().orderNumber);
    }
}

