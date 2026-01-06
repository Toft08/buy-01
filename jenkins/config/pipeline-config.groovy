// Pipeline Configuration
// Centralized configuration for Jenkins CI/CD pipeline

def getConfig() {
    return [
        // Notification Configuration
        notifications: [
            // Email Configuration
            email: [
                enabled: env.EMAIL_ENABLED ? env.EMAIL_ENABLED.toBoolean() : true,
                recipients: env.EMAIL_RECIPIENTS ?: 'anastasia.suhareva@gmail.com',
                subject: 'Jenkins Build: ${PROJECT_NAME} - ${BUILD_STATUS}',
                notifyOnSuccess: false,
                notifyOnFailure: true,
                notifyOnUnstable: true
            ]
        ],

        // Docker Configuration
        docker: [
            registry: env.DOCKER_REGISTRY ?: '',
            registryCredentials: env.DOCKER_REGISTRY_CREDENTIALS ?: '',
            imageTag: env.BUILD_NUMBER ?: 'latest',
            buildArgs: [
                'API_URL': 'https://api-gateway:8080'
            ]
        ],

        // Test Configuration
        tests: [
            backend: [
                enabled: true,
                failOnFailure: true,
                coverageThreshold: 0.0  // Minimum coverage percentage (0 = no threshold)
            ],
            frontend: [
                enabled: true,
                failOnFailure: true,
                coverageThreshold: 0.0
            ],
            integration: [
                enabled: true,
                failOnFailure: true,
                timeout: 300  // seconds
            ]
        ],

        // Deployment Configuration
        deployment: [
            enabled: true,
            environment: env.DEPLOY_ENV ?: 'ci',
            healthCheckTimeout: 120,  // seconds
            healthCheckInterval: 5,   // seconds
            rollbackOnFailure: true,
            deployOnlyOnMainBranch: true,
            mainBranches: ['main', 'master']
        ],

        // Build Configuration
        build: [
            javaVersion: '17',
            nodeVersion: '18',
            mavenOpts: '-Xmx2048m -XX:MaxPermSize=512m',
            npmRegistry: env.NPM_REGISTRY ?: 'https://registry.npmjs.org/'
        ],

        // Artifact Configuration
        artifacts: [
            archiveBackend: true,
            archiveFrontend: true,
            keepDays: 30,
            maxBuilds: 50
        ],

        // Cleanup Configuration
        cleanup: [
            removeOldContainers: true,
            removeOldImages: true,
            keepLastNImages: 5,
            removeUntaggedImages: true
        ]
    ]
}

// Helper function to check if deployment should run
def shouldDeploy(branch) {
    def config = getConfig()
    if (!config.deployment.enabled) {
        return false
    }
    if (config.deployment.deployOnlyOnMainBranch) {
        return config.deployment.mainBranches.contains(branch)
    }
    return true
}

// Helper function to get notification message
def getNotificationMessage(buildStatus, buildUrl, buildNumber, commitMessage) {
    def statusEmoji = buildStatus == 'SUCCESS' ? '✅' : '❌'
    def statusColor = buildStatus == 'SUCCESS' ? 'good' : 'danger'

    return """
        ${statusEmoji} *Build ${buildStatus}*

        *Project:* ${env.JOB_NAME}
        *Build Number:* #${buildNumber}
        *Branch:* ${env.GIT_BRANCH ?: 'unknown'}
        *Commit:* ${commitMessage ?: 'N/A'}
        *Build URL:* ${buildUrl}
        *Duration:* ${currentBuild.durationString ?: 'N/A'}
    """.stripIndent()
}

return this


