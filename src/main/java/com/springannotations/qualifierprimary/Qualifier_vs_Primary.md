# @Primary vs @Qualifier in Spring

Both annotations handle the **"Multiple Bean Injection Problem"**:
*   *Scenario:* You have an interface `PaymentGateway` and two implementations: `CreditCardGateway` and `PayPalGateway`.
*   *Problem:* When you `@Autowired PaymentGateway`, Spring doesn't know which one to inject and throws a `NoUniqueBeanDefinitionException`.

---

## 1. @Primary
**"The Default Choice"**

*   **Definition:** Marks a bean as the **default** candidate to be injected when multiple candidates are present.
*   **Usage:** Only **one** bean of a specific type can have `@Primary`.
*   **Behavior:** If Spring finds multiple beans, it looks for the one with `@Primary`. If found, it injects it. If not found, it throws an error.
*   **Use Case:** When 90% of your application uses a `CreditCardService`, but only one specific corner case uses `PayPalService`. Marking `CreditCardService` as `@Primary` saves you from adding `@Qualifier` everywhere.

```java
@Component
@Primary // Default choice
public class CreditCardGateway implements PaymentGateway { ... }

@Service
public class OrderService {
    // Injects CreditCardGateway automatically
    @Autowired
    private PaymentGateway gateway; 
}
```

---

## 2. @Qualifier("name")
**"The Specific Choice"**

*   **Definition:** Used at the **injection point** to specify exactly which bean name/ID you want.
*   **Usage:** Can be used on fields, constructor arguments, or setter methods.
*   **Behavior:** It overrides `@Primary`. Even if a bean is `@Primary`, if you specify `@Qualifier("otherBean")`, Spring will inject "otherBean".
*   **Use Case:** When you need finer control to select a specific implementation in a specific class.

```java
@Component("paypal") // Bean Name
public class PayPalGateway implements PaymentGateway { ... }

@Service
public class SupportService {
    // Specifically asks for "paypal", ignoring any @Primary
    @Autowired
    @Qualifier("paypal")
    private PaymentGateway gateway; 
}
```

---

## 3. Comparison Table

| Feature | @Primary | @Qualifier |
| :--- | :--- | :--- |
| **Location** | On the **Bean Class** (Implementation). | On the **Injection Point** (Constructor/Field). |
| **Purpose** | Defines a **Global Default**. | Defines a **Local Selection**. |
| **Precendence** | Lower. | Higher (First priority). |
| **Frequency** | Once per interface type. | Many times (wherever needed). |

---

## 4. Design Pattern: The Strategy Pattern

`@Qualifier` and `@Primary` are heavily used to implement the **Strategy Design Pattern** in Spring.

### How it maps?
1.  **Strategy Interface:** `PaymentGateway`
2.  **Concrete Strategies:** `CreditCardGateway`, `PayPalGateway`
3.  **Context:** The classes injecting the interface (e.g., `OrderService`).
4.  **Selection:**
    *   Spring acts as the **Strategy Factory**.
    *   `@Qualifier` acts as the **Selector** (or Key) to pick the right strategy at runtime/startup time.

### Example Scenario
Imagine a Notification System:
*   **Interface:** `NotificationService`
*   **Implementations:** `EmailService`, `SmsService`, `PushNotificationService`.
*   **Requirement:**
    *   By default, send Emails (`@Primary` on `EmailService`).
    *   But for "Urgent Alerts", send SMS.
    *   **Code:**
        ```java
        class AlertController {
             // Injects EmailService (Default)
             @Autowired NotificationService defaultService; 
             
             // Injects SmsService (Urgent)
             @Autowired @Qualifier("sms") NotificationService urgentService; 
        }
        ```

