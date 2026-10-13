// CI/CD pipeline: build + test -> Docker images -> push to registry -> deploy to ROSA (dev) -> smoke test
// -> manual approval -> deploy to ROSA (prod).
//
// Jenkins prerequisites (see docs/REPORT.md, section 6):
//   Agent with JDK 21, Maven 3.9, Docker CLI + daemon, oc CLI and kustomize.
//   Credentials:
//     registry-credentials  (Username/Password)  e.g. Quay.io robot account or ECR
//     rosa-dev-token        (Secret text)        token of the jenkins-deployer service account in payments-dev
//     rosa-prod-token       (Secret text)        token of the jenkins-deployer service account in payments-prod
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 60, unit: 'MINUTES')
    }

    parameters {
        string(name: 'REGISTRY', defaultValue: 'quay.io/andrewsselvaraj', description: 'Image registry/namespace')
        string(name: 'ROSA_API_URL', defaultValue: 'https://api.<cluster>.<id>.p1.openshiftapps.com:6443', description: 'ROSA cluster API URL')
        booleanParam(name: 'DEPLOY_PROD', defaultValue: false, description: 'Promote to payments-prod after dev smoke test')
    }

    environment {
        SERVICES = 'api-gateway order-service payment-service notification-service'
        IMAGE_TAG = "${env.GIT_COMMIT ? env.GIT_COMMIT.take(7) : 'local'}-${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                sh 'mvn -B clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    env.SERVICES.split().each { svc ->
                        sh "docker build --build-arg SERVICE=${svc} -t ${params.REGISTRY}/${svc}:${env.IMAGE_TAG} ."
                    }
                }
            }
        }

        stage('Image Scan') {
            steps {
                script {
                    // Fails the build on CRITICAL vulnerabilities when Trivy is installed on the agent.
                    env.SERVICES.split().each { svc ->
                        sh """
                          if command -v trivy >/dev/null 2>&1; then
                            trivy image --exit-code 1 --severity CRITICAL --ignore-unfixed ${params.REGISTRY}/${svc}:${env.IMAGE_TAG}
                          else
                            echo 'trivy not installed, skipping image scan'
                          fi
                        """
                    }
                }
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'registry-credentials',
                        usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
                    script {
                        def registryHost = params.REGISTRY.tokenize('/')[0]
                        sh 'echo "$REG_PASS" | docker login -u "$REG_USER" --password-stdin ' + registryHost
                        env.SERVICES.split().each { svc ->
                            sh "docker push ${params.REGISTRY}/${svc}:${env.IMAGE_TAG}"
                        }
                    }
                }
            }
        }

        stage('Deploy to ROSA (dev)') {
            steps {
                deployTo('payments-dev', 'dev', 'rosa-dev-token')
            }
        }

        stage('Smoke Test (dev)') {
            steps {
                withCredentials([string(credentialsId: 'rosa-dev-token', variable: 'OC_TOKEN')]) {
                    sh '''
                      oc login --token="$OC_TOKEN" --server="$ROSA_API_URL" >/dev/null
                      HOST=$(oc get route api-gateway -n payments-dev -o jsonpath='{.spec.host}')
                      ./scripts/smoke-test.sh "https://$HOST"
                    '''
                }
            }
        }

        stage('Approve Prod') {
            when { expression { params.DEPLOY_PROD } }
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input message: "Deploy ${env.IMAGE_TAG} to payments-prod?", ok: 'Deploy'
                }
            }
        }

        stage('Deploy to ROSA (prod)') {
            when { expression { params.DEPLOY_PROD } }
            steps {
                deployTo('payments-prod', 'prod', 'rosa-prod-token')
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
        failure {
            echo "Pipeline failed for ${env.IMAGE_TAG}"
        }
    }
}

// Pins the new image tag in the overlay, applies it, and waits for every rollout.
// A failed rollout is rolled back automatically.
def deployTo(String namespace, String overlay, String tokenId) {
    withCredentials([string(credentialsId: tokenId, variable: 'OC_TOKEN')]) {
        sh """
          oc login --token="\$OC_TOKEN" --server="${params.ROSA_API_URL}" >/dev/null
          cd k8s/overlays/${overlay}
          for svc in ${env.SERVICES}; do
            kustomize edit set image \$svc=${params.REGISTRY}/\$svc:${env.IMAGE_TAG}
          done
          cd -
          oc apply -k k8s/overlays/${overlay}
          for svc in ${env.SERVICES}; do
            if ! oc rollout status deployment/\$svc -n ${namespace} --timeout=300s; then
              oc rollout undo deployment/\$svc -n ${namespace}
              exit 1
            fi
          done
        """
    }
}
