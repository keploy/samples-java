ARG JAVA_VERSION=8
FROM eclipse-temurin:${JAVA_VERSION}-jre

WORKDIR /app

RUN groupadd --gid 10001 appuser \
    && useradd --uid 10001 --gid 10001 --home-dir /home/appuser --create-home --shell /usr/sbin/nologin appuser

COPY --chown=10001:10001 target/java-dedup-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --chown=10001:10001 target/jacocoagent.jar /app/jacocoagent.jar
COPY --chown=10001:10001 target/classes /app/target/classes

ENV KEPLOY_JAVA_CLASS_DIRS=/app/target/classes
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-javaagent:/app/jacocoagent.jar", "-jar", "/app/app.jar"]
