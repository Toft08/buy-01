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
                              -v ${WORKSPACE}/frontend:/workspace \
                              --tmpfs /tmp:rw,exec,nosuid,size=2g \
                              --cap-add=SYS_ADMIN \
                              zenika/alpine-chrome:with-node \
                              sh -c "
                                cd /workspace && \
                                npm install --legacy-peer-deps --cache /tmp/.npm && \
                                CHROME_BIN=/usr/bin/chromium-browser npm run test -- --watch=false --browsers=ChromeHeadless --code-coverage
                              " || {
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
                sh '''
                    # Stop old containers
                    docker-compose -f docker-compose.yml -f docker-compose.ci.yml down || true

                    # Start new containers
                    docker-compose -f docker-compose.yml -f docker-compose.ci.yml up -d

                    # Wait for services to be healthy
                    echo "Waiting for services to start..."
                    sleep 30

                    # Verify services are running
                    docker-compose -f docker-compose.yml -f docker-compose.ci.yml ps
                '''
            }
            post {
                failure {
                    script {
                        echo "Deployment failed - Rolling back"
                        sh '''
                            docker-compose -f docker-compose.yml -f docker-compose.ci.yml down || true
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
