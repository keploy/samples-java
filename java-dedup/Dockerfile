ARG JAVA_VERSION=8
FROM eclipse-temurin:${JAVA_VERSION}-jre

WORKDIR /app
COPY target/java-dedup-0.0.1-SNAPSHOT.jar /app/app.jar
COPY target/jacocoagent.jar /app/jacocoagent.jar
COPY target/classes /app/target/classes

ENV KEPLOY_JAVA_CLASS_DIRS=/app/target/classes
EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:/app/jacocoagent.jar=address=127.0.0.1,port=36320,destfile=/tmp/jacoco-keploy.exec,output=tcpserver", "-jar", "/app/app.jar"]
