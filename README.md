# 🌐 TerraRent Backend (Spring Boot Core)

Welcome to the **TerraRent Backend**, a secure, high-performance RESTful API built with **Spring Boot** and **Java 17**. This service powers the TerraRent portal, handling identity management, property listings, messaging, booking management, reviews, and external API integrations with Booking.com.

---

## 🛠️ Tech Stack & Architecture

- **Core Framework**: Spring Boot 3.2.2 & Java 17 (LTS)
- **Database Access**: Spring Data JPA (Hibernate 6)
- **Primary Database**: PostgreSQL (for persistent user, booking, property, and message records)
- **In-Memory Database**: H2 Database (optional, configured for local staging/testing)
- **Security & Session**: Spring Security 6 & JWT (JSON Web Tokens)
- **API Documentation**: Swagger UI & OpenAPI 3 (Springdoc WebMVC)
- **Tooling**: Lombok (for boilerplate reduction), Maven Wrapper

---

## ⚙️ Prerequisites

Before launching the backend, ensure you have:
1. **Java 17 JDK** installed and configured in your environment path.
2. **PostgreSQL** running locally on port `5432`.
3. A PostgreSQL database named **`terrarent_db`**.
   - *Default Username*: `postgres`
   - *Default Password*: `2007`

---

## 🚀 Getting Started Locally

### 1. Database Configuration
The application relies on `src/main/resources/application.yml`. By default, it connects to PostgreSQL at:
`jdbc:postgresql://localhost:5432/terrarent_db`

You can override these values in your environment variables if needed:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### 2. Automatic Role & Admin Seeder
To simplify local setup, the backend contains an automatic seeder (`AdminSeeder.java`). On the first run:
- It creates the roles `ROLE_RENTER`, `ROLE_LANDLORD`, and `ROLE_ADMIN` if they do not exist.
- It seeds a default administrator account:
  - **Email**: `admin@terrarent.com`
  - **Password**: `Admin123!`

### 3. Run the Server
Use the Maven wrapper to compile and run the application:

```bash
# In the terrarent-backend directory
./mvnw.cmd spring-boot:run
```

The server will start up and listen on port **`8081`**.

---

## 🗺️ API Documentation (Swagger)

Once the backend is running, you can explore, test, and interact with all endpoints through the Swagger UI:
- **Swagger URL**: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)

---

## 📂 Core Endpoints & Components

| Component | Responsibility | Key Endpoints |
| :--- | :--- | :--- |
| **Authentication** | Sign up, verification emails, JWT logins | `/api/auth/register`, `/api/auth/login` |
| **Properties** | Public listings, map search, filtering | `/api/properties`, `/api/properties/{id}` |
| **Bookings** | Creating reservations, payment records | `/api/bookings`, `/api/bookings/{id}` |
| **Landlord Hub** | Landlord profiles and property lists | `/api/landlords/profile`, `/api/landlords/properties` |
| **Renter Hub** | Renter dashboard, favorites lists | `/api/renters/profile`, `/api/renters/favorites` |
| **Messaging** | Direct landlord-renter chat channels | `/api/conversations`, `/api/messages` |
| **Booking.com API** | External booking integration | `/api/bookingcom/hotels` |

---

## 📦 Logs & Auditing
Application activity and database query logs are written to the console and stored in a rolling log file:
- **Log file path**: `terrarent-backend/backend.log` (configured at `com.terrarent: DEBUG`)
