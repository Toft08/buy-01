# Jenkins CI/CD Pipeline Setup Guide

This guide provides comprehensive instructions for setting up and configuring the Jenkins CI/CD pipeline for the e-commerce platform.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Jenkins Installation](#jenkins-installation)
- [Plugin Installation](#plugin-installation)
- [Jenkins Configuration](#jenkins-configuration)
- [Pipeline Setup](#pipeline-setup)
- [Notification Configuration](#notification-configuration)
- [Running the Pipeline](#running-the-pipeline)
- [Troubleshooting](#troubleshooting)

## Overview

The Jenkins pipeline automates the entire software delivery lifecycle:

1. **Checkout**: Fetches latest code from GitHub
2. **Build**: Compiles backend (Maven) and frontend (Angular)
3. **Test**: Runs unit and integration tests
4. **Docker Build**: Creates Docker images for all services
5. **Deploy**: Deploys using Docker Compose
6. **Health Check**: Verifies all services are healthy
7. **Notify**: Sends build status notifications
8. **Rollback**: Automatically rolls back on failure

## Prerequisites

### System Requirements

- **Operating System**: Linux, macOS, or Windows (with WSL2)
- **RAM**: Minimum 8GB (16GB recommended)
- **Disk Space**: At least 20GB free space
- **Java**: JDK 17 or higher
- **Node.js**: Version 18 or higher
- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **Git**: Latest version

### Verify Prerequisites

```bash
# Check Java version
java -version  # Should be 17+

# Check Node.js version
node --version  # Should be 18+

# Check Docker
docker --version
docker-compose --version

# Check Git
git --version
```

## Jenkins Installation

### Option 1: Docker (Recommended)

**Using Docker Compose (includes Docker CLI):**

```bash
cd jenkins
docker-compose up -d
cd ..
```

**Or using Docker directly:**

```bash
# Build custom Jenkins image with Docker CLI
cd jenkins
docker build -t jenkins-with-docker:lts .
cd ..

# Create Jenkins data directory
mkdir -p ~/jenkins_home

# Run Jenkins container
docker run -d \
  --name jenkins \
  -p 8082:8080 \
  -p 50000:50000 \
  -v ~/jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins-with-docker:lts

# Get initial admin password
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

**Note:** The custom image includes Docker CLI, so you don't need to install it manually.

### Option 2: Native Installation

#### macOS (using Homebrew)

```bash
brew install jenkins-lts
brew services start jenkins-lts
```

#### Linux (Ubuntu/Debian)

```bash
wget -q -O - https://pkg.jenkins.io/debian/jenkins.io-2023.key | sudo apt-key add -
sudo sh -c 'echo deb http://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list'
sudo apt-get update
sudo apt-get install jenkins
sudo systemctl start jenkins
sudo systemctl enable jenkins
```

### Initial Setup

1. Open Jenkins in browser: `http://localhost:8080`
2. Enter initial admin password (from installation step)
3. Install suggested plugins
4. Create admin user
5. Configure Jenkins URL (default: `http://localhost:8080`)

## Plugin Installation

Install the following required plugins:

1. Go to **Manage Jenkins** → **Manage Plugins** → **Available**
2. Search and install:

### Required Plugins

- **Pipeline** (usually pre-installed)
- **Docker Pipeline**
- **Git**
- **GitHub** (for GitHub integration)
- **JUnit** (for test reports)
- **Workspace Cleanup**

### Optional Plugins (for notifications)

- **Slack Notification** (for Slack integration)
- **Email Extension Plugin** (for email notifications)
- **AnsiColor** (for colored console output)
- **Timestamper** (for build timestamps)

### Install via Jenkins CLI

```bash
# List of plugin IDs
PLUGINS="workflow-aggregator docker-workflow git github junit ws-cleanup slack email-ext ansicolor timestamper"

# Install via Jenkins CLI
jenkins-plugin-cli --plugins $PLUGINS
```

## Jenkins Configuration

### 1. Configure Global Tools

Go to **Manage Jenkins** → **Global Tool Configuration**:

#### JDK Configuration

- **Name**: `JDK-17`
- **JAVA_HOME**: `/usr/lib/jvm/java-17-openjdk` (Linux) or `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home` (macOS)
- Or select **Install automatically** and choose version 17

#### Node.js Configuration

- **Name**: `NodeJS-18`
- **Install automatically**: Check
- **Version**: 18.x or latest LTS

### 2. Configure Credentials

Go to **Manage Jenkins** → **Credentials** → **System** → **Global credentials**:

#### GitHub Credentials

1. Click **Add Credentials**
2. **Kind**: SSH Username with private key or Username with password
3. **ID**: `github-credentials`
4. Enter your GitHub credentials

#### Docker Hub Credentials (Optional)

1. **Kind**: Username with password
2. **ID**: `docker-hub-credentials`
3. Enter Docker Hub username and password

#### Slack Webhook (Optional)

1. **Kind**: Secret text
2. **ID**: `slack-webhook-url`
3. **Secret**: Your Slack webhook URL

### 3. Configure Email (Optional)

Go to **Manage Jenkins** → **Configure System**:

- **SMTP server**: `smtp.gmail.com` (or your SMTP server)
- **Default user e-mail suffix**: `@yourdomain.com`
- **Use SMTP Authentication**: Check
- **User Name**: Your email
- **Password**: App password (for Gmail)
- **Use SSL**: Check
- **SMTP Port**: 465

## Pipeline Setup

### 1. Create New Pipeline Job

1. Click **New Item** on Jenkins dashboard
2. Enter job name: `e-com-pipeline`
3. Select **Pipeline**
4. Click **OK**

### 2. Configure Pipeline

#### General Settings

- **Description**: "E-commerce Platform CI/CD Pipeline"
- **GitHub project**: Check and enter your repository URL
- **Build Triggers**:
  - **GitHub hook trigger for GITScm polling**: Check (for automatic builds)
  - Or **Poll SCM**: `H/5 * * * *` (every 5 minutes)

#### Pipeline Definition

- **Definition**: Pipeline script from SCM
- **SCM**: Git
- **Repository URL**: Your GitHub repository URL
- **Credentials**: Select your GitHub credentials
- **Branches to build**: `*/main` or `*/master`
- **Script Path**: `Jenkinsfile`

### 3. Configure Environment Variables

In the pipeline job configuration, add environment variables:

- **SLACK_ENABLED**: `true` or `false`
- **SLACK_WEBHOOK_URL**: Your Slack webhook URL (or use credentials)
- **EMAIL_ENABLED**: `true` or `false`
- **EMAIL_RECIPIENTS**: `anastasia.suhareva@gmail.com`
- **DEPLOY_ENV**: `ci`

## Notification Configuration

### Slack Setup

1. Create a Slack app at https://api.slack.com/apps
2. Enable **Incoming Webhooks**
3. Create webhook URL
4. Add webhook URL to Jenkins credentials or environment variables

**In Jenkinsfile**, set:

```groovy
environment {
    SLACK_ENABLED = 'true'
    SLACK_WEBHOOK_URL = credentials('slack-webhook-url')
}
```

### Email Setup

1. Configure SMTP in Jenkins (see above)
2. Set email recipients in pipeline environment variables

**In Jenkinsfile**, set:

```groovy
environment {
    EMAIL_ENABLED = 'true'
    EMAIL_RECIPIENTS = 'anastasia.suhareva@gmail.com'
}
```

## Running the Pipeline

### Manual Execution

1. Go to your pipeline job
2. Click **Build Now**
3. Monitor progress in **Console Output**

### Automatic Execution

The pipeline runs automatically when:

- Code is pushed to `main` or `master` branch (if webhook configured)
- Pull request is created (if configured)
- Scheduled time (if cron trigger configured)

### Viewing Results

- **Build History**: See all builds and their status
- **Console Output**: View detailed build logs
- **Test Results**: See test reports and coverage
- **Artifacts**: Download build artifacts

## Pipeline Stages Explained

### 1. Checkout

- Fetches code from GitHub
- Records commit hash and branch name

### 2. Environment Setup

- Verifies Java, Node.js, Docker versions
- Makes scripts executable

### 3. Backend Build

- Compiles all Spring Boot microservices
- Creates JAR artifacts

### 4. Frontend Build

- Installs npm dependencies
- Builds Angular application

### 5. Backend Tests

- Runs Maven unit tests
- Generates JUnit XML reports

### 6. Frontend Tests

- Runs Angular unit tests
- Generates test reports

### 7. Docker Build

- Builds all Docker images
- Tags images with build number

### 8. Integration Tests

- Starts all services
- Runs E2E integration tests
- Stops services after tests

### 9. Deploy (main/master only)

- Deploys using Docker Compose
- Waits for health checks
- Tags successful deployment

### 10. Health Check

- Verifies all services are healthy
- Checks API Gateway and Eureka

## Rollback Strategy

### Automatic Rollback

The pipeline automatically rolls back if:

- Deployment fails
- Health checks fail after deployment
- Running on `main` or `master` branch

### Manual Rollback

To manually rollback:

```bash
# SSH into Jenkins server or use Jenkins agent
cd $JENKINS_HOME/workspace/e-com-pipeline

# Run rollback script
export WORKSPACE=$(pwd)
export BUILD_NUMBER=<previous-build-number>
bash jenkins/scripts/rollback.sh previous
```

Or create a separate Jenkins job for rollback:

1. Create new **Freestyle project**: `e-com-rollback`
2. Add **Execute shell** build step:

```bash
cd $WORKSPACE
export BUILD_NUMBER=$ROLLBACK_BUILD_NUMBER
bash jenkins/scripts/rollback.sh $ROLLBACK_BUILD_NUMBER
```

3. Add **String Parameter**: `ROLLBACK_BUILD_NUMBER`

## Troubleshooting

### Common Issues

#### 1. "Docker command not found"

**Solution**: Ensure Docker is installed and Jenkins user has access:

```bash
# Add Jenkins user to docker group (Linux)
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

#### 2. "Permission denied" on scripts

**Solution**: Scripts should be executable. The pipeline makes them executable, but if issues persist:

```bash
chmod +x jenkins/scripts/*.sh
```

#### 3. "Java version mismatch"

**Solution**: Ensure JDK 17 is configured in Jenkins Global Tool Configuration.

#### 4. "Node.js not found"

**Solution**: Install Node.js tool in Jenkins or ensure Node.js is in PATH.

#### 5. "SSL certificate errors"

**Solution**: The pipeline generates SSL certificates automatically. If issues persist:

```bash
cd frontend
./generate-ssl-certs.sh
```

#### 6. "Services not starting"

**Solution**: Check Docker logs:

```bash
docker-compose logs
docker-compose ps
```

#### 7. "Integration tests timeout"

**Solution**: Increase timeout in Jenkinsfile:

```groovy
timeout(time: 60, unit: 'MINUTES')  // Increase from 30
```

#### 8. "Slack/Email notifications not working"

**Solution**:

- Verify credentials are configured
- Check webhook URL is correct
- Verify SMTP settings for email
- Check Jenkins logs: `docker logs jenkins`

### Debugging Tips

1. **Check Console Output**: Always start here for detailed error messages
2. **Verify Environment Variables**: Use `env` command in pipeline
3. **Test Scripts Manually**: Run scripts outside Jenkins to isolate issues
4. **Check Docker**: Ensure Docker daemon is running
5. **Review Logs**: Check service logs with `docker-compose logs`

### Getting Help

- **Jenkins Logs**: `docker logs jenkins` or `/var/log/jenkins/jenkins.log`
- **Pipeline Logs**: View in Jenkins Console Output
- **Service Logs**: `docker-compose logs <service-name>`

## Best Practices

1. **Keep Jenkins Updated**: Regularly update Jenkins and plugins
2. **Backup Jenkins**: Regularly backup `JENKINS_HOME`
3. **Use Credentials**: Never hardcode passwords or tokens
4. **Monitor Builds**: Set up alerts for failed builds
5. **Clean Up**: Regularly clean old builds and artifacts
6. **Security**: Use least privilege for Jenkins user
7. **Version Control**: Keep Jenkinsfile in Git
8. **Test Changes**: Test pipeline changes in a branch first

## Additional Resources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

## Support

For issues or questions:

1. Check this README
2. Review Jenkins logs
3. Check service logs
4. Consult team documentation

---

**Last Updated**: 2024
**Pipeline Version**: 1.0
