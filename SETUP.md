# Project Setup Documentation

## Project Structure Overview

The Buy-01 e-commerce platform development environment is configured to ensure consistent code formatting and efficient team collaboration.

### Configuration Files

#### Code Formatting & Standards
- **`.editorconfig`** - Universal editor configuration for consistent formatting across all editors
- **`.prettierrc`** - Prettier configuration for JavaScript/TypeScript/Angular code formatting
- **`.prettierignore`** - Files to exclude from Prettier formatting
- **`.java-style`** - Java code formatting rules following Google Java Style Guide

#### VS Code Workspace
- **`.vscode/settings.json`** - Workspace-specific settings with auto-formatting enabled
- **`.vscode/extensions.json`** - Recommended extensions for the team
- **`.vscode/launch.json`** - Debug configurations for all services
- **`.vscode/tasks.json`** - Pre-configured build and test tasks

#### Team Guidelines
- **`CONTRIBUTING.md`** - Team guidelines for development workflow

## Key Features for Team Collaboration

### Automatic Code Formatting
- **Format on Save**: Enabled for all file types
- **Consistent Indentation**: 2 spaces for web files, 4 spaces for Java
- **Import Organization**: Automatic import sorting and cleanup
- **Line Endings**: Standardized to LF (Unix-style)

### Development Workflow
1. **Create Feature Branch**: `git checkout -b feature/your-feature`
2. **Write Code**: Auto-formatting handles code style
3. **Test Manually**: Run `mvn test` and `npm test` before pushing
4. **Create Pull Request**: Simple PR with description
5. **Code Review**: Quick review and merge

### Quality Tools
- **Auto-formatting**: Prettier and EditorConfig handle formatting
- **Code Analysis**: SonarLint integration for real-time feedback
- **VS Code Integration**: Recommended extensions for best experience

## Getting Started for Team Members

### Initial Setup
```bash
# Clone the repository
git clone https://github.com/YOUR_TEAM/buy-01.git
cd buy-01

# Install recommended VS Code extensions (prompted automatically)
# Open in VS Code
code .
```

### Development Workflow
1. **Create Feature Branch**: `git checkout -b feature/your-feature`
2. **Write Code**: Auto-formatting handles style automatically
3. **Test Before Pushing**: 
   ```bash
   mvn test        # Test Java code
   npm test        # Test Angular code
   ```
4. **Push and Create PR**: Simple workflow without complex templates

### Available Commands (VS Code Tasks)
- `Ctrl/Cmd + Shift + P` → "Tasks: Run Task"
  - **Build All Services**: Compile all Java microservices
  - **Test All Services**: Run all tests
  - **Format Code**: Format frontend code
  - **Docker: Build All**: Build all Docker containers
  - **Docker: Start All**: Start the entire application stack

## Benefits for Development Team

### Consistency
- Same code formatting across all team members
- Standardized file structure and organization
- No debates about tabs vs spaces or formatting

### Efficiency
- Automatic formatting on save (no manual formatting needed)
- Pre-configured debug settings for all services
- One-click build and test commands
- Simple, fast workflow

### Quality
- Built-in linting and code analysis
- Manual testing before push ensures quality
- Consistent code style across the team

### Collaboration
- Simple workflow that doesn't interfere with development
- Focus on building features, not process overhead
- Accessible for developers new to GitHub workflows

## Setup Instructions

1. **Install VS Code Extensions**: Accept the recommended extensions when prompted
2. **Review CONTRIBUTING.md**: Read team guidelines for development workflow
3. **Set Up Development Environment**: Follow setup instructions for Java and Angular
4. **Begin Development**: Start building e-commerce platform features

This configuration ensures that files are automatically formatted according to team standards when saved, similar to ESLint but covering the entire project stack.

## Manual Testing Workflow

The project uses manual testing to maintain code quality without automated CI complexity.

### Before Every Push:
```bash
# Backend testing
mvn clean test              # Run all Java tests
mvn clean install         # Ensure everything builds

# Frontend testing  
npm test                   # Run Angular unit tests
npm run build             # Ensure production build works
```

### Before Creating Pull Request:
- Test the specific feature implementation
- Verify formatting is applied correctly (should be automatic)
- Write clear description of changes made
- Document any manual testing performed

This approach maintains code quality while keeping the development process simple and focused.