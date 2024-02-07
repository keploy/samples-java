pipeline {
    agent any

    tools {
        // Install the Maven version configured as "M3" and add it to the path.
        maven "M3"
    }

    stages {
        stage('Build') {
            steps {
                // Get code from a specific branch of a GitHub repository
                git branch: 'petclinic-junit', url: 'https://github.com/keploy/samples-java.git'
                
                // Download and prepare Keploy binary
                sh "curl --silent --location 'https://github.com/keploy/keploy/releases/latest/download/keploy_linux_arm64.tar.gz' | tar xz -C /tmp"
                sh "sudo mkdir -p /usr/local/bin && sudo mv /tmp/keploy /usr/local/bin/keploybin"

                // Change to the desired directory and execute Maven commands
                dir('spring-petclinic/spring-petclinic-rest') {
                    sh """
                    mvn clean install -Dmaven.test.skip=true
                    sudo -E env PATH="$PATH" keploybin test -c "mvn test" --delay 30 --coverage
                    """
                }
                
                // Note: For Windows agents, replace sh with bat commands as needed.
            }

            post {
                // If Maven was able to run the tests, even if some of the test failed, record the test results and archive the jar file.
                success {
                    junit '**spring-petclinic/spring-petclinic-rest/target/surefire-reports/TEST-*.xml'
                    archiveArtifacts 'spring-petclinic/spring-petclinic-rest/target/*.jar'
                    jacoco execPattern: '**/spring-petclinic/spring-petclinic-rest/target/jacoco.exec',
                          classPattern: '**/spring-petclinic/spring-petclinic-rest/target/classes',
                          sourcePattern: '**spring-petclinic/spring-petclinic-rest//src/main/java'
                }
            }
        }
    }
}
