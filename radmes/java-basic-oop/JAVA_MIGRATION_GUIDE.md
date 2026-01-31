# Java Migration Guide

This guide provides steps and considerations for migrating Java applications across major versions.

---

## 1. Migrating from Java 8 to Java 11

Java 11 was the first Long Term Support (LTS) release after the massive changes in Java 9 (Modular System).

### Key Challenges
*   **Modular System (Jigsaw):** While you can still run apps on the classpath, some internal APIs are now restricted.
*   **Removed Java EE Modules:** Modules like JAXB, JAX-WS, and JTA were removed from the JDK. You must add them as external dependencies.
*   **Garbage Collector:** G1 is now the default garbage collector.

### Migration Steps
1.  **Update Build Tools:** Ensure Maven (3.5+) or Gradle (5.0+) is updated.
2.  **Add Missing Dependencies:** If your project uses JAXB or other removed EE modules, add them to your `pom.xml` or `build.gradle`:
    ```groovy
    implementation 'javax.xml.bind:jaxb-api:2.3.1'
    implementation 'org.glassfish.jaxb:jaxb-runtime:2.3.1'
    ```
3.  **Update Compiler Plugins:** Set `sourceCompatibility` and `targetCompatibility` to 11.
4.  **Check for Deprecated APIs:** Use the `jdeps` tool to find dependencies on internal JDK APIs.
5.  **JVM Options:** Some old flags may be removed or ignored. Check your startup scripts.

---

## 2. Migrating from Java 11 to Java 21

Java 21 is a significant LTS release bringing many features from Project Loom (Concurrency) and Project Amber (Syntax).

### Key Features and Changes
*   **Virtual Threads:** Lightweight threads that significantly reduce the overhead of high-concurrency applications.
*   **Pattern Matching for Switch:** Simplifies complex conditional logic.
*   **Record Classes:** Concise syntax for data-only classes.
*   **Sealed Classes/Interfaces:** Provides better control over inheritance hierarchies.

### Migration Steps
1.  **Update Frameworks:** Ensure Spring Boot (3.2+) or other frameworks are updated to versions that support Java 21.
2.  **Update Tooling:** Gradle 8.4+ or Maven 3.9+ is recommended for Java 21.
3.  **Language Level:** Update your build configuration:
    ```groovy
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
    ```
4.  **Refactor for Virtual Threads:** If using Spring Boot 3.2+, you can enable virtual threads by setting:
    ```properties
    spring.threads.virtual.enabled=true
    ```
5.  **Use Records for DTOs:** Replace verbose Lombok or manual DTOs with `record` types.
6.  **Switch Expressions:** Refactor `if-else` chains or old `switch` statements to use the new switch expressions and pattern matching.

---

## General Migration Tips
*   **Test Extensively:** Run your full test suite at each step.
*   **Update Dependencies:** External libraries (Hibernate, Jackson, etc.) often need updates to work with newer JDKs.
*   **Check Docker Images:** Update your `Dockerfile` base images (e.g., from `openjdk:8` to `eclipse-temurin:21`).

