# What is a DTO (Data Transfer Object)?

A **DTO** is a plain object (POJO) used strictly to transfer data between different processes or layers of an application (e.g., from Backend to Frontend, or Microservice A to Microservice B).

It contains **no business logic**. It only has fields, getters, and setters.

---

## 1. Why do we need DTOs? (The Problems they solve)

### Problem A: Security Leaks (Over-Fetching)
*   **Without DTO:** If your Controller returns a `UserEntity` directly from the database, Jackson (JSON converter) will serialize **everything**.
    *   Result: `{"id": 1, "username": "admin", "password": "bcrypt_hash_123", "ssn": "..."}`
    *   **CRITICAL SECURITY FLAW**. You just exposed passwords and internal IDs.
*   **With DTO:** You create a `UserResponseDTO` that only has `username` and `email`.
    *   Result: `{"username": "admin"}`. Secure.

### Problem B: Circular Dependencies
*   **Without DTO:** Entities often have relationships.
    *   `User` has List of `Orders`. `Order` has a `User`.
    *   If you serialize `User`, it grabs `Orders`, which grabs `User`, which grabs `Orders`...
    *   **Result:** `StackOverflowError` (Infinite JSON recursion).
*   **With DTO:** A `UserDTO` simply doesn't include the `Order` list, or includes a simplified list of Order IDs.

### Problem C: Tight Initial Coupling vs API Contract
*   **Without DTO:** Your Front-End binds directly to your Database Schema.
    *   If you rename the DB column `first_name` to `fname`, your **API BREAKS**, and the Frontend breaks.
*   **With DTO:** The DB can change however it likes. You just update the mapper logic. The `UserDTO` stays the same, so the Frontend never knows the DB changed.

### Problem D: Network Performance (N+1 Problem)
*   **Without DTO:** To display a screen, the frontend might need data from User, Orders, and Addresses. If you send full entities, you might be sending 100 fields when the UI only needs 3.
*   **With DTO:** You can create specific DTOs (e.g., `UserSummaryDTO`) that aggregate data from multiple tables into one tiny optimized object for that specific screen.

---

## 2. Design Patterns used with DTOs

### A. The DTO Pattern Itself
While often called a POJO, it is a formal design pattern used to reduce the number of remote calls by aggregating data into a single object.

### B. Builder Pattern
Since DTOs are mostly used to transfer data, we often use the **Builder Pattern** (via Lombok's `@Builder`) to construct them cleanly without huge constructors.
```java
UserDTO dto = UserDTO.builder()
                     .username("john")
                     .email("john@abc.com")
                     .build();
```

### C. Assembler / Mapper Pattern
To solve the problem of converting Entity -> DTO, we use an **Assembler** (or Mapper). This pattern decouples the conversion logic from the business logic.
*   **Implementation:** MapStruct, ModelMapper, or manual `UserAssembler` class.

### D. Value Object (V.O) Pattern
In some architectures, DTOs are treated as **Value Objects**. Once created, they are immutable (using `final` fields and no setters) to ensure data stays consistent while moving between layers.

---

## 3. When to use DTOs?

1.  **Incoming Data (`CreateUserRequest`)**:
    *   Don't accept an Entity in the controller. The Entity might have an `id` or `creationDate` field that the user shouldn't set. Use a DTO to whitelist allowed fields.
2.  **Outgoing Data (`UserResponse`)**:
    *   Always return DTOs to hide internal implementation details.

---

## 4. How to Map? (Entity <-> DTO)

In real projects, writing `dto.setName(entity.getName())` 50 times is boring. We use libraries:

1.  **MapStruct:** (Best Performance, generated at compile time).
2.  **ModelMapper:** (Easier to setup, uses Reflection/Runtime - slower).
3.  **BeanUtils:** (Legacy, generally avoid).

### Example Pattern
```java
@RestController
class UserController {
    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        UserEntity entity = repo.findById(id);
        
        // Converting Entity to DTO before returning
        return new UserDTO(entity.getName(), entity.getEmail());
    }
}
```
