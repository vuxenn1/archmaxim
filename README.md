# Archmaxim: Smart University File Archive

A university file archive that stores course materials by their **relationships** (course, lecturer, type, term) rather than in fixed folders, and lets users find them through **natural-language semantic search** in Turkish or English.

## Features

- **Semantic search**: find files by meaning, not exact keywords, across languages ("programlama 2 vize" finds the Computer Programming II midterm).
- **Query understanding**: course aliases ("BP2"), course codes ("BIL201") and typo-tolerant course names are recognised before the search, and files of that course rank higher. Turkish characters and roman numerals are handled ("Programlama II" matches "programlama 2").
- **Filters**: type, season, year, department, and "My courses" for lecturers and students.
- **Roles**:
  - Admins manage faculties, departments, courses, aliases, accounts, course assignments and enrollments.
  - Lecturers upload files to the courses they are assigned to.
  - Students search and download.
- **Background indexing**: files are embedded on upload. If the embedding service is down at that moment, they are picked up automatically a few minutes later.
- **Security**:
  - JWT login with expiry and a login lockout after repeated failures.
  - Every endpoint requires a role.
  - Passwords are hashed with BCrypt.
  - The web server adds a strict Content-Security-Policy.

## Architecture

Four services, orchestrated with Docker Compose:

| Service | Technology | Purpose |
|---------|-----------|---------|
| **frontend** | HTML, CSS, JavaScript, nginx | Web interface; proxies `/api` to the backend |
| **backend** | Java 25, Spring Boot 4 | REST API, business logic, authentication |
| **embedding** | Python, FastAPI, BGE-M3 | Turns text into 1024-dimensional vectors |
| **db** | PostgreSQL 17 + pgvector | Data storage and vector similarity search |

The backend asks the embedding service for a vector of every file's searchable text (course, aliases, department, type, term, lecturer, file name and description) and stores it in PostgreSQL. A search query is embedded the same way and ranked by cosine similarity using an HNSW index. The schema is managed with Flyway.

Only the frontend is published on the host. The browser talks to nginx, and nginx forwards `/api/...` to the backend inside the Docker network.

## Running the project

### Requirements
- Docker Desktop

### Steps
1. Create your configuration from the template and fill in the empty values:
   ```
   cp .env.example .env
   ```
   | Variable | What it is |
   |---|---|
   | `POSTGRES_PASSWORD` | Database password |
   | `ARCHMAXIM_JWT_SECRET` | Signing key for login tokens, at least 32 characters (`openssl rand -base64 48`) |
   | `ARCHMAXIM_ADMIN_EMAIL` | Email of the first admin account |
   | `ARCHMAXIM_ADMIN_PASSWORD` | Password of the first admin account, 8 to 72 characters |
   | `ARCHMAXIM_PORT` | Host port of the web interface (default 3000) |
   | `ARCHMAXIM_SWAGGER_ENABLED` | `true` serves the API docs |

2. Start everything from the project root:
   ```
   docker compose up --build
   ```
   The first run downloads the embedding model (about 4.5 GB on disk) and can take several minutes. Search answers with "starting up" until the model is ready.

3. Open **http://localhost:3000** and log in with the admin account from your `.env`.

A typical first setup:
1. Create a faculty, a department and some courses.
2. Add aliases for the courses.
3. Create lecturer and student accounts.
4. Assign lecturers to their courses, and enroll students.

Lecturers can only upload to courses they are assigned to.

### API documentation
Set `ARCHMAXIM_SWAGGER_ENABLED=true` in `.env` and restart. The docs are then at **http://localhost:3000/api/swagger-ui.html**.

### Reaching the backend or database directly
Only the web interface is published by default. To expose other services for debugging, add a `docker-compose.override.yml`:
```yaml
services:
  backend:
    ports: ["8080:8080"]
  db:
    ports: ["5432:5432"]
```

### Running the backend without Docker
Start PostgreSQL with pgvector and the embedding service, then:
```
cd archmaxim
SPRING_DATASOURCE_PASSWORD=... ARCHMAXIM_JWT_SECRET=... ARCHMAXIM_ADMIN_PASSWORD=... ./mvnw spring-boot:run
```
Defaults point to `localhost:5432` and `localhost:8000`. You can also keep personal settings in `archmaxim/src/main/resources/application-local.properties` (git-ignored) and run with `-Dspring-boot.run.profiles=local`.

## Tests

```
cd archmaxim
./mvnw verify
```
Unit, web and security tests run everywhere. The integration tests start a real PostgreSQL with pgvector through Testcontainers and are skipped when Docker is not available.

Embedding service tests (no model download needed):
```
cd archmaxim-embedding-service
pip install -r requirements-dev.txt
pytest
```

## Project structure
```
archmaxim/                     Spring Boot backend
archmaxim-embedding-service/   Python embedding service
archmaxim-frontend/            Web interface and nginx config
docker-compose.yml             Orchestrates all services
.env.example                   Configuration template
```

## Notes
- Built as a 20-day internship project.
- Semantic search uses the multilingual BGE-M3 model, running fully locally with no external API.
