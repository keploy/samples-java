pipeline {
    agent {
            image 'ubuntu:22.04'
    }
    stages {
        stage('Keploy Tests') {
            steps {
                sh '''
                apt-get update && apt-get install -y curl kmod linux-headers-generic bpfcc-tools git openjdk-17-jdk

                # Clone the repo
                git clone -b chore/Integrate-github-cicd https://github.com/Achanandhi-M/samples-java.git
                cd samples-java/spring-petclinic/spring-petclinic-rest

                mkdir -p /sys/kernel/debug
                mkdir -p /sys/kernel/tracing

                # Download and install Keploy
                curl --silent -O -L https://keploy.io/install.sh && bash install.sh

                mount -t debugfs nodev /sys/kernel/debug || true
                mount -t tracefs nodev /sys/kernel/tracing || true

                # Build the app (assuming Maven)
                ./mvnw package -DskipTests

                # Run Keploy test
                keploy test -c "java -jar target/spring-petclinic-rest-3.0.2.jar" --delay 20 --language java
                '''
            }
        }
    }
}
