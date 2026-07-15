# ADR 001: JWT validation at the API Gateway

## Status

Accepted

## Context

CTU-Connect exposes multiple microservices behind a Spring Cloud Gateway. Clients send JWTs (Bearer or cookies) for protected routes. The gateway must validate tokens before forwarding traffic to downstream services.

## Decision

- Keep JWT validation in the **API Gateway** via `JwtAuthenticationFilter`, forwarding authenticated user claims to downstream services via headers where needed.
- **Auth service** remains the source of truth for issuing and refreshing tokens; public endpoints are explicitly permitted in `SecurityConfig` and receive **rate limiting** at the gateway (Redis-backed token bucket) to reduce brute-force and abuse.

## Consequences

- **Positive:** Single entry point for authentication checks; consistent rejection of invalid tokens before inner services.
- **Positive:** Scoped rate limits on `/api/auth/login`, `/api/auth/register`, and related public auth routes without touching WebSocket/chat routes.
- **Trade-off:** Gateway must reach Redis for rate limiting; Redis must be available when the gateway is deployed with rate limiting enabled.
