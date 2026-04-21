FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY lowcode/workflow-engine/pom.xml ./pom.xml
COPY lowcode/workflow-engine/src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/target/workflow-engine.jar /app/workflow-engine.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app/workflow-engine.jar"]
