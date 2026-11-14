package com.example.customers;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean active,
        Instant createdAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getFirstName(), c.getLastName(),
                c.getEmail(), c.getPhone(), c.isActive(), c.getCreatedAt());
    }

}
