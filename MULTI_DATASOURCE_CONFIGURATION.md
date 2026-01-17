# Configuring Multiple DataSources in Spring Boot

In a standard Spring Boot application, the framework auto-configures a single `DataSource`, `EntityManagerFactory`, and `TransactionManager`. When you need to connect to two different databases (e.g., one for User data, one for Orders, or a read-replica setup), you must disable the default auto-configuration and manually configure the beans for each database.

## 1. Application Properties
Define two distinct sets of database properties in `application.properties`. We use custom prefixes (e.g., `app.datasource.primary` and `app.datasource.secondary`) to separate them.

```properties
# --- Primary Database ---
app.datasource.primary.url=jdbc:mysql://localhost:3306/db_primary
app.datasource.primary.username=root
app.datasource.primary.password=pass
app.datasource.primary.driver-class-name=com.mysql.cj.jdbc.Driver

# --- Secondary Database ---
app.datasource.secondary.url=jdbc:postgresql://localhost:5432/db_secondary
app.datasource.secondary.username=postgres
app.datasource.secondary.password=pass
app.datasource.secondary.driver-class-name=org.postgresql.Driver

# JPA specific (if needed per DB)
spring.jpa.hibernate.ddl-auto=update
```

## 2. Package Structure
To keep things clean, separate your JPA Repositories into different packages. This allows us to tell Spring: "All repos in `com.example.repo.primary` belong to DB1, and `com.example.repo.secondary` belong to DB2."

```
src/main/java/com/example
  ├── config
  │     ├── PrimaryDbConfig.java
  │     └── SecondaryDbConfig.java
  ├── domain
  │     ├── primary
  │     │     └── User.java
  │     └── secondary
  │           └── Order.java
  └── repo
        ├── primary
        │     └── UserRepository.java  <-- Uses Primary DB
        └── secondary
              └── OrderRepository.java <-- Uses Secondary DB
```

## 3. Primary Configuration Class
This class configures the `DataSource`, `EntityManagerFactory`, and `TransactionManager` for the first database. We mark the beans as `@Primary` so they are used by default if no qualifier is specified.

```java
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.example.repo.primary", // <--- Important: Point to Primary Repos
    entityManagerFactoryRef = "primaryEntityManagerFactory",
    transactionManagerRef = "primaryTransactionManager"
)
public class PrimaryDbConfig {

    @Primary
    @Bean(name = "primaryDataSourceProperties")
    @ConfigurationProperties("app.datasource.primary")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties("app.datasource.primary.configuration")
    public DataSource dataSource(@Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("primaryDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.domain.primary") // <--- Point to Primary Entities
                .persistenceUnit("primary")
                .build();
    }

    @Primary
    @Bean(name = "primaryTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
```

## 4. Secondary Configuration Class
This is almost identical to the Primary one, but **without `@Primary`** and pointing to different packages/properties.

```java
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.example.repo.secondary", // <--- Important: Point to Secondary Repos
    entityManagerFactoryRef = "secondaryEntityManagerFactory",
    transactionManagerRef = "secondaryTransactionManager"
)
public class SecondaryDbConfig {

    @Bean(name = "secondaryDataSourceProperties")
    @ConfigurationProperties("app.datasource.secondary")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "secondaryDataSource")
    @ConfigurationProperties("app.datasource.secondary.configuration")
    public DataSource dataSource(@Qualifier("secondaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "secondaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("secondaryDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.domain.secondary") // <--- Point to Secondary Entities
                .persistenceUnit("secondary")
                .build();
    }

    @Bean(name = "secondaryTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("secondaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
```

## 5. Usage in Services/Controllers
Since the repositories are bound to specific `EntityManagerFactories` via the package scanning in the config classes, you just inject the repositories normally.

```java
@Service
public class MultiDbService {

    private final UserRepository userRepo;   // Automatically uses Primary DB
    private final OrderRepository orderRepo; // Automatically uses Secondary DB

    public MultiDbService(UserRepository userRepo, OrderRepository orderRepo) {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
    }

    public void saveDataAcrossDbs() {
        // Saves to DB 1
        userRepo.save(new User("Alice")); 
        
        // Saves to DB 2
        orderRepo.save(new Order("Book"));
    }
}
```

### Transaction Management Note
If you need a transaction that spans **both** databases (Distributed Transaction / XA), standard `JpaTransactionManager` is insufficient. You would need to use **JTA (Java Transaction API)** with a transaction manager like **Atomikos** or **Bitronix**. 

With the setup above, `@Transactional` will use the `@Primary` transaction manager by default. to use the secondary one:
```java
@Transactional(transactionManager = "secondaryTransactionManager")
public void doSomethingInSecondary() { ... }
```

