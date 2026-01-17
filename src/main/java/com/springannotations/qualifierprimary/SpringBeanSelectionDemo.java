package com.springannotations.qualifierprimary;
/*
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

public class SpringBeanSelectionDemo {

    public static void main(String[] args) {
        // Initialize Spring Context based on this configuration class
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        System.out.println("--- 1. Testing Default Injection (@Primary) ---");
        OrderService orderService = context.getBean(OrderService.class);
        orderService.processOrder();

        System.out.println("\n--- 2. Testing Specific Injection (@Qualifier) ---");
        SupportService supportService = context.getBean(SupportService.class);
        supportService.processRefund();

        context.close();
    }
}

// ---------------------------------------------------------
// 1. THE INTERFACE (The Strategy)
// ---------------------------------------------------------
interface PaymentGateway {
    void pay(double amount);
}

// ---------------------------------------------------------
// 2. IMPLEMENTATIONS (The Concrete Strategies)
// ---------------------------------------------------------

@Component("creditCard")
@Primary // <--- This acts as the DEFAULT if no qualifier is specified
class CreditCardGateway implements PaymentGateway {
    public void pay(double amount) {
        System.out.println("Paying $" + amount + " using Credit Card (Default/Primary).");
    }
}

@Component("paypal")
class PayPalGateway implements PaymentGateway {
    public void pay(double amount) {
        System.out.println("Paying $" + amount + " using PayPal.");
    }
}

// ---------------------------------------------------------
// 3. CONSUMERS
// ---------------------------------------------------------

@Component
class OrderService {

    private final PaymentGateway paymentGateway;

    // Spring sees 2 beans (CreditCard, PayPal).
    // Because CreditCard is @Primary, it picks that one automatically.
    @Autowired
    public OrderService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public void processOrder() {
        System.out.println("OrderService: Processing order...");
        paymentGateway.pay(100.0);
    }
}

@Component
class SupportService {

    private final PaymentGateway paymentGateway;

    // We specifically want PayPal here, ignoring the @Primary CreditCard.
    // @Qualifier takes precedence over @Primary.
    @Autowired
    public SupportService(@Qualifier("paypal") PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public void processRefund() {
        System.out.println("SupportService: Processing refund...");
        paymentGateway.pay(50.0);
    }
}

// ---------------------------------------------------------
// CONFIGURATION (Boilerplate to make this standalone runnable)
// ---------------------------------------------------------
@Configuration
@ComponentScan(basePackages = "com.springannotations.qualifierprimary")
class AppConfig {
    // ComponentScan will find the @Component classes above
}

*/
