# Archmaxim: Smart University File Archive

A university file archive that stores course materials by their **relationships** (course, lecturer, type, term) rather than in fixed folders, and lets users find them through **natural-language semantic search** in Turkish or English.

## Features

- **Semantic search**: Find files by meaning, not exact keywords, across languages (e.g. "programlama 2 vize" finds "Computer Programming II, Midterm").
- **Hybrid query understanding**: Combines semantic embeddings, fuzzy matching for typos, and course abbreviation aliases (e.g. "BP2").
- **Search filters**: Narrow by year, season, file type and department.
- **Authentication & roles**: JWT-based login with three roles: admin, lecturer, student.
- **Role-based access**: Admins manage the system, lecturers upload files, students search and download.
- **Admin console**: Create faculties, departments, courses, lecturers, students and aliases.

## Architecture

Four services, orchestrated with Docker Compose:

| Service | Technology | Purpose |
|---------|-----------|---------|
| **backend** | Java 26, Spring Boot | REST API, business logic, authentication |
| **embedding** | Python, FastAPI, BGE-M3 | Converts text to 1024-dim vectors |
| **db** | PostgreSQL 17 + pgvector | Data storage and vector similarity search |
| **frontend** | HTML / CSS / JavaScript | Web interface |

The backend calls the embedding service to turn queries and files into vectors, stores them in PostgreSQL via the pgvector extension, and ranks results by cosine similarity.

## Running the project

### Requirements
- Docker Desktop

### Steps
1. Copy the configuration template and fill in your own values:
   ```
   cp archmaxim/src/main/resources/application.properties.example archmaxim/src/main/resources/application.properties
   ```
2. From the project root, start everything:
   ```
   docker compose up --build
   ```
   The first run downloads the embedding model (~2 GB) and may take several minutes.
3. Open the app at **http://localhost:3000**

### Default admin account
On first run, an admin account is created automatically:
- **Email:** admin@archmaxim.edu
- **Password:** admin123

Use it to create faculties, departments, courses, and user accounts.

## Project structure
```
archmaxim/                     Spring Boot backend
archmaxim-embedding-service/   Python embedding microservice
archmaxim-frontend/            Web interface
docker-compose.yml             Orchestrates all services
```

## Notes
- Built as a 20-day internship project.
- Semantic search uses the multilingual BGE-M3 model, running fully locally (no external API).