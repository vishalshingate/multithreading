package com.dto;

import java.time.LocalDate;

public class DtoDemo {

    public static void main(String[] args) {
        // 1. The Database Entity (Contains Sensitive Data)
        UserEntity dbUser = new UserEntity(1L, "john_doe", "SecretPassword123", "123-456-7890", LocalDate.of(1990, 1, 1));

        System.out.println("--- Scenario 1: Exposing Entity Directly (Bad) ---");
        // Problem: We just leaked the password and SSN to the frontend!
        System.out.println("API Response: " + dbUser);


        System.out.println("\n--- Scenario 2: Using DTO (Good) ---");
        // Solution: Map Entity -> DTO
        UserDTO userResponse = new UserDTO(dbUser.getUsername(), dbUser.getDateOfBirth());

        // Benefit: We only expose what the client needs. Password is gone.
        System.out.println("API Response: " + userResponse);
    }
}

// ==========================================
// 1. THE ENTITY (Database Representation)
// ==========================================
// Mimics a Hibernate/JPA Entity full of data
class UserEntity {
    private Long id;
    private String username;
    private String password; // SENSITIVE!
    private String ssn;      // SENSITIVE!
    private LocalDate dateOfBirth;

    public UserEntity(Long id, String username, String password, String ssn, LocalDate dateOfBirth) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.ssn = ssn;
        this.dateOfBirth = dateOfBirth;
    }

    // Getters
    public String getUsername() { return username; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }

    @Override
    public String toString() {
        return "UserEntity{id=" + id + ", username='" + username + "', password='" + password + "', ssn='" + ssn + "'}";
    }
}

// ==========================================
// 2. THE DTO (API Contract)
// ==========================================
// "Data Transfer Object" - Pure container for data moving between processes
class UserDTO {
    private String username;
    private int age; // Computed field! DTOs can reshape data.

    public UserDTO(String username, LocalDate dob) {
        this.username = username;
        this.age = LocalDate.now().getYear() - dob.getYear(); // Simple age calc
    }

    @Override
    public String toString() {
        return "UserDTO{username='" + username + "', age=" + age + "}";
    }
}

