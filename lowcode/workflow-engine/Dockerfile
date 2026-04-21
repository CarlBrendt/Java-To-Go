FROM sregistry.mts.ru/ip/devops/docker/java:17.0.10_7-jre-jammy-WinCAG-nonroot

ARG JAR_FILE=target/*.jar
COPY --chown=appuser:appuser ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

# ENV JAR_NAME workflow-engine.jar
# ENV WD /app/
# RUN mkdir -p "$WD"
# WORKDIR $WD

# COPY . $WD

# EXPOSE 8080 3500

# WORKDIR $WD/target

# ENTRYPOINT java  -Duser.timezone=GMT -jar $JAR_NAME
