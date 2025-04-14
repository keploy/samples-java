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
                    curl --silent -O -L https://keploy.io/install.sh && sudo bash install.sh

                    # Verify keploy installation
                    which keploy

                    # Set up the environment (ensure docker is running)
                    sudo systemctl start docker || true

                    # Run keploy test
                    # Using single quotes for the command to avoid escaping issues
                    sudo keploy test -c 'docker-compose up' --container-name 'javaApp' -t 'test-set-0' --build-delay 50 --delay 20 --debug
                    """
                }
            }
        }
    }
}