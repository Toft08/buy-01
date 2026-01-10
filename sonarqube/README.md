# SonarQube Setup Guide

## Overview

SonarQube is used for continuous code quality inspection and security analysis of the Safe Zone e-commerce platform.

## Quick Start

### 1. Start SonarQube

```bash
cd sonarqube
docker-compose up -d
```

### 2. Access SonarQube Dashboard

- URL: http://localhost:9000
- Default credentials:
  - Username: `admin`
  - Password: `admin`
- **Important:** Change the default password on first login!

### 3. Create Project

1. Click **"Create Project"** → **"Manually"**
2. Enter project details:
   - **Project key:** `safe-zone`
   - **Display name:** `SafeZone E-commerce Platform`
3. Click **"Set Up"**

### 4. Generate Authentication Token

1. Select **"With Jenkins"** as integration method
2. Or go to: **User Menu** → **My Account** → **Security** → **Generate Token**
3. Token name: `jenkins-safe-zone`
4. **Copy the token** (you won't see it again!)

### 5. Configure Jenkins Credentials

1. Open Jenkins: http://localhost:8090
2. Go to: **Manage Jenkins** → **Credentials** → **System** → **Global credentials**
3. Click **"Add Credentials"**
4. Configure:
   - **Kind:** Secret text
   - **Secret:** Paste your SonarQube token
   - **ID:** `sonarqube-token`
   - **Description:** SonarQube authentication token
5. Click **"Create"**

### 6. Run Jenkins Pipeline

Push code to trigger the pipeline, or manually run the build. The pipeline will:

- Run tests
- Execute SonarQube analysis
- Check quality gate
- Fail if quality gate doesn't pass

## View Analysis Results

After running the pipeline:

1. Go to http://localhost:9000
2. Click on **"safe-zone"** project
3. View:
   - Code smells
   - Bugs
   - Vulnerabilities
   - Security hotspots
   - Code coverage (if configured)
   - Technical debt

## Stopping SonarQube

```bash
cd sonarqube
docker-compose down
```

## Resource Usage

- **Memory:** 2GB limit
- **CPU:** 1 core
- **Port:** 9000

## Troubleshooting

### SonarQube won't start

- Check if port 9000 is already in use: `lsof -i :9000`
- Increase Docker memory allocation (min 2GB required)

### Analysis fails in Jenkins

- Verify SonarQube is running: `docker ps | grep sonarqube`
- Check Jenkins credentials ID is exactly: `sonarqube-token`
- Verify SonarQube URL is accessible from Jenkins container

### Quality Gate always fails

- Review quality gate rules in SonarQube dashboard
- Check specific issues preventing the gate from passing
- Initial runs may have many issues - fix incrementally

## Documentation

- [SonarQube Documentation](https://docs.sonarqube.org/latest/)
- [Maven SonarQube Scanner](https://docs.sonarqube.org/latest/analyzing-source-code/scanners/sonarscanner-for-maven/)
