# Java2Go Migration Pipeline

Intelligent assistant for automatic migration of Java Spring Boot microservices to Go (Gin).

# What it does

- **Parses Java projects via AST (JavaParser)** — deterministic, no AI involved
- **Analyzes the structure: controllers, services, DTOs, repositories, API contracts**
- **Generates Go code via LLM while preserving API contracts**
- **Verifies the result: checks endpoints, types, and structure**
- **Compiles and automatically fixes common errors**
- **Documents the entire process: report.md, report.json, BUILD_REPORT.md**

## Quick start

```
# Clone
git clone <repo-url>
cd task-repo

# Edit .env
cp .env.example .env
# Edit MODEL_API_KEY, MINIO_* 

# Run
docker-compose up --build -d
# or
make up
```

## Availability

- Service: http://localhost:8585 <br>
- MinIO Console: http://localhost:9001 (mws / minio123) <br>
- Swagger UI: http://localhost:8585/api/openapi <br>

# Endpoints

## 1. Upload ZIP and migrate immediately
POST /minio-upload-zip?user_id=user123&auto_migrate=true

## 2. Or upload separately, migrate separately
POST /minio-upload-zip?user_id=user123
POST /migrate?user_id=user123

## 3. Download the result
GET /minio-download-ready-zip?user_id=user123


## 🚀 What makes this project unique

This project is a full-fledged migration copilot that:

✅ Converts Java → Go automatically with high accuracy
✅ Validates the migration through automated testing (endpoint compatibility, data types, response structure)
✅ Provides actionable recommendations to the engineer for manual improvements or edge cases
✅ Generates comprehensive reports so you know exactly what worked, what was fixed, and what needs review


# Future Development: VS Code Plugin

The next evolution of the Java2Go Migration Pipeline is a VS Code extension that brings the entire migration workflow directly into your editor. Here's what the plugin will do: <br>

🧩 What the VS Code Plugin Does

## 1. Interactive Migration Assessment

- Analyzes your Java Spring Boot project directly in VS Code
- Generates a comprehensive migration readiness report before any code is converted
- Identifies potential blockers: unsupported patterns, complex dependencies, concurrency models
- Categorizes issues by severity: 🔴 Mandatory, 🟡 Potential, 🟢 Optional
- Provides clickable links to affected files and line numbers

## 2. Step-by-Step Migration Copilot

Instead of a black-box conversion, the plugin guides you through an Assess → Plan → Execute workflow:

```
Phase -> What Happens <br>
Assess -> Scan Java codebase, inventory APIs, map dependencies to Go equivalents <br>
Plan- > Generate editable migration plan with file-by-file strategy <br>
Execute	-> Convert code incrementally with human approval at each stage <br>
```

## 3. Dual Migration Modes

### Fast Track (for simple projects):

One-click migration for standard CRUD microservices
Automatic detection of common patterns (JPA → GORM, Spring Security → middleware)

### Guided Track (for complex migrations):

- Interactive review of each converted file
- Manual override for ambiguous cases
- Real-time diff viewer showing Java vs Go side-by-side

## 4. Automated CVE & Security Scanning

- Scans all Java dependencies before migration
- Checks migrated Go dependencies for known vulnerabilities
- Proposes secure alternatives for deprecated or vulnerable libraries
- Generates security compliance report

## 5. Test Suite Migration

- Converts JUnit tests to Go testing framework
- Maps Mockito mocks to Go interfaces
- Provides test coverage comparison (before/after)
- Auto-fixes common test assertion patterns

## 6. Real-Time Validation Loop

The plugin creates a fix-and-test loop during migration:

- Convert Java file → Go file
- Attempt compilation
- If errors → auto-fix common issues (imports, type conversions, error handling)
- Run equivalent Go tests
- Report success/failure before moving to next file

## 7. Engineer Recommendations Panel

Based on the migration analysis, the plugin provides actionable recommendations:

```
📋 RECOMMENDATIONS

🔴 MUST REVIEW (3)
- Java CompletableFuture → Go goroutines: Manual review needed for async chain
- Spring @Transactional → Go manual transaction handling: Add retry logic
- Hibernate lazy loading → No direct equivalent: Restructure data access layer

🟡 CONSIDER (5)  
- Replace Java streams with Go slice operations for performance
- Implement connection pooling (Java HikariCP → Go pgx pool)
- Add context.Context for timeout handling

🟢 OPTIONAL (2)
- Consider interface{} replacement with generics (Go 1.18+)
- Add structured logging (zap/logrus recommended)

```