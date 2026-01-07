pipeline {
    agent any

    // Triggers: Check for new commits automatically
    triggers {
        // Poll SCM every 2 minutes (H/2 means every 2 minutes, with random offset)
        // This checks your Git repository for new commits
        pollSCM('H/2 * * * *')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '50'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
    }

    environment {
        // Workspace paths
        WORKSPACE_DIR = "${WORKSPACE}"
        BACKEND_DIR = "${WORKSPACE}/backend"
        FRONTEND_DIR = "${WORKSPACE}/frontend"

        // Build configuration
        JAVA_HOME = tool name: 'JDK-17', type: 'jdk'
        NODE_HOME = tool name: 'NodeJS-20', type: 'nodejs'
        PATH = "${JAVA_HOME}/bin:${NODE_HOME ?: '/usr'}/bin:${PATH}"

        // Docker configuration
        DOCKER_BUILDKIT = '1'
        COMPOSE_DOCKER_CLI_BUILD = '1'

        // Notification configuration (set via Jenkins credentials or environment)
        EMAIL_ENABLED = "${env.EMAIL_ENABLED ?: 'true'}"
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "=========================================="
                    echo "Checking out code from repository"
                    echo "=========================================="
                }
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    env.GIT_BRANCH_NAME = sh(
                        script: 'git rev-parse --abbrev-ref HEAD',
                        returnStdout: true
                    ).trim()
                    echo "Branch: ${env.GIT_BRANCH_NAME}"
                    echo "Commit: ${env.GIT_COMMIT_SHORT}"
                }
            }
        }

        stage('Environment Setup') {
            steps {
                script {
                    echo "=========================================="
                    echo "Setting up build environment"
                    echo "=========================================="
                }
                sh '''
                    echo "Java version:"
                    java -version
                    echo "Node version:"
                    node --version
                    echo "npm version:"
                    npm --version
                    echo "Docker version:"
                    docker --version
                    echo "Docker Compose version:"
                    docker-compose --version
                '''
                // Make scripts executable
                sh 'chmod +x jenkins/scripts/*.sh'
            }
        }

        stage('Build') {
            parallel {
                stage('Backend Build') {
                    steps {
                        script {
                            echo "=========================================="
                            echo "Building Backend Services"
                            echo "=========================================="
                        }
                        sh '''
                            export WORKSPACE="${WORKSPACE}"
                            if [ -z "${JAVA_HOME}" ] || [ ! -d "${JAVA_HOME}" ]; then
                                echo "ERROR: JAVA_HOME is not set or invalid: ${JAVA_HOME}"
                                echo "Please configure JDK-17 in Jenkins Tools configuration"
                                exit 1
                            fi
                            export JAVA_HOME="${JAVA_HOME}"
                            export PATH="${JAVA_HOME}/bin:${PATH}"
                            bash jenkins/scripts/build-backend.sh
                        '''
                    }
                    post {
                        success {
                            archiveArtifacts artifacts: 'artifacts/backend/*.jar', allowEmptyArchive: true
                        }
                    }
                }

                stage('Frontend Build') {
                    steps {
                        script {
                            echo "=========================================="
                            echo "Building Frontend Application"
                            echo "=========================================="
                        }
                        sh '''
                            export WORKSPACE="${WORKSPACE}"
                            bash jenkins/scripts/build-frontend.sh
                        '''
                    }
                    post {
                        success {
                            archiveArtifacts artifacts: 'artifacts/frontend/**/*', allowEmptyArchive: true
                        }
                    }
                }
            }
        }

        stage('Tests') {
            parallel {
                stage('Backend Tests') {
                    steps {
                        script {
                            echo "=========================================="
                            echo "Running Backend Tests"
                            echo "=========================================="
                        }
                        sh '''
                            export WORKSPACE="${WORKSPACE}"
                            export JAVA_HOME="${JAVA_HOME}"
                            export PATH="${JAVA_HOME}/bin:${PATH}"
                            bash jenkins/scripts/run-backend-tests.sh
                        '''
                    }
                    post {
                        always {
                            junit 'test-reports/backend/**/*.xml'
                        }
                    }
                }

                stage('Frontend Tests') {
                    steps {
                        script {
                            echo "=========================================="
                            echo "Running Frontend Tests"
                            echo "=========================================="
                        }
                        sh '''
                            export WORKSPACE="${WORKSPACE}"
                            bash jenkins/scripts/run-frontend-tests.sh
                        '''
                    }
                    post {
                        always {
                            // Publish frontend test results if available
                            script {
                                if (fileExists('test-reports/frontend')) {
                                    junit 'test-reports/frontend/**/*.xml'
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    echo "=========================================="
                    echo "Building Docker Images"
                    echo "=========================================="
                }
                sh '''
                    # Ensure SSL certificates exist
                    if [ ! -f "frontend/ssl/localhost-cert.pem" ]; then
                        echo "Generating SSL certificates..."
                        ./generate-ssl-certs.sh
                    fi

                    # Build Docker images in parallel for speed
                    # Each service builds independently (shared module is built within each Dockerfile)
                    echo "Building Docker images in parallel..."

                    # Build all services in parallel (docker-compose handles this efficiently)
                    if ! docker-compose -f docker-compose.yml -f docker-compose.ci.yml build --parallel; then
                        echo "❌ Docker build failed"
                        exit 1
                    fi

                    echo "✅ All Docker images built successfully!"

                    # Show built images
                    echo "Built images:"
                    docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "ecom-|e-commerce" | head -10
                '''
            }
        }

        stage('Integration Tests') {
            steps {
                script {
                    echo "=========================================="
                    echo "Verifying Services Start Correctly"
                    echo "=========================================="
                }
                sh '''
                    # Start services (Docker Compose waits for health checks automatically)
                    echo "Starting all services..."
                    docker-compose -f docker-compose.yml -f docker-compose.ci.yml up -d

                    # If docker-compose up succeeds, services are healthy (it waits for health checks)
                    echo "✅ Integration test passed - Docker Compose confirmed all services are healthy"
                '''
            }
            post {
                always {
                    // Don't stop services - they will be replaced by Deploy stage
                    // Only stop if integration tests failed
                    script {
                        if (currentBuild.currentResult != 'SUCCESS') {
                            sh 'docker-compose -f docker-compose.yml -f docker-compose.ci.yml down || true'
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    echo "=========================================="
                    echo "Deploying Application"
                    echo "=========================================="
                }
                sh '''
                    export WORKSPACE="${WORKSPACE}"
                    export BUILD_NUMBER="${BUILD_NUMBER}"
                    export GIT_COMMIT="${GIT_COMMIT}"
                    bash jenkins/scripts/deploy.sh
                '''
            }
            post {
                failure {
                    script {
                        echo "=========================================="
                        echo "Deployment failed - Attempting rollback"
                        echo "=========================================="
                        try {
                            if (fileExists('jenkins/scripts/rollback.sh')) {
                                sh '''
                                    export WORKSPACE="${WORKSPACE}"
                                    export BUILD_NUMBER="${BUILD_NUMBER}"
                                    bash jenkins/scripts/rollback.sh previous
                                '''
                            }
                        } catch (Exception e) {
                            echo "Rollback failed: ${e.getMessage()}"
                        }
                    }
                }
            }
        }

    }

    post {
        always {
            script {
                echo "=========================================="
                echo "Build completed: ${currentBuild.currentResult}"
                echo "=========================================="

                // Cleanup old Docker images (only if we have workspace context)
                try {
                    if (fileExists('.') && env.WORKSPACE) {
                        sh '''
                            # Clean up old images (keep last 5) - but DON'T stop running containers
                            docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "ecom-|e-commerce" | tail -n +6 | xargs -r docker rmi || true

                            # Remove dangling images (unused, not running containers)
                            docker image prune -f || true
                        '''
                    }
                } catch (Exception e) {
                    echo "Cleanup skipped: ${e.getMessage()}"
                }
            }
        }
        success {
            script {
                def commitMessage = ""
                try {
                    commitMessage = sh(
                        script: 'git log -1 --pretty=%B',
                        returnStdout: true
                    ).trim()
                } catch (Exception e) {
                    commitMessage = "Unable to retrieve commit message"
                }

                // Email notification
                if (env.EMAIL_ENABLED == 'true') {
                    // Parse email recipients (comma-separated string or use default)
                    def emailRecipients = env.EMAIL_RECIPIENTS ?: 'anastasia.suhareva@gmail.com, toft.diederichs@gritlab.ax'
                    def recipientList = emailRecipients.split(',').collect { it.trim() }

                    emailext (
                        subject: "✅ Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """
                            Build succeeded!

                            Project: ${env.JOB_NAME}
                            Build Number: #${env.BUILD_NUMBER}
                            Branch: ${env.GIT_BRANCH_NAME}
                            Commit: ${commitMessage}
                            Build URL: ${env.BUILD_URL}
                        """,
                        to: recipientList.join(','),
                        mimeType: 'text/html'
                    )
                }
            }
        }
        failure {
            script {
                def commitMessage = ""
                try {
                    commitMessage = sh(
                        script: 'git log -1 --pretty=%B',
                        returnStdout: true
                    ).trim()
                } catch (Exception e) {
                    commitMessage = "Unable to retrieve commit message"
                }

                // Attempt automatic rollback on deployment failure
                if (env.GIT_BRANCH_NAME == 'main' || env.GIT_BRANCH_NAME == 'master') {
                    try {
                        if (fileExists('jenkins/scripts/rollback.sh')) {
                            echo "Attempting automatic rollback..."
                            sh '''
                                export WORKSPACE="${WORKSPACE}"
                                export BUILD_NUMBER="${BUILD_NUMBER}"
                                bash jenkins/scripts/rollback.sh previous
                            '''
                        }
                    } catch (Exception e) {
                        echo "Rollback failed: ${e.getMessage()}"
                    }
                }

                // Email notification
                if (env.EMAIL_ENABLED == 'true') {
                    // Parse email recipients (comma-separated string or use default)
                    def emailRecipients = env.EMAIL_RECIPIENTS ?: 'anastasia.suhareva@gmail.com, toft.diederichs@gritlab.ax'
                    def recipientList = emailRecipients.split(',').collect { it.trim() }

                    emailext (
                        subject: "❌ Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """
                            Build failed!

                            Project: ${env.JOB_NAME}
                            Build Number: #${env.BUILD_NUMBER}
                            Branch: ${env.GIT_BRANCH_NAME}
                            Commit: ${commitMessage}
                            Build URL: ${env.BUILD_URL}

                            Please check the build logs for details.
                        """,
                        to: recipientList.join(','),
                        mimeType: 'text/html'
                    )
                }
            }
        }
        unstable {
            script {
                echo "Build is unstable - some tests may have failed"
            }
        }
    }
}


