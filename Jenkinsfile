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
                    
                    # Print current directory for debugging
                    pwd
                    ls -la
                    
                    # Run keploy test
                    keploy test -c "java -jar target/spring-petclinic-rest-3.0.2.jar" --delay 20
                    """
                }
            }
        }
    }
}