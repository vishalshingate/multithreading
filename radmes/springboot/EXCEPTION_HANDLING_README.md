# Spring Boot Exception Handling Guide

This document explains the exception handling mechanism in Spring Boot, the role of ControllerAdvice, and provides interview resources.

## 1. Exception Handling Flow in Spring Boot

When an exception occurs in a Spring Boot application (specifically within the Web layer), the flow typically proceeds as follows:

1.  **Request Initiation**: A client sends an HTTP request to an endpoint (e.g., `GET /users/1`).
2.  **DispatcherServlet**: The `DispatcherServlet` receives the request and routes it to the appropriate Controller method.
3.  **Controller Execution**: The Controller method attempts to execute the business logic (calling Services, Repositories).
4.  **Exception Thrown**: If an error occurs (e.g., User not found), an Exception is thrown (e.g., `UserNotFoundException`).
5.  **Exception Resolution**:
    *   Spring checks if there is a local method annotated with `@ExceptionHandler` inside the Controller itself.
    *   If not found, it checks for a global Exception Handler class annotated with `@ControllerAdvice` or `@RestControllerAdvice`.
6.  **Global Handler (ControllerAdvice)**:
    *   The `DispatcherServlet` delegates the exception to the `@ControllerAdvice` bean.
    *   The class scans for a method annotated with `@ExceptionHandler` that matches the thrown exception type.
    *   Example: A method annotated with `@ExceptionHandler(UserNotFoundException.class)` will catch `UserNotFoundException`.
7.  **Response Creation**:
    *   The handler method constructs a custom error response object (e.g., `ErrorDetails` containing timestamp, message, details).
    *   It returns a `ResponseEntity` containing the error object and the appropriate HTTP Status Code (e.g., 404 Not Found).
8.  **Response to Client**: The `ResponseEntity` is converted to JSON (by Jackson) and sent back to the client.

### Diagrammatic Flow
```
Client Request -> DispatcherServlet -> Controller -> Service (Throw Exception)
                                          |
                                          v
                                    DispatcherServlet (Catches Exception)
                                          |
                                          v
                                    @ControllerAdvice / @RestControllerAdvice
                                          |
                                          v
                                    @ExceptionHandler Method (Constructs Error Response)
                                          |
                                          v
                                    Client Response (JSON Error Details)
```

---

## 2. ControllerAdvice vs RestControllerAdvice

These annotations are used to define a global exception handler or global data binding/model attributes.

### Why do we need them?
*   **Centralized Handling**: Instead of using try-catch blocks in every controller method or repeating `@ExceptionHandler` in every controller, we can handle exceptions in one place.
*   **Consistency**: Ensures a consistent error response structure across the entire API.
*   **Separation of Concerns**: optionally separates error handling logic from successful business logic.

### Differences

| Feature | @ControllerAdvice | @RestControllerAdvice |
| :--- | :--- | :--- |
| **Definition** | It is a specialization of the `@Component` annotation. It allows you to handle exceptions across the whole application in one global handling component. | It is a convenience annotation that combines `@ControllerAdvice` and `@ResponseBody`. |
| **Response Body** | Methods returning data need to be explicitly annotated with `@ResponseBody` if you want to return JSON/XML directly. Typically used with MVC views. | Every method is assumed to return the response body (JSON/XML) by default. You do **not** need `@ResponseBody` on individual methods. |
| **Use Case** | Traditional Spring MVC applications (returning Views/JSP/Thymeleaf) or when you need flexibility. | RESTful Web Services where the response is always data (JSON/XML). |

**Summary**: Use `@RestControllerAdvice` for REST APIs to save typing `@ResponseBody` on every handler method. Use `@ControllerAdvice` if you are building an application with server-side rendering (Views).

---

## 3. Interview Questions and Answers

**Q1: What is the default exception handling mechanism in Spring Boot?**
**A:** Spring Boot provides a default `/error` mapping. When an exception occurs that isn't handled, it forwards the request to `/error`. By default, it returns a JSON response with fields like `timestamp`, `status`, `error`, `message`, and `path` for machine clients, and a "Whitelabel Error Page" for browser clients.

**Q2: How do you handle a specific exception in Spring Boot?**
**A:** By creating a method and annotating it with `@ExceptionHandler(SpecificException.class)`. This can be done inside a specific Controller (local scope) or in a `@ControllerAdvice` class (global scope).

**Q3: What corresponds to 404 Not Found in Spring MVC exception handling?**
**A:** `NoHandlerFoundException` occurs if no handler is found for the request. However, by default, Spring Boot sends a 404 response without throwing this exception. To handle it globally, you must set `spring.mvc.throw-exception-if-no-handler-found=true` and `spring.web.resources.add-mappings=false`.

**Q4: Can we return a custom status code with `@ExceptionHandler`?**
**A:** Yes. You can use `@ResponseStatus(HttpStatus.BAD_REQUEST)` on the handler method or return a `ResponseEntity` object where you specify the `HttpStatus` manually (e.g., `return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);`).

**Q5: What is the purpose of extending `ResponseEntityExceptionHandler`?**
**A:** A custom advice class often extends `ResponseEntityExceptionHandler`. This base class provides default handling for standard Spring MVC exceptions (like `MethodArgumentNotValidException` for validation errors, `HttpRequestMethodNotSupportedException`, etc.). By extending it, you can override specific methods to customize the response for these standard errors while keeping the rest working by default.

---

## 4. Scenario-Based Interview Questions

**Scenario 1: Validation Handling**
*   **Scenario:** "You have a User registration API. The user passes an invalid email format. How do you ensure the API returns a proper 400 Bad Request with a clear message like 'Email should be valid' instead of a generic server error?"
*   **Answer:**
    1.  Use specific Jakarta Validation annotations (like `@Email`, `@Size`) on the DTO fields.
    2.  Annotate the `@RequestBody` parameter in the controller with `@Valid`.
    3.  Create a global exception handler typically extending `ResponseEntityExceptionHandler`.
    4.  Override the `handleMethodArgumentNotValid` method.
    5.  Extract the validation message from `ex.getBindingResult()` and return a custom error structure with `HttpStatus.BAD_REQUEST`.

**Scenario 2: Resource Not Found**
*   **Scenario:** "When a client requests a user ID that doesn't exist, the application currently returns a 500 Internal Server Error with a NullPointerException stack trace. How do you fix this to return a 404?"
*   **Answer:**
    1.  In the Service layer, check if the ID exists.
    2.  If not, throw a custom custom exception: `UserNotFoundException("id-" + id)`.
    3.  Create an `@ExceptionHandler(UserNotFoundException.class)` in the `@ControllerAdvice` class.
    4.  The handler should return a `ResponseEntity` with `HttpStatus.NOT_FOUND` and a friendly error message.

**Scenario 3: Global Unexpected Errors**
*   **Scenario:** "Your API needs to be secure. You don't want to expose stack traces or internal DB errors (like SQLSyntaxErrorException) to the frontend client. How do you achieve this?"
*   **Answer:**
    1.  Implement a "catch-all" exception handler annotated with `@ExceptionHandler(Exception.class)` in the `@ControllerAdvice`.
    2.  This method will catch any exception not handled by more specific handlers.
    3.  Log the real stack trace internally for debugging.
    4.  Return a generic, safe response to the client (e.g., "An unexpected error occurred, please contact support") with `HttpStatus.INTERNAL_SERVER_ERROR`.

**Scenario 4: Validating Path Variables**
*   **Scenario:** "We have an endpoint `/users/{id}` where {id} must be numeric. A user calls `/users/abc` and gets a 400 Bad Request. How do we customize this message locally without affecting other controllers?"
*   **Answer:**
    1.  This triggers a `MethodArgumentTypeMismatchException`.
    2.  Since the requirement is *local* (without affecting other controllers), add an `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` method directly inside the `UserController` class, not in the global advice.
    3.  Return the custom message from there.

---

## 4. Checked vs. Unchecked Exceptions in the Real World

This is a critical distinction that separates "coding" from "engineering" in Java applications.

### 1. Checked Exceptions: "Force to Recover"
**Philosophy:** The compiler *forces* you to acknowledge that "this operation might fail, and you MUST have a Plan B." It represents **recoverable conditions** outside the program's control (Network, Disk, Database).

**Why it matters in Spring Boot?**
It prevents critical flows from silencing failures. It forces the developer to write contingency logic.

**Real Industry Example: The Payment Gateway Integration**
Imagine you are building a Fintech app integrated with Stripe/PayPal.
*   **The Scenario:** You call `paymentService.chargeCreditCard(details)`.
*   **The Problem:** The network might timeout, or the bank might decline.
*   **Checked Exception (`PaymentProcessingException`):**
    
    **Step 1: Create the Exception** (Must extend `Exception`, not `RuntimeException`)
    ```java
    public class PaymentProcessingException extends Exception {
        public PaymentProcessingException(String message) {
            super(message);
        }
        // Important to preserve the root cause
        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    ```

    **Step 2: Use it in Service Layer**
    ```java
    // Service Layer
    public void processOrder(Order order) throws PaymentProcessingException {
        try {
             externalGateway.charge(order.getAmount());
        } catch (IOException e) {
             // You are FORCED to handle this.
             // Plan B: Mark order as "PENDING_RETRY" and trigger a background job.
             throw new PaymentProcessingException("Gateway timeout", e); 
        }
    }
    ```

    **How do I make it compulsory? (The "Catch or Declare" Rule)**
    *   **The Magic:** Just by making your class extend `Exception` (instead of `RuntimeException`), you trigger the Java Compiler's enforcement.
    *   **If you remove `throws PaymentProcessingException`:** The code **will not compile**. The compiler will say: *"Unhandled exception type PaymentProcessingException"*.
    *   **The Result:** You created a contract. Anyone who calls `processOrder()` **MUST** now wrap it in a `try-catch` block or re-throw it. You have successfully "forced" them to handle the failure.
*   **The "Actual Difference":** If this was unchecked, a lazy developer might ignore it. The transaction would fail, the stack trace would vanish into logs, and the user would think the order is placed but no money was moved. Checked exceptions force the **Architecture to handle resilience**.

**Real Industry Example: File Processing (Regulatory Reports)**
*   **The Scenario:** Your app reads a daily CSV report from a shared drive to reconcile accounts.
*   **Checked Exception (`FileNotFoundException`, `IOException`):**
    *   The file might be locked by another process.
    *   The disk might be full.
    *   **Handling:** The compiler ensures you catch this. Your `catch` block should probably send an alert to the DevOps team (PagerDuty) or try a backup file path.

---

### 2. Unchecked Exceptions (RuntimeExceptions): "Fail Fast"
**Philosophy:** These represent **programming errors** or **irrecoverable system faults**. If these happen, the code is likely buggy (e.g., passing null) or the environment is broken (e.g., OutOfMemory). You generally *cannot* recover from these in the business logic flow.

**Importance:**
*   **Code Cleanliness:** You don't want `try-catch` blocks polluting every single method call for things you can't fix (like `NullPointerException`).
*   **Transaction Management:** In Spring, **Unchecked Exceptions trigger a Transaction Rollback by default**. Checked exceptions DO NOT.

**Real Industry Example: Data Integrity Validation**
*   **The Scenario:** A user tries to transfer money to a negative account ID.
*   **Unchecked Exception (`IllegalArgumentException`):**
    ```java
    public void transfer(String fromAccount, String toAccount, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive"); 
        }
        // ...
    }
    ```
*   **Why Unchecked?** There is no "Plan B" for the code to fix a negative amount. The *caller* (client) sent bad data. The request should simply fail immediately (fail-fast) and return a 400 Bad Request.

**How Unchecked Exceptions are Handled in Spring Boot:**

1.  **Transactional Boundaries (`@Transactional`):**
    *   By default, if an unchecked exception (`RuntimeException`) bubbles out of a `@Transactional` method, Spring marks the transaction for **rollback**.
    *   If a checked exception bubbles out, Spring **commits** the transaction (unless you specify `rollbackFor = Exception.class`).
    
    *   **Why this difference?** Spring assumes Check Exceptions are "business events" (like "InsufficientFunds") where you might still want to save the audit log of the failed attempt. Unchecked exceptions are "system crashes" where everything should be nuked to maintain consistency.

2.  **Global Exception Handling (`@ControllerAdvice`):**
    *   Unchecked exceptions bubble all the way up to the Controller.
    *   We use `@ControllerAdvice` to catch them globally and convert them into standard JSON error responses.

    ```java
    @RestControllerAdvice
    public class GlobalExceptionHandler {

        // Handle logical errors (Unchecked)
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<String> handleBadInput(IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }

        // Handle unexpected crashes (Unchecked)
        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<String> handleRuntime(RuntimeException ex) {
            // Log full stack trace for devs
            log.error("Unknown error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong");
        }
    }
    ```

### Summary Table

| Feature | Checked Exceptions | Unchecked Exceptions |
| :--- | :--- | :--- |
| **Java Parent** | `Exception` | `RuntimeException` |
| **Philosophy** | **Recoverable** contingency (Plan B required). | **Irrecoverable** error or Bug (Fix the code). |
| **Spring Transaction** | Does **NOT** rollback by default. | **DOES** rollback by default. |
| **Use Case** | External API failures, File IO, "Insufficient Funds". | Null pointers, Invalid Arguments, Database Down. |
| **Who Handles?** | The immediate caller (forced by compiler). | Often the Global Exception Handler (framework level). |
