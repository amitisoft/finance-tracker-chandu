# Personal Finance Tracker

Hackathon-ready full-stack app built with:

- Backend: Java 21, Spring Boot 3.3, Spring Security, JWT, Flyway, JPA, MySQL, H2 tests
- Frontend: React, Vite, TypeScript, TanStack Query, Zustand, Recharts, Vitest

## What is included

- JWT authentication with refresh tokens
- Accounts, categories, transactions, budgets, goals, recurring payments
- Dashboard and report endpoints
- Layered backend structure with custom exception handling
- Backend integration tests
- Frontend production build and frontend tests
- MySQL-first config with PostgreSQL-ready profile

## Backend run

From [`/backend`](/d:/CodexathanProject/backend):

```powershell
.\mvnw.cmd spring-boot:run
```

Default MySQL configuration expects:

- Database: `hackathon`
- Username: `root`
- Password: `root`

Override with environment variables:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/hackathon?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
$env:JWT_SECRET="your-very-long-secret-key"
```

## Frontend run

From [`/frontend`](/d:/CodexathanProject/frontend):

```powershell
npm.cmd install
npm.cmd run dev
```

Optional API override:

```powershell
$env:VITE_API_URL="http://localhost:8080"
```

## Tests

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm.cmd test
npm.cmd run build
```

## PostgreSQL switch later

The schema and service layer were kept database-portable. To move later:

1. Add PostgreSQL driver dependency if you want to drop MySQL entirely.
2. Start the backend with the `postgres` profile.
3. Point `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` to PostgreSQL.

Example:

```powershell
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_URL="jdbc:postgresql://localhost:5432/hackathon"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
```
