FROM sregistry.mts.ru/ip/devops/docker/java:17.0.10_7-jre-jammy-WinCAG-nonroot

ARG JAR_FILE=target/*.jar
COPY --chown=appuser:appuser ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]