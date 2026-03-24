# Security Policy

## Supported Versions

This project is under active development. Security fixes are applied to the latest `main` branch.

## Reporting a Vulnerability

Please report vulnerabilities responsibly and do not disclose them publicly before a fix is available.

1. Send details to maintainers via private channel (email or private issue).
2. Include reproduction steps, impact, and affected components.
3. If possible, suggest a remediation path.

Expected response timeline:
- Acknowledgement: within 72 hours
- Initial triage: within 7 days
- Remediation target: based on severity and release window

## Security Controls In Place

- JWT-based authentication and refresh token flow
- Password hashing with BCrypt
- reCAPTCHA validation for bot protection in auth flows
- Request validation in API DTOs (Bean Validation)
- XSS mitigation in frontend using DOM sanitization
- Centralized exception handling in backend services

## Contributor Security Checklist

Before opening a PR:

- Do not commit `.env` or any secrets.
- Use environment variables for credentials.
- Validate all request inputs (`@Valid`, `@NotBlank`, `@Size`, etc.).
- Avoid exposing test/debug endpoints in production profiles.
- Ensure dependencies are up to date and free of known vulnerabilities.

## Dependency Update Policy

- Dependabot is enabled for Maven and npm ecosystems.
- Security updates should be reviewed and merged with high priority.
- Patch/minor updates should be applied regularly.
- Major updates require compatibility validation and smoke testing.
