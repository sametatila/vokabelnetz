# 🤝 Contributing Guide

Thank you for your interest in contributing to Vokabelnetz! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Git Workflow](#git-workflow)
- [Pull Request Process](#pull-request-process)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)

---

## Code of Conduct

We are committed to providing a welcoming and inclusive environment. Please:

- Be respectful and inclusive
- Use welcoming and inclusive language
- Accept constructive criticism gracefully
- Focus on what is best for the community
- Show empathy towards other community members

---

## Getting Started

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 25+ | Backend development |
| Node.js | 24+ | Frontend development |
| PostgreSQL | 18+ | Database |
| Docker | Latest | Containerization |
| Git | Latest | Version control |

### Fork & Clone

```bash
# Fork the repository on GitHub, then:
git clone https://github.com/YOUR_USERNAME/vokabelnetz.git
cd vokabelnetz

# Add upstream remote
git remote add upstream https://github.com/vokabelnetz/vokabelnetz.git
```

---

## Development Setup

### Option 1: Docker (Recommended)

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Access the application
# Frontend: http://localhost:4200
# Backend:  http://localhost:8080
# Database: localhost:5432
```

### Option 2: Local Development

**Backend:**
```bash
cd vokabelnetz-backend

# Start PostgreSQL (or use Docker)
docker run -d --name vokabelnetz-db \
  -e POSTGRES_DB=vokabelnetz \
  -e POSTGRES_USER=vokabelnetz \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 postgres:18

# Run backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Frontend:**
```bash
cd vokabelnetz-frontend

# Install dependencies
npm install

# Start development server
npm start

# Access at http://localhost:4200
```

### Environment Configuration

Create `.env` file in project root:

```bash
# Copy example
cp .env.example .env

# Edit with your values
nano .env
```

---

## Project Structure

```
vokabelnetz/
├── vokabelnetz-backend/          # Spring Boot Backend
│   ├── src/main/java/
│   │   └── com/vokabelnetz/
│   │       ├── config/           # Configuration classes
│   │       ├── controller/       # REST controllers
│   │       ├── service/          # Business logic
│   │       ├── algorithm/        # SM-2, Elo implementations
│   │       ├── repository/       # Data access
│   │       ├── entity/           # JPA entities
│   │       ├── dto/              # Data transfer objects
│   │       ├── exception/        # Custom exceptions
│   │       └── security/         # JWT, auth
│   └── src/test/                 # Tests
│
├── vokabelnetz-frontend/         # Angular Frontend
│   ├── src/app/
│   │   ├── core/                 # Singleton services, guards
│   │   │   ├── services/
│   │   │   ├── state/            # Signal stores
│   │   │   ├── guards/
│   │   │   └── interceptors/
│   │   ├── features/             # Feature modules
│   │   │   ├── auth/
│   │   │   ├── dashboard/
│   │   │   ├── learning/
│   │   │   ├── progress/
│   │   │   └── settings/
│   │   └── shared/               # Shared components
│   └── src/assets/
│       ├── i18n/                 # Translations
│       └── audio/                # Pronunciation files
│
├── docs/                         # Documentation
├── docker/                       # Docker configs
└── data/                         # Seed data (JSON)
```

---

## Coding Standards

### Java (Backend)

**Style Guide:** Google Java Style Guide

```java
// ✅ Good: Clear naming, proper annotations
@Service
@Transactional(readOnly = true)
public class WordService {
    
    private final WordRepository wordRepository;
    
    public WordService(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
    }
    
    public Optional<Word> findById(Long id) {
        return wordRepository.findById(id);
    }
}

// ❌ Bad: Field injection, unclear naming
@Service
public class WS {
    @Autowired
    WordRepository wr;
    
    public Word get(Long x) {
        return wr.findById(x).get();
    }
}
```

**Key Guidelines:**
- Use constructor injection (not `@Autowired` on fields)
- Use `Optional` instead of returning `null`
- Add `@Transactional` at service level
- Write meaningful Javadoc for public methods
- Use DTOs for API responses (not entities)

### TypeScript (Frontend)

**Style Guide:** Angular Style Guide

```typescript
// ✅ Good: Signals, typed, standalone
@Component({
  selector: 'app-word-card',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  template: `
    <div class="card">
      <h2>{{ word().german }}</h2>
      <p>{{ translation() }}</p>
    </div>
  `
})
export class WordCardComponent {
  word = input.required<Word>();
  
  private languageService = inject(LanguageService);
  
  translation = computed(() => 
    this.languageService.getTranslation(this.word())
  );
}

// ❌ Bad: Any types, manual subscriptions
@Component({...})
export class WordCardComponent implements OnInit, OnDestroy {
  @Input() word: any;
  translation: string;
  private sub: Subscription;
  
  ngOnInit() {
    this.sub = this.languageService.language$.subscribe(lang => {
      this.translation = this.word[lang];
    });
  }
  
  ngOnDestroy() {
    this.sub.unsubscribe();
  }
}
```

**Key Guidelines:**
- Use standalone components
- Use signals (`signal`, `computed`, `effect`)
- Use `inject()` function instead of constructor injection
- Avoid `any` type
- Use strict TypeScript settings

### SQL

```sql
-- ✅ Good: Descriptive names, proper constraints
CREATE TABLE user_word_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    ease_factor DECIMAL(4,2) DEFAULT 2.5 
        CHECK (ease_factor >= 1.3 AND ease_factor <= 5.0),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, word_id)
);

-- ❌ Bad: Vague names, no constraints
CREATE TABLE uwp (
    id SERIAL,
    uid INT,
    wid INT,
    ef FLOAT
);
```

---

## Git Workflow

### Branch Naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/description` | `feature/add-streak-freeze` |
| Bug Fix | `fix/description` | `fix/login-token-expiry` |
| Docs | `docs/description` | `docs/api-endpoints` |
| Refactor | `refactor/description` | `refactor/learning-service` |

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting (no code change)
- `refactor`: Code restructuring
- `test`: Adding tests
- `chore`: Maintenance

**Examples:**
```bash
# Feature
git commit -m "feat(learning): add streak freeze functionality"

# Bug fix
git commit -m "fix(auth): handle expired refresh token correctly"

# Documentation
git commit -m "docs(api): add learning endpoints documentation"

# With body
git commit -m "feat(algorithm): implement Elo rating system

- Add EloRatingService with K-factor of 32
- Update user and word ratings after each answer
- Add unit tests for edge cases

Closes #42"
```

### Workflow

```bash
# 1. Sync with upstream
git checkout main
git pull upstream main

# 2. Create feature branch
git checkout -b feature/my-feature

# 3. Make changes and commit
git add .
git commit -m "feat(scope): description"

# 4. Push to your fork
git push origin feature/my-feature

# 5. Create Pull Request on GitHub
```

---

## Pull Request Process

### Before Submitting

```
□ Code compiles without errors
□ All tests pass locally
□ New features have tests
□ Documentation updated if needed
□ No console.log or debug statements
□ Code follows style guidelines
□ Commit messages follow convention
```

### PR Template

```markdown
## Description
Brief description of changes.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Related Issues
Closes #123

## Testing
Describe tests added or manual testing done.

## Screenshots (if applicable)
Add screenshots for UI changes.

## Checklist
- [ ] My code follows the project style guidelines
- [ ] I have performed a self-review
- [ ] I have added tests that prove my fix/feature works
- [ ] New and existing tests pass locally
- [ ] I have updated documentation accordingly
```

### Review Process

1. **Automated Checks**: CI must pass
2. **Code Review**: At least 1 approval required
3. **Testing**: Reviewer may test locally
4. **Merge**: Squash and merge to main

---

## Testing Guidelines

### Backend Tests

```java
// Unit Test Example
@ExtendWith(MockitoExtension.class)
class SpacedRepetitionServiceTest {
    
    @Mock
    private UserWordProgressRepository progressRepository;
    
    @InjectMocks
    private SpacedRepetitionService service;
    
    @Test
    @DisplayName("Should reset repetition when quality < 3")
    void shouldResetRepetitionWhenQualityBelowThree() {
        // Given
        UserWordProgress progress = createProgress(3, 6, 2.5);
        
        // When
        UserWordProgress result = service.calculateNextReview(progress, 2);
        
        // Then
        assertThat(result.getRepetition()).isEqualTo(0);
        assertThat(result.getIntervalDays()).isEqualTo(1);
    }
}

// Integration Test Example
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldLoginSuccessfully() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "test@example.com", "password": "password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists());
    }
}
```

**Run Tests:**
```bash
cd vokabelnetz-backend

# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=SpacedRepetitionServiceTest

# With coverage
./mvnw verify
```

### Frontend Tests

```typescript
// Component Test Example
describe('WordCardComponent', () => {
  let component: WordCardComponent;
  let fixture: ComponentFixture<WordCardComponent>;
  
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WordCardComponent],
      providers: [
        { provide: LanguageService, useValue: mockLanguageService }
      ]
    }).compileComponents();
    
    fixture = TestBed.createComponent(WordCardComponent);
    component = fixture.componentInstance;
  });
  
  it('should display German word', () => {
    fixture.componentRef.setInput('word', mockWord);
    fixture.detectChanges();
    
    const heading = fixture.nativeElement.querySelector('h2');
    expect(heading.textContent).toContain('arbeiten');
  });
});

// Service Test Example
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });
  
  it('should login and store tokens', () => {
    service.login('test@example.com', 'password').subscribe(response => {
      expect(response.accessToken).toBeDefined();
    });
    
    const req = httpMock.expectOne('/api/auth/login');
    req.flush({ data: { accessToken: 'token123' } });
  });
});
```

**Run Tests:**
```bash
cd vokabelnetz-frontend

# All tests
npm test

# With coverage
npm run test:coverage

# Specific file
npm test -- --include=**/auth.service.spec.ts
```

### Test Coverage Requirements

| Area | Minimum Coverage |
|------|-----------------|
| Backend Services | 80% |
| Backend Controllers | 70% |
| Frontend Services | 80% |
| Frontend Components | 60% |

---

## Documentation

### When to Update Docs

- New API endpoint → Update `API.md`
- Architecture change → Update `ARCHITECTURE.md`
- New algorithm/logic → Update `ALGORITHMS.md`
- Database schema change → Update `DATABASE.md`
- Deployment change → Update `DEPLOYMENT.md`

### Writing Style

- Use clear, simple language
- Include code examples
- Add diagrams for complex concepts
- Keep Turkish and English users in mind

---

## Getting Help

- **Questions**: Open a GitHub Discussion
- **Bugs**: Open a GitHub Issue
- **Security Issues**: Email security@vokabelnetz.com

---

## Recognition

Contributors are recognized in:
- README.md contributors section
- GitHub contributors page
- Release notes

Thank you for contributing to Vokabelnetz! 🎉
