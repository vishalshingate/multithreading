# Spring Boot Internals: What happens inside `SpringApplication.run()`?

When you write this simple line of code:

```java
SpringApplication.run(SpringDemoApplication.class, args);
```

You are triggering a complex sequence of ~15 steps. Here is the simplified breakdown of what happens internally, step-by-step.

---

## 1. The Setup (Before execution starts)
Even before the line executes, Spring checks:
- **Application Type**: Is this a Web Application (Servlet)? A Reactive Application? or a Standalone CLI app? It decides this by checking if specific classes (like `DispatcherServlet`) are present on the classpath.
- **Bootstrappers**: It loads initializers and listeners defined in `META-INF/spring.factories` (or `spring.factories` + `imports` in Boot 3).

---

## 2. The `run()` Method Execution Flow

### Step A: Start the Stopwatch
Spring starts a `StopWatch`. This is purely to calculate those seconds you see in the logs: *"Started Application in 2.43 seconds"*.

### Step B: Prepare the Environment
1. **Load Properties**: It gathers all configuration from:
   - System Environment Variables.
   - Java System Properties.
   - Command Line Arguments (`args`).
   - `application.properties` / `application.yml` files.
2. **Active Profiles**: It decides which profile is active (e.g., `dev`, `prod`).

### Step C: Create the ApplicationContext (The "Container")
The "Context" is the brain of your application where all Beans live.
- If it's a **Web App**, it creates an `AnnotationConfigServletWebServerApplicationContext`.
- If it's **Reactive**, it creates a `ReactiveWebServerApplicationContext`.
- Otherwise, a generic `AnnotationConfigApplicationContext`.

### Step D: Prepare the Context
Before creating beans, Spring needs to configure the empty container:
1. It links the **Environment** (Step B) to the Context.
2. It registers your main class (`SpringDemoApplication`) as the **first Bean** in the factory. This is crucial because this class has the `@SpringBootApplication` annotation, which triggers everything else.

### Step E: Refresh the Context (The Heavy Lifting)
This is where 90% of the work happens. The method is called `refresh()`.

1. **Scan and Parse (Order Matters!)**:
   - **First**: It processes **`@ComponentScan`**. It finds all your user-defined beans (`@Controller`, `@Service`, `@Repository`) and registers their definitions.
   - **Second**: It processes **`@EnableAutoConfiguration`**. It looks at the classpath and tries to add default beans.
   - **Why this order?**: Auto-configuration heavily uses `@ConditionalOnMissingBean`. It needs to see if *you* created a bean first. If you did (via Component Scan), auto-config skips creating its default version.
2. **Bean Destinition Loading**: It doesn't create objects yet; it creates "definitions" (plans) for every bean it found.
3. **Web Server Startup**:
   - If it's a web app, Spring creates and starts the Embedded Web Server (Tomcat, Jetty, or Undertow).
   - This happens *during* the refresh.
4. **Instantiate Singletons**:
   - Now it actually **creates the objects** (Instances).
   - It performs **Dependency Injection** (injecting Service into Controller, Repository into Service).
   - It runs `@PostConstruct` methods.

### Step F: Finish Up
1. **StopWatch Stop**: Timer ends.
2. **Log Startup**: Prints the banner and the "Started..." log.

### Step G: Run Runners
If you have any beans that implement `CommandLineRunner` or `ApplicationRunner`, their `run()` methods are executed **now**, after the context is fully ready.

### Deep Dive: Runners (Executing code after startup)
Spring Boot provides two interfaces to run specific code **once** right after the application context is loaded and the app has started.

1. **`CommandLineRunner`**
   - **Method**: `void run(String... args)`
   - **Input**: Receives raw command line arguments as a String array (just like `main` method).
   - **Use Case**: Simple tasks where you don't need to parse complex flags (e.g., `--server.port=8080`).

2. **`ApplicationRunner`**
   - **Method**: `void run(ApplicationArguments args)`
   - **Input**: Receives an `ApplicationArguments` object which separates "option arguments" (starting with `--`) from "non-option arguments".
   - **Use Case**: Better when you need to parse flags like `--mode=import` vs `filename.txt`.

**Ordering**: If you have multiple runners, use `@Order(1)`, `@Order(2)` to control execution sequence.

---

## Summary Diagram

```mermaid
graph TD
    Start((Start)) --> StopWatch[1. Start StopWatch]
    StopWatch --> Env[2. Prepare Environment]
    Env --> CreateCtx{3. Create Context}
    
    CreateCtx -- Web App --> WebCtx[ServletWebServerApplicationContext]
    CreateCtx -- Standard --> StdCtx[AnnotationConfigApplicationContext]
    
    WebCtx & StdCtx --> Register[4. Register Main Class]
    Register --> Refresh[5. REFRESH CONTEXT]
    
    subgraph Refresh_Cycle [Inside Context Refresh]
    direction TB
        Refresh --> Scan[A. Scan & Import AutoConfig]
        Scan --> ServerCreate["B. Create Web Server Tomcat Object"]
        ServerCreate --> DI[C. Instantiate Singletons & Dependency Injection]
        DI --> ServerStart["D. Start Web Server Bind Port 8080"]
    end
    
    Refresh_Cycle --> Finalize[6. Stop StopWatch]
    Finalize --> Runners[7. Call Runners]
    Runners --> End((Ready))

    style Refresh_Cycle fill:#f9f9f9,stroke:#333,stroke-dasharray: 5 5
    style ServerCreate fill:#e1f5fe
    style ServerStart fill:#e1f5fe
```

---

## Why does this matter for interviews?
1. **"When does Tomcat start?"**: It starts during the context refresh step, specifically when the `ServletWebServerFactory` bean is requested.
2. **"Why is my CommandLineRunner null?"**: It can't be null if it runs, but if dependencies inside it are null, maybe the context didn't initialize them correctly.
3. **"How does Spring know to look in my package?"**: Because your main class is registered first, and `@SpringBootApplication` includes `@ComponentScan` which defaults to the main class's package.
