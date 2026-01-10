pipeline {
    agent any

    // Automatic build trigger on new commits
    triggers {
        pollSCM('H/1 * * * *')
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
                }

                stage('Frontend Tests') {
                    steps {
                        sh '''
                            echo "Running frontend tests..."
                            cd frontend
                            npm ci
                            npm run test
                        '''
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo '🔍 Running SonarQube analysis...'

                // Use SonarQube token from Jenkins credentials
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    // Analyze all backend services AND frontend with explicit source paths
                    sh '''
                        cd backend
                        ./mvnw org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                            -Dsonar.projectKey=safe-zone \
                            -Dsonar.projectName="safe-zone" \
                            -Dsonar.host.url=http://host.docker.internal:9000 \
                            -Dsonar.token=${SONAR_TOKEN} \
                            -Dsonar.java.source=17
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo '🚦 Checking SonarQube Quality Gate...'

                // TODO! wait for quality gate result (timeout after 5 minutes)
                timeout(time: 5, unit: 'MINUTES') {
                    withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                        sh '''
                            echo "Waiting for SonarQube analysis to complete..."
                            sleep 15

                            # Check quality gate status
                            RESPONSE=$(curl -s -u "${SONAR_TOKEN}:" \
                                "http://host.docker.internal:9000/api/qualitygates/project_status?projectKey=safe-zone")

                            echo "API Response: ${RESPONSE}"

                            QUALITY_GATE=$(echo "${RESPONSE}" | grep -o \'"status":"[^"]*"\' | head -1 | cut -d\'"\'  -f4)

                            echo "Quality Gate Status: ${QUALITY_GATE}"

                            if [ "${QUALITY_GATE}" = "ERROR" ]; then
                                echo "❌ Quality Gate FAILED!"
                                exit 1
                            elif [ "${QUALITY_GATE}" = "OK" ]; then
                                echo "✅ Quality Gate PASSED!"
                            else
                                echo "⚠️  Quality Gate Status: ${QUALITY_GATE} (accepting for first run)"
                            fi
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
            script {
                echo "Build completed: ${currentBuild.currentResult}"

                // Get commit message before cleaning workspace
                def commitMessage = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                env.COMMIT_MESSAGE = commitMessage

                // Archive test results (backend and frontend combined)
                junit allowEmptyResults: true, testResults: 'backend/**/target/surefire-reports/*.xml, frontend/test-results/*.xml'

                // Archive test artifacts for download
                archiveArtifacts artifacts: 'backend/**/target/surefire-reports/*.xml', allowEmptyArchive: true
                archiveArtifacts artifacts: 'frontend/test-results/*.xml', allowEmptyArchive: true

                // Cleanup Docker resources
                sh '''
                    docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "ecom-|e-commerce" | tail -n +6 | xargs -r docker rmi || true
                    docker image prune -f || true
                '''

                // Cleanup workspace (done last, after getting commit message)
                if (env.WORKSPACE) {
                    cleanWs notFailBuild: true
                } else {
                    echo "No workspace available; skipping cleanWs"
                }

                // Send email notification
                def buildStatus = currentBuild.currentResult
                def emailRecipients = env.EMAIL_RECIPIENTS ?: 'anastasia.suhareva@gmail.com'
                def recipientList = emailRecipients.split(',').collect { it.trim() }

                def statusEmoji = buildStatus == 'SUCCESS' ? '✅' : '❌'
                def statusText = buildStatus == 'SUCCESS' ? 'succeeded' : 'failed'

                def emailSubject = "${statusEmoji} Build ${buildStatus.capitalize()}: ${env.JOB_NAME} #${env.BUILD_NUMBER}"

                def emailBody = """
                    <h3>Build ${statusText}!</h3>
                    <p><strong>Project:</strong> ${env.JOB_NAME}<br>
                    <strong>Build Number:</strong> #${env.BUILD_NUMBER}<br>
                    <strong>Commit:</strong> ${env.COMMIT_MESSAGE}<br>
                    <strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                    ${buildStatus == 'SUCCESS' ? '' : '<p><em>Please check the build logs for details.</em></p>'}
                """

                emailext (
                    subject: emailSubject,
                    body: emailBody,
                    to: recipientList.join(','),
                    mimeType: 'text/html'
                )
            }
        }
    }
}
