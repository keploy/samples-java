# syntax=docker/dockerfile:1.7
#
# Uses amazoncorretto:21 — Amazon Linux 2023-based, minimal, JDK-ready.
# Matches the Maven toolchain (pom.xml targets Java 21). The jar is built
# outside Docker (mvn package); this image just packages it and runs it.
# K8s securityContext enforces the non-root runtime UID (1001).
#
# Docker Hub pull rate limits are avoided by relying on the locally cached
# amazoncorretto:21 image.

FROM amazoncorretto:21

WORKDIR /app

# Make /app world-readable so the K8s-enforced UID 1001 can read the jar.
COPY target/customer360.jar /app/customer360.jar
RUN chmod 755 /app && chmod 644 /app/customer360.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/customer360.jar"]
