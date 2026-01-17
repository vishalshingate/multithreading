# REST API Parameters: When to use what?

In Spring Boot (and REST in general), there are three main ways to pass data from the Client to the Server.

---

## 1. Path Variable (`@PathVariable`)

### **"The Identify-er"**

*   **Syntax:** `/users/{id}`
*   **Annotation:** `@PathVariable`
*   **Purpose:** To **Identify** a specific resource (Resource Locator).
*   **Rule:** If the parameter is mandatory and part of the URL hierarchy, use Path Variable.

**✅ Use Case:**
*   Get User by ID: `GET /users/5`
*   Get Orders for a User: `GET /users/5/orders`
*   Delete a specific item: `DELETE /items/101`

**❌ Bad Usage:**
*   `GET /users/active` (where 'active' is a status, not an ID. Better to use Query Param or separate endpoint).

---

## 2. Query Parameters (`@RequestParam`)

### **"The Filter-er"**

*   **Syntax:** `/users?role=admin&age=25`
*   **Annotation:** `@RequestParam`
*   **Purpose:** To **Sort, Filter, or Search** existing resources.
*   **Rule:** If the parameter is optional, or affects the list of results (metadata), use Query Params.

**✅ Use Case:**
*   Filtering: `GET /products?color=red`
*   Pagination: `GET /products?page=2&size=10`
*   Sorting: `GET /products?sort=price_asc`
*   Search: `GET /users?q=john`

**❌ Bad Usage:**
*   `GET /users?id=5` (This works, but strictly speaking, ID 5 is a specific resource, so `/users/5` is more RESTful).

---

## 3. Request Body (`@RequestBody`)

### **"The Payload"**

*   **Syntax:** JSON in Body `{ "name": "John", "age": 30 }`
*   **Annotation:** `@RequestBody`
*   **Purpose:** To transmit **Complex Data** that doesn't fit in a simple string query. Used for Creation (`POST`) and Updates (`PUT`/`PATCH`).
*   **Rule:** If you are saving data, or the data structure is complex (Objects, Lists), use Body.

**✅ Use Case:**
*   Create User: `POST /users` (Body: User details)
*   Update Order: `PUT /orders/1` (Body: New status, dates)
*   Search (Complex): `POST /search` (If search criteria is too huge for URL).

---

## Summary Cheat Sheet

| Scenario | Use Strategy | Example URL |
| :--- | :--- | :--- |
| **"I want a specific thing"** | **Path Variable** | `/employee/101` |
| **"I want to filter/sort the list"** | **Query Param** | `/employee?dept=IT` |
| **"I want to create a new thing"** | **Request Body** | `POST /employee` |
| **"I want to update a thing"** | **Path Variable** (ID) + **Body** (Data) | `PUT /employee/101` |

