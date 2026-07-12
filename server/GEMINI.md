# Server Module (Core Backend)

## Overview
A Spring Boot 3.x application providing core e-commerce logic (Product, Order, Stock, User/Role management).

## Tech Stack
- **Framework:** Spring Boot 3.x, Spring Data JPA, Spring Security.
- **Caching:** Redis via Spring Cache (TTL: 5m).
- **Serialization:** Custom Converters for Enums (e.g., `OrderStatusConverter`).

## Architectural Mandates

### 1. Domain Model & Entities
- **Auditing:** All persistent entities must extend `BaseEntity` (createdAt, updatedAt).
- **Encapsulation:** Domain logic should be encapsulated in `@Service` layers. Avoid "Anemic Domain Models" where possible by keeping business rules centralized in services like `ProductService.updateQuantity`.
- **Transactions:** Use `@Transactional` for all state-changing operations.

### 2. API & Controller Layer
- **Response Shape:** Every endpoint must return an `ApiResponse<T>` object.
- **DTOs:** Never expose Entities directly. Use specific Request/Response DTOs.
- **Validation:** Use JSR-303 (`@Valid`) on controllers.
- **Enums:** Use Spring `Converter` to map string path/query params to Enums automatically.

### 3. Caching Strategy
- **Dual Eviction:** When updating data that appears in both public and admin views (e.g., Products), use `@Caching` to evict both keys (`product` and `productEdit`).
- **Granularity:** Prefer key-based eviction (`key = "#id"`) over clearing entire cache values unless necessary.

### 4. Security
- **RBAC:** Use `@PreAuthorize` with `Permission` enum values (e.g., `hasAuthority(T(...).ADMIN_ACCESS.getValue())`).
- **Data Ownership:** Use `@PostAuthorize` to verify user ownership of resources (e.g., `returnObject.user.id == principal.id`).

## Testing Standards
- **Coverage:** Mandatory unit tests for all `@Service` logic.
- **Tooling:** JUnit 5, Mockito.
- **Style:** Use BDD-style naming: `givenX_whenY_thenZ`.
