# Guide: Optimizing Spring Boot Startup Time

Spring Boot applications can sometimes take a while to start, especially as they grow. Here are the most effective strategies to reduce startup time for both **local development** and **production**.

---

## 1. Lazy Initialization (The Biggest Impact)
By default, Spring creates all beans at startup. Enabling lazy initialization means beans are created **only when needed** (on first HTTP request).

### How to Enable
Add to `application.properties`:
```properties
spring.main.lazy-initialization=true
```

**Pros:**
*   Drastically reduces startup time (can be 50%+ faster).
*   Great for local development and testing.

**Cons:**
*   Errors in bean configuration (e.g., missing dependencies) won't be caught until runtime (first request).
*   Slight latency on the very first request to an endpoint.

---

## 2. Spring Context Indexer (Avoid Classpath Scanning)
By default, Spring scans your classpath at startup to find components (`@Component`, `@Service`, etc.). As the project grows, this scanning becomes slow.

The **Context Indexer** creates a metadata file (`META-INF/spring.components`) at compile time. Spring reads this file instead of scanning the classpath.

### How to Enable
Add this dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context-indexer</artifactId>
    <optional>true</optional>
</dependency>
```
Rebuild your project (`mvn clean compile`). Spring will automatically detect the index.

---

## 3. Exclude Unused Auto-Configurations
Spring Boot auto-configures features based on what's on the classpath. Sometimes you might have a library (e.g., RabbitMQ, Redis) but you aren't using it yet.

### How to Diagnose
Run with debug enabled:
```bash
java -jar myapp.jar --debug
```
Look for "Positive matches" in the logs.

### How to Fix
Explicitly exclude unnecessary configurations in your main class or `application.properties`.

```java
@SpringBootApplication(exclude = {
    RabbitAutoConfiguration.class, 
    RedisAutoConfiguration.class
})
public class Application { ... }
```
Or in properties:
```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
```

---

## 4. JVM Tuning for Development (Tiered Compilation)
The JVM uses JIT compilation to optimize code. By default, it aggressively optimizes for peak performance, which slows down startup.

For **local development**, you can tell the JVM to stop optimizing early.

### VM Argument
Add this to your IDE run configuration:
```bash
-XX:TieredStopAtLevel=1
```
or
```bash
-noverify -XX:TieredStopAtLevel=1
```

**Impact:**
*   Startup time improves significantly (can be 2-5x faster).
*   **Warning:** Runtime performance will be worse. Do NOT use this in Production.

---

## 5. Hibernate/JPA Optimization
Schema validation and DDL generation can be slow.

### Production Settings
Ensure you are not automatically updating the schema in prod:
```properties
# Good for Prod
spring.jpa.hibernate.ddl-auto=validate
# avoid this in Prod (slow and dangerous)
spring.jpa.hibernate.ddl-auto=update 
```

### Disable Validation on Startup
If you trust your schema, you can disable startup validation:
```properties
spring.jpa.properties.hibernate.validator.apply_to_ddl=false
```

---

## 6. Exploded JARs (Unpacking)
Running a Spring Boot application from a fat JAR (nested JARs) requires extracting files to a temporary location at runtime, which costs CPU and disk I/O.

### Solution
Unpack the JAR and run the classes directly. This is common in containerized (Docker) environments.

```bash
# Instead of java -jar app.jar
mkdir target/dependency
cd target/dependency
jar -xf ../*.jar
java -cp .:BOOT-INF/classes:BOOT-INF/lib/* com.example.MyApplication
```
This is often included in "Layered Jar" Docker builds.

---

## 7. Class Data Sharing (CDS) - Advanced (Spring Boot 3.3+)
CDS allows the JVM to share class metadata between processes, skipping the parsing of classes at startup.

### Steps (Simplified)
1.  **Training Run:** Run the app to generate a cache.
    ```bash
    java -XX:ArchiveClassesAtExit=application.jsa -jar app.jar
    ```
2.  **Run with Cache:**
    ```bash
    java -XX:SharedArchiveFile=application.jsa -jar app.jar
    ```
This creates a massive boost in startup time for repeated runs (e.g., stopping/starting in Kubernetes).

---

## Summary Checklist

| Strategy | Ideal For | Effort | Impact |
| :--- | :--- | :--- | :--- |
| **Lazy Init** | Local Dev, Non-Critical Prods | Low | ⭐⭐⭐⭐⭐ |
| **Context Indexer** | Large Monoliths | Low | ⭐⭐⭐ |
| **Exclude Config** | Bloated Classpaths | Medium | ⭐⭐⭐ |
| **-XX:TieredStopAtLevel=1** | **Local Dev ONLY** | Low | ⭐⭐⭐⭐ |
| **CDS / GraalVM** | Production / Serverless | High | ⭐⭐⭐⭐⭐ |

