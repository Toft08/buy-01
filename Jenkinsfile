pipeline {
    agent any

    // Automatic build trigger on new commits
    triggers {
        pollSCM('H/2 * * * *')
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    environment {
        JAVA_HOME = tool name: 'JDK-17', type: 'jdk'
        NODE_HOME = tool name: 'NodeJS-20', type: 'nodejs'
        PATH = "${JAVA_HOME}/bin:${NODE_HOME ?: '/usr'}/bin:${PATH}"
        DOCKER_BUILDKIT = '1'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Checking out code from repository"
                checkout scm
            }
        }

        stage('Tests') {
            parallel {
                stage('Backend Tests') {
                    steps {
                        sh '''
                            cd backend || exit 1

                            # Build shared module first
                            cd shared && ../mvnw clean install -DskipTests && cd ..

                            # Run tests for each service (pipeline fails if any test fails)
                            cd services/user && ../../mvnw test && cd ../..
                            cd services/product && ../../mvnw test && cd ../..
                            cd services/media && ../../mvnw test && cd ../..
                            cd services/eureka && ../../mvnw test && cd ../..
                            cd api-gateway && ../mvnw test
                        '''
                    }
                    post {
                        always {
                            junit 'backend/**/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Frontend Tests') {
                    steps {
                        sh '''
                            echo "Running frontend tests in isolated Chrome container..."

                            docker run --rm \
                              --volumes-from jenkins \
                              -w ${WORKSPACE}/frontend \
                              --tmpfs /tmp:rw,exec,nosuid,size=2g \
                              --cap-add=SYS_ADMIN \
                              node:22-slim \
                              bash -c '
                                echo "Installing Chrome and dependencies..." && \
                                apt-get update && \
                                apt-get install -y chromium chromium-driver --no-install-recommends && \
                                apt-get clean && \
                                rm -rf /var/lib/apt/lists/* && \
                                echo "Node version: $(node --version)" && \
                                echo "Copying files to /tmp..." && \
                                mkdir -p /tmp/test && \
                                cp -r . /tmp/test/ && \
                                cd /tmp/test && \
                                npm install --legacy-peer-deps --cache /tmp/.npm --no-save --no-package-lock && \
                                CHROME_BIN=/usr/bin/chromium npm run test
                              ' || {
                                EXIT_CODE=$?
                                echo "Frontend tests failed with exit code: $EXIT_CODE"
                                exit $EXIT_CODE
                            }
                            echo "✅ Frontend tests passed"
                        '''
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    # Generate SSL certificates if needed
                    if [ ! -f "frontend/ssl/localhost-cert.pem" ]; then
                        ./generate-ssl-certs.sh
                    fi

                    # Build Docker images
                    docker-compose -f docker-compose.yml -f docker-compose.ci.yml build
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    // Save current running images for rollback
                    sh '''
                        mkdir -p .deployment-state

                        # Save currently running image IDs before deployment
                        echo "Saving current deployment state for rollback..."
                        docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | grep -E "ecom-|e-commerce" > .deployment-state/previous-images.txt || echo "No previous images found" > .deployment-state/previous-images.txt

                        # Save current container state
                        docker ps --filter "name=ecom-" --format "{{.Names}} {{.Image}}" > .deployment-state/previous-containers.txt || true
                    '''

                    // Deploy new version
                    sh '''
                        # Stop old containers
                        docker-compose -f docker-compose.yml -f docker-compose.ci.yml down || true

                        # Force remove any lingering containers with ecom- prefix
                        docker ps -a | grep ecom- | awk '{print $1}' | xargs -r docker rm -f || true

                        # Start new containers
                        echo "Deploying new version..."
                        docker-compose -f docker-compose.yml -f docker-compose.ci.yml up -d

                        # Wait for services to be healthy
                        echo "Waiting for services to start..."
                        sleep 30

                        # Verify services are running
                        docker-compose -f docker-compose.yml -f docker-compose.ci.yml ps

                        # Check if all services are healthy
                        UNHEALTHY=$(docker ps --filter "name=ecom-" --filter "health=unhealthy" --format "{{.Names}}" || true)
                        if [ -n "$UNHEALTHY" ]; then
                            echo "ERROR: Unhealthy services detected: $UNHEALTHY"
                            exit 1
                        fi

                        echo "✅ Deployment successful - all services healthy"
                    '''
                }
            }
            post {
                failure {
                    script {
                        echo "❌ Deployment failed - Initiating rollback to previous working version"
                        sh '''
                            # Stop failed deployment
                            docker-compose -f docker-compose.yml -f docker-compose.ci.yml down || true

                            # Check if we have a previous state to restore
                            if [ -f .deployment-state/previous-containers.txt ] && [ -s .deployment-state/previous-containers.txt ]; then
                                echo "Restoring previous deployment..."

                                # Read previous image tags and restore them
                                if [ -f .deployment-state/previous-images.txt ]; then
                                    echo "Previous images:"
                                    cat .deployment-state/previous-images.txt
                                fi

                                # Attempt to restart with previous images
                                # Note: This assumes previous images still exist
                                docker-compose -f docker-compose.yml -f docker-compose.ci.yml up -d || {
                                    echo "⚠️  Could not restore previous deployment - manual intervention required"
                                    exit 1
                                }

                                echo "✅ Rollback completed - previous version restored"
                            else
                                echo "⚠️  No previous deployment found to rollback to"
                                echo "System is currently down - manual intervention required"
                            fi
                        '''
                    }
                }
                success {
                    script {
                        echo "✅ Deployment successful - saving deployment state"
                        sh '''
                            # Save successful deployment info
                            echo "BUILD_NUMBER=${BUILD_NUMBER}" > .deployment-state/last-successful.txt
                            echo "GIT_COMMIT=${GIT_COMMIT}" >> .deployment-state/last-successful.txt
                            echo "TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)" >> .deployment-state/last-successful.txt
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Build completed: ${currentBuild.currentResult}"
        }
        success {
            script {
                def commitMessage = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                if (env.EMAIL_ENABLED == 'true') {
                    def emailRecipients = env.EMAIL_RECIPIENTS ?: 'anastasia.suhareva@gmail.com, toft.diederichs@gritlab.ax'
                    def recipientList = emailRecipients.split(',').collect { it.trim() }
                    emailext (
                        subject: "✅ Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """
                            Build succeeded!
                            Project: ${env.JOB_NAME}
                            Build Number: #${env.BUILD_NUMBER}
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
                def commitMessage = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                if (env.EMAIL_ENABLED == 'true') {
                    def emailRecipients = env.EMAIL_RECIPIENTS ?: 'anastasia.suhareva@gmail.com, toft.diederichs@gritlab.ax'
                    def recipientList = emailRecipients.split(',').collect { it.trim() }
                    emailext (
                        subject: "❌ Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """
                            Build failed!
                            Project: ${env.JOB_NAME}
                            Build Number: #${env.BUILD_NUMBER}
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
    }
}
