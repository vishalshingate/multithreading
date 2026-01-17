# Spring Annotations: Controller vs RestController & Service vs Repository

## 1. @Controller vs @RestController

Yes, your formula is correct!

```java
@RestController = @Controller + @ResponseBody
```

### Breakdown
1.  **`@Controller`**
    *   **Goal:** Primarily used for **MVC** (Model-View-Controller) apps where you return a "View" (like JSP, Thymeleaf, HTML).
    *   **Behavior:** The return value of the method is interpreted as the **name of a file** to load.
    *   **Example:** `return "home";` -> Looks for `home.html` or `home.jsp`.

2.  **`@ResponseBody`**
    *   **Goal:** Tells Spring "Do not look for a file. Just take the return data, convert it to JSON/XML, and write it directly to the HTTP response."

3.  **`@RestController`**
    *   **Goal:** Used for **REST APIs** (Microservices).
    *   **Behavior:** Implicitly adds `@ResponseBody` to every single method in the class. It returns Data (JSON), not Views.

### Code Comparison

#### Traditional Controller (MVC)
```java
@Controller
public class WebController {
    
    @GetMapping("/hello")
    public String sayHello() {
        return "welcome"; // Looks for "welcome.html"
    }

    // If you want JSON here, you MUST add @ResponseBody manually
    @GetMapping("/api/hello")
    @ResponseBody
    public String sayApiHello() {
        return "Hello World"; // Returns string "Hello World"
    }
}
```

#### RestController (API)
```java
@RestController
public class ApiController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello World"; // Automatically returns JSON/Text response.
                              // Does NOT look for "Hello World.html"
    }
}
```

---

## 2. Can we use @Service and @Repository interchangeably?

**Short Answer:**
*   **Technically:** YES (The code will run).
*   **Functionally:** NO (You lose key features).
*   **Standard Practice:** NEVER do it.

### Why do they seem interchangeable?
All three annotations (`@Controller`, `@Service`, `@Repository`) are essentially children of the generic **`@Component`** annotation.
*   Spring Component Scanning looks for `@Component`.
*   Since `@Service` and `@Repository` are meta-annotated with `@Component`, Spring finds them, creates beans, and puts them in the container.
*   So, if you put `@Service` on a DAO class, dependency injection (`@Autowired`) will still work.

### The Critical Differences

| Feature | `@Component` | `@Service` | `@Repository` |
| :--- | :--- | :--- | :--- |
| **Meaning** | Generic Bean | Business Logic | Data Access Layer |
| **Special Behavior** | None | None (Purely Semantic mostly) | **Exception Translation** 🚨 |

### The Secret Power of `@Repository`
The main reason we strictly use `@Repository` for DB classes is **Exception Translation**.

1.  Different DB drivers throw different low-level exceptions (e.g., `SQLException`, `HibernateException`, `JpaException`).
2.  Spring's `@Repository` includes a post-processor that catches these messy, technology-specific exceptions and re-throws them as **Spring's uniform `DataAccessException` hierarchy** (unchecked exceptions).
    *   e.g., Catches `ORA-0001` (Oracle) -> Throws `DuplicateKeyException` (Spring).
3.  **If you use `@Service` on a DAO:** You lose this translation. You will have to handle raw `SQLExceptions` manually in your business layer, making your code dependent on the DB implementation.

### Summary
1.  **Use `@Service`** for business logic (semantics).
2.  **Use `@Repository`** for database interactions (semantics + **Exception Translation**).
3.  **Use `@Component`** for utility classes that don't fit the other layers.

