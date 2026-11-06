FROM bellsoft/liberica-openjdk-alpine:21

ARG COMMIT_HASH
ARG VERSION
ARG BUILD_DATE

ARG UID=1000
ARG GID=1000
ARG USER=erpione
ARG GROUP=erpione

ENV VERSION=$VERSION
ENV COMMIT_HASH=$COMMIT_HASH
ENV BUILD_DATE=$BUILD_DATE

RUN apk update  \
    && apk upgrade  \
    && addgroup -g "$GID" "$GROUP" \
    && adduser -D -h "/home/$USER" -G "$GROUP" -u "$UID" "$USER"

USER $USER
WORKDIR "/home/$USER"

COPY target/erpione-server-*.jar app.jar
COPY --chmod=555 entrypoint.sh entrypoint.sh

RUN mkdir -p data && chmod 700 data


LABEL de.gematik.vendor="gematik GmbH"
LABEL maintainer="alexander.kaiser@gematik.de"
LABEL de.gematik.app="Erpione"
LABEL de.gematik.git-repo-name="https://gitlab.prod.ccs.gematik.solutions/git/erezept/erpione/erpione-application"
LABEL de.gematik.commit-sha=$COMMIT_HASH
LABEL de.gematik.version=$VERSION

ENTRYPOINT ["./entrypoint.sh"]