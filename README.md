# Personal Finance Manager

A full-stack web application for managing personal finances, tracking expenses, and budget management.

## Tech Stack

**Backend:**
- Spring Boot
- MySQL
- Spring Data JPA
- Spring Security

**Frontend:**
- React

## Features

- User registration and authentication
- Expense tracking
- Budget management
- Financial reports and insights

## Getting Started

### Prerequisites

- JDK 17 or higher
- MySQL 8.0 or higher

### Backend Setup

1. Clone the repository
```bash
git clone https://github.com/Adib-Mahfuj/FinTrack.git
```

2. Configure database in `application.properties`
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/fintrackappdb
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. Run the application
```bash
./mvnw spring-boot:run
```

Backend will start on `http://localhost:8080`

### Frontend Setup
```bash
cd frontend
npm install
npm start
```

Frontend will start on `http://localhost:3000`

## API Endpoints

- `POST /api/v1.0/register`       - User registration
- `POST /api/v1.0/login`          - User Login
- `POST /api/v1.0/categories`     - Create Category
- `PUT /api/v1.0/categories/id`   - Update Category
- `GET /api/v1.0/categories`      - Read Categories
- `GET /api/v1.0/categories/type` - Read Categories By Type
- `POST /api/v1.0/expenses`       - Add Expense
- `GET /api/v1.0/expenses`        - Read Expenses
- `DELETE /api/v1.0/expenses/id`  - Delete Expenses
- `POST /api/v1.0/incomes`        - Add Income
- `GET /api/v1.0/incomes`         - Read Incomes
- `DELETE /api/v1.0/incomes`      - Delete Incomes
- `POST /api/v1.0/filter`         - Filter Transactions
- `GET /api/v1.0/dashboard`       - Dashboard Data

## Project Status

🚧 Currently in development

## License

MIT License

## Contact

Adib Mahfuj - [GitHub Profile](https://github.com/Adib-Mahfuj)
