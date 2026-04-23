FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY lowcode/workflow-scheduler/pom.xml ./pom.xml
COPY lowcode/workflow-scheduler/src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/target/workflow-scheduler-*.jar /app/workflow-scheduler.jar

EXPOSE 9016

ENTRYPOINT ["java", "-jar", "/app/workflow-scheduler.jar"]
