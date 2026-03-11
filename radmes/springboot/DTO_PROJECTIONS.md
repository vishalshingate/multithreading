# DTO Projections in Spring Data JPA

Fetching entire entities (`@Entity`) when you only need a few fields is inefficient. It can also lead to the **N+1 problem** if you accidentally trigger lazy loading of relationships.

**DTO (Data Transfer Object) Projections** allow you to fetch *only the data you need* directly into a custom Java object or Interface, skipping the overhead of managing Entity state (dirty checking, caching).

---

## 1. Interface-Based Projection (The "Spring Data" Way)
The easiest way. Define an interface with getter methods matching the property names.

### Step 1: Define the Interface
```java
public interface UserSummary {
    String getUsername();
    String getEmail();
    
    // You can even access nested properties (Open Projection)
    // Be careful: This might trigger N+1 if not optimized!
    // @Value("#{target.firstName + ' ' + target.lastName}") 
    // String getFullName();
}
```

### Step 2: Use in Repository
Return the interface instead of the Entity.

```java
public interface UserRepository extends JpaRepository<User, Long> {
    List<UserSummary> findByStatus(String status);
}
```

**SQL Generated:**
Spring selects *only* the required columns:
```sql
SELECT u.username, u.email FROM users u WHERE u.status = ?
```

---

## 2. Class-Based Projection (DTOs) with Constructor Expression
This is the most powerful and performance-friendly way, especially for joining data. It uses the `new` keyword in JPQL.

### Step 1: Define the DTO Class
It must have a matching constructor.
```java
package com.example.dto;

public class UserPostDTO {
    private String username;
    private String postTitle;

    public UserPostDTO(String username, String postTitle) {
        this.username = username;
        this.postTitle = postTitle;
    }
    // getters...
}
```

### Step 2: Use JPQL in Repository
Use the fully qualified class name in the query.

```java
@Query("SELECT new com.example.dto.UserPostDTO(u.username, p.title) " +
       "FROM User u JOIN u.posts p")
List<UserPostDTO> fetchUserAndPostTitles();
```

**Why this solves N+1:**
*   It performs a single SQL `JOIN`.
*   It selects exactly two columns.
*   It returns a flat list of DTOs.
*   No Entities are managed, no lazy loading can happen later.

---

## 3. Dynamic Projections
You can define the return type dynamically at runtime using Generics.

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // calling code decides if it wants User.class, UserSummary.class, or UserDto.class
    <T> List<T> findByUsername(String username, Class<T> type);
}
```

**Usage:**
```java
List<UserSummary> summaries = repo.findByUsername("john", UserSummary.class);
List<UserDto> dtos = repo.findByUsername("john", UserDto.class);
```

---

## Summary: Entity vs Projection for N+1

| Feature | Fetching Entities (`User`) | Fetching Projections (`UserDTO`) |
| :--- | :--- | :--- |
| **Data Loaded** | All columns (heavy) | Only needed columns (light) |
| **N+1 Risk** | High (if accessing lazy collections) | Zero (relationships are flattened in query) |
| **Managed?** | Yes (Hibernate session tracks changes) | No (Read-only) |
| **Use Case** | When you need to update/save data. | When you only need to read/display data. |

