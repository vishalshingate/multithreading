# Spring Boot Auto-Configuration (How It Works)

A practical walkthrough of how Spring Boot discovers, filters, and applies auto-configuration. Assumes Spring Boot 3.x (notes included for 2.x differences).

---

## Quick Mental Model
1) Collect auto-config classes from the classpath list. 2) Evaluate conditions against environment, classpath, and existing beans. 3) Import the passing configs; they contribute beans when not already provided by the user. 4) User code and properties override defaults.

### Startup Flow (ASCII)
```
SpringApplication.run()
  -> Prepare Environment (profiles, properties)
  -> Create ApplicationContext
  -> Process @SpringBootApplication
      -> AutoConfigurationImportSelector
          -> Load candidates from META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports (Boot 3)
             or META-INF/spring.factories (Boot 2)
          -> Evaluate @Conditional* on each candidate
          -> Order them (@AutoConfigureBefore/After/Order)
          -> Import passing configs (standard @Configuration classes)
  -> Refresh context, instantiate non-lazy singletons
  -> Application ready
```

### Condition Evaluation Highlights
- `@ConditionalOnClass` / `@ConditionalOnMissingClass`: gates by classpath.
- `@ConditionalOnBean` / `@ConditionalOnMissingBean`: gates by existing beans; enables user overrides.
- `@ConditionalOnProperty`: gates by configuration properties.
- `@ConditionalOnResource`, `@ConditionalOnWebApplication`, `@ConditionalOnExpression`, etc.: gates by resources/runtime type/SpEL.

### Ordering
- `@AutoConfigureAfter` / `@AutoConfigureBefore` / `@AutoConfigureOrder` coordinate sequencing (e.g., `JpaRepositoriesAutoConfiguration` after `DataSourceAutoConfiguration`).

### Bean Registration Pattern
- Auto-config classes are ordinary `@Configuration` classes with `@Bean` methods.
- Most beans are guarded with `@ConditionalOnMissingBean` so user-defined beans of the same type/name win.
- `@EnableConfigurationProperties` binds externalized properties onto POJOs that are injected into those beans.

### How Auto-Config Is Discovered
- Boot 3.x: list in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (one fully-qualified class per line).
- Boot 2.x: `META-INF/spring.factories` property `org.springframework.boot.autoconfigure.EnableAutoConfiguration`.

---

## Customizing or Disabling
- Exclude globally: property `spring.autoconfigure.exclude=com.example.FooAutoConfiguration` (comma-separated) or `@SpringBootApplication(exclude = FooAutoConfiguration.class)`.
- Opt-out of a module: many auto-configs expose a property via `@ConditionalOnProperty` (e.g., `spring.datasource.*`, `spring.jpa.*`).
- Override a bean: declare your own bean of the same type/name. In Boot 3, bean definition overriding is off by default; enable with `spring.main.allow-bean-definition-overriding=true` only if necessary (prefer type-based `@ConditionalOnMissingBean`).
- Narrow the import scope: use `@ImportAutoConfiguration` on a slice configuration (useful for tests).

---

## Inspecting What Happened
- Startup report: run with `--debug` or set `spring.main.log-startup-info=true` to log the condition evaluation report (matched/unmatched auto-configs and conditions).
- Actuator: `/actuator/conditions` (formerly `/actuator/autoConfig`) lists applied and skipped configs with reasons; `/actuator/beans` shows the bean graph.
- Classpath proof: check `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` files from your dependencies to see which auto-configs a starter brings in.

Handy command when running locally (swap your main class/jar as needed):
```powershell
mvn spring-boot:run -Dspring-boot.run.arguments="--debug"
```

---

## Common Pitfalls
- **Classpath leakage**: extra libraries (e.g., adding Kafka) may trigger their auto-config; disable via exclude or property gate.
- **Ordering surprises**: custom configuration may need `@AutoConfigureBefore/After` when extending starters.
- **Hidden bean overrides**: if you reintroduce bean overriding, ensure diagnostics; otherwise prefer conditional beans.
- **Profile interactions**: active profiles change property values and `@Profile` conditions, altering which auto-configs pass.

---

## Minimal Example (conceptual)
```java
@SpringBootApplication
public class DemoApp {
    public static void main(String[] args) {
        SpringApplication.run(DemoApp.class, args);
    }
}

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(DataSourceProperties.class)
class DataSourceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    DataSource dataSource(DataSourceProperties props) {
        return DataSourceBuilder.create()
                .url(props.getUrl())
                .username(props.getUsername())
                .password(props.getPassword())
                .build();
    }
}
```
- If `DataSource` is absent on the classpath -> auto-config is skipped.
- If the user defines their own `DataSource` bean -> auto-config bean is skipped.

---

## Quick Checklist When Tuning Auto-Config
- Identify which starter pulled in the auto-config (inspect `AutoConfiguration.imports`).
- Check why it applied or not (debug report or `/actuator/conditions`).
- Override via property first; exclude only if needed.
- Keep custom configs ordered with `@AutoConfigureBefore/After` when extending defaults.

---

## Version Notes
- Boot 3.x uses the `AutoConfiguration.imports` resource and Jakarta packages; Boot 2.x uses `spring.factories` and javax packages. Adjust examples accordingly.
