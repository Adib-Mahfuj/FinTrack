<div align="center">

# FinTrack — Backend API

**RESTful API for a personal finance management application.**
Secure authentication, income and expense tracking, category management, advanced filtering, Excel export, and email notifications.

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.0-6DB33F?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?logo=springsecurity&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Overview](#api-overview)
- [Authentication](#authentication)
- [Frontend](#frontend)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Author](#author)

---

## Overview

FinTrack Backend is a Spring Boot REST API that powers the FinTrack personal finance manager. It handles user authentication, transaction management, category management, filtering, spreadsheet export, and scheduled email reminders. The service is stateless and secured with JWT.

## Features

- **JWT authentication** — Stateless, token-based security with Spring Security
- **Income management** — Full CRUD for income records
- **Expense management** — Full CRUD for expense records
- **Category management** — Income and expense categories with duplicate-name validation
- **Advanced filtering** — Filter by type, date range, keyword, and sort order
- **Excel export** — Download transactions as an `.xlsx` file
- **Email delivery** — Transactional emails and daily reminders through Brevo
- **Profile support** — User profile data with photo URL storage
- **Database flexibility** — MySQL for local development, PostgreSQL for deployment

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.0 |
| Security | Spring Security, JWT |
| Persistence | Spring Data JPA (Hibernate) |
| Database | MySQL (local), PostgreSQL (production) |
| Email | Brevo |
| Utilities | Lombok |
| Build tool | Maven |

## Architecture

The project follows a layered architecture with a clear separation of concerns:

```text
Client (React)
      │
      ▼
 Controller   → Handles HTTP requests and responses
      │
      ▼
  Service     → Business logic
      │
      ▼
 Repository   → Data access (Spring Data JPA)
      │
      ▼
  Database    → MySQL / PostgreSQL
```

DTOs are used to separate API contracts from JPA entities.

## Project Structure

```text
FinTrack/
├── src/
│   ├── main/
│   │   ├── java/.../
│   │   │   ├── config/        # Security configuration
│   │   │   ├── controller/    # REST controllers
│   │   │   ├── service/       # Business logic
│   │   │   ├── repository/    # Spring Data JPA repositories
│   │   │   ├── exception/     # Global exception handler
│   │   │   ├── entity/        # JPA entities
│   │   │   ├── dto/           # Request and response objects
│   │   │   ├── security/      # JWT filter and security configuration
│   │   │   ├── util/          # JWT util and Transaction validator
│   │   │   └── ...
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── pom.xml
└── README.md
```

## Getting Started

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- MySQL 8+ (local development)
- A [Brevo](https://www.brevo.com/) account for email delivery

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/Adib-Mahfuj/FinTrack.git

# 2. Move into the project directory
cd FinTrack

# 3. Create the local database
mysql -u ***REMOVED*** -p -e "CREATE DATABASE fintrack;"

# 4. Configure the application (see the next section)

# 5. Run the application
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Build a production JAR

```bash
mvn clean package
java -jar target/*.jar
```

## Configuration

Configure the application in `src/main/resources/application.properties`, or use environment variables. Do not commit real secrets.

```properties
server.port=8080

# Database (MySQL for local development)
spring.datasource.url=jdbc:mysql://localhost:3306/fintrack
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=your_long_random_secret

# Brevo (email)
brevo.api.key=your_brevo_api_key
```

| Setting | Description |
| --- | --- |
| `server.port` | Port the API listens on (default `8080`) |
| `spring.datasource.*` | Database connection details |
| `jwt.secret` | Secret key used to sign JWT tokens |
| `brevo.api.key` | API key used for sending email through Brevo |

**Switching to PostgreSQL for deployment:** replace the datasource URL with a PostgreSQL JDBC URL and make sure the PostgreSQL driver is included in `pom.xml`.

## API Overview

| Module | Base Path | Description |
| --- | --- | --- |
| Authentication | — | Registration, login, and account access |
| Categories | — | Create, read, and update categories |
| Income | `/incomes` | Manage income records |
| Expense | `/expenses` | Manage expense records |
| Filter | `/filter` | Filter and sort transactions |

All endpoints except the public authentication routes require a valid JWT.

## Authentication

Protected requests must include the token in the `Authorization` header:

```http
Authorization: Bearer <your_jwt_token>
```

## Frontend

The React client lives in a separate repository:
[Adib-Mahfuj/FinTrack-Frontend](https://github.com/Adib-Mahfuj/FinTrack-Frontend)

## Roadmap

- [ ] Deploy the API (live URL will be added here)
- [ ] Add API documentation with OpenAPI / Swagger

## Contributing

Contributions, issues, and feature requests are welcome.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push the branch: `git push origin feature/your-feature`
5. Open a Pull Request

## License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for details.

## Author

**Adib Mahfuj**
Junior Full-Stack Developer (React & Spring Boot)

- GitHub: [@Adib-Mahfuj](https://github.com/Adib-Mahfuj)
- LinkedIn: [Adib Mahfuj](https://www.linkedin.com/in/adib-mahfuj-669493398)
- Email: adibmahfuj.dev@gmail.com
