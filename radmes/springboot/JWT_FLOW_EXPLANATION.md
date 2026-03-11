# Understanding JWT Flow in Microservices

This document explains the standard flow of JSON Web Tokens (JWT) in a microservices architecture, specifically focusing on how authentication and authorization are handled end-to-end.

---

## 1. High-Level Flow Diagram

Here is a visual representation of how a user authenticates and accesses a protected resource.

```mermaid
sequenceDiagram
    participant User as Client (Browser/App)
    participant Gateway as API Gateway
    participant IdP as Identity Provider (Auth Server)
    participant Service as Microservice (Resource Server)

    Note over User, IdP: **Phase 1: Authentication**
    User->>Gateway: 1. Request to Login (e.g., /login)
    Gateway-->>User: 2. Redirect to IdP Login Page
    User->>IdP: 3. Enter Credentials (User/Pass)
    IdP->>IdP: 4. Validate Credentials
    IdP-->>User: 5. Return Authorization Code
    User->>Gateway: 6. Send Authorization Code
    Gateway->>IdP: 7. Exchange Code for Tokens (Access + ID + Refresh)
    IdP-->>Gateway: 8. Return JWT (Access Token)
    Gateway-->>User: 9. Set Session / Return Token to Frontend

    Note over User, Service: **Phase 2: Authorization & Resource Access**
    User->>Gateway: 10. API Request + Header `Authorization: Bearer <JWT>`
    Gateway->>Gateway: 11. Validate Token Signature (Public Key)
    Gateway->>Service: 12. Forward Request + `Authorization: Bearer <JWT>`
    Service->>Service: 13. Validate Token & Check Scopes/Roles
    Service-->>Gateway: 14. Return Protected Data
    Gateway-->>User: 15. Return Response
```

**Phase 1: Authentication**
1.  **User -> Gateway**: Request to Login (e.g., /login)
2.  **Gateway -> User**: Redirect to IdP Login Page
3.  **User -> IdP**: Enter Credentials (User/Pass)
4.  **IdP**: Validate Credentials
5.  **IdP -> User**: Return Authorization Code
6.  **User -> Gateway**: Send Authorization Code
7.  **Gateway -> IdP**: Exchange Code for Tokens (Access + ID + Refresh)
8.  **IdP -> Gateway**: Return JWT (Access Token)
9.  **Gateway -> User**: Set Session / Return Token to Frontend

**Phase 2: Authorization & Resource Access**
10. **User -> Gateway**: API Request + Header `Authorization: Bearer <JWT>`
11. **Gateway**: Validate Token Signature (Public Key)
12. **Gateway -> Service**: Forward Request + `Authorization: Bearer <JWT>`
13. **Service**: Validate Token & Check Scopes/Roles
14. **Service -> Gateway**: Return Protected Data
15. **Gateway -> User**: Return Response

---

## 2. Step-by-Step Breakdown

### Phase 1: Authentication (Getting the Token)

1.  **Login Request**: The user tries to access a protected resource or clicks "Login".
2.  **Redirection**: The application (often the API Gateway or a frontend app) redirects the user to the **Identity Provider (IdP)** (e.g., Keycloak, Auth0, Okta).
3.  **Credentials**: The user enters their username and password directly on the IdP's secure page. *Note: The microservice never sees the password.*
4.  **Token Issuance**: If valid, the IdP generates a **JWT (JSON Web Token)**. This token contains:
    *   **Subject (sub)**: User ID.
    *   **Expiration (exp)**: When the token dies.
    *   **Authorities/Roles**: What the user can do (e.g., `ROLE_ADMIN`).
    *   **Signature**: A cryptographic proof that the IdP issued this token.

### Phase 2: Authorization (Using the Token)

1.  **API Call**: The client attaches the JWT to every subsequent HTTP request in the header:
    ```http
    GET /api/orders HTTP/1.1
    Host: api.example.com
    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI...
    ```
2.  **Edge Verification (Gateway)**: The API Gateway intercepts the request. It checks:
    *   Is the token expired?
    *   Is the signature valid? (using the IdP's public key).
3.  **Forwarding**: If valid, the Gateway forwards the request to the specific microservice, keeping the `Authorization` header intact (Token Relay).
4.  **Service Verification**: The destination microservice (e.g., Order Service) receives the request. It parses the JWT to know *who* the user is and *if* they have permission (e.g., `@PreAuthorize("hasRole('ADMIN')")`).

---

## 3. Anatomy of a JWT

A JWT is just a long string separated by two dots (`.`) into three parts: `Header.Payload.Signature`.

### Part A: Header (Algorithm & Token Type)
Tells the server how to validate the signature.
```json
{
  "alg": "RS256",  // Algorithm used (e.g., RSA SHA-256)
  "typ": "JWT"
}
```

### Part B: Payload (Data / Claims)
The actual data about the user. This is readable by anyone (Base64 encoded), so **never put secrets like passwords here**.
```json
{
  "sub": "1234567890",       // Subject (User ID)
  "name": "John Doe",        // Custom claim
  "iat": 1516239022,         // Issued At (Timestamp)
  "exp": 1516242622,         // Expiration Time
  "roles": ["admin", "editor"] // Permissions
}
```

### Part C: Signature (Security)
Used to verify that the token hasn't been tampered with.
```
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  your-256-bit-secret-key
)
```
If a hacker changes the payload (e.g., changes `role: user` to `role: admin`), the signature calculated by the server won't match the signature in the token, and the request will be rejected.

---

## 4. Why Use JWT in Microservices?

1.  **Stateless**: The server doesn't need to store session data in memory or a database (like Redis). All info is *inside* the token.
2.  **Scalable**: Any microservice can validate the token independently if they have the public key. No need to call the Auth Server for every single request.
3.  **Decoupled**: The Authentication logic (IdP) is completely separate from the Business logic (Microservices).

