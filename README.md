# Archmaxim — Smart University File Archive

A university file archive that stores course materials by their **relationships** (course, lecturer, type, term) rather than in fixed folders, and lets users find them through **natural-language semantic search** in Turkish or English.

## Features

- **Semantic search** — find files by meaning, not exact keywords, across languages (e.g. "programlama 2 vize" finds "Computer Programming II — Midterm").
- **Hybrid query understanding** — combines semantic embeddings, fuzzy matching for typos, and course abbreviation aliases (e.g. "BP2").
- **Search filters** — narrow by year, season, file type, and department.
- **Authentication & roles** — JWT-based login with three roles: admin, lecturer, student.
- **Role-based access** — admins manage the system, lecturers upload files, students search and download.
- **Admin console** — create faculties, departments, courses, lecturers, students, and aliases.

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
```

That's a solid, professional README — it explains what the project does, the architecture (with a nice table), exactly how to run it, and the default login. This is what makes a repo look finished rather than a code dump.

Create `README.md` in the projects root, then run:
```
git add .
git status | findstr README
```

Confirm it shows `README.md` staged, and tell me. Then Step 6 is the first commit, and Step 7 is pushing to GitHub (where you'll need your GitHub account).