pipeline {
    agent any
    stages {
        stage('Keploy Tests') {
            steps {
                // Clone the git repository
                git branch: 'chore/Integrate-github-cicd', url: 'https://github.com/Achanandhi-M/samples-java.git'
                // switch to the directory and run test
                dir('spring-petclinic/spring-petclinic-rest') {
                    sh """
                    # Download and install Keploy binary
                    curl --silent -O -L https://keploy.io/install.sh && bash install.sh

                    which keploy

                    # keploy test -c "docker compose up" --container-name "ginMongoApp" --delay 15
                    #sudo keploy test -c \"docker-compose up\" --container-name \"javaApp\" -t \"test-set-0" --build-delay 50 --delay 20 --debug
                    docker-compose up
                    """
                }
            }
        }
    }
}