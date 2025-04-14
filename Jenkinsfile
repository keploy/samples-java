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
                    apt-get update && apt-get install -y kmod linux-headers-generic bpfcc-tools

                    mkdir -p /sys/kernel/debug
                    mkdir -p /sys/kernel/tracing
                    # Download and install Keploy binary
                    curl --silent -O -L https://keploy.io/install.sh && sudo bash install.sh

                    # Verify keploy installation
                    which keploy

                    # Set up the environment (ensure docker is running)
                    
                    # Print current directory for debugging
                    pwd
                    ls -la
                    mount -t debugfs nodev /sys/kernel/debug || true
                    mount -t tracefs nodev /sys/kernel/tracing || true
                    
                    # Run keploy test
                    keploy test -c "java -jar target/spring-petclinic-rest-3.0.2.jar" --delay 20
                    """
                }
            }
        }
    }
}