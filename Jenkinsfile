pipeline {
    agent any
    stages {
        stage('Keploy Tests') {
            steps {
                git branch: 'chore/Integrate-github-cicd', url: 'https://github.com/Achanandhi-M/samples-java.git'
                dir('spring-petclinic/spring-petclinic-rest') {
                    sh '''
                    # Download and install Keploy binary
                    curl --silent -O -L https://keploy.io/install.sh && bash install.sh

                    which keploy

                    # Set env variables to fix docker run volume issues
                    export KEPLOY_CONFIG_DIR="$(pwd)/.keploy-config"
                    export KEPLOY_TEST_DIR="$(pwd)/.keploy"

                    mkdir -p "$KEPLOY_CONFIG_DIR" "$KEPLOY_TEST_DIR"

                    # Run Keploy test with proper volume mounts
                    sudo keploy test -c "docker-compose up" \
                        --container-name "javaApp" \
                        -t "test-set-0" \
                        --build-delay 50 \
                        --delay 20 \
                        --debug
                    '''
                }
            }
        }
    }
}
