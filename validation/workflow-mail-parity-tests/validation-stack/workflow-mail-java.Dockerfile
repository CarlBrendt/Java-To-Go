FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY lowcode/workflow-mail/pom.xml ./pom.xml
RUN mvn -DskipTests dependency:go-offline

COPY lowcode/workflow-mail/src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/target/workflow-mail-*.jar /app/workflow-mail.jar

EXPOSE 9018

ENTRYPOINT ["java", "-jar", "/app/workflow-mail.jar"]
