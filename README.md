# Patient Management System

A microservices-based patient management system built with Spring Boot and gRPC communication.

## Architecture

This project consists of two main microservices:

- **Patient Service**: Manages patient information and data
- **Billing Service**: Handles billing operations and communicates with Patient Service via gRPC

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.5**
- **Spring Data JPA**
- **PostgreSQL** (Production database)
- **H2** (Development/Testing database)
- **gRPC** (Inter-service communication)
- **Maven** (Build tool)
- **Docker** (Containerization)
- **Swagger/OpenAPI** (API documentation)

## Project Structure

```
patient-managment/
├── patient-service/          # Patient management microservice
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── billing-service/          # Billing microservice
│   ├── src/
│   └── pom.xml
├── grpc-requests/           # gRPC request examples
│   └── create-billing-account.http
└── README.md
```

## Services

### Patient Service
- **Port**: 8080 (default)
- **Database**: PostgreSQL/H2
- **Features**:
  - Patient CRUD operations
  - Data validation
  - RESTful API
  - Swagger documentation available at `/swagger-ui.html`

### Billing Service  
- **Port**: 8081 (default)
- **Features**:
  - Billing account management
  - gRPC client for Patient Service communication
  - RESTful API

## Prerequisites

- Java 21
- Maven 3.6+
- Docker (optional, for containerization)
- PostgreSQL (for production)

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/patient-managment.git
cd patient-managment
```

### 2. Build the Project

```bash
# Build all services
mvn clean install

# Or build individual services
cd patient-service
mvn clean install

cd ../billing-service
mvn clean install
```

### 3. Run the Services

#### Patient Service
```bash
cd patient-service
mvn spring-boot:run
```
The service will be available at `http://localhost:8080`

#### Billing Service
```bash
cd billing-service
mvn spring-boot:run
```
The service will be available at `http://localhost:8081`

### 4. Using Docker

If you prefer to use Docker:

```bash
# Build and run Patient Service
cd patient-service
docker build -t patient-service .
docker run -p 8080:8080 patient-service

# Build and run Billing Service  
cd ../billing-service
docker build -t billing-service .
docker run -p 8081:8081 billing-service
```

## Database Configuration

### Development (H2)
By default, the services use H2 in-memory database for development. No additional setup required.

### Production (PostgreSQL)
For production, configure PostgreSQL connection in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/patient_management
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## API Documentation

### Patient Service
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Billing Service
- Base URL: `http://localhost:8081`

## gRPC Communication

The Billing Service communicates with the Patient Service using gRPC. Protocol buffer definitions and generated classes are included in the project.

## Testing

Run tests for all services:
```bash
mvn test
```

Or test individual services:
```bash
cd patient-service
mvn test

cd ../billing-service  
mvn test
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Development Setup

### IDE Configuration
This project is configured for IntelliJ IDEA with appropriate run configurations and project settings.

### Code Style
- Follow standard Java conventions
- Use meaningful variable and method names
- Add appropriate comments and documentation

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

If you encounter any issues or have questions, please create an issue in the GitHub repository.
