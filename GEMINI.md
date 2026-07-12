# E-Commerce System (Root)

## Overview
A microservices-based e-commerce platform featuring real-time chat, inventory management, and a modern React frontend.

## System Architecture
The system is composed of four primary modules:
1.  **Gateway:** The entry point for all API requests.
2.  **Server:** Core business logic and inventory management (Spring Boot).
3.  **Chat Server:** Real-time messaging service (Node.js/Socket.io).
4.  **Client:** The React-based user interface.

## Module Documentation
For detailed architectural mandates and coding standards, refer to the following module-specific instructions:
- [Gateway Instructions](./gateway/GEMINI.md)
- [Server Instructions](./server/GEMINI.md)
- [Chat Server Instructions](./server_chat/GEMINI.md)
- [Client Instructions](./client/GEMINI.md)

## Development Workflow
- **Monorepo:** All services are contained within this repository.
- **Docker:** Use `docker-compose.yml` for local environment orchestration.
- **CI/CD:** Standards for testing and deployment are defined in each module's specific instructions.
