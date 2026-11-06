FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG COMMIT_HASH
ARG VERSION
ARG BUILD_DATE

ENV VERSION=$VERSION
ENV COMMIT_HASH=$COMMIT_HASH
ENV BUILD_DATE=$BUILD_DATE

# Non-root user
RUN addgroup -S bff && adduser -S bff -G bff
USER bff

COPY target/erpione-oauth2-bff-*.jar app.jar

EXPOSE 8080

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

LABEL de.gematik.vendor="gematik GmbH"
LABEL maintainer="alexander.kaiser@gematik.de"
LABEL de.gematik.app="Erpione BFF"
LABEL de.gematik.git-repo-name="https://gitlab.prod.ccs.gematik.solutions/git/erezept/erpione/erpione-application"
LABEL de.gematik.commit-sha=$COMMIT_HASH
LABEL de.gematik.version=$VERSION

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS $JAVA_TOOL_OPTIONS -jar app.jar"]

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
