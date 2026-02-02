# N+1 Problem in Hibernate

The **N+1 problem** is a performance issue that happens when fetching a collection of entities related to another entity.

## Example Scenario

You have `User` entities and each user has many `Post` entities (One-to-Many).

1.  **Query 1**: You fetch all `User` entities.
    ```sql
    SELECT * FROM user_details;
    ```
    This returns `N` users.

2.  **Query N**: You loop through these users and access their posts (`user.getPost()`).
    Because `fetch = FetchType.LAZY` (default for OneToMany), Hibernate fires a separate query for **each** user to get their posts.
    ```sql
    SELECT * FROM post WHERE user_id = ?;
    SELECT * FROM post WHERE user_id = ?;
    ...
    ```

**Total Queries:** 1 (for users) + N (for posts) = **N+1 queries**.

If you have 1000 users, that's 1001 database queries, which is very slow.

## Solution

I have added a solution to your `UserRepository.java`.

### 1. JOIN FETCH (Implemented)

Using `JOIN FETCH` in a JPQL query tells Hibernate to fetch the related entities in the same initial query using a SQL JOIN.

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.post")
List<User> findAllWithPosts();
```

Use `userService.findAllWithPosts()` (or call repository directly) when you need users AND their posts.

### 2. @EntityGraph (Alternative)

You can also use `@EntityGraph` on a repository method.

```java
@EntityGraph(attributePaths = {"post"})
List<User> findAll();
```

### 3. @BatchSize (Alternative)

In `User.java`, you can add `@BatchSize(size = 10)` on the `post` collection.

```java
@OneToMany(...)
@BatchSize(size = 10)
private List<Post> post;
```

This reduces queries by fetching posts for 10 users at once (e.g., `WHERE user_id IN (?, ?, ...)`).

---

## Simulation Scenario

To see the impact of N+1, we have initialized the database with:
- **100 Users**
- **50 Posts per User** (Total 5,000 posts)

### Performance Impact:
1. **With N+1 (Lazy loading without Join Fetch):**
   - Fetching all users and their posts will trigger **101 queries**. 
   - Many small queries cause significant database overhead.

2. **With Solution (JOIN FETCH):**
   - Fetching all users and their posts will trigger **1 query**.
   - One large optimized query is much faster.

You can test this by calling the `findAllWithPosts()` method in `UserRepository` and observing the SQL logs.

## Other Errors Mentioned

### User Not Found (404)
The error `User not found id: 9` is a custom 404 response you likely implemented. It means a user with ID 9 does not exist in the database.

### GetMapping vs RequestMapping

*   **@RequestMapping**: General-purpose annotation to map web requests. Can handle any HTTP method (GET, POST, etc.) if not specified.
    ```java
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    ```

*   **@GetMapping**: A shortcut specifically for HTTP GET requests.
    ```java
    @GetMapping("/users")
    ```
    (Equivalent to the above `RequestMapping`).
