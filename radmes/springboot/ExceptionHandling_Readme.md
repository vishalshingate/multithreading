# Exception Handling in Spring Boot

## 1. Detailed Flow of Exception Handling
When an exception occurs in a Spring Boot application, the following flow is typically followed:

1.  **Exception Thrown:** An exception is thrown during the processing of a web request, usually inside a `@Controller` or `@RestController` method.
2.  **Local @ExceptionHandler:** Spring first looks for an `@ExceptionHandler` method defined within the *same* Controller class where the exception occurred.
3.  **Controller Advice (@ControllerAdvice):** If no local handler is found, Spring searches for any classes annotated with `@ControllerAdvice` or `@RestControllerAdvice`. It checks if any of these classes have an `@ExceptionHandler` method that matches the thrown exception type.
4.  **ResponseStatusException / @ResponseStatus:** If no `@ExceptionHandler` is found, Spring checks if the exception itself is annotated with `@ResponseStatus` or if it is a `ResponseStatusException`. If so, it uses the provided status and message.
5.  **Default Error Handling (BasicErrorController):** If no custom handler is found, the exception is propagated to the servlet container. Spring Boot's `BasicErrorController` (registered by default) then takes over.
    - If the request is from a browser, it usually renders a "Whitelabel Error Page" (HTML).
    - If the request is from a REST client (like Postman), it returns a JSON response with details like timestamp, statuscode, and error message.

## 2. @ControllerAdvice vs @RestControllerAdvice
- **`@ControllerAdvice`**: Is a specialization of `@Component` that allows handling exceptions across the whole application in one global handling component. It can be used with both traditional MVC controllers (returning views) and REST controllers.
- **`@RestControllerAdvice`**: Is a convenience annotation that combines `@ControllerAdvice` and `@ResponseBody`. This means that the return value of the methods within the class will be automatically serialized into the response body (typically JSON), which is perfect for RESTful APIs.

**Why do we need them?**
- **Centralized Handling:** Avoid repeating try-catch blocks in every controller.
- **Consistency:** Ensure that all error responses follow the same structure across the entire API.
- **Clean Code:** Keeps business logic in controllers separate from error handling logic.

## 3. Interview Questions and Answers

**Q1: What is the purpose of `@ExceptionHandler`?**
**A1:** It is used to define a method that handles specific exceptions thrown by controller methods. It can return a custom response body and HTTP status code.

**Q2: How does `@ResponseStatus` work?**
**A2:** It allows you to mark an exception class with a specific HTTP status code (e.g., `404 Not Found`). When that exception is thrown, Spring automatically returns that status code to the client.

**Q3: Can we have multiple `@ControllerAdvice` classes?**
**A3:** Yes. You can use the `basePackages` or `assignableTypes` attributes to limit their scope, and use `@Order` to define the precedence if they handle the same exceptions.

**Q4: What is `ResponseEntityExceptionHandler`?**
**A4:** It is a convenient base class for `@ControllerAdvice` classes. It provides methods that you can override to handle standard Spring MVC exceptions (like `MethodArgumentNotValidException`, `NoHandlerFoundException`, etc.) and provide a custom response body.

## 4. Scenario-Based Questions

**Scenario 1: You want to return a specific JSON structure for all validation errors. How would you do it?**
**Answer:** I would create a `@RestControllerAdvice` class extending `ResponseEntityExceptionHandler`. I would override the `handleMethodArgumentNotValid` method to extract errors from the `BindingResult` and return them in my custom `ErrorDetails` format.

**Scenario 2: An exception occurs in a Filter. Will `@ControllerAdvice` catch it?**
**Answer:** No. `@ControllerAdvice` only catches exceptions thrown by the `DispatcherServlet` (Controllers). Filters are executed before the `DispatcherServlet`. To handle filter exceptions, you need to handle them within the filter itself or use a custom `ErrorController`.

**Scenario 3: You have a local `@ExceptionHandler` and a global `@ControllerAdvice` for the same exception. Which one wins?**
**Answer:** The local `@ExceptionHandler` in the Controller takes precedence over the global `@ControllerAdvice`.

---
*Created on: 2026-02-01*
