# SOLID Principles in Spring Boot

This guide explains how to apply the **SOLID** principles in a Spring Boot application, providing real-world examples suitable for technical interviews.

## 1. Single Responsibility Principle (SRP)
**Definition:** A class should have one, and only one, reason to change. It should have a single responsibility.

**In Spring Boot context:**
We typically separate concerns into **Controller**, **Service**, and **Repository** layers.
- **Controller:** Handles HTTP requests/responses, payload validation.
- **Service:** Contains business logic.
- **Repository:** Handles database interactions.

**Example (Violation):**
A `CurrencyExchangeController` that calculates exchange rates, connects to the database, and generates the JSON response.

**Example (Correction):**
Splitting the responsibilities.

```java
// 1. Controller: ONLY handles HTTP request mapping and response structure
@RestController
@RequestMapping("/currency-exchange")
public class CurrencyExchangeController {
    
    private final CurrencyExchangeService service;

    public CurrencyExchangeController(CurrencyExchangeService service) {
        this.service = service;
    }

    @GetMapping("/from/{from}/to/{to}")
    public ExchangeValue retrieveExchangeValue(@PathVariable String from, @PathVariable String to) {
        return service.calculateExchangeValue(from, to);
    }
}

// 2. Service: ONLY handles business logic
@Service
public class CurrencyExchangeService {
    
    private final CurrencyExchangeRepository repository;

    public CurrencyExchangeService(CurrencyExchangeRepository repository) {
        this.repository = repository;
    }

    public ExchangeValue calculateExchangeValue(String from, String to) {
        ExchangeValue exchangeValue = repository.findByFromAndTo(from, to, "USD"); // ... logic
        if (exchangeValue == null) {
            throw new RuntimeException("Unable to find data");
        }
        return exchangeValue;
    }
}

// 3. Repository: ONLY handles data access
public interface CurrencyExchangeRepository extends JpaRepository<ExchangeValue, Long> {
    ExchangeValue findByFromAndTo(String from, String to);
}
```

---

## 2. Open/Closed Principle (OCP)
**Definition:** Software entities (classes, modules, functions, etc.) should be open for extension, but closed for modification.

**In Spring Boot context:**
Use **Interfaces** and **Polymorphism** (often with the Strategy Pattern) to allow adding new behaviors without changing existing code.

**Example (Real World - Discount/Fees Calculation):**
Suppose you have a `BankService` that calculates transaction fees. If you hardcode the logic with `if-else` for every bank type, you modify the class every time a new bank is added (Violation).

**Correction:**

```java
// The Abstraction
public interface FeeStrategy {
    BigDecimal calculateFee(BigDecimal amount);
    String getBrandName();
}

// Strategy 1: Standard Bank
@Component
public class StandardFeeStrategy implements FeeStrategy {
    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(new BigDecimal("0.01"));
    }
    @Override
    public String getBrandName() { return "STANDARD"; }
}

// Strategy 2: Premium Bank (Added later without changing core logic)
@Component
public class PremiumFeeStrategy implements FeeStrategy {
    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(new BigDecimal("0.005")); // Lower fee
    }
    @Override
    public String getBrandName() { return "PREMIUM"; }
}

// The Usage (Closed for modification)
@Service
public class TransactionService {
    
    private final Map<String, FeeStrategy> strategies;

    // Spring auto-injects all implementations into a Map
    public TransactionService(List<FeeStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(FeeStrategy::getBrandName, Function.identity()));
    }

    public BigDecimal processTransaction(BigDecimal amount, String accountType) {
        FeeStrategy strategy = strategies.get(accountType);
        return strategy.calculateFee(amount);
    }
}
```
*To add a "VIP" account type, you simply create a new class implementing `FeeStrategy`. You do NOT touch `TransactionService`.*

---

## 3. Liskov Substitution Principle (LSP)
**Definition:** Objects of a superclass should be replaceable with objects of its subclasses without breaking the application.

**In Spring Boot context:**
If you have an interface, any implementation of it should behave correctly when swapped in. Do not throw `UnsupportedOperationException` for methods defined in the interface.

**Example (Violation):**
An interface `AccountService` has `withdraw()` and `deposit()`. A `FixedDepositAccount` implements it but throws an error for `withdraw()` because withdrawals aren't allowed before maturity.

**Correction:**
Use a hierarchy that separates capabilities.

```java
// Base interface
public interface Account {
    void deposit(BigDecimal amount);
}

// Interface for accounts that allow withdrawal
public interface WithdrawableAccount extends Account {
    void withdraw(BigDecimal amount);
}

@Service
public class SavingsAccount implements WithdrawableAccount {
    public void deposit(BigDecimal amount) { /*...*/ }
    public void withdraw(BigDecimal amount) { /*...*/ }
}

@Service
public class FixedDepositAccount implements Account {
    public void deposit(BigDecimal amount) { /*...*/ }
    // No withdraw method, so LSP is not violated by having a dummy/throwing method.
}

// Client code
public class TransferService {
    public void transfer(WithdrawableAccount from, Account to, BigDecimal amount) {
        from.withdraw(amount); // We know this is safe
        to.deposit(amount);
    }
}
```

---

## 4. Interface Segregation Principle (ISP)
**Definition:** Clients should not be forced to depend on interfaces they do not use.

**In Spring Boot context:**
Avoid "God Interfaces" (fat interfaces). Break them down into smaller, specific interfaces.

**Example (Real World - Reporting):**
Suppose you have a `ReportGenerator` interface.

```java
// Violation: Fat Interface
public interface ReportGenerator {
    void generatePdf();
    void generateExcel(); // Not all reports support Excel
    void generateHtml();
}

public class TransactionReport implements ReportGenerator {
    public void generatePdf() { /* logic */ }
    public void generateExcel() { /* logic */ }
    public void generateHtml() { /* logic */ }
}

public class AuditLogReport implements ReportGenerator {
    public void generatePdf() { /* logic */ }
    
    // Audit logs are text-only, Excel makes no sense here
    public void generateExcel() { 
        throw new UnsupportedOperationException(); // Violation of ISP (and LSP)
    } 
    public void generateHtml() { /* logic */ }
}
```

**Correction:**
Split the interfaces.

```java
public interface PdfReportable {
    void generatePdf();
}

public interface ExcelReportable {
    void generateExcel();
}

@Service
public class AuditLogReport implements PdfReportable { // implements only what it needs
    public void generatePdf() { /* logic */ }
}
```

---

## 5. Dependency Inversion Principle (DIP)
**Definition:** High-level modules should not depend on low-level modules. Both should depend on abstractions.

**In Spring Boot context:**
This is the core of Spring's Dependency Injection (DI). We inject **Interfaces** (Abstractions) rather than concrete classes.

**Example (Violation):**
A `PaymentService` that instantiates a specific `PayPalProcessor`.

```java
@Service
public class PaymentService {
    // Hard dependency on concrete class - High coupling
    private final PayPalProcessor processor = new PayPalProcessor(); 

    public void pay(BigDecimal amount) {
        processor.sendPayment(amount);
    }
}
```

**Correction (Spring DI):**
Depend on `PaymentProcessor` interface.

```java
// 1. Abstraction
public interface PaymentProcessor {
    void sendPayment(BigDecimal amount);
}

// 2. Low-level module implementation
@Component
public class PayPalProcessor implements PaymentProcessor {
    public void sendPayment(BigDecimal amount) {
        System.out.println("Applying Paypal Logic...");
    }
}

// 3. High-level module (Service) - Depends on Abstraction
@Service
public class PaymentService {
    
    private final PaymentProcessor paymentProcessor;

    // Spring injects the implementation (Inversion of Control)
    public PaymentService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    public void pay(BigDecimal amount) {
        paymentProcessor.sendPayment(amount);
    }
}
```
*Benefits: You can easily switch `PayPalProcessor` with `StripeProcessor` or `MockPaymentProcessor` (for testing) without changing `PaymentService`.*
