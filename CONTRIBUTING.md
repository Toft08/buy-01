# Contributing to Buy-01 E-commerce Platform

This document outlines the development workflow and guidelines for contributing to the Buy-01 e-commerce project.

## Project Structure

The project is a microservices e-commerce platform consisting of:

- **Backend**: Java Spring Boot microservices (User, Product, Media services)
- **Frontend**: Angular application
- **Database**: MongoDB
- **Communication**: REST APIs

## Prerequisites

### Required Software

- Java 17+
- Node.js 18+
- Docker
- Git

### Getting Started

1. Clone the repository: `git clone https://github.com/YOUR_USERNAME/buy-01.git`
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Commit and push: `git commit -m "your message" && git push`
5. Create a pull request

## Development Setup

### Auto-Formatting Configuration

- **Format on Save**: Enabled automatically in VS Code
- **Consistent Style**: EditorConfig and Prettier handle formatting
- **Manual Formatting**: Not required - automatic on file save

### Recommended VS Code Extensions

The project includes recommended extensions that VS Code will suggest upon opening:

- Java Extension Pack
- Angular Language Service
- Prettier
- GitLens

## Development Workflow

### Working on Features

- Create descriptive branch names: `feature/user-login`, `fix/media-upload`
- Make small, focused commits
- Push regularly to avoid merge conflicts

### Commit Messages

Use clear, descriptive commit messages. Consider following these conventions:

#### Common Commit Types:

```
feat: add new feature or functionality
fix: bug fixes
docs: documentation changes
style: code formatting, missing semicolons, etc (no code change)
refactor: code refactoring without adding features or fixing bugs
test: adding or updating tests
chore: maintenance tasks, dependency updates
```

#### Examples by Scenario:

```
# Adding new features
feat: add user authentication system
feat: implement product search functionality
feat: add media upload validation

# Bug fixes
fix: resolve media upload size validation
fix: correct user login redirect issue
fix: handle null pointer in product service

# Documentation updates
docs: update API documentation
docs: add setup instructions for Docker
docs: update task description and requirements

# Code improvements
refactor: simplify user service authentication logic
style: format code according to team standards
chore: update dependencies to latest versions

# Testing
test: add unit tests for product service
test: add integration tests for user authentication
```

### Pull Requests

- Create PR when feature is ready for review
- Include description of changes made
- Document any testing performed

## Testing

### Backend (Java)

```bash
mvn test                    # Run all tests
mvn test -Dtest=ClassName   # Run specific test
```

### Frontend (Angular)

```bash
npm test                    # Run unit tests
npm run e2e                 # Run end-to-end tests
```

## Common Commands

### Backend Development

```bash
mvn clean install          # Build all services
mvn spring-boot:run        # Start a service
```

### Frontend Development

```bash
npm install                # Install dependencies
npm start                  # Start dev server (http://localhost:4200)
npm run build              # Build for production
```

### Docker (Optional)

```bash
docker-compose up          # Start all services
docker-compose down        # Stop all services
```

## Code Guidelines

### Java/Spring Boot Standards

- Use meaningful variable and method names
- Add comments for complex logic
- Follow REST conventions for APIs
- Write tests for business logic

### Angular/TypeScript Standards

- Use Angular CLI for generating components
- Keep components small and focused
- Implement proper error handling
- Use reactive programming patterns (RxJS) where appropriate

#### Component Styling

- Use external SCSS files (`styleUrls`) instead of inline styles
- Import shared styles from `frontend/src/styles/` for consistency:
  ```typescript
  @use '../../../styles/variables' as *;
  @use '../../../styles/mixins' as *;
  ```
- Reuse existing button, form, and card classes where applicable
- Inline styles are acceptable for simple components (< 10 lines only)

### General Guidelines

- Keep functions small and focused
- Do not commit commented-out code
- Remove debug statements before committing
- Use descriptive commit messages

## Development Restrictions

- Do not push directly to `main` branch
- Do not commit large files or dependencies
- Do not ignore linting errors
- Do not skip testing changes before submission

## Support Resources

- Review existing code for implementation examples
- Use pull request comments for code-specific questions
- Refer to Task.md file for project requirements
- Contact team members directly for assistance

## Definition of Done

A feature is considered complete when:

- Code functions as expected
- Basic testing has been completed
- Code is properly formatted (automatic)
- Pull request has been created and reviewed

This workflow emphasizes simplicity while maintaining code quality and team collaboration.
