# Microservice Architecture Project

This project demonstrates a microservice architecture using Spring Boot, Spring Cloud, Docker, and Kubernetes.

## Project Modules

- **[API Gateway](./api-gateway/)**: Central entry point for all requests.
- **[Naming Server (Eureka)](./naming-server/)**: Service discovery via Netflix Eureka.
- **[Currency Exchange Service](./currency-exchange-service/)**: Provides currency exchange rates and handles JPA persistence.
- **[Currency Conversion Service](./currency-conversion-service/)**: Converts currency by calling the exchange service.
- **[Spring Cloud Config Server](./spring-cloud-config-server/)**: Centralized configuration management.
- **[Limits Service](./limits-service/)**: Manages configuration limits.

## Useful Guides

- [**Spring Architecture Flow**](./SPRING_ARCHITECTURE_FLOW.md): Detailed explanation of Controller-Service-Repository flow, Idempotency, and Optimistic Locking (`@Version`).
- **[REST API Best Practices](./REST_API_BEST_PRACTICES.md)**: Industry standards for API design.
- **[Java Migration Guide](./JAVA_MIGRATION_GUIDE.md)**: Steps to migrate from Java 8 to 11 and 11 to 21.
- **[Deployment Guide](./DEPLOYMENT.md)**: How to deploy this project to Kubernetes (GCP/Azure).
- **[Troubleshooting & Operations](./TROUBLESHOOTING.md)**: Zipkin tracing, SQL logging, and more.

## Quick Links (Local Development)

### API Gateway Routes
- [Exchange Service via Gateway](http://localhost:8765/currency-exchange/from/EUR/to/INR)
- [Conversion Service via Gateway](http://localhost:8765/currency-conversion-feign/from/USD/to/INR/quantity/10)

### Infrastructure Dashboards
- [Eureka Discovery Dashboard](http://localhost:8761/)
- [Zipkin Tracing UI](http://localhost:9411/)

### Services (Direct Access)
- [Currency Exchange](http://localhost:8000/currency-exchange/from/EUR/to/INR)
- [Currency Conversion](http://localhost:8100/currency-conversion/from/USD/to/INR/quantity/10)

## Implementation Details
- **Spring Boot 3.x**: Latest version of Spring Boot.
- **Resilience4j**: Used for Circuit Breaker and Retry patterns.
- **Micrometer Tracing**: Integrated with Zipkin for distributed tracing.
- **Optimistic Locking**: Implemented using JPA `@Version` in the exchange service.

