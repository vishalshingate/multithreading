# Database Credentials Management in Production

In real-world production environments, security is paramount. We never store sensitive data like usernames and passwords in the source code.

## 1. Environment Variables (The 12-Factor App)
This is the most common approach. Credentials are injected into the application's runtime environment.
- **Why**: Keeps config separate from code.
- **Implementation**:
  In `application.properties`:
  ```properties
  spring.datasource.url=${DB_URL}
  spring.datasource.username=${DB_USERNAME}
  spring.datasource.password=${DB_PASSWORD}
  ```
  The values are set on the server or Docker container environment.

## 2. Dedicated Secret Managers (Enterprise Grade)
For higher security, auditing, and automatic rotation.
- **Tools**:
    - **HashiCorp Vault**: Platform-agnostic secret management.
    - **AWS Secrets Manager**: Integrated with AWS IAM.
    - **Azure Key Vault**: Microsoft ecosystem.
- **Workflow**: The application identifies itself via IAM roles/Managed Identity (no password required) and requests the DB password from the vault at startup.

## 3. Kubernetes Secrets
When running on Kubernetes:
- Secrets are created in the cluster (e.g., `kubectl create secret`).
- Secrets are mounted into Pods as Environment Variables or Files.

## 4. Spring Cloud Config Server (Encrypted)
- Configuration is stored in a Git repo.
- Sensitive values are encrypted using keys (e.g., `{cipher}AQB...`).
- The Config Server decrypts them before sending them to the application.

## 5. CI/CD Injection
- Credentials are stored in the deployment pipeline settings (Jenkins Credentials, GitHub Actions Secrets).
- The pipeline injects them while deploying the application to the server.

## ❌ Anti-Patterns (What NOT to do)
- **Hardcoding**: Never write `password=123` in Java files.
- **Committing to Git**: Never push `application.properties` containing live passwords to version control.

## 6. How to Handle Compromised Secrets (Zero-Downtime Rotation)

If a secret is compromised, you must rotate it immediately. Doing this without downtime in Spring Boot requires specific strategies.

### A. The "Dual Credential" Rolling Update (Most Robust)
Since we cannot instantly update the password on the DB and the App simultaneously without breaking connections, we use a transitional phase.

1.  **Database Side**: Create a NEW user/password (or add a secondary password if the DB supports it) while keeping the OLD one active.
2.  **Config Side**: Update the Spring Boot configuration (Config Server / Environment Variable) to use the NEW credentials.
3.  **App Side**: Trigger a **Rolling Deployment** (Kubernetes/Swarm).
    *   New instances start up using the NEW password.
    *   Old instances continue processing (draining) using the OLD password.
    *   Once the rollout is complete, only the NEW password is being used.
4.  **Cleanup**: Revoke/Delete the OLD credentials in the database.

### B. Spring Cloud `@RefreshScope` (Runtime Update)
If you are using Spring Cloud Config or Spring Cloud Vault, you can reload properties without restarting the JVM.

1.  **Setup**:
    *   Add `spring-boot-starter-actuator`.
    *   Annotate beans (e.g., configuration classes) with `@RefreshScope`.
2.  **Action**:
    *   Update the value in the Config Server/Vault.
    *   Trigger `POST /actuator/refresh` on the application instances (or use Spring Cloud Bus to broadcast the event).
3.  **Caveat for DataSources**:
    Simply changing the password string won't automatically update open connections in the connection pool (HikariCP). Only new connections *might* use it, or you need to force a bean refresh of the DataSource itself, which is complex and can disrupt active transactions. **Strategy A is preferred for database passwords.**

### C. AWS Secrets Manager / HashiCorp Vault (Dynamic Secrets)
These tools can automate the rotation.
*   **Rotation**: The secret manager automatically generates a new password, updates the database, and stores it interactively.
*   **Spring Integration**: Using `spring-cloud-starter-aws-secrets-manager-config` or `spring-cloud-starter-vault-config`, the application usually fetches the secret at startup. For runtime changes, it often relies on the restart/refresh strategies mentioned above, or specific listeners provided by the library.
