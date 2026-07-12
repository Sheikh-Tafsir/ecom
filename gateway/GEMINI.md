# API Gateway Module

## Overview
Spring Cloud Gateway acting as the central entry point for the microservices ecosystem.

## Tech Stack
- **Framework:** Spring Cloud Gateway.
- **Build System:** Gradle.

## Architectural Mandates

### 1. Routing & Load Balancing
- **Path Mapping:** 
  - `/api/v1/chat/**` -> Routes to `server_chat`.
  - `/api/v1/**` (default) -> Routes to the core `server`.
- **Filters:** Use Global Filters for request logging and header normalization (e.g., stripping sensitive headers before forwarding).

### 2. Security & Cross-Cutting Concerns
- **CORS:** Centralized CORS configuration is managed here. Avoid configuring CORS in individual microservices.
- **Authentication:** Validates incoming JWTs and injects user identity headers into downstream requests.

### 3. Operational Resilience
- **Timeouts:** Define response timeouts for each route to prevent cascading failures.
- **Circuit Breaker:** (If configured) Use Resilience4j for failing fast when downstream services are unhealthy.

## Common Configuration Tasks
- **Route Updates:** Modify `application.yml` (or Java config) to add new service routes.
- **Error Standard:** Ensure the Gateway returns a standardized JSON error if a service is unreachable.
