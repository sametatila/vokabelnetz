# 🕸️ Vokabelnetz

An intelligent, open-source language learning platform that helps Turkish and English speakers learn German vocabulary using adaptive algorithms — no expensive AI APIs required.

[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white)](https://angular.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)

---

## 🎯 What is Vokabelnetz?

**Vokabelnetz** (German for "vocabulary network") combines two proven algorithms to create a personalized learning experience:

| Algorithm | Purpose |
|-----------|---------|
| **SM-2 Spaced Repetition** | Optimizes *when* you review words (same as Anki) |
| **Elo Rating System** | Matches words to your skill level (adapted from chess) |

```
🇹🇷 Turkish Speakers ─────┐
                          ├────▶ 🇩🇪 German
🇬🇧 English Speakers ─────┘
```

---

## ✨ Features

### Core
- 📚 **Flashcard System** — Interactive vocabulary cards with translations
- 🌍 **Bilingual Support** — Switch between TR→DE and EN→DE modes
- 🔄 **Adaptive Review** — SM-2 schedules reviews at optimal intervals
- 📊 **Difficulty Matching** — Elo rating ensures words match your level
- 🔥 **Streak System** — Daily streaks with freeze protection
- 📈 **Progress Tracking** — Detailed statistics and visualizations

### Technical
- 🔐 JWT Authentication
- 📱 Responsive Design
- 🐳 Docker Support
- 📖 OpenAPI Documentation
- ✅ Comprehensive Tests

---

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Git

### Run with Docker

```bash
# Clone repository
git clone https://github.com/yourusername/vokabelnetz.git
cd vokabelnetz

# Start all services
docker-compose up -d

# Access the application
open http://localhost:4200
```

### Run Locally

```bash
# Backend (requires Java 25, PostgreSQL 18)
cd vokabelnetz-backend
./mvnw spring-boot:run

# Frontend (requires Node.js 24)
cd vokabelnetz-frontend
npm install
npm start
```

---

## 📁 Project Structure

```
vokabelnetz/
├── vokabelnetz-backend/     # Spring Boot API
├── vokabelnetz-frontend/    # Angular SPA
├── docker/                  # Docker configurations
├── data/                    # Vocabulary JSON files
├── docs/                    # Documentation
│   ├── ARCHITECTURE.md      # System design & patterns
│   ├── DATABASE.md          # Schema & data seeding
│   ├── ALGORITHMS.md        # SM-2, Elo, Streak logic
│   ├── API.md               # REST API reference
│   ├── DEPLOYMENT.md        # Docker, Nginx, CI/CD
│   └── CONTRIBUTING.md      # Contribution guide
│   └── SECURITY.md          # Security documentation
├── docker-compose.yml
└── README.md
```

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Architecture](docs/ARCHITECTURE.md) | System design, state management, language system |
| [Database](docs/DATABASE.md) | Schema, migrations, data seeding strategy |
| [Algorithms](docs/ALGORITHMS.md) | SM-2, Elo rating, streak system details |
| [API Reference](docs/API.md) | Complete REST API documentation |
| [Security](docs/SECURITY.md) | Authentication, authorization, token management |
| [Deployment](docs/DEPLOYMENT.md) | Docker, Nginx, CI/CD pipelines |
| [Contributing](docs/CONTRIBUTING.md) | How to contribute to the project |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| **Frontend** | Angular 21, TypeScript, Tailwind CSS |
| **Backend** | Java 25, Spring Boot 4.0, Spring Security 7 |
| **Database** | PostgreSQL 18, Hibernate 7.1 |
| **Infrastructure** | Docker, Nginx, GitHub Actions |

---

## 📊 Dataset

Vocabulary based on official **Goethe Institut** word lists:

| Level | Words | Description |
|-------|-------|-------------|
| A1 | ~650 | Basic vocabulary |
| A2 | ~1,300 | Elementary vocabulary |
| B1 | ~2,400 | Intermediate vocabulary |

---

## 🗺 Roadmap

- [x] Core flashcard system
- [x] SM-2 + Elo algorithms
- [x] User authentication
- [x] Progress tracking
- [ ] Gamification features
- [ ] Community features
- [ ] Advanced statistics & insights

---

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guide](docs/CONTRIBUTING.md) for details.

```bash
# Fork & clone
git clone https://github.com/yourusername/vokabelnetz.git

# Create feature branch
git checkout -b feature/amazing-feature

# Commit changes
git commit -m 'Add amazing feature'

# Push & create PR
git push origin feature/amazing-feature
```

---

## 📄 License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)** - see the [LICENSE](LICENSE) file for details.

This means:
- ✅ You can use, modify, and distribute this software
- ✅ You must disclose source code when distributing
- ✅ You must use the same license for derivative works
- ⚠️ **Network use is distribution** - if you run a modified version as a web service, you must make your source code available to users

---

## 🙏 Acknowledgements

- [Goethe Institut](https://www.goethe.de/) — Vocabulary word lists
- [Anki](https://apps.ankiweb.net/) — SM-2 algorithm inspiration
- [Chess.com](https://www.chess.com/) — Elo rating system reference

---

## 📬 Contact

- **GitHub Issues** — Bug reports & feature requests
- **Discussions** — Questions & ideas

---

<p align="center">
  Made with ❤️ for German learners
</p>
