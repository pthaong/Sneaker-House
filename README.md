# Sneaker House — BE1 Authentication API

Starter backend Spring Boot 3.4 / Java 17 / SQL Server for BE1 auth.

## Endpoints
- `POST /api/auth/register` — `{ "email":"user@example.com", "password":"Password123", "fullName":"Nguyen Van A" }`
- `POST /api/auth/login` — `{ "email":"user@example.com", "password":"Password123" }`
- `POST /api/auth/google` — `{ "idToken":"<Google ID token from Google Identity Services>" }`
- `POST /api/auth/forgot-password` — `{ "email":"user@example.com" }`
- `POST /api/auth/reset-password` — `{ "token":"<token from email>", "newPassword":"NewPassword123" }`
- `POST /api/auth/logout`
- `GET /api/auth/me`

Successful register/login responses return user JSON. Login uses a server-side HTTP session cookie (`JSESSIONID`). Browser clients must use credentials and send CSRF token for state-changing requests after security is configured for the deployed frontend.

## Setup
1. Install JDK 17+ and Maven (or use Maven wrapper if the team repository has one).
2. Create SQL Server database `SneakerHouse`.
3. Set environment variables in PowerShell before starting:
   ```powershell
   $env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=SneakerHouse;encrypt=true;trustServerCertificate=true"
   $env:DB_USERNAME="your_sql_login"
   $env:DB_PASSWORD="your_sql_password"
   $env:GOOGLE_CLIENT_ID="your-google-web-client-id.apps.googleusercontent.com"
   $env:FRONTEND_URL="http://localhost:3000"
   $env:MAIL_HOST="smtp.gmail.com"
   $env:MAIL_PORT="587"
   $env:MAIL_USERNAME="your-sender@gmail.com"
   $env:MAIL_PASSWORD="your-google-app-password"
   $env:MAIL_FROM="your-sender@gmail.com"
   ```
4. Run `mvn spring-boot:run`.
5. Set Google OAuth Web Client ID in Google Cloud Console and configure frontend authorized origins (for local Vite, add `http://localhost:5173`; for React default, `http://localhost:3000`).
6. Set exact frontend origin in `SecurityConfig.corsConfigurationSource()` for deployment. Never commit real passwords or OAuth secrets.

## Important integration note
This is a standalone starter because the repository branches inspected so far contain only `.gitignore` and `README.md`; merge it into the team's agreed backend structure before integrating with frontend. CSRF is enabled by Spring Security defaults. Frontend must obtain/send the CSRF token for POST requests once session auth is enabled. Add a dedicated CSRF-token endpoint/cookie flow before production use. Reset tokens are stored as SHA-256 hashes, expire after 30 minutes, and are single-use. SMTP must be configured for reset emails.
