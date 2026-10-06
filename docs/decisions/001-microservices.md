# ADR 001 — Microservices Architecture

**Status:** Accepted  
**Date:** 2024

## Decision

Split the application into four independently deployable services: Gateway, Main Server, Chat Server, and Client.

## Why

- **Chat Server** has fundamentally different runtime requirements (stateful WebSocket connections, Node.js event loop) than the REST API (Spring Boot thread-per-request model). Mixing them in one process would require concurrency compromises.
- **Gateway** needs to be the single authority for cross-cutting concerns (CORS, JWT validation, rate limiting, logging). A dedicated service prevents these from being duplicated or inconsistently applied across other services.
- **Horizontal scaling** — each service can be scaled independently. The chat server can scale out without touching the inventory/order logic.

## Trade-offs Accepted

| ✅ Benefit                             | ⚠️ Cost                                      |
|----------------------------------------|----------------------------------------------|
| Independent deployability per service  | More operational complexity (4 Dockerfiles)  |
| Polyglot: Node.js for chat, JVM for REST | Shared DB — schema ownership must be disciplined |
| Isolated failure domains               | Network hops between services                |

## Constraints

- All services share one PostgreSQL instance. Each service owns its own tables.
- Inter-service communication is HTTP only (via Gateway or direct Docker internal network). No shared in-process calls.
