FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl gnupg && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc && \
    chmod a+r /etc/apt/keyrings/docker.asc && \
    . /etc/os-release && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" > /etc/apt/sources.list.d/docker.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/validation-orchestrator-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8095

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
