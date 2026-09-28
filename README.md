# Campus Swap Backend

A marketplace REST API for college students to buy, sell, swap, or donate campus essentials.

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.1**
- **Spring Data JPA**
- **PostgreSQL**
- **Maven**
- **Lombok**
- **Bean Validation**

## Features

- User management (CRUD operations)
- Listing management with pagination and filtering
- Wishlist functionality
- Reporting system for suspicious listings
- Global exception handling
- Input validation
- Comprehensive logging

## Project Structure

```
src/main/java/com/campusswap/
├── controller/          # REST API endpoints
├── service/             # Business logic
├── repository/          # Data access layer
├── model/               # JPA entities
│   └── enums/          # Enum types
├── dto/                 # Data Transfer Objects
│   ├── request/        # Request DTOs
│   └── response/       # Response DTOs
├── mapper/              # Entity-DTO mappers
├── exception/           # Custom exceptions and handler
└── CampusSwapApplication.java
```

## Database Schema

### Entities

1. **User**
   - id, email, password, fullName, phoneNumber, college, hostelOrDorm, role
   - Relationships: One-to-Many with Listing, Wishlist, Report

2. **Listing**
   - id, title, description, price, category, transactionType, itemCondition, status
   - Relationships: Many-to-One with User, One-to-Many with ListingImage, Wishlist, Report

3. **ListingImage**
   - id, imageUrl, s3Key, isPrimary
   - Relationship: Many-to-One with Listing

4. **Wishlist**
   - id, addedAt
   - Relationships: Many-to-One with User and Listing
   - Unique constraint on (userId, listingId)

5. **Report**
   - id, reason, status, reportedAt, resolvedAt
   - Relationships: Many-to-One with User and Listing

### Enums

- Role: STUDENT, ADMIN
- Category: BOOKS, ELECTRONICS, STATIONERY, HOSTEL_ITEMS, OTHER
- TransactionType: SELL, SWAP, DONATE
- ItemCondition: NEW, LIKE_NEW, GOOD, FAIR, POOR
- ListingStatus: AVAILABLE, SOLD, REMOVED
- ReportStatus: PENDING, RESOLVED, DISMISSED

## Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL 15+

## Setup

1. **Install PostgreSQL**
   ```bash
   # Create database
   createdb campusswap
   ```

2. **Configure Database**
   
   Update `src/main/resources/application.yml` with your PostgreSQL credentials:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/campusswap
       username: your_username
       password: your_password
   ```

3. **Build the Project**
   ```bash
   mvn clean install
   ```

4. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

## API Endpoints

### User Endpoints

- `POST /api/users` - Create new user
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/email/{email}` - Get user by email
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Listing Endpoints

- `POST /api/listings?sellerId={id}` - Create listing
- `GET /api/listings/{id}` - Get listing by ID
- `GET /api/listings` - Get all listings (with pagination and filters)
  - Query params: search, category, transactionType, page, size, sortBy, sortDirection
- `GET /api/listings/user/{userId}` - Get user's listings
- `PUT /api/listings/{id}?userId={id}` - Update listing
- `DELETE /api/listings/{id}?userId={id}` - Delete listing
- `PATCH /api/listings/{id}/mark-sold?userId={id}` - Mark listing as sold

### Wishlist Endpoints

- `POST /api/wishlist?userId={id}&listingId={id}` - Add to wishlist
- `GET /api/wishlist/user/{userId}` - Get user's wishlist
- `DELETE /api/wishlist?userId={id}&listingId={id}` - Remove from wishlist
- `GET /api/wishlist/check?userId={id}&listingId={id}` - Check if in wishlist

### Report Endpoints

- `POST /api/reports?reporterId={id}` - Create report
- `GET /api/reports` - Get all reports (admin)
- `GET /api/reports/pending` - Get pending reports (admin)
- `GET /api/reports/listing/{listingId}` - Get reports for listing
- `PATCH /api/reports/{id}/resolve` - Resolve report (admin)
- `PATCH /api/reports/{id}/dismiss` - Dismiss report (admin)

## Testing

Run tests with:
```bash
mvn test
```

## Error Handling

The API uses standard HTTP status codes and returns error responses in the following format:

```json
{
  "timestamp": "2024-01-20T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Error description",
  "path": "/api/endpoint",
  "validationErrors": {
    "field": "error message"
  }
}
```

## Development Notes

- The application uses `ddl-auto: update` for automatic schema generation in development
- All entities have automatic `createdAt` and `updatedAt` timestamps
- Proper indexes are added for performance optimization
- Input validation is enforced using Bean Validation annotations
- All service methods are transactional
- Comprehensive logging is implemented at INFO and DEBUG levels

## Next Steps

- Implement JWT authentication and Spring Security
- Add AWS S3 integration for image uploads
- Implement email notifications
- Add integration tests
- Set up CI/CD pipeline
- Deploy to AWS

## License

Campus Swap - MVP for Hackathon
