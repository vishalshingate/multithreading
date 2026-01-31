# HTTPS in Spring Boot

## What is HTTPS?
HTTPS (Hypertext Transfer Protocol Secure) is an extension of HTTP. It is used for secure communication over a computer network. In HTTPS, the communication protocol is encrypted using Transport Layer Security (TLS) or, formerly, Secure Sockets Layer (SSL).

In the context of Spring Boot:
*   **Security**: Encrypts data exchanged between the client (browser/consumer) and the server (Spring Boot app).
*   **Trust**: Validates the identity of the server using certificates.
*   **Compliance**: Essential for handling sensitive data (passwords, credit cards, PII).

## How to Enable HTTPS in Spring Boot

### Step 1: Generate a Self-Signed Certificate
For development or testing purposes, you can generate a self-signed certificate using the `keytool` utility (bundled with the JDK).

Open your terminal and run:

```bash
keytool -genkeypair -alias library-app -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore library-keystore.p12 -validity 3650
```

*   **-alias**: A unique identifier for the key entry.
*   **-keyalg**: The algorithm to use (RSA).
*   **-keysize**: The size of the key (2048 bits).
*   **-storetype**: The format of the keystore (PKCS12 is the industry standard).
*   **-keystore**: The name of the output file.
*   **-validity**: How many days the certificate is valid.

Follow the prompts to set a password (e.g., `password`) and enter details (or just press enter to skip).

**Place the generated `library-keystore.p12` file in `src/main/resources/`.**

### Step 2: Configure `application.yaml`
Update your configuration file to tell Spring Boot where to find the keystore and the password.

```yaml
server:
  port: 8443 # HTTPS typically uses 443, but for local dev 8443 is common
  ssl:
    enabled: true
    key-store: classpath:library-keystore.p12
    key-store-password: password
    key-store-type: PKCS12
    key-alias: library-app
```

### Step 3: Run and Test
1.  Start your Spring Boot application.
2.  Access the URL: `https://localhost:8443`
3.  **Note**: Since it is a self-signed certificate, your browser will warn you that the connection is not private. You can proceed by clicking "Advanced" -> "Proceed to localhost (unsafe)".

### Step 4: Redirect HTTP to HTTPS (Optional)
If you want to force HTTPS, you can configure a second connector (programmatically) to redirect traffic from port 8080 (HTTP) to 8443 (HTTPS). Ideally, in a production environment, this is handled by a reverse proxy (Nginx, AWS ALB) rather than the Spring Boot app itself.

## Production Considerations
*   **CA Certificate**: In production, do **not** use self-signed certificates. Purchase one from a Certificate Authority (DigiCert, Let's Encrypt, etc.).
*   **Secrets Management**: Do not hardcode the keystore password in `application.yaml`. Use environment variables or a secret manager.
    ```yaml
    server:
      ssl:
        key-store-password: ${SSL_PASSWORD}
    ```
*   **Reverse Proxy**: Usually, SSL termination is done at the Load Balancer or Nginx level, and the internal traffic to the Spring Boot pod is HTTP. This offloads encryption overhead from the application.
