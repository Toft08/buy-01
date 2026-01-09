# SonarQube Setup for E-commerce Platform

This directory contains the SonarQube setup for continuous code quality and security analysis.

## Quick Start

### 1. Start SonarQube

```bash
cd sonarqube
docker-compose up -d
```

Wait ~30 seconds for SonarQube to start.

### 2. Access SonarQube

- **URL**: http://localhost:9000
- **Default credentials**:
  - Username: `admin`
  - Password: `admin`

⚠️ **Important**: Change the default password on first login!

### 3. Generate Authentication Token

1. Log in to SonarQube
2. Click on your avatar → My Account → Security
3. Generate a token (name it `jenkins` or `local-analysis`)
4. **Save this token** - you'll need it for Jenkins and local analysis

### 4. Configure Jenkins Integration

In Jenkins:

1. Install **SonarQube Scanner** plugin:
   - Manage Jenkins → Plugins → Available → Search "SonarQube Scanner"

2. Configure SonarQube server:
   - Manage Jenkins → Configure System → SonarQube servers
   - Name: `SonarQube`
   - Server URL: `http://sonarqube:9000` (Docker network) or `http://localhost:9000` (local)
   - Add credential: Secret text → Paste your token

3. Add SonarQube webhook in SonarQube:
   - Administration → Configuration → Webhooks
   - Create webhook:
     - Name: `Jenkins`
     - URL: `http://jenkins:8080/sonarqube-webhook/` (adjust based on your Jenkins URL)

## Running Analysis

### Via Jenkins Pipeline

Analysis runs automatically in the Jenkins pipeline:

```groovy
stage('SonarQube Analysis') {
    // Automatic analysis for backend services
}

stage('Quality Gate') {
    // Blocks deployment if quality gate fails
}
```

### Manual Local Analysis

```bash
# Backend analysis
cd backend
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=e-com-backend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_TOKEN_HERE

# Individual service analysis
cd backend/services/user
../../mvnw clean verify sonar:sonar \
  -Dsonar.projectKey=e-com-user-service \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_TOKEN_HERE
```

## What Gets Analyzed

- **Code Quality**: Bugs, code smells, maintainability issues
- **Security**: Vulnerabilities, security hotspots
- **Coverage**: Test coverage reports (via JaCoCo)
- **Duplications**: Duplicated code blocks
- **Complexity**: Cyclomatic complexity

## Quality Gates

Default quality gate fails if:

- **Coverage** < 80%
- **Duplicated Lines** > 3%
- **Maintainability Rating** worse than A
- **Reliability Rating** worse than A
- **Security Rating** worse than A

You can customize quality gates in SonarQube:
- Administration → Quality Gates → Create

## Project Structure

```
sonarqube/
├── docker-compose.yml   # SonarQube + PostgreSQL setup
└── README.md           # This file
```

## Services

| Service | Port | Purpose |
|---------|------|---------|
| SonarQube | 9000 | Web UI and API |
| PostgreSQL | 5432 | SonarQube database (internal) |

## Troubleshooting

### SonarQube won't start

```bash
# Check logs
docker-compose logs sonarqube

# Common issue: max_map_count too low (Linux)
sudo sysctl -w vm.max_map_count=524288
sudo sysctl -w fs.file-max=131072
```

### Analysis fails with "Insufficient privileges"

- Ensure you're using a valid authentication token
- Check token permissions in SonarQube → My Account → Security

### Quality gate always fails

- Check quality gate rules in SonarQube UI
- Review analysis results for specific failures
- Adjust quality gate thresholds if needed

### Jenkins can't connect to SonarQube

- Verify SonarQube is running: `docker-compose ps`
- Check network connectivity: `docker network ls`
- Use `http://sonarqube:9000` if Jenkins runs in Docker
- Use `http://localhost:9000` if Jenkins runs on host

## Stopping SonarQube

```bash
cd sonarqube
docker-compose down
```

To remove all data:

```bash
docker-compose down -v
```

## Resources

- [SonarQube Documentation](https://docs.sonarsource.com/sonarqube/latest/)
- [SonarQube Maven Plugin](https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/scanners/sonarscanner-for-maven/)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)

## Integration with VS Code

Your project already has **SonarLint** configured (see `.vscode/extensions.json`).

To connect SonarLint to your SonarQube server:

1. Install SonarLint extension in VS Code
2. Open Settings → SonarLint → Connected Mode
3. Add SonarQube connection:
   - URL: `http://localhost:9000`
   - Token: Your authentication token
4. Bind your workspace to SonarQube projects

This provides **real-time code quality feedback** while coding!
