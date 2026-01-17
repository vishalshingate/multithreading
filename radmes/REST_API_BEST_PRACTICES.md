# Best Practices for REST APIs

This document outlines the industry standards and best practices for designing and implementing RESTful APIs.

## 1. Use Nouns, Not Verbs
URIs should refer to resources (nouns) rather than actions (verbs). The action is defined by the HTTP method used.

- **Bad**: `/getUsers`, `/createUser`, `/deleteUser/123`
- **Good**: `GET /users`, `POST /users`, `DELETE /users/123`

## 2. Use Plural Nouns
Keep URIs consistent by always using plural nouns for collections.

- **Example**: `/users`, `/users/123`, `/orders`, `/products`

## 3. Standard HTTP Methods
Use the appropriate HTTP method for the intended operation:

| Method   | Description |
| -------- | ----------- |
| `GET`    | Retrieve a resource or collection. |
| `POST`   | Create a new resource. |
| `PUT`    | Update/Replace an existing resource (full replacement). |
| `PATCH`  | Partially update an existing resource. |
| `DELETE` | Remove a resource. |

## 4. Use HTTP Status Codes
Return the correct status code to communicate the result of the request.

### 1xx: Informational
- **100 Continue**: The server has received the request headers and the client should proceed to send the request body.
- **101 Switching Protocols**: The requester has asked the server to switch protocols.

### 2xx: Success
- **200 OK**: The standard response for successful HTTP requests.
- **201 Created**: The request has been fulfilled, resulting in the creation of a new resource.
- **202 Accepted**: The request has been accepted for processing, but the processing has not been completed.
- **204 No Content**: The server successfully processed the request and is not returning any content.

### 3xx: Redirection
- **301 Moved Permanently**: This and all future requests should be directed to the given URI.
- **302 Found**: Tells the client to look at (browse to) another URL.
- **304 Not Modified**: Indicates that the resource has not been modified since the version specified by the request headers.

### 4xx: Client Error
- **400 Bad Request**: The server cannot or will not process the request due to an apparent client error.
- **401 Unauthorized**: Similar to 403 Forbidden, but specifically for use when authentication is required and has failed or has not yet been provided.
- **403 Forbidden**: The request was valid, but the server is refusing action. The user might not have the necessary permissions.
- **404 Not Found**: The requested resource could not be found but may be available in the future.
- **405 Method Not Allowed**: A request method is not supported for the requested resource.
- **408 Request Timeout**: The server timed out waiting for the request.
- **409 Conflict**: Indicates that the request could not be processed because of conflict in the current state of the resource.
- **429 Too Many Requests**: The user has sent too many requests in a given amount of time (Rate Limiting).

### 5xx: Server Error
- **500 Internal Server Error**: A generic error message, given when an unexpected condition was encountered.
- **501 Not Implemented**: The server either does not recognize the request method, or it lacks the ability to fulfill the request.
- **502 Bad Gateway**: The server was acting as a gateway or proxy and received an invalid response from the upstream server.
- **503 Service Unavailable**: The server cannot handle the request (because it is overloaded or down for maintenance).
- **504 Gateway Timeout**: The server was acting as a gateway or proxy and did not receive a timely response from the upstream server.

## 5. Versioning
Always version your API to prevent breaking changes for existing clients. Versioning is usually done in the URI or via headers.

- **URI Versioning**: `/api/v1/users`
- **Header Versioning**: `Accept: application/vnd.example.v1+json`

## 6. Filtering, Sorting, and Pagination
For endpoints that return collections, provide ways to manage the data.

- **Filtering**: `GET /users?role=admin`
- **Sorting**: `GET /users?sort=name,asc`
- **Pagination**: `GET /users?page=2&size=50`

## 7. Error Handling
Return consistent error messages with helpful information.

```json
{
  "error": "Resource Not Found",
  "message": "User with ID 123 does not exist.",
  "status": 404,
  "timestamp": "2026-01-16T10:00:00Z"
}
```

## 8. Use JSON
JSON is the standard for modern REST APIs. Ensure your API sets the `Content-Type: application/json` header.

## 9. Security
- **Use HTTPS**: Always encrypt data in transit.
- **Authentication**: Use standard protocols like OAuth2 or JWT.
- **Input Validation**: Never trust client input; validate everything on the server.

## 10. Documentation
Document your API using tools like **Swagger (OpenAPI)**. Clear documentation is essential for developer adoption.
