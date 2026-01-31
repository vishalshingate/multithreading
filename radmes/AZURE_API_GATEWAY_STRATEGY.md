# Azure API Gateway Strategy for Spring Microservices

When deploying Spring Microservices to Azure, you essentially have two powerful patterns for the API Gateway:

1.  **Azure API Management (APIM)**: A fully managed, enterprise-grade PaaS offering.
2.  **Spring Cloud Gateway**: The native Spring way, which can be deployed as a standalone app or used as a managed component in Azure Spring Apps (Enterprise Tier).

## 1. Which Gateway Should You Use?

### Option A: Azure API Management (APIM)
**Best for**: Enterprise governance, external-facing APIs, monetization, and protocol transformation.
*   **Pros**: 
    *   Language agnostic (can front Java, .NET, Node.js services).
    *   Built-in Developer Portal for API consumers.
    *   Advanced policies (XML-based) for transformation, caching, and security.
    *   Direct integration with Azure AD (Microsoft Entra ID).
*   **Cons**: Can be expensive (especially Premium tier); separate configuration language (XML policies) vs. Java/YAML.

### Option B: Spring Cloud Gateway (Managed or Self-Hosted)
**Best for**: "Spring-native" teams, internal microservice-to-microservice routing, and dynamic routing logic requiring Java code.
*   **Pros**:
    *   Configuration is in `application.yaml` or Java Code (familiar to Spring devs).
    *   Seamless integration with Application Config, Eureka, or Kubernetes discovery.
    *   Managed version available in **Azure Spring Apps**.
*   **Cons**: Requires managing the JVM application (if self-hosted); less out-of-the-box governance features compared to APIM.

### **The Hybrid Approach (Recommended)**
Often, the best architecture is **Azure APIM (External Gateway)** -> **Spring Cloud Gateway (Internal Gateway)** -> **Microservices**.
*   **APIM** handles public rate limiting, auth offloading, and external developer onboarding.
*   **Spring Cloud Gateway** handles complex routing rules, circuit breaking, and cross-cutting concerns within the cluster.

---

## 2. Implementing Rate Limiting

### In Azure API Management (APIM)
APIM uses **Policies** defined in XML. You can apply these at the Global, Product, or API level.

**Example**: Limit calls to 100 requests per 60 seconds per IP address.
```xml
<policies>
    <inbound>
        <base />
        <!-- Throttle based on Client IP -->
        <rate-limit-by-key calls="100" 
                           renewal-period="60" 
                           counter-key="@(context.Request.IpAddress)" />
        
        <!-- ALTERNATIVE: Throttle based on JWT (User ID) -->
        <!-- <rate-limit-by-key calls="1000" 
                               renewal-period="3600" 
                               counter-key="@(context.Request.Headers.GetValueOrDefault("Authorization","").AsJwt()?.Subject)" /> -->
    </inbound>
    <backend>
        <base />
    </backend>
    <outbound>
        <base />
    </outbound>
</policies>
```

### In Spring Cloud Gateway (application.yaml)
Spring Cloud Gateway uses Redis to track request counts. You need `spring-boot-starter-data-redis-reactive` on your classpath.

**Example**: Limit 10 requests per second with a burst capacity of 20.
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: library-service-route
          uri: lb://LIBRARY-SERVICE
          predicates:
            - Path=/library/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10   # Tokens per second
                redis-rate-limiter.burstCapacity: 20   # Max bucket size
                redis-rate-limiter.requestedTokens: 1  # Cost per request
                # Key Resolver Bean Name (Java code needed to define 'userKeyResolver')
                key-resolver: "#{@userKeyResolver}" 
```

**Java Code for Key Resolver (Rate limit by User/IP):**
```java
@Configuration
public class RateLimitConfig {
    @Bean
    KeyResolver userKeyResolver() {
        // Rate limit by IP Address
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }
}
```

---

## 3. Other Key Configurations

### A. Routing
*   **APIM**: Done via the GUI or ARM Templates. You map a "Suffix" (e.g., `/library`) to a Backend URL (e.g., `http://library-service:8080`).
*   **Spring Cloud Gateway**: Done via Route Predicates.
    ```yaml
    predicates:
      - Path=/books/**
      - Method=GET
      - Header=X-Tenant-ID, \d+
    ```

### B. Security (OAuth2 / JWT Validation)
*   **APIM**: Can pre-validate a JWT before successful routing. This offloads CPU work from your microservices.
    ```xml
    <validate-jwt header-name="Authorization" failed-validation-httpcode="401" failed-validation-error-message="Unauthorized">
        <openid-config url="https://login.microsoftonline.com/{tenant-id}/v2.0/.well-known/openid-configuration" />
        <required-claims>
            <claim name="aud">
                <value>api://my-library-api</value>
            </claim>
        </required-claims>
    </validate-jwt>
    ```

*   **Spring Cloud Gateway**: Acts as an OAuth2 Resource Server.
    ```yaml
    spring:
      security:
        oauth2:
          resourceserver:
            jwt:
              issuer-uri: https://login.microsoftonline.com/{tenant-id}/v2.0
    ```

### C. SSL Termination
*   **APIM**: Handles SSL termination automatically at the edge.
*   **Azure Spring Apps**: Managed Ingress handles SSL for you. You just upload the certificate to the Azure Portal context.

---

## 4. Step-by-Step Implementation Guide (Azure)

1.  **Provision the Service**:
    *   Create an **Azure API Management** instance (Developer tier for testing, Consumption/Standard for production).
    *   Ensure your **Spring Apps** or **AKS** cluster is running.
2.  **Define Backends**:
    *   In APIM, define your backend services (Library Service, Discovery Service).
3.  **Import API Definitions**:
    *   Use the "OpenAPI" (Swagger) import feature in APIM. Point it to your Spring Boot app's `v3/api-docs` endpoint.
4.  **Apply Policies (The "Configuration")**:
    *   Select the API in the Design tab.
    *   Add **Inbound Policies** for `rate-limit`, `validate-jwt`, or `cors`.
5.  **Test**:
    *   Use the "Test" tab in the Azure Portal or Postman.
    *   Verify that exceeding the rate limit returns `429 Too Many Requests`.

---

## 5. Real-World Industry Standard: The "Hybrid" Pattern

In large-scale, real-world enterprise projects, you rarely choose just one. The industry standard is the **Hybrid Pattern** (also known as the "Gateway Offloading" pattern).

### Why the Hybrid Pattern?
*   **Separation of Duties**:
    *   **Platform/Security Team** manages **Azure APIM**. They care about billing, global rate limits, legal compliance, and blocking malicious IPs. They don't know (or care) about your Java code.
    *   **Development Team** manages **Spring Cloud Gateway**. They care about circuit breakers, retry logic, canary deployments, and complex routing based on business headers.
*   **Flow**: `Internet -> Azure Front Door (Global LB) -> Azure APIM (Edge Security) -> Spring Cloud Gateway (Routing logic) -> Microservices`.

### Implementation Reality
In a real production environment, **nobody configures this manually in the Azure Portal**.

#### 1. Infrastructure as Code (IaC) for APIM
Changes to Azure API Management are handled via **Terraform** or **Bicep**.
*   **Scenario**: You want to add a new API endpoint.
*   **The "Industry" Way**: You update a Terraform script that defines the `azurerm_api_management_api` resource and push it to Git. A DevOps pipeline (GitHub Actions/Azure DevOps) applies the change.

**Example Terraform for Rate Limiting**:
```hcl
resource "azurerm_api_management_api_policy" "example" {
  api_name            = azurerm_api_management_api.example.name
  api_management_name = azurerm_api_management.example.name
  resource_group_name = azurerm_resource_group.example.name

  xml_content = <<XML
    <policies>
      <inbound>
        <rate-limit calls="50" renewal-period="15" />
        <base />
      </inbound>
    </policies>
  XML
}
```

#### 2. GitOps for Spring Cloud Gateway
Configuration for the internal gateway sits in a **Config Server** or **Kubernetes ConfigMaps**.
*   **Scenario**: You need to change a route from Service A to Service B (Blue/Green deployment).
*   **The "Industry" Way**: You edit `gateway-config.yaml` in a Git repository. ArgoCD (or Flux) sees the change and automatically syncs it to the Kubernetes cluster running Spring Cloud Gateway. No restart is usually required if using Spring Cloud Config Monitor.

### Summary: Who uses what?
| Feature | Azure APIM (Ops/Security Team) | Spring Cloud Gateway (Dev Team) |
| :--- | :--- | :--- |
| **Authentication** | Validates JWT Signature (is this a valid token?) | Extracts User Context (who is this user?) |
| **Rate Limiting** | Global limits (1000 req/min strategy) to protect infra | User-specific limits (Premium vs Free tier logic) |
| **Routing** | Simple path mapping (`/api/v1/*` -> Internal Gateway) | Complex logic (Header versions, A/B testing weighting) |
| **SSL** | Terminates Public SSL | Internal mTLS (Mutual TLS) |

